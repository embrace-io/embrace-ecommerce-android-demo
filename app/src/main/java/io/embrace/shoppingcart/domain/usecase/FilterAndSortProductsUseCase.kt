package io.embrace.shoppingcart.domain.usecase

import io.embrace.shoppingcart.domain.model.Product
import io.embrace.shoppingcart.domain.model.ProductFilters
import io.embrace.shoppingcart.domain.model.SortOption
import javax.inject.Inject

class FilterAndSortProductsUseCase @Inject constructor() {
    
    operator fun invoke(products: List<Product>, filters: ProductFilters): List<Product> {
        return products
            .filter { product ->
                // Filtro por búsqueda
                val matchesSearch = filters.searchQuery.isEmpty() ||
                    product.name.contains(filters.searchQuery, ignoreCase = true) ||
                    product.description.contains(filters.searchQuery, ignoreCase = true)
                
                // Filtro por categoría
                val matchesCategory = filters.selectedCategory == null ||
                    product.category == filters.selectedCategory
                
                // Filtro por precio
                val matchesPrice = (filters.minPrice == null || product.priceCents >= filters.minPrice) &&
                    (filters.maxPrice == null || product.priceCents <= filters.maxPrice)
                
                // Filtro por stock
                val matchesStock = !filters.inStockOnly || product.inStock
                
                // Filtro por rating
                val matchesRating = product.rating >= filters.minRating
                
                matchesSearch && matchesCategory && matchesPrice && matchesStock && matchesRating
            }
            .sortedWith(getSortComparator(filters.sortOption))
    }
    
    private fun getSortComparator(sortOption: SortOption): Comparator<Product> {
        return when (sortOption) {
            SortOption.NAME_ASC -> compareBy { it.name }
            SortOption.NAME_DESC -> compareByDescending { it.name }
            SortOption.PRICE_ASC -> compareBy { it.priceCents }
            SortOption.PRICE_DESC -> compareByDescending { it.priceCents }
            SortOption.RATING_DESC -> compareByDescending { it.rating }
            SortOption.NEWEST -> compareByDescending { it.id } // Asumiendo que ID más alto = más reciente
            SortOption.POPULAR -> compareByDescending { it.reviewCount }
        }
    }
}

