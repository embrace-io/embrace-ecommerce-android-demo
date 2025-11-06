package io.embrace.shoppingcart.network.order

data class OrderRequestItem(
    val productId: String,
    val quantity: Int
)

data class ShippingInfo(
    val name: String,
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String
)

data class OrderRequest(
    val userId: String,
    val items: List<OrderRequestItem>,
    val totalCents: Int,
    val paymentMethodId: String,
    val shipping: ShippingInfo
)

data class OrderResponse(
    val orderId: String,
    val status: String = "confirmed"
)

