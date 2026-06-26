package io.embrace.shoppingcart.presentation.payment

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.embrace.shoppingcart.presentation.components.MessageSnackbar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.testTag
import io.embrace.shoppingcart.presentation.testutil.UiTestOverrides
import io.embrace.shoppingcart.telemetry.EmbraceTelemetryService

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PaymentMethodsScreen(viewModel: PaymentMethodsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Payment Methods") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.list) { pm ->
                    Text("${pm.brand} •••• ${pm.last4}  exp ${pm.expiryMonth}/${pm.expiryYear}")
                    Spacer(Modifier.height(8.dp))
                }
            }

            OutlinedTextField(state.brand, viewModel::updateBrand, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth().testTag("brand_field"))
            OutlinedTextField(state.last4, viewModel::updateLast4, label = { Text("Last 4") }, modifier = Modifier.fillMaxWidth().testTag("last4_field"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(state.expiryMonth, viewModel::updateMonth, label = { Text("MM") }, modifier = Modifier.weight(1f).testTag("month_field"))
                OutlinedTextField(state.expiryYear, viewModel::updateYear, label = { Text("YYYY") }, modifier = Modifier.weight(1f).testTag("year_field"))
            }
            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth().testTag("save_btn")) { Text("Save Payment Method") }

            // DEMO CODE — DELETE IN A REAL APP.
            // Crashes ~30% of taps to generate an intermittent payment-flow crash
            // for demo data. The crash is async (Handler.postDelayed) so the test
            // thread can settle first — mirrors the Home crash button. We emit a
            // log + breadcrumb first so the session has context, then throw an
            // unhandled RuntimeException (Embrace's UncaughtExceptionHandler
            // captures it — Android has no Embrace.crash()).
            // UiTestOverrides.forcePaymentCrash forces 100% so PaymentCrashTest
            // is deterministic.
            OutlinedButton(
                onClick = {
                    val shouldCrash = UiTestOverrides.forcePaymentCrash || (1..100).random() <= 30
                    if (shouldCrash) {
                        val telemetry = EmbraceTelemetryService.instance
                        telemetry.logError(
                            "Payment method save failed: invalid card token",
                            mapOf("crash_type" to "payment_methods", "trigger" to "payment_crash_button"),
                        )
                        telemetry.addBreadcrumb("Crash from payment methods screen")
                        Handler(Looper.getMainLooper()).postDelayed({
                            throw RuntimeException("Payment method save failed: invalid card token")
                        }, 100)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("payment_crash_button")
            ) { Text("Validate card (demo crash)") }
        }

        state.message?.let { msg ->
            MessageSnackbar(message = msg, onDismiss = { viewModel.clearMessage() })
        }
    }
}
