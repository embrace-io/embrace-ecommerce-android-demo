package io.embrace.shoppingcart.telemetry

import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.Severity
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.ErrorCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [TelemetryService] backed by the Embrace Android SDK.
 *
 * Inject via Hilt (preferred) or use [instance] from code outside DI scope.
 * All public methods are safe to call before `Embrace.start()` — the
 * underlying SDK no-ops or buffers as appropriate.
 */
@Singleton
class EmbraceTelemetryService @Inject constructor() : TelemetryService {

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
        Embrace.logMessage(message, Severity.INFO, properties)
    }

    override fun logWarning(message: String, properties: Map<String, Any>) {
        Embrace.logMessage(message, Severity.WARNING, properties)
    }

    override fun logError(message: String, properties: Map<String, Any>) {
        Embrace.logMessage(message, Severity.ERROR, properties)
    }

    override fun logException(
        throwable: Throwable,
        properties: Map<String, Any>,
        message: String?,
    ) {
        Embrace.logException(throwable, Severity.ERROR, properties, message)
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
        return Embrace.startSpan(name).takeIf { it.isRecording }
    }

    override fun recordCompletedSpan(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        attributes: Map<String, String>,
        errorCode: ErrorCode?,
    ) {
        Embrace.recordCompletedSpan(
            name = name,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            errorCode = errorCode,
            attributes = attributes,
        )
    }

    override fun <T> recordSpan(
        name: String,
        attributes: Map<String, String>,
        block: () -> T,
    ): T {
        return Embrace.recordSpan(name = name, attributes = attributes, code = block)
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
        Embrace.addSessionProperty(key, value, permanent)
    }

    override fun removeSessionProperty(key: String) {
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
        /**
         * Singleton accessor for code that runs outside Hilt scope
         * (Application.onCreate, top-level Composables). Prefer constructor
         * injection in feature code.
         */
        val instance: TelemetryService by lazy { EmbraceTelemetryService() }
    }
}
