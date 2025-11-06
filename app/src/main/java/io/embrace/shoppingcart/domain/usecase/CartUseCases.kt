package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.data.repository.CartRepository
import io.embrace.shoppingcart.domain.model.CartLineItem
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCartUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(userId: String): Flow<List<CartLineItem>> = repository.observeCart(userId)
}

class UpdateCartItemQuantityUseCase @Inject constructor(
    private val repository: CartRepository
) {
    suspend operator fun invoke(userId: String, productId: String, quantity: Int) =
        repository.upsert(userId, productId, quantity)
}

class RemoveCartItemUseCase @Inject constructor(
    private val repository: CartRepository
) {
    suspend operator fun invoke(userId: String, productId: String) =
        repository.remove(userId, productId)
}

class CalculateCartTotalsUseCase @Inject constructor() {
    data class Totals(
        val itemsCount: Int,
        val subtotalCents: Int
    )

    operator fun invoke(items: List<CartLineItem>): Totals {
        val itemsCount = items.sumOf { it.quantity }
        val subtotal = items.sumOf { it.subtotalCents }
        return Totals(itemsCount, subtotal)
    }
}


