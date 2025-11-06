package io.embrace.shoppingcart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
        entities =
                [
                        ProductEntity::class,
                        UserEntity::class,
                        AddressEntity::class,
                        PaymentMethodEntity::class,
                        CartItemEntity::class,
                        OrderEntity::class],
        version = 5,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao
    abstract fun addressDao(): AddressDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun cartItemDao(): CartItemDao
    abstract fun orderDao(): OrderDao
}
