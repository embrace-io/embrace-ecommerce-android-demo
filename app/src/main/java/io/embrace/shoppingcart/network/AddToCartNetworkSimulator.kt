package io.embrace.shoppingcart.network

import io.embrace.shoppingcart.telemetry.TelemetryService
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simulates an "add to cart" network request (no real HTTP I/O).
 * Routes through the telemetry wrapper with randomized 1–2s duration.
 */
class AddToCartNetworkSimulator @Inject constructor(
    private val telemetry: TelemetryService,
) {

    suspend fun simulate(productId: String, quantity: Int) = withContext(Dispatchers.IO) {
        val url = "https://example.com/cart/add?productId=${productId}&qty=${quantity}"
        val start = System.currentTimeMillis()
        val delayMs = Random.nextLong(1_000L, 2_001L)
        delay(delayMs)
        val end = start + delayMs

        telemetry.recordNetworkRequest(
            url = url,
            method = "POST",
            startTimeMs = start,
            endTimeMs = end,
            statusCode = 200,
        )
    }
}

