---
name: embrace-android-wrapper
description: Use when an Android developer asks for help building, designing, or refactoring a wrapper / facade / abstraction around the Embrace Android SDK (io.embrace.android.embracesdk.Embrace). Triggers on phrases like "wrap the Embrace SDK", "TelemetryService for Embrace", "abstract Embrace behind an interface", "facade for Embrace", "decouple our code from Embrace", or any request to centralize Embrace calls behind an interface so feature code stops importing Embrace directly.
---

# Embrace Android SDK — Wrapper Skill

Help an Android developer build a thin, testable wrapper around the Embrace Android SDK in their own codebase. The goal is **one interface their feature code holds**, with the Embrace SDK only imported in one place.

A worked, build-verified example exists in this repo at `app/src/main/java/io/embrace/shoppingcart/telemetry/`. Read those files when you need a concrete reference.

---

## Why customers wrap the SDK (motivate the work, don't just code)

Three reasons that justify the effort. If none apply, the wrapper may be over-engineering — say so.

1. **Testability.** Feature code holds an interface. Tests inject a `NoOp` impl. No real SDK in unit tests.
2. **No-op fallback.** Feature code can call telemetry at any point in the app lifecycle without guarding `if (Embrace.isStarted)`. The wrapper absorbs SDK readiness.
3. **Single point of change.** SDK upgrades, vendor swaps, instrumentation changes, naming-convention enforcement — all land in one file.

If the customer is shipping a tiny app with a handful of Embrace calls and no test suite, a wrapper is overkill. Don't push it.

---

## Phase 1 — Discovery (do BEFORE writing any code)

Read the customer's codebase first. Do not skip this — the helpers you generate must match the app's domain, or the wrapper is just busywork.

Run these in parallel:

1. **Find existing Embrace usage.**
   ```bash
   grep -rn "io.embrace.android.embracesdk\|Embrace\." --include="*.kt" --include="*.java" app/src/main/
   ```
   This tells you which SDK methods they actually use today and which features call them.

2. **Find their DI setup.** Look for `@Module` (Hilt), `module { ... }` (Koin), or none. The wrapper must be injectable in the customer's DI style.

3. **Find their domain.** Look at `domain/`, `data/`, ViewModels, repository interfaces. Identify 4–8 user-facing flows that deserve domain helpers (e.g. login, search, checkout, content view). Skip generic CRUD — those don't need helpers.

4. **Confirm the SDK version.** Read `gradle/libs.versions.toml` or `build.gradle(.kts)`. The API in this skill is for **Embrace Android SDK 8.x**. If they're on 7.x or earlier, some method signatures differ — flag it before writing code.

Report what you found in 4–6 bullets, then ask:
- Do they want a singleton, Hilt-bound, Koin-bound, or both?
- Should the wrapper enforce naming conventions (snake_case spans, dotted attribute keys), or pass through whatever callers send?
- Do they want domain helpers now, or just the primitive surface (logs/spans/breadcrumbs) and add domain helpers later?

If the user already gave clear answers, skip the questions — acknowledge and move on.

---

## Phase 2 — Design decisions

### What to expose

Mirror this surface (proven shape, matches the iOS wrapper at the company so dashboards work cross-platform):

- **Logs:** `logInfo` / `logWarning` / `logError` / `logException` — each takes `message: String, properties: Map<String, Any> = emptyMap()`.
  - Note: Android `Severity` has only INFO / WARNING / ERROR. **No DEBUG.** Don't expose `logDebug`.
- **Spans:**
  - `recordSpan(name, attributes) { block }` — preferred. Block-based, auto-ends.
  - `recordCompletedSpan(name, startTimeMs, endTimeMs, attributes, errorCode)` — past work / replay.
  - `startSpan(name): EmbraceSpan?` — manual start/stop. Last resort.
- **Breadcrumbs:** `addBreadcrumb(message)`.
- **Session properties:** `addSessionProperty(key, value, permanent: Boolean)` / `removeSessionProperty(key)`.
- **User identity:** `setUser(id, email = null, name = null)` / `clearUser()`. Document that email/name map to deprecated SDK methods and most apps should pass `null`.
- **Manual network capture:** `recordNetworkRequest(url, method, startTimeMs, endTimeMs, statusCode, bytesSent, bytesReceived, errorMessage, traceId, w3cTraceparent)`. Use only for transports the SDK can't auto-capture (gRPC, WebSocket, native HTTP).
- **Push:** `recordPushNotification(title, body, from, messageId, data)`. The SDK's `logPushNotification(...)` is **deprecated in 8.x** — emit a structured `logMessage` + breadcrumb instead.
- **Domain helpers** (the reason this wrapper exists): one method per important user flow. See "Domain helper template" below.

### What NOT to expose

