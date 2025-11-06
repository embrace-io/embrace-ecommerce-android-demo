package io.embrace.shoppingcart.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.data.local.AddressDao
import io.embrace.shoppingcart.data.local.OrderDao
import io.embrace.shoppingcart.data.local.OrderEntity
import io.embrace.shoppingcart.data.local.PaymentMethodDao
import io.embrace.shoppingcart.domain.model.CartLineItem
import io.embrace.shoppingcart.domain.usecase.CalculateCartTotalsUseCase
import io.embrace.shoppingcart.domain.usecase.ObserveCartUseCase
import io.embrace.shoppingcart.domain.usecase.PlaceOrderUseCase
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import io.embrace.shoppingcart.network.order.OrderRequest
import io.embrace.shoppingcart.network.order.OrderRequestItem
import io.embrace.shoppingcart.network.order.ShippingInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CheckoutStep { Review, Shipping, Payment, Confirm }

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val authService: MockAuthService,
    private val observeCart: ObserveCartUseCase,
    private val calcTotals: CalculateCartTotalsUseCase,
    private val addressDao: AddressDao,
    private val paymentDao: PaymentMethodDao,
    private val orderDao: OrderDao,
    private val placeOrderUseCase: PlaceOrderUseCase,
) : ViewModel() {

    data class UiState(
        val step: CheckoutStep = CheckoutStep.Review,
        val items: List<CartLineItem> = emptyList(),
        val itemsCount: Int = 0,
        val subtotalCents: Int = 0,
        // Shipping
        val name: String = "",
        val street: String = "",
        val city: String = "",
        val state: String = "",
        val zip: String = "",
        val country: String = "",
        // Payment
        val paymentMethodId: String? = null,
        val availablePaymentMethods: List<io.embrace.shoppingcart.data.local.PaymentMethodEntity> = emptyList(),
        // Confirmation
        val placingOrder: Boolean = false,
        val orderId: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun currentUserId(): String? = (authService.state.value as? AuthState.LoggedIn)?.userId

    init {
        viewModelScope.launch {
            val uid = currentUserId() ?: return@launch
            // Observe cart
            launch {
                observeCart(uid).collectLatest { list ->
                    val totals = calcTotals(list)
                    _state.update { it.copy(items = list, itemsCount = totals.itemsCount, subtotalCents = totals.subtotalCents) }
                }
            }
            // Load existing payment methods
            launch {
                paymentDao.observeByUser(uid).collectLatest { pms ->
                    _state.update { it.copy(availablePaymentMethods = pms) }
                }
            }
            // Prefill shipping from latest address if any
            launch {
                addressDao.observeByUser(uid).collectLatest { addrs ->
                    val a = addrs.firstOrNull() ?: return@collectLatest
                    _state.update { it.copy(street = a.street, city = a.city, state = a.state, zip = a.zip, country = a.country) }
                }
            }
        }
    }

    fun goTo(step: CheckoutStep) { _state.update { it.copy(step = step) } }
    fun next() {
        val next = when (_state.value.step) {
            CheckoutStep.Review -> CheckoutStep.Shipping
            CheckoutStep.Shipping -> CheckoutStep.Payment
            CheckoutStep.Payment -> CheckoutStep.Confirm
            CheckoutStep.Confirm -> CheckoutStep.Confirm
        }
        goTo(next)
    }
    fun back() {
        val prev = when (_state.value.step) {
            CheckoutStep.Review -> CheckoutStep.Review
            CheckoutStep.Shipping -> CheckoutStep.Review
            CheckoutStep.Payment -> CheckoutStep.Shipping
            CheckoutStep.Confirm -> CheckoutStep.Payment
        }
        goTo(prev)
    }

    // Shipping field updates
    fun updateName(v: String) = _state.update { it.copy(name = v) }
    fun updateStreet(v: String) = _state.update { it.copy(street = v) }
    fun updateCity(v: String) = _state.update { it.copy(city = v) }
    fun updateState(v: String) = _state.update { it.copy(state = v) }
    fun updateZip(v: String) = _state.update { it.copy(zip = v) }
    fun updateCountry(v: String) = _state.update { it.copy(country = v) }
    fun selectPayment(id: String) = _state.update { it.copy(paymentMethodId = id) }

    fun canContinueFromShipping(): Boolean = _state.value.run {
        street.isNotBlank() && city.isNotBlank() && zip.isNotBlank() && country.isNotBlank()
    }
    fun canContinueFromPayment(): Boolean = _state.value.paymentMethodId != null

    fun clearError() { _state.update { it.copy(error = null) } }

    fun placeOrder() {
        val uid = currentUserId() ?: return
        val s = _state.value
        viewModelScope.launch {
            try {
                _state.update { it.copy(placingOrder = true, error = null) }
                val request = OrderRequest(
                    userId = uid,
                    items = s.items.map { item ->
                        OrderRequestItem(productId = item.product.id, quantity = item.quantity)
                    },
                    totalCents = s.subtotalCents,
                    paymentMethodId = s.paymentMethodId ?: "",
                    shipping = ShippingInfo(
                        name = s.name,
                        street = s.street,
                        city = s.city,
                        state = s.state,
                        zip = s.zip,
                        country = s.country,
                    )
                )

                val response = placeOrderUseCase(request)

                val order = OrderEntity(
                        id = response.orderId,
                        userId = uid,
                        totalCents = s.subtotalCents,
                        shippingAddressId = "", // not persisted, simple mock
                        paymentMethodId = s.paymentMethodId ?: "",
                        createdAtEpochMs = System.currentTimeMillis()
                )
                orderDao.upsert(order)
                _state.update {
                    it.copy(placingOrder = false, orderId = order.id)
                }
            } catch (e: Exception) {
                _state.update { it.copy(placingOrder = false, error = e.message ?: "Failed to place order") }
            }
        }
    }

    fun finish() {

    }
}
