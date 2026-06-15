package io.embrace.shoppingcart.telemetry

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.ErrorCode
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

// =============================================================================
// SPAN LIFECYCLE HELPER — the typed handle that makes the common span failure
// modes hard to hit (Wrapper Use Cases §4 "Span lifecycle helpers" and §8
// "Holding strong references to spans").
//
// THE THREE FAILURE MODES THIS COVERS:
//   1. Abandonment   — a span started on a screen the user backgrounds away
//                      from is never stopped, so it never reaches the
//                      dashboard. `endOnBackground = true` closes it with
//                      USER_ABANDON when the app leaves the foreground.
//   2. Hangs         — work that never completes leaves the span open
//                      forever. `timeoutMs` closes it with FAILURE so the
//                      hang is VISIBLE in the dashboard instead of missing.
//   3. Double-stop / stop-after-owner-death — `stop()` is idempotent. The
//                      first close (caller, timeout, or background) wins;
//                      later calls no-op.
//
// OWNERSHIP RULE (§8 "Ending spans you don't own"): the creator of an
// ActiveSpan is its only owner. Pass the VALUE of attributes onward, never
// the handle — code that receives a span it didn't start must not stop it.
// This mirrors Strava's TraceSpan → ActiveSpan split, where helpers that
// receive a parent span literally cannot call stop() on it.
//
// PREFERENCE ORDER for spans is unchanged:
//   recordSpan { }  >  recordCompletedSpan(...)  >  startActiveSpan(...)
// Reach for this handle only when start and end live in different functions
// and a block can't bridge them (e.g. checkout begins on tap, ends on a
// callback three layers away).
// =============================================================================

interface ActiveSpan {
    /** Adds an attribute. No-op once the span is stopped. */
    fun addAttribute(key: String, value: String)

    /**
     * Stops the span. Idempotent — the first close wins (caller, timeout, or
     * background abandonment), later calls no-op. Pass an [ErrorCode] to mark
     * failure; null means success.
     */
    fun stop(errorCode: ErrorCode? = null)
}

/** Returned when the SDK is disabled — feature code never null-checks. */
internal object NoOpActiveSpan : ActiveSpan {
    override fun addAttribute(key: String, value: String) {}
    override fun stop(errorCode: ErrorCode?) {}
}

internal class EmbraceActiveSpan(
    private val span: EmbraceSpan,
    timeoutMs: Long?,
    endOnBackground: Boolean,
) : ActiveSpan {

    private val stopped = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    private var timeoutRunnable: Runnable? = null
    private var backgroundObserver: LifecycleEventObserver? = null

    init {
        if (timeoutMs != null) {
            val runnable = Runnable {
                if (stopInternal(ErrorCode.FAILURE)) {
                    Timber.w(
                        "Span '%s' hit its %dms timeout and was closed as FAILURE.",
                        span.spanId,
                        timeoutMs,
                    )
                }
            }
            timeoutRunnable = runnable
            handler.postDelayed(runnable, timeoutMs)
        }
        if (endOnBackground) {
            // ProcessLifecycleOwner observers must be added on the main thread.
            handler.post {
                if (stopped.get()) return@post
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        stopInternal(ErrorCode.USER_ABANDON)
                    }
                }
                backgroundObserver = observer
                ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            }
        }
    }

    override fun addAttribute(key: String, value: String) {
        if (stopped.get()) return
        span.addAttribute(key, value)
    }

    override fun stop(errorCode: ErrorCode?) {
        stopInternal(errorCode)
    }

    /** Returns true if this call performed the stop. */
    private fun stopInternal(errorCode: ErrorCode?): Boolean {
        if (!stopped.compareAndSet(false, true)) return false
        if (errorCode != null) span.stop(errorCode) else span.stop()
        cleanUp()
        return true
    }

    private fun cleanUp() {
        timeoutRunnable?.let(handler::removeCallbacks)
        timeoutRunnable = null
        // Observer removal must also happen on the main thread.
        handler.post {
            backgroundObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
            backgroundObserver = null
        }
    }
}
