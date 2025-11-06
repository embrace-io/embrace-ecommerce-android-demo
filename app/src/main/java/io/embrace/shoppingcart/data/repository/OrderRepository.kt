package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse

interface OrderRepository {
    suspend fun placeOrder(request: OrderRequest): OrderResponse
}

