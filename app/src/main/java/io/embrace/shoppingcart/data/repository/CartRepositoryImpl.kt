package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.data.local.CartItemDao
import io.embrace.shoppingcart.data.local.CartItemEntity
import io.embrace.shoppingcart.data.local.ProductDao
import io.embrace.shoppingcart.domain.model.CartLineItem
import io.embrace.shoppingcart.domain.model.Product
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CartRepositoryImpl
@Inject
constructor(
    private val cartItemDao: CartItemDao,
    private val productDao: ProductDao
) : CartRepository {

    override fun observeCart(userId: String): Flow<List<CartLineItem>> {
        return combine(
            cartItemDao.observeCart(userId),
            productDao.observeAll()
        ) { cartEntities, productEntities ->
            val productMap = productEntities.associateBy { it.id }
            val quantitiesByProduct = cartEntities
                .groupBy { it.productId }
                .mapValues { entry -> entry.value.sumOf { it.quantity } }

            quantitiesByProduct.mapNotNull { (productId, qty) ->
                val p = productMap[productId] ?: return@mapNotNull null
                val product = Product(
                    id = p.id,
                    name = p.name,
                    priceCents = p.priceCents,
                    description = p.description,
                    imageUrls = listOf(p.imageUrl),
                    category = p.category,
                    rating = p.rating,
                    reviewCount = p.reviewCount,
                    inStock = p.inStock,
                    discountPercentage = p.discountPercentage
                )
                CartLineItem(product = product, quantity = qty)
            }
        }
    }

    override suspend fun upsert(userId: String, productId: String, quantity: Int) {
        require(quantity >= 0) { "quantity must be non-negative" }
        if (quantity == 0) {
            cartItemDao.delete(userId, productId)
        } else {
            cartItemDao.upsert(
                CartItemEntity(
                    userId = userId,
                    productId = productId,
                    quantity = quantity
                )
            )
        }
    }

    override suspend fun remove(userId: String, productId: String) {
        cartItemDao.delete(userId, productId)
    }
}

