package io.embrace.shoppingcart.telemetry

import android.content.Context
import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.ErrorCode

// =============================================================================
// REFERENCE WRAPPER — DESIGN NOTES (Android counterpart of EmbraceService.swift)
// =============================================================================
//
// This file is the protocol facade for the Embrace Android SDK wrapper. It
// mirrors EmbraceService.swift in the Embrace Ecommerce iOS app so the two
// platforms share the same telemetry surface (helper names, span names,
// attribute keys) and dashboards built on one work for the other.
//
// 8 design principles, in order of importance:
//
//   1. WHY WRAP AT ALL
//      - Testability: feature code depends on TelemetryService (an interface),
//        not on the Embrace singleton. Tests inject NoOpTelemetryService — no
//        real SDK in unit tests.
//      - No-op fallback: feature code can call the wrapper at any point in the
//        app lifecycle. The wrapper absorbs SDK readiness; callers never
//        guard with `if (Embrace.isStarted)`.
//      - Single point of change: SDK upgrades, vendor swaps, and instrumentation
//        changes (e.g. swapping in OTel-direct calls) land here.
//
//   2. WRAPPER ARCHITECTURE
//      - Interface-fronted (this file) so feature code holds an abstraction.
//      - EmbraceTelemetryService is the production impl. NoOpTelemetryService
//        is the test/disabled impl.
//      - Both a singleton (EmbraceTelemetryService.instance) and a Hilt-bound
//        injection are available. Prefer constructor injection in feature
//        code; the singleton is here for code that runs outside DI scope
//        (Application.onCreate, top-level Composables).
//
//   3. LIFECYCLE & INITIALIZATION
//      - The wrapper OWNS SDK lifecycle: ShoppingCartApp.onCreate calls
//        `initialize(context, config)` once, and the wrapper decides whether
//        to start the SDK (kill switch, consent, sampling) and absorbs start
//        failures — feature code never sees an error.
//      - Consent: `optOut()` persists the preference and calls
//        Embrace.disable(); `optIn()` re-enables (on the next launch if the
//        SDK was already disabled this process — disable() is one-way until
//        relaunch). On launch, initialize() skips the SDK entirely for
//        opted-out users, so no pre-consent data is captured.
//      - Manual session boundaries: `startNewSession()` ends the current
//        session and starts a fresh one. Call it at meaningful UX boundaries
//        (login, the opt-in moment).
//      - All wrapper methods are safe to call before the SDK is started — the
//        underlying SDK no-ops or buffers as appropriate.
//
//   4. SIGNAL AUTHORING — span vs log vs breadcrumb
//      - Span:       bounded work with start/end and a duration that matters
//                    (purchase_attempt, search_performed).
//      - Log:        a discrete event with a severity (info/warn/error) where
//                    the message itself is the payload.
//      - Breadcrumb: a low-cost UX marker for crash/session context. Cheap.
//                    Default to breadcrumb when in doubt.
//      - Domain helpers (trackPurchaseAttempt, trackLoginFailure, ...) compose
//        all three so feature code makes one call and the wrapper emits a
//        consistent shape.
//
//   5. PRIVACY & DATA GOVERNANCE
//      - Use a stable, opaque, non-PII identifier via setUser(id = ...).
//      - email / username are deprecated on the Android SDK and discouraged.
//        The wrapper accepts them for cross-platform parity but most apps
//        should pass null.
//      - On user opt-out: call optOut() — it persists the preference, clears
//        user identity, and calls Embrace.disable().
//      - PII scrubbing: spans forwarded to external OTel exporters pass
//        through PiiScrubbingSpanExporter, which redacts emails / card-like /
//        phone-like values at the exporter layer so call sites can't bypass it.
//
//   6. OPERATIONAL CONTROLS (TelemetryConfig)
//      - Kill switch: TelemetryConfig.enabled = false no-ops the wrapper and
//        never starts the SDK. In a real app, read it from remote config.
//      - Partial enablement: spans/logs/breadcrumbs/network are gated
//        independently (TelemetryConfig.*Enabled).
//      - Sampling: sampleRolloutPercent picks a stable per-install bucket.
//      - Debug vs release: TelemetryConfig.forBuild(isDebug) — debug builds
//        get a larger log budget and skip sampling.
//
//   7. CROSS-PLATFORM PARITY (key divergences from iOS Swift wrapper)
//      - Severity: Android has only INFO / WARNING / ERROR — there is NO DEBUG.
//        `logDebug` is intentionally absent. Use logInfo for diagnostics or
//        gate them behind BuildConfig.DEBUG and skip the call entirely.
//      - Session property lifespan: iOS supports session/process/permanent;
//        Android supports session/permanent only. The wrapper uses `permanent:
//        Boolean` for parity.
//      - Attributes are Map<String, String> only. Flatten typed values at the
//        call site (or inside helpers).
//      - SpanType: the iOS SDK exposes it; Android does not. Not on the API.
//      - Personas: deliberately not exposed in this wrapper.
//      - Push notifications: Embrace.logPushNotification(...) is deprecated in
//        8.x. The wrapper records push as a structured log + breadcrumb.
//      - Crash: there is no Embrace.crash() on Android. Demo crashes throw an
//        unhandled RuntimeException; Embrace's UncaughtExceptionHandler picks
//        it up and associates it with the current session.
//      - recordSpan block: Android's SDK does NOT pass the EmbraceSpan into
//        the block. Use startSpan(...) if you need the span handle inside.
//
//   8. ANTI-PATTERNS THIS WRAPPER PREVENTS
//      - Holding strong references to EmbraceSpan across screens/services and
//        forgetting to stop them. Prefer recordSpan { ... }.
//      - High-cardinality span/log/attribute names (user IDs, full URLs,
//        timestamps in names). Names live in the wrapper, not in features.
//      - Stopping spans you don't own. Span ownership is one-to-one.
//      - Logging in tight per-frame or per-item loops.
//      - Treating failures as success spans with an error log next to them.
//        Use errorCode = ErrorCode.FAILURE so the dashboard sees the failure.
//
// =============================================================================

