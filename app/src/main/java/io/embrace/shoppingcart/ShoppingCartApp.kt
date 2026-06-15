package io.embrace.shoppingcart

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.embrace.shoppingcart.telemetry.EmbraceTelemetryService
import io.embrace.shoppingcart.telemetry.TelemetryConfig
import io.embrace.shoppingcart.telemetry.TelemetryService
import kotlin.random.Random

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
        val telemetry = EmbraceTelemetryService.instance
        telemetry.initialize(
            context = this,
            config = TelemetryConfig.forBuild(isDebug = BuildConfig.DEBUG),
        )

        simulateAuthSdkInit(telemetry)
    }

    /**
     * Demo scenario: a 3rd-party auth SDK that blocks app startup.
     * 25% of cold starts hit a slow path (~2–2.5s), the rest are fast (~100–250ms).
     *
     * The Thread.sleep simulates real blocking work and runs regardless of
     * telemetry; the timing is reported through the wrapper as a child of the
     * startup trace.
     */
    private fun simulateAuthSdkInit(telemetry: TelemetryService) {
        val isSlow = Random.nextDouble() < SLOW_STARTUP_PROBABILITY
        val delayMs = if (isSlow) {
            Random.nextLong(SLOW_AUTH_MIN_MS, SLOW_AUTH_MAX_MS)
        } else {
            Random.nextLong(FAST_AUTH_MIN_MS, FAST_AUTH_MAX_MS)
        }
        val scenario = if (isSlow) "slow" else "fast"

        telemetry.addSessionProperty("startup_scenario", scenario, permanent = false)

        val startMs = System.currentTimeMillis()
        Thread.sleep(delayMs)
        val endMs = System.currentTimeMillis()

        telemetry.recordStartupChildSpan(
            name = "auth-sdk",
            startTimeMs = startMs,
            endTimeMs = endMs,
            attributes = mapOf(
                "auth.scenario" to scenario,
                "auth.provider" to "demo-3p-auth",
            ),
        )
    }

    private companion object {
        const val SLOW_STARTUP_PROBABILITY = 0.25
        const val SLOW_AUTH_MIN_MS = 2000L
        const val SLOW_AUTH_MAX_MS = 2500L
        const val FAST_AUTH_MIN_MS = 100L
        const val FAST_AUTH_MAX_MS = 250L
    }
}
