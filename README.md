# just_row-

Custom launcher and `Row+` prototype for a Hydrow rowing machine tablet.

This repository contains:

- an Android launcher app that can replace the stock Hydrow home screen
- project briefs and handoff prompts for coding, reverse engineering, and UX work
- helper scripts related to ADB / OTA experimentation

It does **not** contain a full rooted device dump or a complete reverse-engineered telemetry implementation yet.

## Current State

Validated on a live Hydrow tablet:

- device codename: `foenix_h`
- Android family: Android 10
- `adb` access is working
- custom app package: `com.listinglab.rowplus`
- the custom app has been installed on-device
- the custom app has been set as the default Android `HOME` app

The current app is a safe prototype:

- custom launcher shell
- two internal profiles (`You`, `Wife`)
- local mock session history
- Browser launch
- Spotify launch if installed
- hidden admin access to stock Hydrow / Android Settings

Live rowing metrics are not integrated yet.

## Repository Layout

- [hydrow-rowplus-launcher](./hydrow-rowplus-launcher)
  - Android app project for the custom launcher
- [HYDROW_CUSTOM_APP_BRIEF.md](./HYDROW_CUSTOM_APP_BRIEF.md)
  - high-level product and engineering brief
- [HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](./HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)
  - current implementation status, installs, and rollback notes
- [device_recon/HYDROW_PACKAGE_RECON.md](./device_recon/HYDROW_PACKAGE_RECON.md)
  - validated package and APK reconnaissance notes
- [LLM_HANDOFF_PROMPT.md](./LLM_HANDOFF_PROMPT.md)
  - general-purpose handoff prompt
- [LLM_HANDOFF_PROMPT_CODING.md](./LLM_HANDOFF_PROMPT_CODING.md)
  - coding-focused handoff prompt
- [LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md](./LLM_HANDOFF_PROMPT_REVERSE_ENGINEERING.md)
  - reverse-engineering handoff prompt
- [LLM_HANDOFF_PROMPT_PRODUCT_UX.md](./LLM_HANDOFF_PROMPT_PRODUCT_UX.md)
  - product / UX handoff prompt

## Build

The Android app project is already configured to use a local SDK path via `local.properties` on the original development machine, but that file is intentionally not committed.

To build on a new machine:

1. Install Android Studio or an Android SDK + Gradle-compatible setup.
2. Open [hydrow-rowplus-launcher](./hydrow-rowplus-launcher).
3. Create a local `local.properties` pointing to your SDK.
4. Build:

```powershell
cd hydrow-rowplus-launcher
.\gradlew.bat assembleDebug
```

Expected debug APK output:

- `hydrow-rowplus-launcher/app/build/outputs/apk/debug/app-debug.apk`

## Install To Device

```powershell
adb install -r hydrow-rowplus-launcher\app\build\outputs\apk\debug\app-debug.apk
```

Set the custom launcher as default home:

```powershell
adb shell cmd package set-home-activity com.listinglab.rowplus/.MainActivity
```

Verify the current home activity:

```powershell
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Rollback

Restore the stock Hydrow launcher as the default home app:

```powershell
adb shell cmd package set-home-activity com.truerowing.hydrowlauncher/.MainActivity
```

Optionally uninstall the custom launcher after restoring stock home:

```powershell
adb uninstall com.listinglab.rowplus
```

## Intentionally Excluded From Git

Some files stay local and are ignored on purpose:

- downloaded Android SDK / Gradle toolchains
- `platform-tools`
- `mtkclient`
- pulled APKs from the physical device
- local build outputs
- machine-specific `local.properties`
- one-off binaries and utilities

These are excluded because they are:

- large and noisy
- reproducible from upstream sources
- machine-specific
- generated artifacts
- in some cases tied to a specific physical device

## Recommended Next Work

1. Replace mock local storage with Room / SQLite.
2. Harden admin access with a hidden gesture + passcode.
3. Improve the launcher dashboard into a more polished appliance UI.
4. Reverse engineer the Hydrow rowing app to map the real telemetry path.
5. Replace mock rowing metrics with live hardware telemetry.

