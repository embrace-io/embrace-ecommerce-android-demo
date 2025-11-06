package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.domain.model.CartLineItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(userId: String): Flow<List<CartLineItem>>
    suspend fun upsert(userId: String, productId: String, quantity: Int)
    suspend fun remove(userId: String, productId: String)
}


