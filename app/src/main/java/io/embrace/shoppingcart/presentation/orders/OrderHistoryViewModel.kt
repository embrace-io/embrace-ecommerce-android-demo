package io.embrace.shoppingcart.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.data.local.OrderDao
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val authService: MockAuthService
) : ViewModel() {
    data class UiState(
        val orders: List<io.embrace.shoppingcart.data.local.OrderEntity> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun currentUserId(): String {
        val logged = (authService.state.value as? AuthState.LoggedIn)?.userId
        if (logged != null) return logged
        viewModelScope.launch { authService.login("demo-user") }
        return "demo-user"
    }

    init {
        viewModelScope.launch {
            val uid = currentUserId()
            orderDao.observeByUser(uid).collectLatest { list ->
                _state.update { it.copy(orders = list) }
            }
        }
    }
}
