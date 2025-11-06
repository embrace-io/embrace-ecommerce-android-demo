package io.embrace.shoppingcart.presentation.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.data.local.AddressDao
import io.embrace.shoppingcart.data.local.AddressEntity
import io.embrace.shoppingcart.domain.service.GeocodingService
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
class AddressViewModel @Inject constructor(
    private val addressDao: AddressDao,
    private val authService: MockAuthService,
    private val geocoder: GeocodingService
) : ViewModel() {

    data class UiState(
        val list: List<AddressEntity> = emptyList(),
        val street: String = "",
        val city: String = "",
        val state: String = "",
        val zip: String = "",
        val country: String = "",
        val geo: String? = null,
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
            addressDao.observeByUser(uid).collectLatest { list ->
                _state.update { it.copy(list = list) }
            }
        }
    }

    fun updateStreet(v: String) = _state.update { it.copy(street = v) }
    fun updateCity(v: String) = _state.update { it.copy(city = v) }
    fun updateState(v: String) = _state.update { it.copy(state = v) }
    fun updateZip(v: String) = _state.update { it.copy(zip = v) }
    fun updateCountry(v: String) = _state.update { it.copy(country = v) }

    fun geocodeCurrent() {
        viewModelScope.launch {
            val s = _state.value
            val addr = listOf(s.street, s.city, s.state, s.zip, s.country).filter { it.isNotBlank() }.joinToString(", ")
            val pt = geocoder.geocode(addr)
            _state.update { it.copy(geo = pt?.let { g -> "${g.latitude}, ${g.longitude}" } ?: "Not found") }
        }
    }

    fun save() {
        val uid = currentUserId()
        val s = _state.value
        viewModelScope.launch {
            val entity = AddressEntity(
                id = "addr-${System.currentTimeMillis()}",
                userId = uid,
                street = s.street,
                city = s.city,
                state = s.state,
                zip = s.zip,
                country = s.country
            )
            addressDao.upsertAll(listOf(entity))
            _state.update { it.copy(street = "", city = "", state = "", zip = "", country = "", message = "Address saved") }
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
