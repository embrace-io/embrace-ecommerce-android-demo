package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.data.local.ProductDao
import io.embrace.shoppingcart.data.local.ProductEntity
import io.embrace.shoppingcart.domain.model.Product
import io.embrace.shoppingcart.mock.MockNetworkConfig
import io.embrace.shoppingcart.network.ApiService
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl
@Inject
constructor(
        private val apiService: ApiService,
        private val productDao: ProductDao,
        private val networkConfig: MockNetworkConfig
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        delay(networkConfig.defaultDelayMs)
        val products = apiService.getProducts()
        val entities =
                products.map { dto ->
                    ProductEntity(
                            id = dto.id,
                            name = dto.name,
                            priceCents = dto.priceCents,
                            description = dto.description,
                            imageUrl = dto.imageUrl ?: dto.imageUrls?.firstOrNull() ?: "",
                            category = dto.category,
                            rating = dto.rating,
                            reviewCount = dto.reviewCount,
                            inStock = dto.inStock,
                            discountPercentage = dto.discountPercentage
                    )
                }
        productDao.upsertAll(entities)
        return entities.map { entity ->
            // Buscar el DTO original para obtener las variantes y múltiples imágenes
            val originalDto = products.find { it.id == entity.id }
            Product(
                    id = entity.id,
                    name = entity.name,
                    priceCents = entity.priceCents,
                    description = entity.description,
                    imageUrls = originalDto?.imageUrls ?: listOf(entity.imageUrl),
                    category = entity.category,
                    rating = entity.rating,
                    reviewCount = entity.reviewCount,
                    inStock = entity.inStock,
                    discountPercentage = entity.discountPercentage,
                    variants =
                            originalDto?.variants?.let { dto ->
                                io.embrace.shoppingcart.domain.model.ProductVariants(
                                        sizes = dto.sizes,
                                        colors = dto.colors
                                )
                            }
                                    ?: io.embrace.shoppingcart.domain.model.ProductVariants()
            )
        }
    }

    override fun observeProducts() =
            productDao.observeAll().map { list ->
                list.map { entity ->
                    // Para observeProducts, usamos solo la imagen principal ya que no tenemos
                    // acceso a los DTOs originales
                    Product(
                            id = entity.id,
                            name = entity.name,
                            priceCents = entity.priceCents,
                            description = entity.description,
                            imageUrls = listOf(entity.imageUrl),
                            category = entity.category,
                            rating = entity.rating,
                            reviewCount = entity.reviewCount,
                            inStock = entity.inStock,
                            discountPercentage = entity.discountPercentage,
                            variants = io.embrace.shoppingcart.domain.model.ProductVariants()
                    )
                }
            }
}
