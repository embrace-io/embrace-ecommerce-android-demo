package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.data.repository.ProductRepository
import io.embrace.shoppingcart.domain.model.Product
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = repository.observeProducts()
}


