# LLM Handoff Prompt

Use the prompt below as the starting instruction for any LLM that is taking over development of this Hydrow project.

```text
You are continuing an active Android reverse-engineering and product build project for a Hydrow rowing machine tablet.

Your job is not to brainstorm from scratch. Your job is to continue the existing implementation in this workspace, using the current files and the current device state as ground truth.

You must treat the current state as real and already validated unless you find direct evidence otherwise.

## Mission

Build a production-quality custom Android launcher + rowing app for a Hydrow tablet that:

- boots as the default HOME app
- bypasses the stock Hydrow launcher in normal use
- supports two internal user profiles (for example: You and Wife)
- stores rowing sessions and history locally
- provides a better "Just Row" experience without depending on Hydrow subscription UX
- can launch auxiliary apps such as Browser and Spotify
- preserves a hidden admin path to Android Settings and the stock Hydrow app
- eventually replaces mock session metrics with real rowing telemetry from the hardware

## Non-Goals

- Do not redesign the entire project from scratch.
- Do not remove or disable stock Hydrow packages unless explicitly requested and only after the replacement app is proven stable.
- Do not assume root access.
- Do not assume direct read access to Hydrow private app data.
- Do not suggest unsafe or destructive changes as the default path.

## Ground Truth: Device State

These facts are already confirmed on the live device:

- Device codename/model: `foenix_h`
- Build fingerprint: `alps/foenix_h/foenix_h:10/QP1A.190711.020/1725505377:user/release-keys`
- Android family: Android 10
- `adb` is working and authorized
- `adb root` is blocked (`adbd cannot run as root in production builds`)

Installed stock packages confirmed:

- Stock launcher: `com.truerowing.hydrowlauncher`
- Stock rowing app: `com.truerowing.crew`
- Browser present: `com.android.browser`

Current custom app status on the live Hydrow:

- Custom package: `com.listinglab.rowplus`
- Version: `0.1.0`
- Installed successfully
- Set as the current default Android HOME app
- Current resolved HOME activity: `com.listinglab.rowplus/.MainActivity`

This means:

- pressing Home should open `RowPlus`
- the stock Hydrow launcher is still installed, but no longer the default home target

## Ground Truth: Workspace State

Important files already created in this workspace:

- `HYDROW_CUSTOM_APP_BRIEF.md`
- `HYDROW_IMPLEMENTATION_STATUS_BRIEF.md`
- `device_recon/HYDROW_PACKAGE_RECON.md`
- `device_apks/HydrowLauncher.apk`
- `device_apks/crew-base.apk`

Current Android project:

- Project root: `hydrow-rowplus-launcher`

Current key project files:

- `hydrow-rowplus-launcher/app/src/main/AndroidManifest.xml`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/MainActivity.kt`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/SessionActivity.kt`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/SessionStore.kt`
- `hydrow-rowplus-launcher/app/build.gradle.kts`
- `hydrow-rowplus-launcher/local.properties`

Build tooling already installed locally in this workspace:

- Android SDK: `toolchain/android-sdk`
- Gradle: `toolchain/gradle-8.7`
- Gradle wrapper already generated in `hydrow-rowplus-launcher/gradle/wrapper`

Current built artifact:

- `hydrow-rowplus-launcher/app/build/outputs/apk/debug/app-debug.apk`

## Read This First

Before making changes, read these files in order:

1. `HYDROW_CUSTOM_APP_BRIEF.md`
2. `HYDROW_IMPLEMENTATION_STATUS_BRIEF.md`
3. `device_recon/HYDROW_PACKAGE_RECON.md`
4. Current source files in `hydrow-rowplus-launcher`

Do not skip this. Those files contain the actual validated constraints and prior decisions.

## Current App Behavior

The current `RowPlus` app is a prototype launcher that already provides:

- a custom HOME activity
- two internal user profiles (`You`, `Wife`)
- local session history storage using `SharedPreferences` + JSON
- a mock `Row+` session screen
- Browser launch
- Spotify launch if installed
- hidden admin actions via long-press on the app title

The current row session is NOT connected to real rowing hardware metrics yet. It uses simulated values to validate launcher/profile/history flow.

## Primary Engineering Goal

Turn the prototype into a production-quality app in phases.

Do this in the following order unless blocked:

### Phase 1: Stabilize the App Structure

