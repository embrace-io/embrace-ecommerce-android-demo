package io.embrace.shoppingcart.domain.model

data class Product(
        val id: String,
        val name: String,
        val priceCents: Int,
        val description: String,
        val imageUrls: List<String>,
        val category: String = "",
        val rating: Float = 0f,
        val reviewCount: Int = 0,
        val inStock: Boolean = true,
        val discountPercentage: Int = 0,
        val variants: ProductVariants = ProductVariants()
)

data class ProductVariants(
        val sizes: List<String> = emptyList(),
        val colors: List<String> = emptyList()
)

data class ProductVariant(val size: String? = null, val color: String? = null)
