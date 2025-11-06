package io.embrace.shoppingcart.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.embrace.shoppingcart.presentation.components.ProductCard
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import io.embrace.shoppingcart.ui.product.ProductDetailActivity
import androidx.compose.ui.platform.testTag
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.BuildConfig
import io.embrace.shoppingcart.presentation.testutil.UiTestOverrides
import kotlin.random.Random

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val pullRefreshState =
            rememberPullRefreshState(refreshing = state.isLoading, onRefresh = { viewModel.load() })

    val filtered = state.filteredProducts

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                    value = state.filters.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Search products") }
            )

            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(state.categories) { category ->
                    Button(
                            onClick = { viewModel.onSearchQueryChange(category.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                    ) { Text(category.name) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val useVerticalList = BuildConfig.DEBUG && UiTestOverrides.verticalListForTests
            if (useVerticalList) {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag("home_products")) {
                    items(filtered) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = {
                                val intent = ProductDetailActivity.createIntent(context, product.id)
                                context.startActivity(intent)
                            },
                            onAddToCartClick = { viewModel.addToCart(it) },
                            isAddingToCart = state.addingProductIds.contains(product.id),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        //ANR Here
                    }
                }
            } else {
                LazyRow(modifier = Modifier.fillMaxWidth().testTag("home_products")) {
                    items(filtered) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = {
                                val intent = ProductDetailActivity.createIntent(context, product.id)
                                context.startActivity(intent)
                            },
                            onAddToCartClick = { viewModel.addToCart(it) },
                            isAddingToCart = state.addingProductIds.contains(product.id),
                            modifier = Modifier.width(240.dp).padding(8.dp)
                        )
                    }
                }
            }

            Column {
                Button(
                    onClick = { 1 / 0 },
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("crash_button")
                ) { Text("How much is 1 divided by Zero?") }

                Button(
                    onClick = {
                        blockWithThreadSleep(6000)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("anr_sleep_button")
                ) { Text("Trigger Thread Sleep ANR") }

                Button(
                    onClick = {
                        blockWithBusyLoop(3000)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("anr_loop_button")
                ) { Text("Trigger Busy Loop ANR") }
            }
        }

        PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
        )

        state.error?.let { msg ->
            io.embrace.shoppingcart.presentation.components.MessageSnackbar(
                message = msg,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.align(Alignment.BottomCenter),
                actionLabel = "Retry",
                onAction = {
                    viewModel.clearError()
                    viewModel.load()
                }
            )
        }
    }
    val chance = remember { Random.nextInt(10) } // 0..9 inclusive
    val cohort = when {
        chance < 5 -> "A"
        chance < 8 -> "B"
        else -> "C"
    }
    Embrace.getInstance().addSessionProperty("User Cohort", cohort, false)

    blockWithBusyLoop()

}
private const val DEFAULT_BLOCK_DURATION_MS = 15000L

fun blockWithBusyLoop(durationMs: Long = DEFAULT_BLOCK_DURATION_MS) {
    val start = System.currentTimeMillis()
    while (true) {
        if (System.currentTimeMillis() - start >= durationMs) {
            break
        }
    }
}

/**
 * Blocks the main thread with a sleep for the specified duration.
 */
fun blockWithThreadSleep(durationMs: Long = DEFAULT_BLOCK_DURATION_MS) {
    Thread.sleep(durationMs)
}
