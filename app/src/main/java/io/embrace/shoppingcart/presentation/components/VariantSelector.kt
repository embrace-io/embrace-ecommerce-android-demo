package io.embrace.shoppingcart.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.embrace.shoppingcart.domain.model.ProductVariant

@Composable
fun VariantSelector(
        title: String,
        options: List<String>,
        selectedOption: String?,
        onOptionSelected: (String) -> Unit,
        modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    Column(modifier = modifier) {
        Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                        onClick = { onOptionSelected(option) },
                        label = { Text(option) },
                        selected = selectedOption == option
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProductVariantsSection(
        sizes: List<String>,
        colors: List<String>,
        selectedVariant: ProductVariant,
        onSizeSelected: (String) -> Unit,
        onColorSelected: (String) -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (sizes.isNotEmpty()) {
            VariantSelector(
                    title = "Size",
                    options = sizes,
                    selectedOption = selectedVariant.size,
                    onOptionSelected = onSizeSelected
            )
        }

        if (colors.isNotEmpty()) {
            VariantSelector(
                    title = "Color",
                    options = colors,
                    selectedOption = selectedVariant.color,
                    onOptionSelected = onColorSelected
            )
        }
    }
}
