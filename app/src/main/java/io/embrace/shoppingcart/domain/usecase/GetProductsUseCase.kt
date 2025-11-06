package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.data.repository.ProductRepository
import io.embrace.shoppingcart.domain.model.Product
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): List<Product> = repository.getProducts()
}


