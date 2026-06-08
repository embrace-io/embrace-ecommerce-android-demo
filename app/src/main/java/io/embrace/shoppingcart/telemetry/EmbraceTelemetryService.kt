package io.embrace.shoppingcart.telemetry

import android.content.Context
import android.content.SharedPreferences
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.Severity
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import io.embrace.android.embracesdk.otel.java.addJavaSpanExporter
import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.EmbraceSpanEvent
import io.embrace.android.embracesdk.spans.ErrorCode
import io.embrace.shoppingcart.BuildConfig
import timber.log.Timber
import java.util.UUID

/**
 * Production [TelemetryService] backed by the Embrace Android SDK.
 *
 * A true singleton: Hilt provides [instance] (see TelemetryModule), and code
 * outside DI scope uses [instance] directly — both see the same consent and
 * config state. All public methods are safe to call before `Embrace.start()`.
 */
class EmbraceTelemetryService private constructor() : TelemetryService {

    // -------------------------------------------------------------------------
    // Lifecycle & consent.
    //
    // The wrapper owns the start decision: kill switch → consent → sampling.
    // Any "no" means the SDK never starts this launch, so an opted-out user
    // leaks zero pre-consent data (Wrapper Use Cases §3, §5).
    //
    // `capturing = false` also covers a failed Embrace.start: the app keeps
    // running, signals degrade to no-ops, and the failure is a Timber error —
    // feature code never sees it (§3 "Failure handling").
    // -------------------------------------------------------------------------

    @Volatile private var capturing = false
    private var config: TelemetryConfig = TelemetryConfig()
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var logRateLimiter = LogRateLimiter(config.maxLogsPerMinute)

    override val isCapturing: Boolean
        get() = capturing && Embrace.isStarted

    override fun initialize(context: Context, config: TelemetryConfig) {
        this.appContext = context.applicationContext
        this.config = config
        this.logRateLimiter = LogRateLimiter(config.maxLogsPerMinute)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        this.prefs = prefs

        if (!config.enabled) {
            Timber.i("Telemetry kill switch is off; SDK not started.")
            return
        }
        if (prefs.getBoolean(KEY_OPT_OUT, false)) {
            Timber.i("User has opted out of telemetry; SDK not started.")
            return
        }
        if (!config.isInstallSampled(installId(prefs))) {
            Timber.i("Install not in the %d%% sample; SDK not started.", config.sampleRolloutPercent)
            return
        }
        startSdk(context)
    }

    override fun optIn() {
        prefs?.edit()?.putBoolean(KEY_OPT_OUT, false)?.apply()
        val context = appContext ?: return
        if (!config.enabled) return
        when {
            !Embrace.isStarted -> {
                // Consent-delayed start: the SDK comes up now, mid-process.
                startSdk(context)
            }
            capturing -> {
                // Already capturing; mark the consent moment as a session boundary.
                startNewSession()
            }
            else -> {
                // Embrace.disable() was called earlier in this process; it is
                // one-way until relaunch. Consent is persisted, so capture
                // resumes on the next launch.
                Timber.i("Opt-in recorded; capture resumes on next launch.")
            }
        }
    }

    override fun optOut() {
        prefs?.edit()?.putBoolean(KEY_OPT_OUT, true)?.apply()
        if (Embrace.isStarted) {
            clearUser()
            runCatching { Embrace.disable() }
                .onFailure { Timber.e(it, "Embrace.disable() failed") }
        }
        capturing = false
        Timber.i("User opted out; telemetry capture stopped.")
    }

    override fun startNewSession(clearUserInfo: Boolean) {
        if (!capturing) return
        Embrace.endSession(clearUserInfo)
    }

    private fun startSdk(context: Context) {
        runCatching {
            // External OTel exporters must be registered BEFORE start. Spans
            // forwarded to them pass through the PII scrub first (§5).
            Embrace.addJavaSpanExporter(PiiScrubbingSpanExporter(CustomSpanExporter()))
            Embrace.start(context.applicationContext)
        }.onFailure {
            capturing = false
            Timber.e(it, "Embrace SDK failed to start; telemetry disabled for this launch.")
            return
        }
        capturing = true

        // Shared schema (§4 "Consistent session properties"): every session
        // carries build context, set HERE so it can't drift per call site.
        addSessionProperty("flavor_env", BuildConfig.FLAVOR_env, permanent = true)
        addSessionProperty("flavor_embrace", BuildConfig.FLAVOR_embrace, permanent = true)
        addSessionProperty("build_type", if (BuildConfig.DEBUG) "debug" else "release", permanent = true)
    }

