package io.embrace.shoppingcart.network

data class ProductDto(
        val id: String,
        val name: String,
        val priceCents: Int,
        val description: String,
        val imageUrls: List<String>? = null,
        val imageUrl: String? = null, // Para compatibilidad con datos existentes
        val category: String = "",
        val rating: Float = 0f,
        val reviewCount: Int = 0,
        val inStock: Boolean = true,
        val discountPercentage: Int = 0,
        val variants: ProductVariantsDto? = null
)

data class ProductVariantsDto(
        val sizes: List<String> = emptyList(),
        val colors: List<String> = emptyList()
)
