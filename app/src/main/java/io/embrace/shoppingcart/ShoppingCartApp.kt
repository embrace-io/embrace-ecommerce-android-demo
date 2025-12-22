package io.embrace.shoppingcart

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.otel.java.addJavaSpanExporter
import io.embrace.shoppingcart.mock.MockNetworkConfig
import io.embrace.shoppingcart.mock.MockNetworkConfigOverrides
import io.embrace.shoppingcart.mock.NetworkScenario
import io.embrace.shoppingcart.telemetry.CustomSpanExporter
import timber.log.Timber

@HiltAndroidApp class ShoppingCartApp : Application() {
    override fun onCreate() {
        super.onCreate()

        /*// Force SUCCESS scenario for mock APIs when using prod flavor (HybridApiService)
        if (!BuildConfig.USE_MOCK) {
            MockNetworkConfigOverrides.override = MockNetworkConfig(
                defaultDelayMs = 0L,
                slowDelayMs = 3000L,
                productsScenario = NetworkScenario.SUCCESS,
                categoriesScenario = NetworkScenario.SUCCESS,
                placeOrderScenario = NetworkScenario.SUCCESS,
            )
        }*/

        Embrace.addJavaSpanExporter(CustomSpanExporter())

        Embrace.start(this)
    }
}
