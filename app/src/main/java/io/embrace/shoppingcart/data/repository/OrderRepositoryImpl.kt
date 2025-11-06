package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.network.ApiService
import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : OrderRepository {
    override suspend fun placeOrder(request: OrderRequest): OrderResponse =
        apiService.placeOrder(request)
}

