# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

Sample Android shopping cart app instrumented with the Embrace SDK. Used to generate demo data in Embrace dashboards and demonstrate how to instrument an e-commerce app with Embrace.

## Build & Dev Commands

```bash
./gradlew assembleMockDebug          # Build mock debug APK
./gradlew installMockDebug           # Install on device/emulator
./gradlew test                       # Run all unit tests
./gradlew testMockDebugUnitTest      # Run unit tests for mock debug variant
./gradlew connectedMockDebugAndroidTest  # Instrumented tests (requires device/emulator)
./gradlew lint                       # Android Lint
./gradlew clean                      # Clean build
```

## Build Variants

Two flavor dimensions:
- **Environment:** `mock` (uses in-repo mock data/APIs via `BuildConfig.USE_MOCK`) and `prod` (real APIs)
- **Embrace:** `demo`, `demoNoAnr`, `europe`, `sandbox` (controls `TRIGGER_ANRS` flag and Embrace app ID)

Prefer `mockDebug` variants for local development and deterministic testing.

## Architecture

Clean Architecture (MVVM + Repository) in a single `:app` module under `io.embrace.shoppingcart`:

- **`domain/`** — Use cases, repository interfaces, domain models (Product, Order, User, Address, etc.)
- **`data/`** — Repository implementations, Room entities/DAOs (local), Retrofit DTOs (remote)
- **`presentation/`** — ViewModels exposing `StateFlow<UiState>`, shared composable components
- **`ui/`** — Jetpack Compose screens organized by feature (home, product, cart, checkout, profile, etc.), Material 3 theme
- **`di/`** — Hilt modules (AppModule, NetworkModule, DatabaseModule, RepositoryModule, etc.). Uses `@RealApi`/`@MockApi` qualifiers
- **`network/`** — Retrofit API service, OkHttp interceptors
- **`mock/`** — Mock API implementations for the `mock` flavor
- **`telemetry/`** — OpenTelemetry span exporter for Embrace SDK

Each feature has its own Activity as entry point with deep link support (`shopping://` scheme). Navigation within features uses Jetpack Navigation Compose.

## Key Technologies

- **Kotlin + Jetpack Compose** (Material 3, BOM-managed)
- **Hilt** for DI, scoped to `SingletonComponent`
- **Room** for local persistence, **Retrofit + Moshi** for networking
- **Coroutines + Flow** for async/state management
- **Embrace SDK** (`embrace-config.json` in `app/src/main/`) with OpenTelemetry export
- **Firebase** (Analytics, Crashlytics, Performance), **Stripe + Google Pay**, **Mixpanel**

## Testing

- Unit tests: JUnit 4, located in `app/src/test/`
- Instrumented tests: AndroidX + Compose UI Test in `app/src/androidTest/`. Key test classes cover checkout flows with different network scenarios (success, failure, slow, error) plus ANR and crash behavior tests
- Mock network scenarios are configurable via `MockNetworkConfigOverrides` and `UiTestOverrides`
- CI runs instrumented tests across API levels 31-34 with 4 flavor combinations

## Coding Conventions

- Kotlin, 4-space indent, no wildcard imports
- Classes: PascalCase; functions/properties: camelCase; constants: UPPER_SNAKE_CASE
- UI composables: `*Screen`/`*Card`; state holders: `*ViewModel`; business logic: `*UseCase`; data access: `*Repository`/`*Dao`
- Commits: Conventional Commits format (`feat(scope):`, `fix(scope):`, `chore:`, etc.)