    /**
     * Stable per-install ID for sampling. Deliberately NOT the Embrace device
     * ID: that is only available after the SDK starts, and starting is what
     * sampling decides.
     */
    private fun installId(prefs: SharedPreferences): String =
        prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString()
            .also { prefs.edit().putString(KEY_INSTALL_ID, it).apply() }

    // -------------------------------------------------------------------------
    // Common attributes (§4 "Consistent session properties and span attributes").
    //
    // Every span and log carries the same build context, merged in HERE so
    // dashboards can always slice by version/flavor without feature code
    // remembering to attach them.
    // -------------------------------------------------------------------------

    private val commonAttributes: Map<String, String> by lazy {
        mapOf(
            "app.version" to BuildConfig.VERSION_NAME,
            "app.flavor" to BuildConfig.FLAVOR_env,
        )
    }

    private fun spanAttributes(attributes: Map<String, String>): Map<String, String> =
        TelemetryGuardrails.sanitizeAttributes(commonAttributes + attributes)

    private fun logProperties(properties: Map<String, Any>): Map<String, Any> =
        commonAttributes + properties

    // -------------------------------------------------------------------------
    // Logs
    //
    // SEVERITY GUIDANCE
    //   - INFO:    notable but expected events. Use sparingly — breadcrumbs are
    //              cheaper for UX context.
    //   - WARNING: recoverable problem. Worth knowing about but not an error.
    //   - ERROR:   a real failure that should surface in the dashboard.
    //
    // RULE: log message is a low-cardinality string ("Purchase failed"), not a
    // templated string with IDs ("Purchase failed for order $id"). Put the IDs
    // in `properties` so aggregation works.
    //
    // Android's Severity has no DEBUG. If you need debug-only diagnostics,
    // use Timber (or Log.d) and skip the Embrace call entirely in release.
    //
    // PARITY: keep message strings identical across iOS/Android so cross-
    // platform queries work.
    // -------------------------------------------------------------------------

    override fun logInfo(message: String, properties: Map<String, Any>) {
        log(message, Severity.INFO, properties)
    }

    override fun logWarning(message: String, properties: Map<String, Any>) {
        log(message, Severity.WARNING, properties)
    }

    override fun logError(message: String, properties: Map<String, Any>) {
        log(message, Severity.ERROR, properties)
    }

    override fun logException(
        throwable: Throwable,
        properties: Map<String, Any>,
        message: String?,
    ) {
        if (!capturing || !config.logsEnabled) return
        Embrace.logException(throwable, Severity.ERROR, logProperties(properties), message)
    }

    /**
     * Single funnel for message logs: gating (kill switch + logsEnabled),
     * rate limiting (§8 "logging in tight loops"), name sanity, and common
     * properties all apply here, once.
     */
    private fun log(message: String, severity: Severity, properties: Map<String, Any>) {
        if (!capturing || !config.logsEnabled) return
        val allowed = logRateLimiter.tryAcquire(System.currentTimeMillis()) { dropped ->
            // One summary per window, so the flood is visible without BEING the flood.
            Embrace.logMessage(
                "Telemetry logs rate-limited",
                Severity.WARNING,
                logProperties(mapOf("dropped_count" to dropped, "max_per_minute" to config.maxLogsPerMinute)),
            )
        }
        if (!allowed) {
            Timber.w("Log dropped by telemetry rate limiter: %s", message)
            return
        }
        Embrace.logMessage(message, severity, logProperties(properties))
    }

