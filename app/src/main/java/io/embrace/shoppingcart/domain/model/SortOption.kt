package io.embrace.shoppingcart.domain.model

enum class SortOption(val displayName: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    PRICE_ASC("Price: Low to High"),
    PRICE_DESC("Price: High to Low"),
    RATING_DESC("Top Rated"),
    NEWEST("Newest"),
    POPULAR("Most Popular")
}
