package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.data.repository.CategoryRepository
import io.embrace.shoppingcart.domain.model.Category
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(): List<Category> = repository.getCategories()
}