    // -------------------------------------------------------------------------
    // Spans — three patterns, in order of preference:
    //
    //   1. recordSpan(name, attributes) { ... }   ← PREFERRED
    //      Block-based. Auto-ends. Block still runs if the SDK isn't started.
    //
    //   2. recordCompletedSpan(...)
    //      Use only when the work already happened in the past (replaying a
    //      webhook, recording a failure that wasn't wrapped in a span). Honors
    //      real start/end times and accepts an ErrorCode so failures show as
    //      errors in the dashboard.
    //
    //   3. startSpan(name) -> EmbraceSpan?
    //      Manual start/stop. Only when start and end live in different
    //      functions and there's no way to bridge them with a block. Caller
    //      owns calling `.stop()`. ANTI-PATTERN: storing this on a ViewModel
    //      and stopping it in `onCleared()` — the ViewModel may outlive the
    //      meaningful work.
    //
    // NAMING CONVENTION (enforced here, not at call sites):
    //   - lowercase snake_case: `purchase_attempt`, not `purchaseAttempt`.
    //   - Low cardinality: `product_view`, NEVER `product_view_<productId>`.
    //   - Attribute keys are dotted: `order.id`, `cart.quantity`,
    //     `http.status_code` (OTel-style).
    //
    // FAILURES MUST BE MARKED:
    //   recordCompletedSpan(..., errorCode = ErrorCode.FAILURE) makes the span
    //   show up as errored in the dashboard. A successful span with an error
    //   log next to it is invisible to most charts.
    // -------------------------------------------------------------------------

    override fun startSpan(name: String): EmbraceSpan? {
        if (!capturing || !config.spansEnabled) return null
        return Embrace.startSpan(TelemetryGuardrails.sanitizeName(name)).takeIf { it.isRecording }
    }

    override fun startActiveSpan(
        name: String,
        attributes: Map<String, String>,
        timeoutMs: Long?,
        endOnBackground: Boolean,
    ): ActiveSpan {
        val span = startSpan(name) ?: return NoOpActiveSpan
        spanAttributes(attributes).forEach { (k, v) -> span.addAttribute(k, v) }
        return EmbraceActiveSpan(span, timeoutMs, endOnBackground)
    }

    override fun recordCompletedSpan(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        attributes: Map<String, String>,
        errorCode: ErrorCode?,
        events: List<EmbraceSpanEvent>,
    ) {
        if (!capturing || !config.spansEnabled) return
        Embrace.recordCompletedSpan(
            name = TelemetryGuardrails.sanitizeName(name),
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            errorCode = errorCode,
            attributes = spanAttributes(attributes),
            events = events,
        )
    }

    override fun <T> recordSpan(
        name: String,
        attributes: Map<String, String>,
        block: () -> T,
    ): T {
        // The block ALWAYS runs — telemetry being off must never change
        // product behavior.
        if (!capturing || !config.spansEnabled) return block()
        return Embrace.recordSpan(
            name = TelemetryGuardrails.sanitizeName(name),
            attributes = spanAttributes(attributes),
            code = block,
        )
    }

    // -------------------------------------------------------------------------
    // Breadcrumbs are the DEFAULT for UX context. Cheap, attach to the
    // session/crash for debugging, no schema required.
    //
    // DECISION TREE:
    //   - "User did X on screen Y"          → breadcrumb
    //   - "X happened with severity Z"      → log
    //   - "X took N ms and may have failed" → span
    // -------------------------------------------------------------------------

    override fun addBreadcrumb(message: String) {
        if (!capturing || !config.breadcrumbsEnabled) return
        Embrace.addBreadcrumb(message)
    }

    // -------------------------------------------------------------------------
    // Session properties.
    //
    // Lifespan options:
    //   permanent = false  → cleared at session end (default)
    //   permanent = true   → persists across sessions until removed
    //
    // SDK LIMIT: ~100 session properties per session. The wrapper is the right
    // place to enforce a domain whitelist if your app risks exceeding this.
    //
    // ANTI-PATTERN: setting a session property per item in a list, or per
    // network response — blows the cap fast.
    // -------------------------------------------------------------------------

    override fun addSessionProperty(key: String, value: String, permanent: Boolean) {
        if (!capturing) return
        Embrace.addSessionProperty(key, value, permanent)
    }

    override fun removeSessionProperty(key: String) {
        if (!capturing) return
        Embrace.removeSessionProperty(key)
    }

