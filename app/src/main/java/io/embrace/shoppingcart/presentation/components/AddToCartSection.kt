package io.embrace.shoppingcart.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AddToCartSection(
        quantity: Int,
        onQuantityChange: (Int) -> Unit,
        onAddToCart: () -> Unit,
        isProductAvailable: Boolean,
        isAdding: Boolean = false,
        modifier: Modifier = Modifier
) {
    Row(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Contador de cantidad
        Row(
                modifier =
                        Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                Text("-", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Add to cart button
        Button(
            onClick = onAddToCart,
            modifier = Modifier.weight(1f),
            enabled = isProductAvailable && !isAdding
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adding…")
            } else {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Cart")
            }
        }
    }
}
