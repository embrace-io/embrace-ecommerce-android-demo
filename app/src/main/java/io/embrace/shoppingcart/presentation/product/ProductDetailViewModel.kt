package io.embrace.shoppingcart.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.domain.model.Product
import io.embrace.shoppingcart.domain.model.ProductVariant
import io.embrace.shoppingcart.domain.usecase.GetProductsUseCase
import io.embrace.shoppingcart.domain.usecase.UpdateCartItemQuantityUseCase
import io.embrace.shoppingcart.network.AddToCartNetworkSimulator
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel
@Inject
constructor(
        private val getProductsUseCase: GetProductsUseCase,
        private val updateCartItemQuantity: UpdateCartItemQuantityUseCase,
        private val authService: MockAuthService,
        private val addToCartNetworkSimulator: AddToCartNetworkSimulator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val products = getProductsUseCase()
                val product = products.find { it.id == productId }
                if (product != null) {
                    _uiState.value = _uiState.value.copy(product = product, isLoading = false)
                } else {
                    _uiState.value =
                            _uiState.value.copy(error = "Product not found", isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                error = "Failed to load product: ${e.message}",
                                isLoading = false
                        )
            }
        }
    }

    fun selectSize(size: String) {
        _uiState.value =
                _uiState.value.copy(
                        selectedVariant = _uiState.value.selectedVariant.copy(size = size)
                )
    }

    fun selectColor(color: String) {
        _uiState.value =
                _uiState.value.copy(
                        selectedVariant = _uiState.value.selectedVariant.copy(color = color)
                )
    }

    fun updateQuantity(quantity: Int) {
        if (quantity > 0) {
            _uiState.value = _uiState.value.copy(quantity = quantity)
        }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        val quantity = _uiState.value.quantity
        val userId = (authService.state.value as? AuthState.LoggedIn)?.userId
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAddingToCart = true)
                // Simulate a network call for "add to cart" that Embrace can monitor.
                addToCartNetworkSimulator.simulate(product.id, quantity)
                // Auto-login de demo si no hay sesión
                val uid = userId ?: run {
                    authService.login("demo-user")
                    "demo-user"
                }
                updateCartItemQuantity(uid, product.id, quantity)
                _uiState.value =
                        _uiState.value.copy(
                                isAddingToCart = false,
                                cartMessage = "Added to cart: ${product.name} x$quantity"
                        )
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(
                                isAddingToCart = false,
                                cartMessage = "Failed to add to cart: ${e.message}"
                        )
            }
        }
    }

    fun clearCartMessage() {
        _uiState.value = _uiState.value.copy(cartMessage = null)
    }

    fun shareProduct() {
        val product = _uiState.value.product ?: return
        // Share functionality placeholder
        _uiState.value = _uiState.value.copy(shareMessage = "Check out this product: ${product.name}!")
    }

    fun clearShareMessage() {
        _uiState.value = _uiState.value.copy(shareMessage = null)
    }
}

data class ProductDetailUiState(
        val product: Product? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedVariant: ProductVariant = ProductVariant(),
        val quantity: Int = 1,
        val isAddingToCart: Boolean = false,
        val cartMessage: String? = null,
        val shareMessage: String? = null
)