- Refactor the app into a clearer architecture:
  - `ui`
  - `data`
  - `domain`
  - `integrations`
- Preserve the current package name unless there is a strong reason not to.
- Keep the current launcher behavior working.

### Phase 2: Replace the Temporary Storage Layer

- Replace `SharedPreferences` JSON history storage with a proper persistence layer.
- Preferred solution:
  - Room + SQLite
- Create data models for:
  - user profiles
  - row sessions
  - optional splits / metrics snapshots
  - aggregate stats / personal bests

### Phase 3: Harden the Launcher UX

- Improve the launcher UI into a polished appliance-style dashboard.
- Keep it simple, large-target, low-friction, and stable on a rowing machine display.
- Add:
  - current user summary
  - recent row summaries
  - quick launch buttons
  - hidden admin panel

### Phase 4: Secure Admin Functions

- Replace the current unprotected long-press admin access with:
  - hidden gesture
  - passcode gate
  - or both
- Admin panel should still allow:
  - launch stock Hydrow app
  - open Android Settings
  - adjust default home behavior if necessary

### Phase 5: Reverse Engineer Real Metrics

- Analyze `device_apks/crew-base.apk`
- Identify likely classes / services / receivers related to:
  - USB state
  - Bluetooth state
  - rower hardware telemetry
  - data ingestion
  - metrics calculation / transformation
- Use package name and previously observed clues:
  - `com.truerowing.crew`
  - `com.truerowing.crew/.service.receivers.UsbStateReceiver`
- The likely path is inside Hydrow's rowing app, not the launcher.

### Phase 6: Integrate Real Telemetry

- Replace mock metrics in the `Row+` session flow with real hardware readings if feasible.
- If direct telemetry is not immediately feasible, produce the clearest possible map of the data path and implement the app so the telemetry adapter can be swapped in cleanly later.

## Required Working Assumptions

- Root is unavailable unless proven otherwise.
- The stock launcher must remain available as a fallback until the new app is clearly stable.
- The live Hydrow is already in a safe reversible state.
- The app should behave like a dedicated appliance front-end, not a generic Android launcher.
- Internal profile switching should be implemented inside the app, not via Android multi-user.

## Device Safety Rules

- Never factory reset the device.
- Never remove the stock Hydrow launcher as a first step.
- Never strand the tablet without a working HOME app.
- Always preserve at least one path back to:
  - Android Settings
  - Hydrow app
  - default home configuration

If you touch anything launcher-related, include rollback instructions.

## Coding Expectations

- Make concrete code changes in the existing Android project.
- Do not only provide conceptual guidance.
- Prefer incremental, testable commits/patches.
- Explain file-by-file what you changed and why.
- If you cannot validate something, say exactly what remains unverified.

When editing:

- preserve the current installability of the app
- avoid unnecessary package renames
- do not introduce dependencies casually
- keep the app robust on Android 10 / API 26+ with target SDK 34

## Build And Deploy Context

The project is already configured for local build:

- SDK path is in `hydrow-rowplus-launcher/local.properties`
- The project builds with:
  - `.\gradlew.bat assembleDebug`

The device is already connected over `adb`.

Useful commands:

- Build:
  - `.\gradlew.bat assembleDebug`
- Install:
  - `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Verify default home:
  - `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`
- Set launcher:
  - `adb shell cmd package set-home-activity com.listinglab.rowplus/.MainActivity`
- Roll back to stock launcher:
  - `adb shell cmd package set-home-activity com.truerowing.hydrowlauncher/.MainActivity`

## What You Should Deliver

When you continue this project, your output should include:

1. Actual code changes in the existing `hydrow-rowplus-launcher` project
2. A short explanation of the implementation decisions
3. Safe device test steps
4. Rollback steps for any launcher-impacting change
5. If working on telemetry, a concrete analysis of the relevant `crew-base.apk` components

## Preferred Immediate Next Task

Start by upgrading the current prototype from a demo launcher into a real app foundation:

- introduce a proper persistent data layer (Room)
- clean up the app structure
- keep the existing launcher behavior stable
- preserve admin escape hatches

After that, move to APK inspection of `device_apks/crew-base.apk` and map the telemetry path.

## Final Instruction

Do not re-litigate whether this should be done. It is already underway.

Continue the project from the current state, keep it safe and reversible, and prioritize shipping a stable custom launcher with a credible path to real rowing metrics.
```

