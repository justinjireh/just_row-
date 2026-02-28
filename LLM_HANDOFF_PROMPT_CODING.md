# Coding LLM Handoff Prompt

Use this prompt when handing the project to an LLM whose job is to continue writing code in the existing Android app.

```text
You are continuing active development of an Android app already in this workspace.

Your task is to keep building the existing app, not restart the architecture discussion.

## Primary Goal

Continue developing the existing `RowPlus` app into a production-quality custom launcher + rowing app for a Hydrow tablet.

The app must:

- remain the default Android HOME app
- boot into the custom launcher instead of the stock Hydrow launcher
- support two internal user profiles
- store rowing history locally
- provide quick access to Browser and Spotify
- preserve a hidden admin path to Android Settings and the stock Hydrow app
- eventually replace mock rowing metrics with real telemetry

## Ground Truth

These are already validated and should be treated as true unless you find direct evidence otherwise:

- Device codename: `foenix_h`
- Android family: Android 10
- `adb` works
- `adb root` does not work
- Stock launcher: `com.truerowing.hydrowlauncher`
- Stock rowing app: `com.truerowing.crew`
- Custom app package: `com.listinglab.rowplus`
- Current default HOME activity: `com.listinglab.rowplus/.MainActivity`

## Workspace Files To Read First

Read these before changing code:

1. `HYDROW_CUSTOM_APP_BRIEF.md`
2. `HYDROW_IMPLEMENTATION_STATUS_BRIEF.md`
3. `device_recon/HYDROW_PACKAGE_RECON.md`
4. source files under `hydrow-rowplus-launcher`

## Project Root

- `hydrow-rowplus-launcher`

## Current Code Shape

The app already has:

- launcher shell in `MainActivity`
- mock session flow in `SessionActivity`
- local session storage in `SessionStore`
- package queries for Hydrow, Browser, Spotify
- installable build setup

This is a prototype. Your job is to evolve it without breaking launcher safety.

## Hard Constraints

- Do not assume root
- Do not remove or disable stock Hydrow packages unless explicitly instructed
- Do not strand the device without a working HOME app
- Preserve admin escape hatches to:
  - Android Settings
  - Hydrow app
  - launcher selection / home settings

## What To Build Next

Build in this order unless blocked:

1. Refactor to a cleaner app structure
   - separate UI, data, and integration logic
2. Replace `SharedPreferences` JSON storage with Room
3. Add stronger admin protection
   - hidden gesture
   - passcode gate
4. Improve the launcher dashboard
   - current user summary
   - recent sessions
   - cleaner visual hierarchy
5. Prepare the codebase for a real telemetry adapter
   - make session capture pluggable

## Coding Expectations

- Make concrete code changes in the existing project
- Keep the app installable after your changes
- Prefer incremental, testable edits
- Explain exactly what files you changed and why
- If a change affects launcher behavior, include rollback steps

## Build And Deploy

Use the existing local setup:

- Build:
  - `.\gradlew.bat assembleDebug`
- Install:
  - `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Verify HOME:
  - `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`
- Set custom launcher:
  - `adb shell cmd package set-home-activity com.listinglab.rowplus/.MainActivity`
- Roll back to stock launcher:
  - `adb shell cmd package set-home-activity com.truerowing.hydrowlauncher/.MainActivity`

## Required Deliverables

When you respond:

1. Make the code changes
2. Summarize the changes file-by-file
3. Provide exact test steps
4. Provide rollback steps if launcher behavior is affected

Do not stop at advice. Continue the implementation.
```

