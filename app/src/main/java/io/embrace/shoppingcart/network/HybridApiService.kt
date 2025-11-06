package io.embrace.shoppingcart.network

import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse

/**
 * Uses mock responses for read-only data while delegating order placement to the real backend.
 * This keeps mock data for products/categories available in production builds.
 */
class HybridApiService(
    private val realApi: ApiService,
    private val mockApi: ApiService
) : ApiService {
    override suspend fun getProducts(): List<ProductDto> = mockApi.getProducts()

    override suspend fun getCategories(): List<CategoryDto> = mockApi.getCategories()

    override suspend fun placeOrder(request: OrderRequest): OrderResponse =
        realApi.placeOrder(request)
}
