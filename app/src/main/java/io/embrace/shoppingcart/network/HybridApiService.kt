package io.embrace.shoppingcart.network

import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uses mock responses for read-only data while delegating order placement to the real backend.
 * This keeps mock data for products/categories available in production builds.
 */
class HybridApiService(
    private val realApi: ApiService,
    private val mockApi: ApiService
) : ApiService {
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)

    override suspend fun getProducts(): List<ProductDto> = mockApi.getProducts()

    override suspend fun getCategories(): List<CategoryDto> = mockApi.getCategories()

    override suspend fun placeOrder(request: OrderRequest): OrderResponse {
        val beforeTime = Embrace.getSdkCurrentTimeMs()
        Timber.d("placeOrder - before request: ${dateFormat.format(Date(beforeTime))}")
        val response = realApi.placeOrder(request)
        val afterTime = Embrace.getSdkCurrentTimeMs()
        Timber.d("placeOrder - after response: ${dateFormat.format(Date(afterTime))}")
        Timber.d("placeOrder - duration: ${afterTime - beforeTime}")
        return response
    }
}
