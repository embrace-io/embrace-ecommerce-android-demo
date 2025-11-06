package io.embrace.shoppingcart.domain.api

data class AuthUser(val id: String, val name: String, val email: String)

interface AuthApi {
    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(name: String, email: String, password: String): AuthUser
}

