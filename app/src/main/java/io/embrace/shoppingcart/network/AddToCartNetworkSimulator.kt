package io.embrace.shoppingcart.network

import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simulates an "add to cart" network request (no real HTTP I/O).
 * Uses Embrace.recordNetworkRequest with randomized 1–2s duration.
 */
class AddToCartNetworkSimulator @Inject constructor() {

    suspend fun simulate(productId: String, quantity: Int) = withContext(Dispatchers.IO) {
        val url = "https://example.com/cart/add?productId=${productId}&qty=${quantity}"
        val start = System.currentTimeMillis()
        val delayMs = Random.nextLong(1_000L, 2_001L)
        delay(delayMs)
        val end = start + delayMs

        val request = EmbraceNetworkRequest.fromCompletedRequest(
            url,
            HttpMethod.fromString("POST"),
            start,
            end,
            0, // bytes sent
            0, // bytes received
            200, // status code
            null, // custom trace id
            null, // traceparent
            null // capture data
        )

        Embrace.getInstance().recordNetworkRequest(request)
    }
}

