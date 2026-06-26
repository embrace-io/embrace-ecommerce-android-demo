package io.embrace.shoppingcart.presentation.testutil

object UiTestOverrides {
    @Volatile
    var verticalListForTests: Boolean = false

    // DEMO/TEST ONLY. When true, the payment crash button on PaymentMethodsScreen
    // crashes 100% of the time instead of its normal ~30% chance, so
    // PaymentCrashTest is deterministic.
    @Volatile
    var forcePaymentCrash: Boolean = false
}

