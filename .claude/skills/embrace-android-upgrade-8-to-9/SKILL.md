---
name: embrace-android-upgrade-8-to-9
description: Guide an Android developer through upgrading the Embrace Android SDK from 8.x to 9.x — bump the version, find and migrate the renamed session APIs (addSessionProperty, removeSessionProperty, endSession, getCurrentSessionId), and verify the build. Use when the user says things like "upgrade Embrace to 9", "migrate Embrace Android SDK from 8 to 9", "Embrace 9.x breaking changes", "Embrace session API changed", or when a build.gradle / version catalog shows io.embrace:embrace-android-sdk on an 8.x version and the user wants to move to 9.x.
---

# Embrace Android SDK: 8.x → 9.x upgrade

Version 9.0.0 of the Embrace Android SDK is a **small, focused breaking change**: it renames the session-related
methods on `Embrace` so they can support a redefinition of what a "Session" is (see "Why this upgrade exists"
below). Nothing else from 8.x changes — the Gradle plugin, module names, minimum Gradle/AGP/Kotlin/JDK versions,
and every non-session API are untouched between 8.x and 9.x.

This means the upgrade is mechanical and low-risk: bump the version, fix a handful of renamed method calls, done.

**Scope check before you start:** this skill assumes the app is already on Embrace Android SDK **8.x**. If it's
still on 7.x or earlier, that's a bigger jump (Gradle plugin rename, new minimum Gradle/AGP/Kotlin/JDK versions,
several removed APIs) — point the user to the "Upgrading from 7.x to 8.x" section of
[the upgrade guide](https://embrace.io/docs/android/upgrading/) first, and treat 8.x→9.x as a second, separate step.

## Why this upgrade exists (plain language, for explaining to a customer)

Embrace is evolving what a "Session" means. Today, a session ends the moment an app is backgrounded (it becomes a
"Background Activity" instead). The new model unifies these into a single, longer-lived "Session" that only ends
after real inactivity, a max-duration timeout, or an explicit manual call — backgrounding/foregrounding no longer
splits it. The old session/background-activity concept still exists underneath, renamed to "Session Part."

9.0.0 ships the **API surface** for this new model, but not the behavior change itself:
- No dashboard UI changes ship with 9.0.0. Timelines, session lists, etc. look identical.
- The backend work to actually stitch Session Parts together into the new Session concept had not shipped as of
  9.0.0 (per internal roadmap) — so upgrading now is safe, forward-compatible groundwork, not a functional change.
- Because the affected methods were **renamed** (not kept under the same name with new behavior), any app that
  compiles against 9.x and didn't change its session-related code will not have silently opted into anything new.
  If your code doesn't call the old session methods at all, this upgrade needs zero code changes beyond the
  version bump.

Source: `docs/android/upgrading.md` ("Upgrading from 8.x to 9.x" section, embrace-docs repo) + Notion "Android
9.0.0" release page.

## Step-by-step

### 1. Confirm the starting point

Find the current SDK version — search for `io.embrace:embrace-android-sdk` (or the `embrace` version alias) in:
- `gradle/libs.versions.toml` (version catalog)
- `app/build.gradle` / `app/build.gradle.kts`, or wherever the dependency is declared

If it's not on 8.x, see the scope check above.

### 2. Bump the version

Update the version reference to the target 9.x release (check
[the changelog](https://embrace.io/docs/android/changelog/) for the latest 9.x version and read its entry — it's
possible a 9.0.x patch fixes something relevant to this app).

Run a Gradle sync. No plugin ID, artifact ID, or module names changed in 9.x, so this step alone should not
produce new errors — if it does, something else in the project changed too, so investigate separately from this
upgrade.

### 3. Find every call site that uses the old session APIs

Grep the project (Kotlin and Java, both `Embrace.getInstance().xxx(...)` and bare `Embrace.xxx(...)` call styles)
for:

```
addSessionProperty(
removeSessionProperty(
endSession(
getCurrentSessionId(
currentSessionId
```

The last one matters for Kotlin call sites: `getCurrentSessionId()`/`getCurrentUserSessionId()` are Kotlin
properties under the hood (`currentSessionId` / `currentUserSessionId`), so Kotlin code may read `Embrace.currentSessionId`
rather than calling `getCurrentSessionId()` — a plain function-call grep will miss those sites.

Every match here is a guaranteed compile error after the version bump — these four properties/methods were
removed, not deprecated-and-kept. There is no other Embrace session API affected by this upgrade.

### 4. Migrate each call site

| Old API (8.x) | New API (9.x) | Notes |
|---|---|---|
| `addSessionProperty(key: String, value: String, permanent: Boolean): Boolean` | `addUserSessionProperty(key: String, value: String, scope: PropertyScope): Boolean` | See scope mapping below — this is not a pure rename, the boolean becomes a 3-way enum. |
| `removeSessionProperty(key: String): Boolean` | `removeUserSessionProperty(key: String): Boolean` | Pure rename, same behavior. |
| `endSession(clearUserInfo: Boolean)` | `endUserSession()` | The boolean is gone. See "Replacing clearUserInfo" below. |
| `getCurrentSessionId(): String?` | `getCurrentUserSessionId(): String?` | **Not a pure rename — the returned value behaves differently.** In 8.x, `getCurrentSessionId()` changed value when the app backgrounded (the old Session ended and a distinct Background Activity ID took over). In 9.x, `getCurrentUserSessionId()` stays the **same** across a foreground → background → foreground cycle, since it now tracks the new Session concept rather than the old Session/Background Activity ("Session Part"). If the app logs or correlates telemetry using this ID, expect fewer ID changes after upgrading — this is a real, user-visible behavior change that the public docs don't spell out explicitly (they only note the ID "refers to a different concept"). Confirmed via the SDK's own integration tests: `PublicApiTest.kt` asserts `assertNotEquals(foregroundSessionId, backgroundSessionId)` at the 8.4.0 tag vs. `assertEquals(foregroundUserSessionId, backgroundUserSessionId)` on 9.x (same file, lines ~68-82). |

New, additive API (no old equivalent, nothing to migrate — just useful to know about):

- `Embrace.addUserSessionListener(listener: UserSessionListener)` — fires `SessionStateEvent.UserSessionActive` /
  `SessionStateEvent.UserSessionEnded`, each exposing a `userSessionId: String` property.

#### PropertyScope mapping (for `addSessionProperty` → `addUserSessionProperty`)

| Old `permanent` value | New `PropertyScope` | Behavior |
|---|---|---|
| `true` | `PropertyScope.PERMANENT` | Survives process death; applies to all future sessions until explicitly removed. Exact equivalent. |
| `false` | `PropertyScope.USER_SESSION` | Cleared when the current session ends. Exact equivalent — this is what `permanent = false` already did. |
| _(no old equivalent)_ | `PropertyScope.PROCESS` | Cleared when the process dies, but **survives across session boundaries** as long as the process is alive. This is a genuinely new middle tier — worth pointing out to the developer, since it's often what they actually wanted when they picked `permanent = true` just to avoid re-setting a property after every backgrounding cycle, without needing it to persist across app restarts. |

Don't do a blind find-and-replace of `true`→`PERMANENT` / `false`→`USER_SESSION` without asking whether `PROCESS`
is actually the better fit for that particular property — that's the one place this "rename" has real judgment
involved.

#### Replacing `clearUserInfo` (for `endSession(Boolean)` → `endUserSession()`)

- If the app always called `endSession(false)` (or the no-arg legacy overload): just call `endUserSession()`.
  Nothing else to do.
- If the app called `endSession(true)` to clear user info on session end: there's no direct equivalent parameter
  anymore. Register a `UserSessionListener` and clear user info explicitly when a `SessionStateEvent.UserSessionEnded`
  event fires:

```kotlin
Embrace.addUserSessionListener { event ->
    if (event is SessionStateEvent.UserSessionEnded) {
        Embrace.clearUserIdentifier()
        Embrace.clearUsername()
        Embrace.clearUserEmail()
        Embrace.clearAllUserPersonas()
    }
}

// wherever endSession(true) used to be called:
Embrace.endUserSession()
```

  (`UserSessionListener` is a Kotlin `fun interface`, so the lambda form above works directly — no need for the
  more verbose anonymous-object syntax shown in the public docs.)

Also note: `endUserSession()` ends both the new Session and the current Session Part — the old method could only
ever end the Session Part (a full "Session" as we now define it didn't exist as an addressable concept in 8.x).

### 5. Rebuild and fix remaining errors

Recompile. If you still see errors referencing symbols beyond the four in the table above, this app is likely
also picking up unrelated 7.x→8.x leftovers or an unrelated dependency conflict — don't assume it's part of the
9.x migration.

### 6. Verify

Install the app, trigger a session, and confirm it still shows up normally on the Embrace dashboard. Since 9.0.0
doesn't change dashboard behavior, a successful session with no visual differences is the expected, correct outcome.

## Known gotcha: don't assume every 9.x release has an identical API surface

This guide covers the actual 9.0.0 release. If the app is targeting a later 9.x patch (e.g. 9.1.x), do a quick
sanity check rather than assuming the API surface is frozen — it briefly wasn't: `Embrace.observeNavigation(Activity, Object)`
(an unrelated, unreleased-in-8.x navigation-tracking API) was removed right before the 9.0.0 tag and restored
afterwards on `main`. It has nothing to do with the session migration above, but it's a concrete example of why
"upgrading to 9.x" should mean checking the changelog for the *specific* target version, not just this guide.

## Quick checklist to give the user

- [ ] Confirm current version is 8.x (not 7.x — different guide applies)
- [ ] Bump `io.embrace:embrace-android-sdk` (and Gradle plugin, if version-pinned separately) to the target 9.x version
- [ ] Gradle sync succeeds
- [ ] Grep for `addSessionProperty(`, `removeSessionProperty(`, `endSession(`, `getCurrentSessionId(`
- [ ] Migrate each hit per the table above, choosing `PropertyScope` deliberately (not just 1:1 boolean mapping)
- [ ] If any `endSession(true)` call sites existed, add a `UserSessionListener` to replicate the user-info clearing
- [ ] Project compiles with no remaining Embrace-related errors
- [ ] Test build installed, session sent, confirmed visible on dashboard

## Sources

- `embrace-docs/docs/android/upgrading.md` — "Upgrading from 8.x to 9.x" section (public upgrade guide)
- `embrace-docs/docs/android/changelog.md` — 9.0.0 changelog entry
- `embrace-android-sdk` repo, verified directly against the public API bytecode dump (confirms the guide is
  accurate and the old methods are fully removed, not deprecated-and-kept):
  - `embrace-android-sdk/api/embrace-android-sdk.api`
  - `embrace-android-api/api/embrace-android-api.api`
  - `embrace-android-api/src/main/kotlin/io/embrace/android/embracesdk/PropertyScope.kt`
  - `embrace-android-api/src/main/kotlin/io/embrace/android/embracesdk/SessionStateEvent.kt`
  - `embrace-android-api/src/main/kotlin/io/embrace/android/embracesdk/UserSessionListener.kt`
  - `embrace-android-api/src/main/kotlin/io/embrace/android/embracesdk/internal/api/UserSessionApi.kt` (KDoc source
    for the `PROCESS` scope nuance, which isn't spelled out in the public docs)
  - `embrace-android-sdk/src/integrationTest/kotlin/io/embrace/android/embracesdk/testcases/PublicApiTest.kt`
    (integration tests proving `getCurrentUserSessionId` is stable across backgrounding, unlike its 8.x predecessor)
- Notion: "Android 9.0.0" release page, "Redefine Sessions SDK Spec" (internal context on why the session model
  is changing — useful for explaining the "why" to a customer, not required for the mechanical upgrade itself)
