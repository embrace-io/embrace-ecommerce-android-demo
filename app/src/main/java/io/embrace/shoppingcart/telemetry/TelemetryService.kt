package io.embrace.shoppingcart.telemetry

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
//      - The wrapper does NOT own SDK lifecycle. `Embrace.start(context)` is
//        called once in ShoppingCartApp.onCreate (see also `applicationInitStart`
//        / `applicationInitEnd` for accurate startup tracing).
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
//      - On user opt-out: call clearUser() AND Embrace.disable() (the wrapper
//        does not own that decision — call it from your consent layer).
//
//   6. OPERATIONAL CONTROLS (not implemented here, but wire-points exist)
//      - Remote config / kill switches: gate Embrace.start() and the wrapper's
//        public methods on a feature flag your app already has.
//      - Partial enablement: gate spans/logs/network independently if needed.
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
