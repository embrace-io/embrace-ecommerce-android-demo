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
import kotlin.random.Random
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

        Embrace.addSessionProperty("flavor_env", BuildConfig.FLAVOR_env, true)
        Embrace.addSessionProperty("flavor_embrace", BuildConfig.FLAVOR_embrace, true)

        simulateAuthSdkInit()
    }

    /**
     * Demo scenario: a 3rd-party auth SDK that blocks app startup.
     * 25% of cold starts hit a slow path (~2–2.5s), the rest are fast (~100–250ms).
     */
    private fun simulateAuthSdkInit() {
        val isSlow = Random.nextDouble() < SLOW_STARTUP_PROBABILITY
        val delayMs = if (isSlow) {
            Random.nextLong(SLOW_AUTH_MIN_MS, SLOW_AUTH_MAX_MS)
        } else {
            Random.nextLong(FAST_AUTH_MIN_MS, FAST_AUTH_MAX_MS)
        }
        val scenario = if (isSlow) "slow" else "fast"

        Embrace.addSessionProperty("startup_scenario", scenario, false)

        val startMs = Embrace.getSdkCurrentTimeMs()
        Thread.sleep(delayMs)
        val endMs = Embrace.getSdkCurrentTimeMs()

        Embrace.addStartupTraceChildSpan(
            name = "auth-sdk",
            startTimeMs = startMs,
            endTimeMs = endMs,
            attributes = mapOf(
                "auth.scenario" to scenario,
                "auth.provider" to "demo-3p-auth",
            ),
            events = emptyList(),
            errorCode = null,
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
