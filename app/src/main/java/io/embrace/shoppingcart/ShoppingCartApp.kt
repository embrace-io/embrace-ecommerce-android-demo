package io.embrace.shoppingcart

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.embrace.shoppingcart.telemetry.EmbraceTelemetryService
import io.embrace.shoppingcart.telemetry.TelemetryConfig

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

        // The wrapper owns SDK startup: kill switch → consent → sampling →
        // Embrace.start, with the PII-scrubbing exporter registered and start
        // failures absorbed. See TelemetryService design notes.
        EmbraceTelemetryService.instance.initialize(
            context = this,
            config = TelemetryConfig.forBuild(isDebug = BuildConfig.DEBUG),
        )
    }
}
