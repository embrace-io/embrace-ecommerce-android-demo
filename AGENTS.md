# Repository Guidelines

## Project Structure & Modules
- Root Gradle project with single Android app module `app`.
- Source: `app/src/main/java/io/embrace/shoppingcart` organized by layer: `data/`, `domain/`, `presentation/`, `ui/`, `network/`, `di/`.
- Resources & assets: `app/src/main/res/` and `app/src/main/assets/` (e.g., `products.json`, `categories.json`).
- Tests: unit in `app/src/test/...`, instrumented in `app/src/androidTest/...`.
- Flavors: `mock` (uses in-repo mocks) and `prod`; flag via `BuildConfig.USE_MOCK`.

## Build, Test, and Dev Commands
- Build (all): `./gradlew assemble`
- Build mock debug APK: `./gradlew assembleMockDebug`
- Install on device/emulator: `./gradlew installMockDebug`
- Unit tests: `./gradlew test` or `./gradlew testMockDebugUnitTest`
- Instrumented tests: `./gradlew connectedMockDebugAndroidTest` (device/emulator required)
- Lint (Android Lint): `./gradlew lint`
- Clean: `./gradlew clean`

## Coding Style & Naming
- Language: Kotlin + Jetpack Compose. Indent 4 spaces, no wildcard imports.
- Packages reflect layers (`data`, `domain`, `presentation`, `ui`, `di`, `network`).
- Classes: PascalCase (`CartRepositoryImpl`, `HomeViewModel`); functions/props: camelCase; constants: UPPER_SNAKE_CASE.
- UI composables end with `Screen`/`*Card`; state holders end with `ViewModel`; business logic with `UseCase`; data access with `Repository`/`Dao`.

## Testing Guidelines
- Frameworks: JUnit4 (unit), AndroidX Instrumentation + Compose UI test (androidTest).
- Naming: mirror class under test, suffix with `Test` (e.g., `CartRepositoryImplTest`).
- Scope: unit tests for `domain` use cases and repositories; UI tests for key screens and flows.
- Run locally with commands above; prefer `mockDebug` variant for deterministic tests.

## Commit & Pull Requests
- Use Conventional Commits: `feat(scope): ...`, `fix(scope): ...`, `chore: ...`, `docs: ...`, `refactor: ...` (examples in git history, e.g., `feat(cart): ...`).
- PRs must include: clear description, linked issue, screenshots/GIFs for UI changes, test plan (commands, variant), and notes on affected flavors.

## Security & Config
- Do not commit secrets. Place Firebase `google-services.json` in `app/` if used.
- Embrace config lives at `app/src/main/embrace-config.json`; keep environment-specific values out of VCS.
