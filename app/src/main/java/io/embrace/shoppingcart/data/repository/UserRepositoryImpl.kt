package io.embrace.shoppingcart.data.repository

import io.embrace.shoppingcart.data.local.UserDao
import io.embrace.shoppingcart.data.local.UserEntity
import io.embrace.shoppingcart.domain.model.User
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl @Inject constructor(private val userDao: UserDao) : UserRepository {
    override suspend fun upsert(user: User) {
        userDao.upsert(UserEntity(user.id, user.name, user.email))
    }

    override fun observeById(id: String): Flow<User?> =
            userDao.observeById(id).map { it?.let { User(it.id, it.name, it.email) } }
}
