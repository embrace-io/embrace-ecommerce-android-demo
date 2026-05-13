# Add Embrace Span Around a Feature

Instrument a feature in this app with an Embrace span that captures its full lifecycle: user action, async operations (network, database), and UI state update.

## Instructions

1. **Identify the feature** the user wants to instrument. Read the relevant ViewModel(s), repository calls, network calls, and UI composables to understand the full flow.

2. **Determine the phases** of the feature. A typical flow in this app follows:
   - **Trigger**: User action (button tap) → ViewModel function called
   - **Async work**: Network requests, database reads/writes, business logic
   - **Completion**: UI state updated, spinner removed, result rendered

3. **Create the span hierarchy**:
   - A **parent span** covering the entire operation, named after the feature (e.g., `"add-to-cart"`, `"place-order"`, `"search-products"`)
   - **Child spans** for each distinct phase (e.g., `"<feature>-network"`, `"<feature>-db-write"`, `"<feature>-render"`)
   - Add **attributes** with relevant context (IDs, names, quantities — whatever makes the span filterable/useful in the Embrace dashboard)
   - On failure, stop the parent span with `ErrorCode.FAILURE`

4. **Apply the instrumentation** in the ViewModel function that orchestrates the feature. If the same feature is triggered from multiple ViewModels, instrument all of them.

## Embrace Span API Reference

Use the `Embrace` singleton directly (do NOT use `Embrace.getInstance()`):

```kotlin
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.spans.ErrorCode

// Create and start a span
val parentSpan = Embrace.startSpan("feature-name")

// Create a child span linked to parent
val childSpan = Embrace.startSpan("feature-name-phase", parent = parentSpan)

// Add attributes (all values must be String)
parentSpan?.addAttribute("key", "value")

// Add events (point-in-time markers within a span)
parentSpan?.addEvent("event-name", attributes = mapOf("detail" to "value"))

// Record an exception on a span
parentSpan?.recordException(exception, attributes = mapOf("context" to "value"))

// Stop successfully
childSpan?.stop()

// Stop with error
parentSpan?.stop(ErrorCode.FAILURE)

// ErrorCode options: FAILURE, USER_ABANDON, UNKNOWN
```

### Alternative: `recordCompletedSpan` for already-timed operations

```kotlin
Embrace.recordCompletedSpan(
    name = "feature-name",
    startTimeMs = startTime,
    endTimeMs = endTime,
    attributes = mapOf("key" to "value"),
    events = listOf(
        EmbraceSpanEvent.create("event-name", timestampMs, attributes)!!
    )
)
```

## Implementation Pattern

```kotlin
fun doFeature(/* params */) {
    viewModelScope.launch {
        val parentSpan = Embrace.startSpan("feature-name")
        parentSpan?.addAttribute("relevant.id", id)

        try {
            // Phase 1: e.g., network call
            val networkSpan = Embrace.startSpan("feature-name-network", parent = parentSpan)
            val result = repository.fetchSomething()
            networkSpan?.stop()

            // Phase 2: e.g., local persistence
            val dbSpan = Embrace.startSpan("feature-name-db", parent = parentSpan)
            repository.save(result)
            dbSpan?.stop()

            // Phase 3: e.g., UI update
            val renderSpan = Embrace.startSpan("feature-name-render", parent = parentSpan)
            _state.update { it.copy(data = result, isLoading = false) }
            renderSpan?.stop()

            parentSpan?.stop()
        } catch (e: CancellationException) {
            // User navigated away or operation was cancelled
            parentSpan?.stop(ErrorCode.USER_ABANDON)
            throw e // must rethrow to preserve structured concurrency
        } catch (e: Exception) {
            parentSpan?.recordException(e)
            parentSpan?.stop(ErrorCode.FAILURE)
            // ... existing error handling ...
        }
    }
}
```

## Span Limits (do not exceed)

- Max 500 spans per session
- Max 100 attributes per span
- Max 10 events per span
- Key names: max 128 chars, alphanumeric ASCII, no `emb-` or `emb.` prefix
- Values: max 1024 chars

## Rules

- Use `Embrace.startSpan()` — NEVER `Embrace.getInstance().startSpan()`
- Always null-safe chain span calls (`span?.stop()`) — spans return null if SDK isn't started
- All attribute values must be Strings
- Do NOT remove existing Embrace instrumentation (breadcrumbs, `recordNetworkRequest`, etc.) — spans are additive
- Import `io.embrace.android.embracesdk.Embrace`, `io.embrace.android.embracesdk.spans.ErrorCode`, and `kotlin.coroutines.cancellation.CancellationException`
- Always catch `CancellationException` separately BEFORE the generic `Exception` catch, stop the span with `ErrorCode.USER_ABANDON`, and **rethrow it** — swallowing it breaks structured concurrency
- Match existing code style: 4-space indent, no wildcard imports
- Name spans with lowercase kebab-case (e.g., `"add-to-cart"`, `"load-products"`)
- Choose meaningful attribute keys that are useful for filtering in the Embrace dashboard
