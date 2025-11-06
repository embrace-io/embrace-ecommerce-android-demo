package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.domain.model.Category

interface CategoryRepository {
    suspend fun getCategories(): List<Category>
}
