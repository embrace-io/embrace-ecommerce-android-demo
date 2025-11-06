package io.embrace.shoppingcart.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.embrace.shoppingcart.data.local.UserDao
import io.embrace.shoppingcart.data.local.UserEntity
import io.embrace.shoppingcart.domain.api.AuthApi
import io.embrace.shoppingcart.mock.MockAuthService
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: AuthApi,
    private val authService: MockAuthService,
    private val userDao: UserDao
) : ViewModel() {

    enum class Mode { Login, Register }

    data class UiState(
        val mode: Mode = Mode.Login,
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null,
        val nameError: String? = null,
        val message: String? = null,
        val success: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun switchMode() { _state.update { it.copy(mode = if (it.mode == Mode.Login) Mode.Register else Mode.Login, message = null) } }
    fun updateName(v: String) { _state.update { it.copy(name = v, nameError = null) } }
    fun updateEmail(v: String) { _state.update { it.copy(email = v, emailError = null) } }
    fun updatePassword(v: String) { _state.update { it.copy(password = v, passwordError = null) } }

    private fun validate(): Boolean {
        val s = _state.value
        var ok = true
        val emailOk = EMAIL_REGEX.matches(s.email.trim())
        val pwOk = s.password.length >= 6
        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null
        if (s.mode == Mode.Register && s.name.isBlank()) { ok = false; nameErr = "Name is required" }
        if (!emailOk) { ok = false; emailErr = "Invalid email" }
        if (!pwOk) { ok = false; passErr = "Password must be at least 6 chars" }
        _state.update { it.copy(nameError = nameErr, emailError = emailErr, passwordError = passErr) }
        return ok
    }

    fun submit() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, message = null) }
                val user = if (s.mode == Mode.Login) {
                    api.login(s.email.trim(), s.password)
                } else {
                    api.register(s.name.trim(), s.email.trim(), s.password)
                }
                // Persist user and set auth state
                userDao.upsert(UserEntity(id = user.id, name = user.name, email = user.email))
                authService.login(user.id)
                _state.update { it.copy(isLoading = false, success = true, message = "Success") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = e.message ?: "Auth failed") }
            }
        }
    }

    fun enterAsGuest() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, message = null) }
                val guestId = "guest"
                userDao.upsert(UserEntity(id = guestId, name = "Guest", email = "guest@example.com"))
                authService.login(guestId)
                _state.update { it.copy(isLoading = false, success = true, message = "Welcome, Guest") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = e.message ?: "Guest entry failed") }
            }
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