    // -------------------------------------------------------------------------
    // User identity.
    //
    // PRIVACY GUIDANCE (the most-asked privacy question):
    //   - Prefer a stable, opaque, non-PII identifier for `id` (your internal
    //     user UUID, not email or username).
    //   - email / name map to deprecated SDK methods and should NOT be passed
    //     unless your privacy review explicitly allows it. They show up in
    //     dashboards and exports.
    //   - On opt-out: clearUser() AND call Embrace.disable() from your
    //     consent layer. Clearing user fields alone does not stop telemetry.
    //   - Set the user identifier as soon as you have it (typically right
    //     after login / consent), not at app launch.
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    override fun setUser(id: String?, email: String?, name: String?) {
        if (id != null) Embrace.setUserIdentifier(id)
        if (email != null) Embrace.setUserEmail(email)
        if (name != null) Embrace.setUsername(name)
    }

    @Suppress("DEPRECATION")
    override fun clearUser() {
        Embrace.clearUserIdentifier()
        Embrace.clearUserEmail()
        Embrace.clearUsername()
    }

    // -------------------------------------------------------------------------
    // Manual network capture.
    //
    // Use ONLY for transports the SDK cannot see (gRPC, WebSocket, native
    // HTTP). The Embrace OkHttp/HttpURLConnection capture already records
    // HTTP traffic — calling this for a regular request will double-record it.
    //
    // The wrapper picks fromCompletedRequest vs fromIncompleteRequest based on
    // whether an errorMessage was provided.
    // -------------------------------------------------------------------------

    override fun recordNetworkRequest(
        url: String,
        method: String,
        startTimeMs: Long,
        endTimeMs: Long,
        statusCode: Int?,
        bytesSent: Long,
        bytesReceived: Long,
        errorMessage: String?,
        traceId: String?,
        w3cTraceparent: String?,
    ) {
        if (!capturing || !config.networkCaptureEnabled) return
        val httpMethod = runCatching { HttpMethod.valueOf(method.uppercase()) }
            .getOrElse { HttpMethod.GET }

        val request = if (errorMessage != null) {
            EmbraceNetworkRequest.fromIncompleteRequest(
                url = url,
                httpMethod = httpMethod,
                startTime = startTimeMs,
                endTime = endTimeMs,
                errorType = errorMessage::class.java.simpleName,
                errorMessage = errorMessage,
                traceId = traceId,
                w3cTraceparent = w3cTraceparent,
            )
        } else {
            EmbraceNetworkRequest.fromCompletedRequest(
                url = url,
                httpMethod = httpMethod,
                startTime = startTimeMs,
                endTime = endTimeMs,
                bytesSent = bytesSent,
                bytesReceived = bytesReceived,
                statusCode = statusCode ?: 0,
                traceId = traceId,
                w3cTraceparent = w3cTraceparent,
            )
        }
        Embrace.recordNetworkRequest(request)
    }

    // -------------------------------------------------------------------------
    // Push notifications.
    //
    // Embrace.logPushNotification(...) is deprecated in 8.x. Record push as a
    // structured log + breadcrumb so the shape is stable and queryable.
    //
    // PARITY: iOS passes the raw userInfo dictionary to the SDK; Android FCM
    // delivers a `RemoteMessage`. Pull title/body/from/messageId out of
    // `RemoteMessage` at the call site and pass them in here.
    // -------------------------------------------------------------------------

    override fun recordPushNotification(
        title: String?,
        body: String?,
        from: String?,
        messageId: String?,
        data: Map<String, String>,
    ) {
        val properties = buildMap<String, Any> {
            title?.let { put("push.title", it) }
            body?.let { put("push.body", it) }
            from?.let { put("push.from", it) }
            messageId?.let { put("push.message_id", it) }
            if (data.isNotEmpty()) put("push.data_keys", data.keys.joinToString(","))
        }
        logInfo("Push notification received", properties)
        addBreadcrumb("Push notification received")
    }

    // -------------------------------------------------------------------------
    // Domain helpers — THE REASON THIS WRAPPER EXISTS.
    //
    // Each helper composes span + breadcrumb + log + session property into a
    // single call site. Feature code calls one method; the wrapper emits a
    // consistent shape across the app.
    //
    // Names, attribute keys, and failure handling live here, ONCE. Feature
    // code can't drift.
    //
    // PARITY: keep helper names AND span/attribute keys IDENTICAL to the iOS
    // wrapper. This is the contract that lets one dashboard query work for
    // both platforms.
    //
    // SCALE: keep helpers small (4–10 lines). When a helper grows, split it
    // into two narrower helpers rather than adding parameters.
    // -------------------------------------------------------------------------

