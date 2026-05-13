package io.embrace.shoppingcart.telemetry

import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.ErrorCode

/**
 * No-op [TelemetryService] for tests and for builds where Embrace is disabled.
 *
 * Methods are intentionally empty. `recordSpan` still runs the block so the
 * production code path is preserved.
 */
object NoOpTelemetryService : TelemetryService {

    override fun logInfo(message: String, properties: Map<String, Any>) {}
    override fun logWarning(message: String, properties: Map<String, Any>) {}
    override fun logError(message: String, properties: Map<String, Any>) {}
    override fun logException(
        throwable: Throwable,
        properties: Map<String, Any>,
        message: String?,
    ) {}

    override fun startSpan(name: String): EmbraceSpan? = null

    override fun recordCompletedSpan(
        name: String,
        startTimeMs: Long,
        endTimeMs: Long,
        attributes: Map<String, String>,
        errorCode: ErrorCode?,
    ) {}

    override fun <T> recordSpan(
        name: String,
        attributes: Map<String, String>,
        block: () -> T,
    ): T = block()

    override fun addBreadcrumb(message: String) {}

    override fun addSessionProperty(key: String, value: String, permanent: Boolean) {}
    override fun removeSessionProperty(key: String) {}

    override fun setUser(id: String?, email: String?, name: String?) {}
    override fun clearUser() {}

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
    ) {}

    override fun recordPushNotification(
        title: String?,
        body: String?,
        from: String?,
        messageId: String?,
        data: Map<String, String>,
    ) {}

    override fun trackUserAction(
        action: String,
        screen: String,
        properties: Map<String, Any>,
    ) {}

    override fun trackScreenView(screenName: String, properties: Map<String, Any>) {}

    override fun trackProductView(
        productId: String,
        productName: String,
        category: String?,
        priceCents: Long?,
    ) {}

    override fun trackAddToCart(productId: String, quantity: Int, priceCents: Long) {}

    override fun trackPurchaseAttempt(orderId: String, totalCents: Long, itemCount: Int) {}
    override fun trackPurchaseSuccess(orderId: String, totalCents: Long, paymentMethod: String) {}
    override fun trackPurchaseFailure(
        orderId: String,
        errorMessage: String,
        failureReason: String,
    ) {}

    override fun trackLoginAttempt(method: String) {}
    override fun trackLoginSuccess(userId: String, method: String) {}
    override fun trackLoginFailure(method: String, errorMessage: String) {}

    override fun trackSearchPerformed(
        query: String,
        resultCount: Int,
        filters: Map<String, String>,
    ) {}

    override fun forceEmbraceCrash() {}
}
