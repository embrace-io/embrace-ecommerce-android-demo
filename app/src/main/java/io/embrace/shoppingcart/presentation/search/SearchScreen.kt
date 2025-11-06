package io.embrace.shoppingcart.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.embrace.shoppingcart.presentation.components.ProductCard
import io.embrace.shoppingcart.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Product Catalog") },
                        actions = {
                            IconButton(onClick = { viewModel.load() }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                )
            }
    ) { paddingValues ->
        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(paddingValues)
        ) {
            items(state.products) { product ->
                ProductCard(
                        product = product,
                        onProductClick = {
                            navController.navigate(
                                    Routes.ProductDetail.replace("{productId}", product.id)
                            )
                        },
                        onAddToCartClick = { },
                        modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.error?.let { msg ->
            io.embrace.shoppingcart.presentation.components.MessageSnackbar(
                message = msg,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.fillMaxSize(),
                actionLabel = "Retry",
                onAction = {
                    viewModel.clearError()
                    viewModel.load()
                }
            )
        }
    }
}
