package io.embrace.shoppingcart.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.domain.model.CartLineItem
import io.embrace.shoppingcart.domain.usecase.CalculateCartTotalsUseCase
import io.embrace.shoppingcart.domain.usecase.ObserveCartUseCase
import io.embrace.shoppingcart.domain.usecase.RemoveCartItemUseCase
import io.embrace.shoppingcart.domain.usecase.UpdateCartItemQuantityUseCase
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val authService: MockAuthService,
    private val observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartItemQuantityUseCase,
    private val removeItem: RemoveCartItemUseCase,
    private val calculateTotals: CalculateCartTotalsUseCase
) : ViewModel() {

    data class UiState(
        val items: List<CartLineItem> = emptyList(),
        val itemsCount: Int = 0,
        val subtotalCents: Int = 0,
        val isLoading: Boolean = true,
        val error: String? = null,
        val snackbarMessage: String? = null,
        val lastRemovedProductId: String? = null,
        val lastRemovedQuantity: Int = 0
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            authService.state.collect { authState ->
                when (authState) {
                    is AuthState.LoggedIn -> subscribe(authState.userId)
                    else -> {
                        _state.update { it.copy(items = emptyList(), itemsCount = 0, subtotalCents = 0, isLoading = false) }
                    }
                }
            }
        }
    }

    private fun subscribe(userId: String) {
        viewModelScope.launch {
            observeCart(userId).collect { list ->
                val totals = calculateTotals(list)
                _state.update {
                    it.copy(
                        items = list,
                        itemsCount = totals.itemsCount,
                        subtotalCents = totals.subtotalCents,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onIncrement(productId: String) {
        modifyQuantity(productId, delta = 1)
    }

    fun onDecrement(productId: String) {
        modifyQuantity(productId, delta = -1)
    }

    fun onRemove(productId: String) {
        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            val removedQty = _state.value.items.firstOrNull { it.product.id == productId }?.quantity ?: 0
            removeItem(userId, productId)
            _state.update { it.copy(
                snackbarMessage = "Item removed",
                lastRemovedProductId = productId,
                lastRemovedQuantity = removedQty
            ) }
        }
    }

    private fun modifyQuantity(productId: String, delta: Int) {
        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            val current = _state.value.items.firstOrNull { it.product.id == productId }?.quantity ?: 0
            val newQty = (current + delta).coerceAtLeast(0)
            updateQuantity(userId, productId, newQty)
        }
    }

    private fun currentUserId(): String? =
        (authService.state.value as? AuthState.LoggedIn)?.userId

    fun onSnackbarDismiss() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun onUndoRemove() {
        viewModelScope.launch {
            val userId = currentUserId() ?: return@launch
            val productId = _state.value.lastRemovedProductId ?: return@launch
            val qty = _state.value.lastRemovedQuantity
            if (qty > 0) updateQuantity(userId, productId, qty)
            _state.update { it.copy(snackbarMessage = null, lastRemovedProductId = null, lastRemovedQuantity = 0) }
        }
    }
}