- `logDebug` — no DEBUG severity on Android.
- `personas` — controversial; many customers find them confusing. Add only if asked.
- `endSession`, `applicationInitStart/End`, `disable`, `startView/endView` — leak SDK lifecycle into feature code. Keep these in `Application.onCreate` directly.
- `getSdkCurrentTimeMs`, `currentSessionId`, `deviceId`, `lastRunEndState` — utility/debug surface, not telemetry. Skip unless the customer specifically needs them.
- `addJavaSpanExporter`, `getJavaOpenTelemetry` — advanced OTel surface. Keep at SDK init; not part of the wrapper.

### Architecture

```
telemetry/
  TelemetryService.kt           — interface
  EmbraceTelemetryService.kt    — production impl (delegates to Embrace)
  NoOpTelemetryService.kt       — for tests / disabled builds
di/ (or wherever DI lives)
  TelemetryModule.kt            — Hilt @Binds (or Koin module, etc.)
```

- Make the impl a `class` with no constructor params, annotated `@Singleton` and `@Inject constructor()` for Hilt. Add a `companion object { val instance by lazy { ... } }` so non-DI code (Application, Composables) can also use it.
- `NoOpTelemetryService` should be an `object`. Methods do nothing except `recordSpan { block() }` which still runs the block (preserve the production code path).
- `Embrace.start(context)` stays in `Application.onCreate` — the wrapper does NOT own SDK lifecycle.

---

## Phase 3 — Write

### Embrace Android SDK 8.x — verified API surface

This is what the impl actually calls. Imports:
```kotlin
import io.embrace.android.embracesdk.Embrace
import io.embrace.android.embracesdk.Severity
import io.embrace.android.embracesdk.network.EmbraceNetworkRequest
import io.embrace.android.embracesdk.network.http.HttpMethod
import io.embrace.android.embracesdk.spans.EmbraceSpan
import io.embrace.android.embracesdk.spans.ErrorCode
```

| Capability | Embrace API |
|---|---|
| Log with severity + properties | `Embrace.logMessage(message, Severity.INFO, properties)` (`Map<String, Any>`) |
| Log exception | `Embrace.logException(throwable, Severity.ERROR, properties, message?)` |
| Block-based span | `Embrace.recordSpan(name, attributes = ..., code = block)` — note: block does **not** receive a span |
| Completed span | `Embrace.recordCompletedSpan(name, startTimeMs, endTimeMs, errorCode, attributes = ...)` |
| Start manual span | `Embrace.startSpan(name)` returns `EmbraceSpan` (caller calls `.stop()`) |
| Breadcrumb | `Embrace.addBreadcrumb(message)` |
| Session property | `Embrace.addSessionProperty(key, value, permanent)` returns `Boolean` |
| User id | `Embrace.setUserIdentifier(id)` / `clearUserIdentifier()` |
| User email/name (DEPRECATED) | `setUserEmail` / `setUsername` / `clearUserEmail` / `clearUsername` — wrap with `@Suppress("DEPRECATION")` |
| Network — completed | `EmbraceNetworkRequest.fromCompletedRequest(url, httpMethod, start, end, bytesSent, bytesReceived, statusCode, traceId, w3cTraceparent)` then `Embrace.recordNetworkRequest(...)` |
| Network — failed | `EmbraceNetworkRequest.fromIncompleteRequest(url, httpMethod, start, end, errorType, errorMessage, traceId, w3cTraceparent)` |
| SDK time | `Embrace.getSdkCurrentTimeMs()` — use this for span start/end times |
| SDK started? | `Embrace.isStarted: Boolean` |

**`ErrorCode` enum:** `FAILURE`, `USER_ABANDON`, `UNKNOWN`. Use `FAILURE` for actual failures, `USER_ABANDON` for user-cancelled flows (login give-up, checkout abandon).

**`Severity` enum:** `INFO`, `WARNING`, `ERROR`. No DEBUG.

### Domain helper template

Each helper composes span + breadcrumb + log into a single call. Example shape:

```kotlin
override fun trackPurchaseAttempt(orderId: String, totalCents: Long, itemCount: Int) {
    recordSpan(
        name = "purchase_attempt",
        attributes = mapOf(
            "order.id" to orderId,
            "order.total_cents" to totalCents.toString(),
            "order.item_count" to itemCount.toString(),
        ),
    ) {
        addSessionProperty("current_order_id", orderId, permanent = false)
        trackUserAction(
            action = "purchase_attempt",
            screen = "checkout",
            properties = mapOf("order_id" to orderId, "total_cents" to totalCents),
        )
    }
}
```

For failure helpers, use `recordCompletedSpan(..., errorCode = ErrorCode.FAILURE)` so the dashboard sees the failure. A successful span with an error log next to it is invisible to most charts.

### Naming conventions (enforced inside the wrapper, not at call sites)

