package io.embrace.shoppingcart.domain.model

data class Order(
        val id: String,
        val userId: String,
        val items: List<CartItem>,
        val totalCents: Int,
        val shippingAddressId: String,
        val paymentMethodId: String,
        val createdAtEpochMs: Long
)
