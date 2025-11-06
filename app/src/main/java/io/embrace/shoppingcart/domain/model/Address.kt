package io.embrace.shoppingcart.domain.model

data class Address(
    val id: String,
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String
)


