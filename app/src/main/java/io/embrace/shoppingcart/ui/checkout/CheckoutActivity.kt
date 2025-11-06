package io.embrace.shoppingcart.ui.checkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.embrace.android.embracesdk.Embrace
import io.embrace.shoppingcart.ui.theme.EmbraceShoppingCartTheme
import io.embrace.shoppingcart.presentation.checkout.CheckoutStep
import io.embrace.shoppingcart.presentation.checkout.CheckoutViewModel
import io.embrace.shoppingcart.presentation.checkout.CartReviewStep
import io.embrace.shoppingcart.presentation.checkout.ShippingStep
import io.embrace.shoppingcart.presentation.checkout.PaymentStep
import io.embrace.shoppingcart.presentation.checkout.ConfirmationStep

@AndroidEntryPoint
class CheckoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmbraceShoppingCartTheme {
                CheckoutScaffold(onFinish = {
                    finish()
                })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckoutActivityPreview() {
    EmbraceShoppingCartTheme { CheckoutScaffold(onFinish = {}) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CheckoutScaffold(viewModel: CheckoutViewModel = hiltViewModel(), onFinish: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            val title = when (state.step) {
                CheckoutStep.Review -> "Review Cart"
                CheckoutStep.Shipping -> "Shipping Info"
                CheckoutStep.Payment -> "Payment Method"
                CheckoutStep.Confirm -> "Confirm Order"
            }
            TopAppBar(title = { Text(title) })
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            val idx = when (state.step) {
                CheckoutStep.Review -> 0
                CheckoutStep.Shipping -> 1
                CheckoutStep.Payment -> 2
                CheckoutStep.Confirm -> 3
            }
            LinearProgressIndicator(progress = (idx + 1) / 4f, modifier = Modifier.fillMaxWidth())
            NavHost(navController = navController, startDestination = "review") {
                composable("review") {
                    viewModel.goTo(CheckoutStep.Review)
                    CartReviewStep(onNext = {
                        navController.navigate("shipping")
                    })
                }
                composable("shipping") {
                    viewModel.goTo(CheckoutStep.Shipping)
                    ShippingStep(onNext = {
                        Embrace.getInstance().addBreadcrumb("CHECKOUT_SHIPPING_COMPLETED")
                        navController.navigate("payment")
                    })
                }
                composable("payment") {
                    viewModel.goTo(CheckoutStep.Payment)
                    PaymentStep(onNext = {
                        Embrace.getInstance().addBreadcrumb("CHECKOUT_PAYMENT_COMPLETED")
                        navController.navigate("confirm")
                    })
                }
                composable("confirm") {
                    viewModel.goTo(CheckoutStep.Confirm)
                    ConfirmationStep(onFinish = onFinish)
                }
            }
        }
    }
}
