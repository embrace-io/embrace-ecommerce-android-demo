package io.embrace.shoppingcart.presentation.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.platform.testTag
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import io.embrace.android.embracesdk.spans.ErrorCode
import io.embrace.shoppingcart.ui.payment.PaymentMethodsActivity
import kotlin.random.Random

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CheckoutTopBar(step: CheckoutStep, onBack: (() -> Unit)?) {
    val title = when (step) {
        CheckoutStep.Review -> "Review Cart"
        CheckoutStep.Shipping -> "Shipping Info"
        CheckoutStep.Payment -> "Payment Method"
        CheckoutStep.Confirm -> "Confirm Order"
    }
    TopAppBar(title = { Text(title) }, navigationIcon = {
        onBack?.let {
            IconButton(onClick = it) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        }
    })
}

@Composable
fun CheckoutProgress(step: CheckoutStep, modifier: Modifier = Modifier) {
    val idx = when (step) {
        CheckoutStep.Review -> 0
        CheckoutStep.Shipping -> 1
        CheckoutStep.Payment -> 2
        CheckoutStep.Confirm -> 3
    }
    LinearProgressIndicator(progress = (idx + 1) / 4f, modifier = modifier)
}

@Composable
fun CartReviewStep(viewModel: CheckoutViewModel = hiltViewModel(), onNext: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val now = System.currentTimeMillis()
    val durationMs by remember { mutableStateOf(kotlin.random.Random.nextLong(200, 1501)) }
    Embrace.getInstance().recordCompletedSpan(
        name = "Render Cart Items",
        startTimeMs = now - durationMs,
        endTimeMs = now
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(Modifier.weight(1f)) {
            items(state.items) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.product.name)
                    Text("x${line.quantity}")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Text("Items: ${state.itemsCount}")
        Text("Subtotal: $" + String.format("%.2f", state.subtotalCents / 100.0))
        Button(onClick = onNext, enabled = state.items.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("to_shipping_btn")) {
            Text("Next: Shipping")
        }
    }
}

@Composable
fun ShippingStep(viewModel: CheckoutViewModel = hiltViewModel(), onNext: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(state.name, viewModel::updateName, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth().testTag("name_field"))
        OutlinedTextField(state.street, viewModel::updateStreet, label = { Text("Street") }, modifier = Modifier.fillMaxWidth().testTag("street_field"))
        OutlinedTextField(state.city, viewModel::updateCity, label = { Text("City") }, modifier = Modifier.fillMaxWidth().testTag("city_field"))
        OutlinedTextField(state.state, viewModel::updateState, label = { Text("State") }, modifier = Modifier.fillMaxWidth().testTag("state_field"))
        OutlinedTextField(state.zip, viewModel::updateZip, label = { Text("ZIP") }, modifier = Modifier.fillMaxWidth().testTag("zip_field"))
        OutlinedTextField(state.country, viewModel::updateCountry, label = { Text("Country") }, modifier = Modifier.fillMaxWidth().testTag("country_field"))
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNext, enabled = viewModel.canContinueFromShipping(), modifier = Modifier.fillMaxWidth().testTag("to_payment_btn")) {
            Text("Next: Payment")
        }
    }
}

@Composable
fun PaymentStep(viewModel: CheckoutViewModel = hiltViewModel(), onNext: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.availablePaymentMethods.isEmpty()) {
            Text("No payment methods saved.")
            Button(onClick = { context.startActivity(Intent(context, PaymentMethodsActivity::class.java)) }, modifier = Modifier.fillMaxWidth().testTag("add_payment_btn")) {
                Text("Add payment method")
            }
        } else {
            val now = System.currentTimeMillis()
            val durationMs by remember { mutableStateOf(kotlin.random.Random.nextLong(200, 1501)) }
            val chanceOfFailure = remember { Random.nextInt(10) } // 0..9 inclusive
            when {
                chanceOfFailure < 3 -> {
                    failedRequestPaymentMethods(now, durationMs)
                    failedRequestPaymentMethods(now, durationMs)
                    failedRequestPaymentMethods(now, durationMs)
                    Embrace.getInstance().recordCompletedSpan(
                        name = "Loaded Payment Methods",
                        startTimeMs = now - durationMs,
                        endTimeMs = now,
                        errorCode = ErrorCode.FAILURE
                    )
                    Embrace.getInstance().logError("Rejected: too many requests")
                }
                else -> Embrace.getInstance().recordCompletedSpan(
                    name = "Loaded Payment Methods",
                    startTimeMs = now - durationMs,
                    endTimeMs = now
                )
            }
            state.availablePaymentMethods.forEach { pm ->
                val selected = state.paymentMethodId == pm.id
                OutlinedButton(onClick = { viewModel.selectPayment(pm.id) }, modifier = Modifier.fillMaxWidth().testTag("visa_added"), enabled = true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${pm.brand} •••• ${pm.last4}")
                        if (selected) Text("Selected")
                    }
                }
            }
            TextButton(onClick = { context.startActivity(Intent(context, PaymentMethodsActivity::class.java)) }) {
                Text("Add another payment method")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNext, enabled = viewModel.canContinueFromPayment(), modifier = Modifier.fillMaxWidth().testTag("to_confirm_btn")) {
            Text("Next: Confirm")
        }
    }
}

@Composable
private fun failedRequestPaymentMethods(now: Long, durationMs: Long) {
    Embrace.getInstance().recordNetworkRequest(
        EmbraceNetworkRequest.fromCompletedRequest(
            url = "https://api.ecommerce.com/payment_methods",
            httpMethod = HttpMethod.GET,
            startTime = now - durationMs + 200,
            endTime = now,
            bytesSent = 0,
            bytesReceived = 0,
            statusCode = 429
        )
    )
}

@Composable
fun ConfirmationStep(viewModel: CheckoutViewModel = hiltViewModel(), onFinish: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().testTag("finish_btn")
            ) {
                Text("Finish!")
            }
            Text("Review & Confirm")
            Text("Ship to: ${state.name} ${state.street}, ${state.city}, ${state.state} ${state.zip}, ${state.country}")
            val pm = state.availablePaymentMethods.firstOrNull { it.id == state.paymentMethodId }
            Text("Pay with: ${pm?.brand ?: ""} •••• ${pm?.last4 ?: ""}")
            Text("Total: $" + String.format("%.2f", state.subtotalCents / 100.0))
            if (state.orderId == null) {
                Button(
                    onClick = { viewModel.placeOrder() },
                    enabled = !state.placingOrder,
                    modifier = Modifier.fillMaxWidth().testTag("place_order_btn")
                ) {
                    if (state.placingOrder) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Place Order")
                }
            } else {
                Box(Modifier.fillMaxSize().testTag("order_placed"), contentAlignment = Alignment.Center) {
                    Text("Order placed! #${state.orderId}")
                }
                val chanceOfFailure = remember { Random.nextInt(10) } // 0..9 inclusive
                if (chanceOfFailure < 3) {
                    Embrace.getInstance().logInfo("Displaying error modal: Sorry, we're having trouble processing your order. Please try again later.")
                }
            }
        }

        state.error?.let { msg ->

            Box(Modifier.fillMaxSize().testTag("order_failure"), contentAlignment = Alignment.Center) {
                Text(msg)
            }

            io.embrace.shoppingcart.presentation.components.MessageSnackbar(
                message = msg,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.align(Alignment.BottomCenter),
                actionLabel = "Retry",
                onAction = {
                    viewModel.clearError()
                    if (!state.placingOrder) viewModel.placeOrder()
                }
            )
        }
    }
}
