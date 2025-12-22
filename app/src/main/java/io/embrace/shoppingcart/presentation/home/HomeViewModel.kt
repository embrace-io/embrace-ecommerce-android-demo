package io.embrace.shoppingcart.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.domain.model.Category
import io.embrace.shoppingcart.domain.model.Product
import io.embrace.shoppingcart.domain.model.ProductFilters
import io.embrace.shoppingcart.domain.model.SortOption
import io.embrace.shoppingcart.domain.usecase.FilterAndSortProductsUseCase
import io.embrace.shoppingcart.domain.usecase.GetCategoriesUseCase
import io.embrace.shoppingcart.domain.usecase.GetProductsUseCase
import io.embrace.shoppingcart.domain.usecase.UpdateCartItemQuantityUseCase
import io.embrace.shoppingcart.network.AddToCartNetworkSimulator
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProducts: GetProductsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val filterAndSortProducts: FilterAndSortProductsUseCase,
    private val updateCartItemQuantity: UpdateCartItemQuantityUseCase,
    private val authService: MockAuthService,
    private val addToCartNetworkSimulator: AddToCartNetworkSimulator
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val products: List<Product> = emptyList(),
        val filteredProducts: List<Product> = emptyList(),
        val categories: List<Category> = emptyList(),
        val filters: ProductFilters = ProductFilters(),
        val error: String? = null,
        val hasReachedEnd: Boolean = false,
        val currentPage: Int = 0,
        val itemsPerPage: Int = 20,
        val addingProductIds: Set<String> = emptySet()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                Embrace.addBreadcrumb("Enter Product Search.")

                val (productsResult, categoriesResult) = kotlinx.coroutines.supervisorScope {
                    val productsDeferred = async { runCatching { getProducts() } }
                    val categoriesDeferred = async { runCatching { getCategories() } }
                    productsDeferred.await() to categoriesDeferred.await()
                }

                val products = productsResult.getOrElse { emptyList() }
                val categories = categoriesResult.getOrElse { emptyList() }

                _state.update { currentState ->
                    currentState.copy(
                        products = products,
                        categories = categories,
                        isLoading = false
                    )
                }
                // Surface any error message to UI via snackbar
                val err = productsResult.exceptionOrNull()?.message
                    ?: categoriesResult.exceptionOrNull()?.message
                if (err != null) {
                    _state.update { it.copy(error = err) }
                }
                applyFilters()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            try {
                val (productsResult, categoriesResult) = kotlinx.coroutines.supervisorScope {
                    val productsDeferred = async { runCatching { getProducts() } }
                    val categoriesDeferred = async { runCatching { getCategories() } }
                    productsDeferred.await() to categoriesDeferred.await()
                }

                val products = productsResult.getOrElse { emptyList() }
                val categories = categoriesResult.getOrElse { emptyList() }

                _state.update { currentState ->
                    currentState.copy(
                        products = products,
                        categories = categories,
                        isRefreshing = false,
                        currentPage = 0,
                        hasReachedEnd = false
                    )
                }
                val err = productsResult.exceptionOrNull()?.message
                    ?: categoriesResult.exceptionOrNull()?.message
                if (err != null) {
                    _state.update { it.copy(error = err) }
                }
                applyFilters()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isRefreshing = false, error = e.message)
            }
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore || _state.value.hasReachedEnd) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            // Simular carga de más datos
            kotlinx.coroutines.delay(1000)
            _state.update { currentState ->
                currentState.copy(
                    currentPage = currentState.currentPage + 1,
                    isLoadingMore = false,
                    hasReachedEnd = currentState.currentPage >= 5 // Simular fin de datos
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(searchQuery = query),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun onCategorySelected(category: String?) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(selectedCategory = category),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun onSortOptionSelected(sortOption: SortOption) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(sortOption = sortOption),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun onPriceRangeChanged(minPrice: Int?, maxPrice: Int?) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(
                    minPrice = minPrice,
                    maxPrice = maxPrice
                ),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun onInStockOnlyChanged(inStockOnly: Boolean) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(inStockOnly = inStockOnly),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun onMinRatingChanged(minRating: Float) {
        _state.update { currentState ->
            currentState.copy(
                filters = currentState.filters.copy(minRating = minRating),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    fun clearFilters() {
        _state.update { currentState ->
            currentState.copy(
                filters = ProductFilters(),
                currentPage = 0,
                hasReachedEnd = false
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val filteredProducts = filterAndSortProducts(_state.value.products, _state.value.filters)
        _state.update { currentState ->
            currentState.copy(filteredProducts = filteredProducts)
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            // mark as adding
            _state.update { it.copy(addingProductIds = it.addingProductIds + product.id) }
            val userId = (authService.state.value as? AuthState.LoggedIn)?.userId ?: run {
                authService.login("demo-user")
                "demo-user"
            }
            try {
                // Simulate a network call for "add to cart" that Embrace can monitor.
                addToCartNetworkSimulator.simulate(product.id, 1)
                updateCartItemQuantity(userId, product.id, 1)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(addingProductIds = it.addingProductIds - product.id) }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