/**
 * Abstraction over the Embrace Android SDK so feature code never imports
 * `io.embrace.android.embracesdk.Embrace` directly. Swap the implementation
 * for [NoOpTelemetryService] in tests.
 *
 * Inject this via Hilt in production, or grab [EmbraceTelemetryService.instance]
 * in code that runs outside DI scope (Application.onCreate, top-level
 * Composables).
 */
interface TelemetryService {

    // ---- Lifecycle & consent ----------------------------------------------

    /**
     * Initializes telemetry for this process. Call ONCE from
     * `Application.onCreate`. The wrapper decides whether the Embrace SDK
     * actually starts, in this order:
     *
     *   1. [TelemetryConfig.enabled] kill switch — off means nothing starts.
     *   2. Persisted consent — opted-out users never start the SDK, so no
     *      pre-consent data is captured.
     *   3. [TelemetryConfig.sampleRolloutPercent] — installs outside the
     *      sample bucket don't capture.
     *
     * If `Embrace.start` throws, the failure is logged and telemetry is
     * disabled for this launch — the app keeps running, feature code never
     * sees an error.
     */
    fun initialize(context: Context, config: TelemetryConfig = TelemetryConfig())

    /** True when the SDK started and the wrapper is currently capturing. */
    val isCapturing: Boolean

    /**
     * Records user consent and starts capture (immediately if possible).
     * If the SDK was disabled by [optOut] earlier in this same process,
     * capture resumes on the next launch — `Embrace.disable()` is one-way
     * within a process.
     */
    fun optIn()

    /**
     * Revokes consent: persists the preference, clears user identity, and
     * calls `Embrace.disable()` — capture stops immediately and unsent data
     * is deleted. Survives app restarts until [optIn] is called.
     */
    fun optOut()

    /**
     * Ends the current session and starts a fresh one (manual session
     * boundary). Use at meaningful UX boundaries: login, the consent opt-in
     * moment, switching accounts. Pass `clearUserInfo = true` to also wipe
     * user identity (e.g. on logout).
     */
    fun startNewSession(clearUserInfo: Boolean = false)

    // ---- Logs ------------------------------------------------------------

    fun logInfo(message: String, properties: Map<String, Any> = emptyMap())
    fun logWarning(message: String, properties: Map<String, Any> = emptyMap())
    fun logError(message: String, properties: Map<String, Any> = emptyMap())
    fun logException(
        throwable: Throwable,
        properties: Map<String, Any> = emptyMap(),
        message: String? = null,
    )

    // ---- Spans -----------------------------------------------------------

    /** Returns a started [EmbraceSpan]. Caller owns calling `.stop()`. */
    fun startSpan(name: String): EmbraceSpan?

