package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun upsert(user: User)
    fun observeById(id: String): Flow<User?>
}