- Span names: lowercase snake_case. `purchase_attempt`, never `purchaseAttempt` or `Purchase Attempt`.
- Span names are **low cardinality**. `product_view`, never `product_view_<productId>`.
- Attribute keys are dotted, OTel-style: `order.id`, `cart.quantity`, `http.status_code`.
- Log messages are low-cardinality strings ("Purchase failed"). Put IDs in `properties`, not in the message.

### Manual network capture — pick the right factory

```kotlin
val request = if (errorMessage != null) {
    EmbraceNetworkRequest.fromIncompleteRequest(
        url, httpMethod, startTimeMs, endTimeMs,
        errorType = errorMessage::class.java.simpleName,
        errorMessage = errorMessage,
        traceId = traceId,
        w3cTraceparent = w3cTraceparent,
    )
} else {
    EmbraceNetworkRequest.fromCompletedRequest(
        url, httpMethod, startTimeMs, endTimeMs,
        bytesSent, bytesReceived,
        statusCode = statusCode ?: 0,
        traceId = traceId,
        w3cTraceparent = w3cTraceparent,
    )
}
Embrace.recordNetworkRequest(request)
```

### Forcing a crash (demo apps only)

There is **no `Embrace.crash()` on Android.** To force a session-associated crash for a demo, throw an unhandled `RuntimeException`. Embrace's `UncaughtExceptionHandler` picks it up. To get distinct crash groups in the dashboard, put each crash in its own private function — separate functions yield separate stack traces.

---

## Anti-patterns to flag if you see them

When reviewing the customer's existing code (or your own draft), call out these:

- **Storing an `EmbraceSpan` on a ViewModel / repository / view** and stopping it later. Use `recordSpan { ... }` instead — there's no way to forget to stop it.
- **High-cardinality span/log/attribute names** — user IDs, full URLs, timestamps in span names. Split: keep the name low-cardinality and put the variable part in attributes.
- **Templated log messages** — `logError("Purchase failed for order $orderId")`. The dashboard can't aggregate. Use `logError("Purchase failed", mapOf("order.id" to orderId))`.
- **`Embrace.recordNetworkRequest(...)` for a request that already went through OkHttp / HttpURLConnection.** The SDK's interceptor already captured it — you'll double-record.
- **Logging in a tight loop** — per frame, per list item, per-recomposition in a Composable. Throttle or move to a span around the whole batch.
- **Treating failures as success spans with an error log next to them** — use `errorCode = ErrorCode.FAILURE` so the failure shows in the dashboard.
- **Setting a session property per item / per response** — there's a ~100-property cap. Use breadcrumbs instead.
- **Wrapping every single call site in if (Embrace.isStarted)** — the SDK no-ops gracefully when not started; the wrapper handles this.

---

## Verifying the work

After writing the wrapper:

1. **Compile.** Run the customer's Kotlin compile task for one variant (e.g. `./gradlew :app:compileDebugKotlin`). If they use Hilt, also assemble (`./gradlew :app:assembleDebug`) so KSP/kapt validates the `@Binds`.
2. **No call site refactor in the same change.** Adding the wrapper and migrating call sites are two separate PRs. Migrating is mechanical; design pushback on the wrapper shape stalls the migration if they're combined.
3. **Verify the test path.** Write or point to one example test that injects `NoOpTelemetryService`. If they have no test infrastructure, document it as a follow-up; don't try to set up testing as part of this work.

---

## Worked example

A complete, build-verified wrapper lives in this repo (when this skill is read from `embrace-ecommerce-android-demo`):

- `app/src/main/java/io/embrace/shoppingcart/telemetry/TelemetryService.kt` — the interface, with the 8 design principles documented at the top
- `app/src/main/java/io/embrace/shoppingcart/telemetry/EmbraceTelemetryService.kt` — production impl with domain helpers and 5 demo crash sites
- `app/src/main/java/io/embrace/shoppingcart/telemetry/NoOpTelemetryService.kt` — test impl
- `app/src/main/java/io/embrace/shoppingcart/di/TelemetryModule.kt` — Hilt binding

When the customer's stack diverges (Koin, manual DI, no domain helpers, etc.), use these files for the **primitive shape** (logs/spans/breadcrumbs/session/user/network/push) and adapt the domain helpers and DI wiring to their app.

---

## Cross-platform parity (call out only if relevant)

If the customer also has an iOS app, the equivalent wrapper there is `EmbraceService.swift`. Keeping the same helper names, span names, and attribute keys across platforms is the contract that lets one dashboard query work for both. Key Android-specific divergences to keep documented in the wrapper file itself:

- No `logDebug` (no DEBUG severity on Android)
- No `process`-scoped session properties (only session and permanent)
- No `Embrace.crash()` (use thrown exception)
- `recordSpan` block does not receive the span
- `setUserEmail` / `setUsername` are deprecated
- `logPushNotification(...)` is deprecated
