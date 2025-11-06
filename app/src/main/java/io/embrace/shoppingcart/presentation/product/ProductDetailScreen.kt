package io.embrace.shoppingcart.presentation.product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.embrace.shoppingcart.presentation.components.AddToCartSection
import io.embrace.shoppingcart.presentation.components.ImageCarousel
import io.embrace.shoppingcart.presentation.components.MessageSnackbar
import io.embrace.shoppingcart.presentation.components.ProductVariantsSection
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
        productId: String,
        onBackPressed: () -> Unit,
        viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val shareLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) {}

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    LaunchedEffect(uiState.cartMessage) {
        uiState.cartMessage?.let {
            delay(3000)
            viewModel.clearCartMessage()
        }
    }

    LaunchedEffect(uiState.shareMessage) {
        uiState.shareMessage?.let {
            delay(2000)
            viewModel.clearShareMessage()
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Product Details") },
                        navigationIcon = {
                                                IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.shareProduct() }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        }
                )
            },
            bottomBar = {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AddToCartSection(
                            quantity = uiState.quantity,
                            onQuantityChange = { viewModel.updateQuantity(it) },
                            onAddToCart = { viewModel.addToCart() },
                            isProductAvailable = uiState.product?.inStock == true,
                            isAdding = uiState.isAddingToCart
                    )
                }
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
        ) {
            uiState.product?.let { product ->
                // Carrusel de imágenes
                ImageCarousel(imageUrls = product.imageUrls, modifier = Modifier.fillMaxWidth())

                // Información del producto
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Precio
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                                text = "$${(product.priceCents / 100.0)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                        )
                        if (product.discountPercentage > 0) {
                            Text(
                                    text = "-${product.discountPercentage}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                        )
                        Text(
                                text = "${product.rating} (${product.reviewCount} reviews)",
                                style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Variantes
                    ProductVariantsSection(
                            sizes = product.variants.sizes,
                            colors = product.variants.colors,
                            selectedVariant = uiState.selectedVariant,
                            onSizeSelected = { viewModel.selectSize(it) },
                            onColorSelected = { viewModel.selectColor(it) }
                    )

                    // Stock status
                    if (!product.inStock) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer
                                        )
                        ) {
                            Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "Out of stock",
                                        color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Estados de carga y error
            uiState.isLoading?.let {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error?.let { error ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Snackbar para mensajes
        uiState.cartMessage?.let { message ->
            MessageSnackbar(message = message, onDismiss = { viewModel.clearCartMessage() })
        }

        uiState.shareMessage?.let { message ->
            MessageSnackbar(message = message, onDismiss = { viewModel.clearShareMessage() })
        }
    }
}
