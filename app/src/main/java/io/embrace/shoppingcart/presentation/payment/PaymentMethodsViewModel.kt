package io.embrace.shoppingcart.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.data.local.PaymentMethodDao
import io.embrace.shoppingcart.data.local.PaymentMethodEntity
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
class PaymentMethodsViewModel @Inject constructor(
    private val dao: PaymentMethodDao,
    private val authService: MockAuthService
) : ViewModel() {

    data class UiState(
        val list: List<PaymentMethodEntity> = emptyList(),
        val brand: String = "",
        val last4: String = "",
        val expiryMonth: String = "",
        val expiryYear: String = "",
        val message: String? = null
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
            dao.observeByUser(uid).collectLatest { list ->
                _state.update { it.copy(list = list) }
            }
        }
    }

    fun updateBrand(v: String) = _state.update { it.copy(brand = v) }
    fun updateLast4(v: String) = _state.update { it.copy(last4 = v.takeLast(4)) }
    fun updateMonth(v: String) = _state.update { it.copy(expiryMonth = v) }
    fun updateYear(v: String) = _state.update { it.copy(expiryYear = v) }

    fun save() {
        val uid = currentUserId()
        val s = _state.value
        val month = s.expiryMonth.toIntOrNull() ?: return
        val year = s.expiryYear.toIntOrNull() ?: return
        viewModelScope.launch {
            dao.upsertAll(
                listOf(
                    PaymentMethodEntity(
                        id = "pm-${System.currentTimeMillis()}",
                        userId = uid,
                        brand = s.brand,
                        last4 = s.last4.takeLast(4),
                        expiryMonth = month,
                        expiryYear = year
                    )
                )
            )
            _state.update { it.copy(brand = "", last4 = "", expiryMonth = "", expiryYear = "", message = "Payment method saved") }
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
