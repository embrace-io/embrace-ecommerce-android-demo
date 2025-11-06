package io.embrace.shoppingcart.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(@PrimaryKey val id: String, val name: String, val email: String)

@Entity(tableName = "addresses")
data class AddressEntity(
        @PrimaryKey val id: String,
        val userId: String,
        val street: String,
        val city: String,
        val state: String,
        val zip: String,
        val country: String
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
        @PrimaryKey val id: String,
        val userId: String,
        val brand: String,
        val last4: String,
        val expiryMonth: Int,
        val expiryYear: Int
)

@Entity(
    tableName = "cart_items",
    indices = [Index(value = ["userId", "productId"], unique = true)]
)
data class CartItemEntity(
        @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
        val userId: String,
        val productId: String,
        val quantity: Int
)

@Entity(tableName = "orders")
data class OrderEntity(
        @PrimaryKey val id: String,
        val userId: String,
        val totalCents: Int,
        val shippingAddressId: String,
        val paymentMethodId: String,
        val createdAtEpochMs: Long
)
