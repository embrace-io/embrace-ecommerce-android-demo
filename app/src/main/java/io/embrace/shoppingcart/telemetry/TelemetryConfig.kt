package io.embrace.shoppingcart.telemetry

import kotlin.math.absoluteValue

// =============================================================================
// OPERATIONAL CONTROLS — kill switches, partial enablement, sampling.
//
// WHY THIS EXISTS
//   Once telemetry ships inside a release, the only way to turn a noisy or
//   broken signal off is a new app release — unless the wrapper is gated on
//   runtime config. This object is that gate. In a real app the values below
//   come from YOUR remote config system (Firebase Remote Config, LaunchDarkly,
//   in-house flags) fetched on the previous launch and read synchronously at
//   startup. The demo hardcodes them per build type.
//
// THE THREE CONTROLS (Wrapper Use Cases §6):
//   1. Kill switch        — `enabled = false` turns the whole wrapper into a
//                           no-op without shipping an update.
//   2. Partial enablement — spans/logs/breadcrumbs/network can be disabled
//                           independently, e.g. kill a log flood while spans
//                           keep flowing.
//   3. Sampling           — deterministic per-install rollout. The same
//                           install always lands in the same bucket, so a
//                           "sample 10% of devices" rollout is stable across
//                           launches (no flapping in/out of capture).
//
// DEBUG vs RELEASE (Wrapper Use Cases §2):
//   Debug builds get a higher log budget (you're actively poking the app) and
//   are exempt from sampling. If you prefer NO telemetry from debug builds at
//   all — so dev noise never pollutes production dashboards — flip `enabled`
//   to false in [forBuild] or bind NoOpTelemetryService for debug variants.
// =============================================================================

data class TelemetryConfig(
    /** Global kill switch. False → the wrapper no-ops everything, SDK never starts. */
    val enabled: Boolean = true,

    /** Partial enablement: gate each signal type independently. */
    val spansEnabled: Boolean = true,
    val logsEnabled: Boolean = true,
    val breadcrumbsEnabled: Boolean = true,
    val networkCaptureEnabled: Boolean = true,

    /**
     * Percentage of installs (0–100) that capture telemetry. Evaluated once
     * per launch against a stable per-install bucket — see [isInstallSampled].
     * In production, drive this from remote config to ramp a version up or down.
     */
    val sampleRolloutPercent: Int = 100,

    /**
     * Log budget per minute (Wrapper Use Cases §8: "logging in tight loops —
     * the wrapper should rate-limit or refuse"). Logs beyond the budget are
     * dropped and counted; a summary warning is emitted once per window.
     */
    val maxLogsPerMinute: Int = 100,
) {

    /**
     * Deterministic sampling decision for this install.
     *
     * @param installId a stable per-install identifier (the wrapper persists
     * one in SharedPreferences — it deliberately does NOT use the Embrace
     * device ID, which is only available after the SDK starts, and starting
     * the SDK is exactly what this decision gates).
     */
    fun isInstallSampled(installId: String): Boolean {
        if (sampleRolloutPercent >= 100) return true
        if (sampleRolloutPercent <= 0) return false
        val bucket = installId.hashCode().absoluteValue % 100
        return bucket < sampleRolloutPercent
    }

    companion object {
        /**
         * Per-build-type defaults. Replace the body with a read from your
         * remote config cache to get true runtime kill switches.
         */
        fun forBuild(isDebug: Boolean): TelemetryConfig =
            if (isDebug) {
                TelemetryConfig(
                    // Debug: never sampled out, generous log budget for local work.
                    sampleRolloutPercent = 100,
                    maxLogsPerMinute = 600,
                )
            } else {
                TelemetryConfig()
            }
    }
}
