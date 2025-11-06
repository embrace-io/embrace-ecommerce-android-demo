package io.embrace.shoppingcart.presentation.cart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.embrace.shoppingcart.presentation.components.MessageSnackbar
import android.content.Intent
import androidx.compose.ui.platform.testTag
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.ui.checkout.CheckoutActivity

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CartScreen(viewModel: CartViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.items, key = { it.product.id }) { lineItem ->
                val dismissState = rememberDismissState(confirmStateChange = { _ ->
                    viewModel.onRemove(lineItem.product.id)
                    true
                })
                SwipeToDismiss(state = dismissState, background = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(MaterialTheme.colorScheme.errorContainer)
                    )
                }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).testTag("cart_item")) {
                            Text(lineItem.product.name)
                            Text("$" + String.format("%.2f", lineItem.product.priceCents / 100.0))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { viewModel.onDecrement(lineItem.product.id) }, enabled = lineItem.quantity > 0) {
                                Text("-")
                            }
                            AnimatedContent(
                                targetState = lineItem.quantity,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                    } else {
                                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                                    }.using(SizeTransform(clip = false))
                                }
                            ) { qty ->
                                Text(qty.toString(), modifier = Modifier.padding(horizontal = 12.dp))
                            }
                            Button(onClick = { viewModel.onIncrement(lineItem.product.id) }) {
                                Text("+")
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Items: ${state.itemsCount}")
            Text("Subtotal: $" + String.format("%.2f", state.subtotalCents / 100.0))
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                Embrace.getInstance().addBreadcrumb("CHECKOUT_STARTED")
                if (state.items.isNotEmpty()) {
                    context.startActivity(Intent(context, CheckoutActivity::class.java))
                }
            }, enabled = state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("checkout_btn")) {
                Text("Continue to checkout")
            }
        }

        state.snackbarMessage?.let { msg ->
            MessageSnackbar(
                message = msg,
                actionLabel = "Undo",
                onAction = { viewModel.onUndoRemove() },
                onDismiss = { viewModel.onSnackbarDismiss() }
            )
        }
    }
}