    override fun trackUserAction(
        action: String,
        screen: String,
        properties: Map<String, Any>,
    ) {
        val breadcrumb = "$action on $screen"
        addBreadcrumb(breadcrumb)
        val merged = properties + mapOf("user_action" to action, "screen" to screen)
        logInfo("User action: $breadcrumb", merged)
    }

    override fun trackScreenView(screenName: String, properties: Map<String, Any>) {
        addBreadcrumb("Viewed $screenName")
        logInfo("Screen view: $screenName", properties + mapOf("screen_name" to screenName))
    }

    override fun trackProductView(
        productId: String,
        productName: String,
        category: String?,
        priceCents: Long?,
    ) {
        val attributes = buildMap<String, String> {
            put("product.id", productId)
            put("product.name", productName)
            category?.let { put("product.category", it) }
            priceCents?.let { put("product.price_cents", it.toString()) }
        }
        recordSpan(name = "product_view", attributes = attributes) {
            trackUserAction(
                action = "product_view",
                screen = "product_detail",
                properties = mapOf("product_id" to productId, "product_name" to productName),
            )
        }
    }

    override fun trackAddToCart(productId: String, quantity: Int, priceCents: Long) {
        recordSpan(
            name = "add_to_cart",
            attributes = mapOf(
                "product.id" to productId,
                "cart.quantity" to quantity.toString(),
                "cart.item_value_cents" to priceCents.toString(),
            ),
        ) {
            trackUserAction(
                action = "add_to_cart",
                screen = "product_detail",
                properties = mapOf(
                    "product_id" to productId,
                    "quantity" to quantity,
                    "value_cents" to priceCents,
                ),
            )
        }
    }

    override fun trackPurchaseAttempt(orderId: String, totalCents: Long, itemCount: Int) {
        recordSpan(
            name = "purchase_attempt",
            attributes = mapOf(
                "order.id" to orderId,
                "order.total_cents" to totalCents.toString(),
                "order.item_count" to itemCount.toString(),
            ),
        ) {
            addSessionProperty("current_order_id", orderId, permanent = false)
            trackUserAction(
                action = "purchase_attempt",
                screen = "checkout",
                properties = mapOf(
                    "order_id" to orderId,
                    "total_cents" to totalCents,
                    "item_count" to itemCount,
                ),
            )
        }
    }

    override fun trackPurchaseSuccess(orderId: String, totalCents: Long, paymentMethod: String) {
        recordSpan(
            name = "purchase_success",
            attributes = mapOf(
                "order.id" to orderId,
                "order.total_cents" to totalCents.toString(),
                "payment.method" to paymentMethod,
            ),
        ) {
            removeSessionProperty("current_order_id")
            addSessionProperty("last_successful_order", orderId, permanent = true)
            logInfo(
                "Purchase completed successfully",
                mapOf(
                    "order_id" to orderId,
                    "total_cents" to totalCents,
                    "payment_method" to paymentMethod,
                ),
            )
        }
    }

    override fun trackPurchaseFailure(
        orderId: String,
        errorMessage: String,
        failureReason: String,
    ) {
        val now = Embrace.getSdkCurrentTimeMs()
        recordCompletedSpan(
            name = "purchase_failure",
            startTimeMs = now - 1,
            endTimeMs = now,
            attributes = mapOf(
                "order.id" to orderId,
                "error.message" to errorMessage,
                "failure.reason" to failureReason,
            ),
            errorCode = ErrorCode.FAILURE,
        )
        logError(
            "Purchase failed",
            mapOf(
                "order_id" to orderId,
                "error_message" to errorMessage,
                "failure_reason" to failureReason,
            ),
        )
    }

    override fun trackLoginAttempt(method: String) {
        recordSpan(name = "login_attempt", attributes = mapOf("auth.method" to method)) {
            trackUserAction(
                action = "login_attempt",
                screen = "authentication",
                properties = mapOf("method" to method),
            )
        }
    }

    override fun trackLoginSuccess(userId: String, method: String) {
        setUser(id = userId)
        addSessionProperty("auth_method", method, permanent = false)
        logInfo(
            "Login successful",
            mapOf("user_id" to userId, "auth_method" to method),
        )
    }

