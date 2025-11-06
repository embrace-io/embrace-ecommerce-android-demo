package io.embrace.shoppingcart.domain.model

data class CartLineItem(
    val product: Product,
    val quantity: Int
) {
    val subtotalCents: Int get() = product.priceCents * quantity
}


