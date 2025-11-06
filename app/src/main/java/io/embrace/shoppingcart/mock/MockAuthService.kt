package io.embrace.shoppingcart.mock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object LoggedOut : AuthState()
    data class LoggedIn(val userId: String) : AuthState()
}

data class AuthConfig(
    val delayMs: Long = 0L,
    val failLogin: Boolean = false
)

@Singleton
class MockAuthService @Inject constructor(
    private val config: AuthConfig
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val state: StateFlow<AuthState> = _state

    init {
        MockAuthOverrides.loggedInUserId?.let { uid ->
            _state.value = AuthState.LoggedIn(uid)
        }
    }

    suspend fun login(userId: String) {
        delay(config.delayMs)
        if (config.failLogin) throw IllegalStateException("Login failed")
        _state.value = AuthState.LoggedIn(userId)
    }

    suspend fun logout() {
        delay(config.delayMs)
        _state.value = AuthState.LoggedOut
    }
}
