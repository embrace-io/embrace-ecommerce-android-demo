# Embrace Android SDK 8.x → 9.x Upgrade — Claude Code Skill

A Claude Code skill that walks a developer through upgrading the Embrace Android SDK from 8.x to 9.x: bumping the
version and migrating the handful of renamed session APIs that make up the entire breaking-change surface of 9.0.0.

## What it does

When a developer asks something like:

> "Help me upgrade Embrace Android SDK from 8 to 9"

The skill will:

1. Confirm the app is on Embrace Android SDK 8.x (and redirect to the 7.x→8.x guide first if not).
2. Bump the SDK version in the Gradle file / version catalog.
3. Grep the codebase for the four removed session methods (`addSessionProperty`, `removeSessionProperty`,
   `endSession`, `getCurrentSessionId`).
4. Migrate each call site to its 9.x replacement, including the non-trivial parts: choosing the right
   `PropertyScope` (not a blind boolean→enum mapping) and replacing `endSession(clearUserInfo = true)` with a
   `UserSessionListener` that explicitly clears user info.
5. Explain, in plain language, why the change exists (Embrace's session model redefinition) so the developer
   understands this is safe, forward-compatible groundwork with no dashboard-visible effect yet.

## Install

- **Project-scoped** (only available in this repo):
  ```bash
  mkdir -p .claude/skills
  cp -r path/to/embrace-android-upgrade-8-to-9-skill .claude/skills/embrace-android-upgrade-8-to-9
  ```
- **User-scoped** (available everywhere):
  ```bash
  mkdir -p ~/.claude/skills
  cp -r path/to/embrace-android-upgrade-8-to-9-skill ~/.claude/skills/embrace-android-upgrade-8-to-9
  ```

## Use it

Describe the upgrade in natural language — e.g. "upgrade Embrace to Android SDK 9", "migrate off Embrace 8.x
session APIs", "why won't my app compile after bumping Embrace to 9.0.0". You can also invoke it explicitly:

```
/embrace-android-upgrade-8-to-9
```

## Keeping this current

This skill was written against Embrace Android SDK 9.0.0 (the only 9.x release at the time of writing) and was
verified directly against the SDK's public API bytecode dump, not just the docs. If a later 9.x release adds more
breaking changes, update `SKILL.md`'s migration table and re-verify against
`embrace-android-sdk/api/embrace-android-sdk.api` and `embrace-android-api/api/embrace-android-api.api` in the
`embrace-android-sdk` repo (diff against the `9.0.0` git tag to isolate what's new).
