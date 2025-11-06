package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(): List<Product>
    fun observeProducts(): Flow<List<Product>>
}