    /**
     * Starts a span and returns a lifecycle-safe [ActiveSpan] handle.
     * Prefer this over [startSpan] when start and end live in different
     * functions — the handle covers the classic failure modes:
     *
     *   - `timeoutMs` — if the span is still open after this long it is
     *     closed as FAILURE, so hangs show up in the dashboard instead of
     *     silently never arriving.
     *   - `endOnBackground` — if the app leaves the foreground the span is
     *     closed as USER_ABANDON.
     *   - `stop()` is idempotent — first close wins (caller, timeout, or
     *     background), later calls no-op.
     *
     * Never returns null: when capture is off you get a no-op handle, so
     * feature code doesn't branch.
     */
    fun startActiveSpan(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        timeoutMs: Long? = null,
        endOnBackground: Boolean = false,
    ): ActiveSpan

    /**
     * Records a span that already happened. Honors real start/end times and
     * optionally marks it as failed so it shows up as an error in the dashboard.
     */
    fun recordCompletedSpan(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        attributes: Map<String, String> = emptyMap(),
        errorCode: ErrorCode? = null,
    )

    /**
     * Block-based span. Preferred for short synchronous work — auto-ends, and
     * the block runs even if the SDK has not started.
     *
     * NOTE: unlike the iOS wrapper, the block does NOT receive an [EmbraceSpan].
     * Use [startSpan] when you need the handle inside the block.
     */
    fun <T> recordSpan(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        block: () -> T,
    ): T

    // ---- Breadcrumbs -----------------------------------------------------

    fun addBreadcrumb(message: String)

    // ---- Session properties ---------------------------------------------

    fun addSessionProperty(key: String, value: String, permanent: Boolean = false)
    fun removeSessionProperty(key: String)

    // ---- User identity ---------------------------------------------------

    /**
     * Sets built-in user fields (id/email/name). Persists across sessions
     * until [clearUser] is called. Pass `null` to leave a field unchanged.
     *
     * Prefer passing only `id` (a stable, opaque, non-PII identifier).
     * `email` and `username` map to deprecated SDK methods and are exposed
     * here for parity with the iOS wrapper only.
     */
    fun setUser(id: String?, email: String? = null, name: String? = null)

    fun clearUser()

    // ---- Manual network capture -----------------------------------------

    /**
     * Use ONLY for transports the SDK cannot auto-capture (gRPC, WebSocket,
     * native HTTP). The Embrace OkHttp/HttpURLConnection capture already
     * records HTTP traffic — calling this for a regular HTTP request will
     * double-record it.
     */
    fun recordNetworkRequest(
        url: String,
        method: String,
        startTimeMs: Long,
        endTimeMs: Long,
        statusCode: Int? = null,
        bytesSent: Long = 0,
        bytesReceived: Long = 0,
        errorMessage: String? = null,
        traceId: String? = null,
        w3cTraceparent: String? = null,
    )

    // ---- Push notifications ---------------------------------------------

    /**
     * Records a push notification arrival. `data` should be the raw
     * `RemoteMessage.getData()` map; copy `notification.title`/`.body` into
     * the dedicated parameters when present.
     */
    fun recordPushNotification(
        title: String? = null,
        body: String? = null,
        from: String? = null,
        messageId: String? = null,
        data: Map<String, String> = emptyMap(),
    )

    // ---- User journey domain helpers ------------------------------------
    //
    // These compose span + breadcrumb + log + session properties into one
    // call. Names live HERE, once. Features can't drift to `view_product`
    // vs `product_view` vs `productView`.

    fun trackUserAction(
        action: String,
        screen: String,
        properties: Map<String, Any> = emptyMap(),
    )

    fun trackScreenView(screenName: String, properties: Map<String, Any> = emptyMap())

    // E-commerce
    fun trackProductView(
        productId: String,
        productName: String,
        category: String? = null,
        priceCents: Long? = null,
    )

    fun trackAddToCart(productId: String, quantity: Int, priceCents: Long)

    fun trackPurchaseAttempt(orderId: String, totalCents: Long, itemCount: Int)
    fun trackPurchaseSuccess(orderId: String, totalCents: Long, paymentMethod: String)
    fun trackPurchaseFailure(orderId: String, errorMessage: String, failureReason: String)

    // Authentication
    fun trackLoginAttempt(method: String)
    fun trackLoginSuccess(userId: String, method: String)
    fun trackLoginFailure(method: String, errorMessage: String)

    // Search
    fun trackSearchPerformed(
        query: String,
        resultCount: Int,
        filters: Map<String, String> = emptyMap(),
    )

    // ---- Demo: force a crash --------------------------------------------
    //
    // DEMO ONLY. Delete in a real app. Each variant produces a distinct
    // stack trace so the demo crashes group separately in the dashboard.

    fun forceEmbraceCrash()
}
