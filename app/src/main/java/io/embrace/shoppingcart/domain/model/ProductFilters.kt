package io.embrace.shoppingcart.domain.model

data class ProductFilters(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val inStockOnly: Boolean = false,
    val minRating: Float = 0f,
    val sortOption: SortOption = SortOption.POPULAR
)

