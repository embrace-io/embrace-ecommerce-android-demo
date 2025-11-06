package io.embrace.shoppingcart.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.data.local.UserDao
import io.embrace.shoppingcart.data.local.UserEntity
import io.embrace.shoppingcart.mock.AuthState
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val authService: MockAuthService
) : ViewModel() {

    data class UiState(
        val name: String = "",
        val email: String = "",
        val isLoading: Boolean = false,
        val message: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private fun currentUserId(): String {
        val logged = (authService.state.value as? AuthState.LoggedIn)?.userId
        if (logged != null) return logged
        viewModelScope.launch { authService.login("demo-user") }
        return "demo-user"
    }

    init {
        viewModelScope.launch {
            val uid = currentUserId()
            userDao.observeById(uid).collectLatest { user ->
                if (user != null) {
                    _state.update { it.copy(name = user.name, email = user.email) }
                }
            }
        }
    }

    fun updateName(name: String) { _state.update { it.copy(name = name) } }
    fun updateEmail(email: String) { _state.update { it.copy(email = email) } }

    fun save() {
        val uid = currentUserId()
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userDao.upsert(UserEntity(id = uid, name = s.name, email = s.email))
            _state.update { it.copy(isLoading = false, message = "Profile saved") }
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
