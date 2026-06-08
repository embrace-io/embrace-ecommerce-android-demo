package io.embrace.shoppingcart.telemetry

import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// =============================================================================
// GUARDRAILS — naming conventions and rate limits, ENFORCED, not just
// documented (Wrapper Use Cases §4 "Naming conventions and character limits —
// enforce in the wrapper" and §8 "Logging in tight loops").
//
// The SDK already truncates silently at its own limits (span name 128 chars,
// attribute key 128, attribute value 1024, 100 custom attributes per span —
// see OtelLimitsConfig in the Android SDK). Silent truncation is the worst
// outcome: the dashboard shows a name nobody wrote and queries stop matching.
// The wrapper validates BEFORE the SDK does, warns loudly in debug builds,
// and keeps the data queryable.
//
// WHAT GETS ENFORCED
//   - snake_case, low-cardinality names: `purchase_attempt`, never
//     `purchaseAttempt`, never `purchase_attempt_<orderId>`.
//   - Length limits matching the SDK's so truncation happens here, visibly,
//     instead of inside the SDK, silently.
//   - A per-minute log budget so a log call inside a per-frame or per-item
//     loop degrades gracefully instead of flooding the session payload.
// =============================================================================

internal object TelemetryGuardrails {

    // Limits mirror the Embrace Android SDK's OtelLimitsConfig. If an SDK
    // upgrade changes them, this is the single place to update.
    const val MAX_NAME_LENGTH = 128
    const val MAX_ATTRIBUTE_KEY_LENGTH = 128
    const val MAX_ATTRIBUTE_VALUE_LENGTH = 1024
    const val MAX_CUSTOM_ATTRIBUTES = 100

    // snake_case, starts with a letter, digits/underscores allowed after.
    private val VALID_NAME = Regex("^[a-z][a-z0-9_]*$")

    // Names that smell like high cardinality: trailing ID-ish digit runs,
    // UUIDs, anything with a dot-separated number. Heuristic, not a proof —
    // it exists to catch `product_view_12345` at development time.
    private val HIGH_CARDINALITY_HINT = Regex("[0-9]{4,}|[0-9a-f]{8}-[0-9a-f]{4}")

    /**
     * Validates and sanitizes a span/log name. Never throws — telemetry must
     * not crash the app — but warns via Timber so misuse is caught while
     * developing, and truncates to the SDK limit so what reaches the
     * dashboard is what this code decided, not a silent SDK cut.
     */
    fun sanitizeName(name: String): String {
        if (!VALID_NAME.matches(name)) {
            Timber.w(
                "Telemetry name '%s' violates naming convention (lowercase snake_case). " +
                    "Fix the call site — names live in the wrapper, not in features.",
                name,
            )
        }
        if (HIGH_CARDINALITY_HINT.containsMatchIn(name)) {
            Timber.w(
                "Telemetry name '%s' looks high-cardinality (embedded ID?). " +
                    "Move the ID to an attribute or the name will explode aggregation.",
                name,
            )
        }
        return if (name.length > MAX_NAME_LENGTH) {
            Timber.w("Telemetry name '%s' exceeds %d chars; truncating.", name, MAX_NAME_LENGTH)
            name.take(MAX_NAME_LENGTH)
        } else {
            name
        }
    }

    /**
     * Truncates attribute keys/values to SDK limits and drops attributes past
     * the per-span cap, warning on each adjustment.
     */
    fun sanitizeAttributes(attributes: Map<String, String>): Map<String, String> {
        if (attributes.isEmpty()) return attributes
        if (attributes.size > MAX_CUSTOM_ATTRIBUTES) {
            Timber.w(
                "Telemetry attributes exceed the SDK cap (%d > %d); dropping the excess.",
                attributes.size,
                MAX_CUSTOM_ATTRIBUTES,
            )
        }
        return attributes.entries
            .take(MAX_CUSTOM_ATTRIBUTES)
            .associate { (key, value) ->
                val k = if (key.length > MAX_ATTRIBUTE_KEY_LENGTH) {
                    Timber.w("Attribute key '%s' too long; truncating.", key)
                    key.take(MAX_ATTRIBUTE_KEY_LENGTH)
                } else {
                    key
                }
                val v = if (value.length > MAX_ATTRIBUTE_VALUE_LENGTH) {
                    Timber.w("Attribute value for '%s' too long; truncating.", key)
                    value.take(MAX_ATTRIBUTE_VALUE_LENGTH)
                } else {
                    value
                }
                k to v
            }
    }
}

/**
 * Fixed-window log rate limiter (Wrapper Use Cases §8).
 *
 * A log call inside a tight loop should degrade, not flood: within each
 * one-minute window the first [maxPerMinute] logs pass, the rest are dropped
 * and counted. When a new window opens, one warning summarizes what was
 * dropped — so the flood is visible in the dashboard without being the flood.
 */
internal class LogRateLimiter(private val maxPerMinute: Int) {

    private val windowStartMs = AtomicLong(0)
    private val countInWindow = AtomicInteger(0)
    private val droppedInWindow = AtomicInteger(0)

    /**
     * Returns true if the log should be emitted. When the previous window had
     * drops, [onWindowSummary] is invoked once with the dropped count.
     */
    fun tryAcquire(nowMs: Long, onWindowSummary: (dropped: Int) -> Unit): Boolean {
        val start = windowStartMs.get()
        if (nowMs - start >= WINDOW_MS) {
            if (windowStartMs.compareAndSet(start, nowMs)) {
                val dropped = droppedInWindow.getAndSet(0)
                countInWindow.set(0)
                if (dropped > 0) onWindowSummary(dropped)
            }
        }
        return if (countInWindow.incrementAndGet() <= maxPerMinute) {
            true
        } else {
            droppedInWindow.incrementAndGet()
            false
        }
    }

    private companion object {
        const val WINDOW_MS = 60_000L
    }
}
