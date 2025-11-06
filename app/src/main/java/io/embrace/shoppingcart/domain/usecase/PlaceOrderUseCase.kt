package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.data.repository.OrderRepository
import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderResponse
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(request: OrderRequest): OrderResponse =
        repository.placeOrder(request)
}

