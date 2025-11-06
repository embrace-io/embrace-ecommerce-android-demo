package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.domain.model.Category
import io.embrace.shoppingcart.mock.MockNetworkConfig
import io.embrace.shoppingcart.network.ApiService
import kotlinx.coroutines.delay
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val networkConfig: MockNetworkConfig
) : CategoryRepository {
    override suspend fun getCategories(): List<Category> {
        delay(networkConfig.defaultDelayMs)
        return apiService.getCategories().map { dto ->
            Category(id = dto.id, name = dto.name)
        }
    }
}
