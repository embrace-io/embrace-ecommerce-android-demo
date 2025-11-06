package io.embrace.shoppingcart.mock

import io.embrace.shoppingcart.domain.api.AuthApi
import io.embrace.shoppingcart.domain.api.AuthUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class MockAuthApi @Inject constructor(
    private val config: AuthConfig
) : AuthApi {
    // In-memory accounts: email -> (id, name, password)
    private val accounts = mutableMapOf<String, Triple<String, String, String>>()

    override suspend fun login(email: String, password: String): AuthUser {
        delay(config.delayMs)
        val acc = accounts[email.lowercase()] ?: throw IllegalArgumentException("Invalid credentials")
        if (acc.third != password) throw IllegalArgumentException("Invalid credentials")
        return AuthUser(id = acc.first, name = acc.second, email = email)
    }

    override suspend fun register(name: String, email: String, password: String): AuthUser {
        delay(config.delayMs)
        val key = email.lowercase()
        if (accounts.containsKey(key)) throw IllegalStateException("Email already registered")
        val id = "user-${accounts.size + 1}"
        accounts[key] = Triple(id, name, password)
        return AuthUser(id = id, name = name, email = email)
    }
}