    override fun trackLoginFailure(method: String, errorMessage: String) {
        val now = Embrace.getSdkCurrentTimeMs()
        recordCompletedSpan(
            name = "login_failure",
            startTimeMs = now - 1,
            endTimeMs = now,
            attributes = mapOf(
                "auth.method" to method,
                "error.message" to errorMessage,
            ),
            errorCode = ErrorCode.USER_ABANDON,
        )
        logError(
            "Login failed",
            mapOf("auth_method" to method, "error_message" to errorMessage),
        )
    }

    override fun trackSearchPerformed(
        query: String,
        resultCount: Int,
        filters: Map<String, String>,
    ) {
        val attributes = buildMap<String, String> {
            put("search.query", query)
            put("search.result_count", resultCount.toString())
            filters.forEach { (k, v) -> put("search.filter.$k", v) }
        }
        recordSpan(name = "search_performed", attributes = attributes) {
            val properties = buildMap<String, Any> {
                put("query", query)
                put("result_count", resultCount)
                filters.forEach { (k, v) -> put(k, v) }
            }
            trackUserAction(action = "search", screen = "search", properties = properties)
        }
    }

    // -------------------------------------------------------------------------
    // Demo: force a crash.
    //
    // DEMO CODE — DELETE IN A REAL APP.
    //
    // Five distinct top-level functions ensure each crash gets its own stack
    // trace and groups separately in the Embrace dashboard. Each function
    // emits a log + breadcrumb before crashing so the session has context
    // when you open the crash.
    //
    // Android has no Embrace.crash() — these throw an unhandled
    // RuntimeException. Embrace's UncaughtExceptionHandler captures it and
    // associates it with the current session.
    // -------------------------------------------------------------------------

    override fun forceEmbraceCrash() {
        when ((0..4).random()) {
            0 -> simulateCartUpdateCrash()
            1 -> simulatePaymentProcessingCrash()
            2 -> simulateProductRecommendationCrash()
            3 -> simulateSearchFilterCrash()
            else -> simulateAuthTokenRefreshCrash()
        }
    }

    private fun simulateCartUpdateCrash() {
        logError(
            "Cart update failed: quantity sync error",
            mapOf("crash_type" to "cart_update", "trigger" to "manual_crash_button"),
        )
        addBreadcrumb("Crash in cart quantity update flow")
        throw RuntimeException("Cart update failed: quantity sync error")
    }

    private fun simulatePaymentProcessingCrash() {
        logError(
            "Payment processing failed: unexpected null response",
            mapOf("crash_type" to "payment_processing", "trigger" to "manual_crash_button"),
        )
        addBreadcrumb("Crash in payment processing flow")
        throw RuntimeException("Payment processing failed: unexpected null response")
    }

    private fun simulateProductRecommendationCrash() {
        logError(
            "Product recommendations failed: index out of range",
            mapOf("crash_type" to "product_recommendation", "trigger" to "manual_crash_button"),
        )
        addBreadcrumb("Crash in product recommendation engine")
        throw RuntimeException("Product recommendations failed: index out of range")
    }

    private fun simulateSearchFilterCrash() {
        logError(
            "Search filter failed: malformed predicate",
            mapOf("crash_type" to "search_filter", "trigger" to "manual_crash_button"),
        )
        addBreadcrumb("Crash in search filter application")
        throw RuntimeException("Search filter failed: malformed predicate")
    }

    private fun simulateAuthTokenRefreshCrash() {
        logError(
            "Auth token refresh failed: expired session",
            mapOf("crash_type" to "auth_token_refresh", "trigger" to "manual_crash_button"),
        )
        addBreadcrumb("Crash in auth token refresh")
        throw RuntimeException("Auth token refresh failed: expired session")
    }

    companion object {
        private const val PREFS_NAME = "telemetry_prefs"
        private const val KEY_OPT_OUT = "telemetry_opt_out"
        private const val KEY_INSTALL_ID = "telemetry_install_id"

        /**
         * The single instance. Hilt provides this same object (see
         * TelemetryModule), so consent/config state is shared between
         * injected call sites and direct accessors (Application.onCreate,
         * top-level Composables).
         */
        val instance: EmbraceTelemetryService by lazy { EmbraceTelemetryService() }
    }
}
