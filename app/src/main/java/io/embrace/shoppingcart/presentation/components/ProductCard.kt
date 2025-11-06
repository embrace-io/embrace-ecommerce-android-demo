package io.embrace.shoppingcart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.domain.model.Product

@Composable
fun ProductCard(
        product: Product,
        onProductClick: (Product) -> Unit,
        onAddToCartClick: (Product) -> Unit,
        isAddingToCart: Boolean = false,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier.fillMaxWidth().clickable { onProductClick(product) },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Imagen del producto
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(
                        model = product.imageUrls.firstOrNull() ?: "",
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )

                // Add to cart button
                IconButton(
                        onClick = {
                            Embrace.getInstance().addBreadcrumb("Added to cart: ${product.name}")
                            onAddToCartClick(product)
                        },
                        enabled = !isAddingToCart && product.inStock,
                        modifier =
                                Modifier.align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .testTag("add_to_cart")
                                        .background(
                                                color =
                                                        MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.8f
                                                        ),
                                                shape = RoundedCornerShape(50)
                                        )
                ) {
                    if (isAddingToCart) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Add to cart",
                                tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Add to cart button
                IconButton(
                    onClick = {
                        Embrace.getInstance().addBreadcrumb("Added to Favorites: ${product.name}")
                    },
                    enabled = !isAddingToCart && product.inStock,
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .testTag("favorite_btn")
                            .background(
                                color =
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.8f
                                    ),
                                shape = RoundedCornerShape(50)
                            )
                ) {
                    if (isAddingToCart) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Badge de descuento
                if (product.discountPercentage > 0) {
                    Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            color = Color.Red,
                            shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                                text = "-${product.discountPercentage}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Out of stock badge
                if (!product.inStock) {
                    Surface(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                                text = "Out of stock",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Información del producto
            Column(modifier = Modifier.padding(16.dp)) {
                // Nombre del producto
                Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = "${product.rating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.reviewCount > 0) {
                        Text(
                                text = " (${product.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Precio
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val originalPrice = product.priceCents / 100.0
                    val discountedPrice =
                            if (product.discountPercentage > 0) {
                                originalPrice * (1 - product.discountPercentage / 100.0)
                            } else {
                                originalPrice
                            }

                    Text(
                            text = "$${String.format("%.2f", discountedPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                    )

                    if (product.discountPercentage > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "$${String.format("%.2f", originalPrice)}",
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Categoría
                if (product.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = product.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
