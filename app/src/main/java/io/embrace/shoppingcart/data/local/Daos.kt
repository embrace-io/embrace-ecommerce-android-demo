package io.embrace.shoppingcart.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(user: UserEntity)
    @Query("SELECT * FROM users WHERE id = :id") fun observeById(id: String): Flow<UserEntity?>
}

@Dao
interface AddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(addresses: List<AddressEntity>)
    @Query("SELECT * FROM addresses WHERE userId = :userId")
    fun observeByUser(userId: String): Flow<List<AddressEntity>>
}

@Dao
interface PaymentMethodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(methods: List<PaymentMethodEntity>)
    @Query("SELECT * FROM payment_methods WHERE userId = :userId")
    fun observeByUser(userId: String): Flow<List<PaymentMethodEntity>>
}

@Dao
interface CartItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: CartItemEntity)
    @Query("DELETE FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun delete(userId: String, productId: String)
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun observeCart(userId: String): Flow<List<CartItemEntity>>
}

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(order: OrderEntity)
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeByUser(userId: String): Flow<List<OrderEntity>>
}
