package io.embrace.shoppingcart.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.embrace.shoppingcart.domain.model.Category
import io.embrace.shoppingcart.domain.model.ProductFilters
import io.embrace.shoppingcart.domain.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filters: ProductFilters,
    categories: List<Category>,
    onFiltersChanged: (ProductFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var localFilters by remember { mutableStateOf(filters) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ordenamiento
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SortOption.values()) { sortOption ->
                    FilterChip(
                        selected = localFilters.sortOption == sortOption,
                        onClick = {
                            localFilters = localFilters.copy(sortOption = sortOption)
                        },
                        label = { Text(sortOption.displayName) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Categories
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = localFilters.selectedCategory == null,
                        onClick = {
                            localFilters = localFilters.copy(selectedCategory = null)
                        },
                        label = { Text("All") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = localFilters.selectedCategory == category.name,
                        onClick = {
                            localFilters = localFilters.copy(selectedCategory = category.name)
                        },
                        label = { Text(category.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Price range
            Text(
                text = "Price range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = localFilters.minPrice?.toString() ?: "",
                    onValueChange = { value ->
                        val minPrice = value.toIntOrNull()
                        localFilters = localFilters.copy(minPrice = minPrice)
                    },
                    label = { Text("Min price") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = localFilters.maxPrice?.toString() ?: "",
                    onValueChange = { value ->
                        val maxPrice = value.toIntOrNull()
                        localFilters = localFilters.copy(maxPrice = maxPrice)
                    },
                    label = { Text("Max price") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Additional filters
            Text(
                text = "Additional filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = localFilters.inStockOnly,
                    onCheckedChange = { checked ->
                        localFilters = localFilters.copy(inStockOnly = checked)
                    }
                )
                Text(
                    text = "In-stock only",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Minimum rating
            Text(
                text = "Minimum rating: ${localFilters.minRating}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = localFilters.minRating,
                onValueChange = { value ->
                    localFilters = localFilters.copy(minRating = value)
                },
                valueRange = 0f..5f,
                steps = 9
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        localFilters = ProductFilters()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear filters")
                }
                Button(
                    onClick = {
                        onFiltersChanged(localFilters)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply filters")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FilterButton(
    onClick: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Filters")
        if (hasActiveFilters) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
