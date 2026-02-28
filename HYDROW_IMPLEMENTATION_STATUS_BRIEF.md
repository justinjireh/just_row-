# Hydrow Implementation Status Brief

## Purpose

This document summarizes the current Hydrow customization work completed so far, including:

- what was discovered on the device
- what was installed on the Windows host
- what was installed on the Hydrow tablet
- what was changed in the tablet's boot / home behavior
- what has not been changed
- the current limitations and next steps

## Executive Summary

The Hydrow tablet is now reachable over `adb`, and a custom launcher app named `RowPlus` has been:

- built locally
- installed on the Hydrow
- set as the default Android `HOME` app

As of now:

- pressing Home opens `RowPlus`
- Android resolves the default home activity to `com.listinglab.rowplus/.MainActivity`
- the stock Hydrow launcher and Hydrow app are still present on the device
- nothing stock has been uninstalled or disabled

This is a safe, reversible launcher replacement, not a root-level modification.

## Device Facts Confirmed

- Device model / product: `foenix_h`
- Android build fingerprint:
  - `alps/foenix_h/foenix_h:10/QP1A.190711.020/1725505377:user/release-keys`
- Android generation:
  - Android 10 family
- `adb` is working and authorized
- `adb root` is not available because this is a production build

## Stock Hydrow Packages Confirmed

- Stock launcher package:
  - `com.truerowing.hydrowlauncher`
- Stock launcher role:
  - original Android `HOME` app
- Stock rowing/subscription app:
  - `com.truerowing.crew`
- Existing browser:
  - `com.android.browser`

## Local Windows Host Changes

### Existing Tools Used

- Existing `platform-tools` folder in this workspace was used for live `adb`
- Existing Java was used:
  - `C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`

### Local Host Configuration Changes

- Added MediaTek vendor ID to local ADB USB scan list:
  - `C:\Users\15086\.android\adb_usb.ini`
- Added:
  - `0x0E8D`

This was necessary so Windows `adb` would scan for the Hydrow / MediaTek vendor ID.

### New Toolchain Installed In Workspace

The following build tooling was installed locally under this workspace:

- Android SDK root:
  - `toolchain/android-sdk`
- Android command-line tools:
  - extracted under `toolchain/android-sdk/cmdline-tools/cmdline-tools`
- Android SDK Platform:
  - `platforms/android-34`
- Android Build Tools:
  - `build-tools/34.0.0`
- Android SDK platform-tools:
  - `toolchain/android-sdk/platform-tools`
- Gradle:
  - `toolchain/gradle-8.7`

Downloaded archives retained in workspace:

- `toolchain/commandlinetools-win.zip`
- `toolchain/gradle-8.7-bin.zip`

## Local Files Created

### Planning / Status Documents

- `HYDROW_CUSTOM_APP_BRIEF.md`
- `device_recon/HYDROW_PACKAGE_RECON.md`
- `HYDROW_IMPLEMENTATION_STATUS_BRIEF.md` (this file)

### Pulled APKs From The Device

- `device_apks/HydrowLauncher.apk`
- `device_apks/crew-base.apk`

These are local copies of the stock launcher and Hydrow crew app for offline inspection.

### Custom Launcher Project

Project root:

- `hydrow-rowplus-launcher`

Key project files:

- `hydrow-rowplus-launcher/app/src/main/AndroidManifest.xml`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/MainActivity.kt`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/SessionActivity.kt`
- `hydrow-rowplus-launcher/app/src/main/java/com/listinglab/rowplus/SessionStore.kt`
- `hydrow-rowplus-launcher/local.properties`

### Build System Files Generated

- Gradle wrapper generated in:
  - `hydrow-rowplus-launcher/gradle/wrapper`
- Wrapper launcher scripts:
  - `hydrow-rowplus-launcher/gradlew`
  - `hydrow-rowplus-launcher/gradlew.bat`

### Built APK

- Debug APK output:
  - `hydrow-rowplus-launcher/app/build/outputs/apk/debug/app-debug.apk`

## Custom App Built And Installed

### App Identity

- Package:
  - `com.listinglab.rowplus`
- Version:
  - `0.1.0`
- Version code:
  - `1`
- Target SDK:
  - `34`

### On-Device Install Status

Confirmed installed on the Hydrow:

- `com.listinglab.rowplus`

On-device package state:

- code path:
  - `/data/app/com.listinglab.rowplus-DoDtmoWHgbn-Uvg_j6mDJQ==`
- data dir:
  - `/data/user/0/com.listinglab.rowplus`

## Hydrow Device Behavior Changes

### Manual Changes Performed On Tablet

These were enabled manually through Android Settings on the device:

- Developer options
- USB debugging
- normal Android USB mode that exposes a working ADB interface

### Launcher Change

The default Android `HOME` activity was changed from:

- `com.truerowing.hydrowlauncher/.MainActivity`

to:

- `com.listinglab.rowplus/.MainActivity`

Current resolved home activity:

- `com.listinglab.rowplus/.MainActivity`

### What This Means In Practice

- Pressing Home should open `RowPlus`
- Boot should resolve to `RowPlus` as the main launcher
- The stock Hydrow launcher remains installed as a fallback, but is no longer the default home target

## What Was Not Changed

The following stock components were **not** removed, disabled, or overwritten:

- `com.truerowing.hydrowlauncher`
- `com.truerowing.crew`
- `com.android.browser`

No root exploit was installed.

No system partition was modified.

No stock app was uninstalled.

No factory reset was performed.

## Current RowPlus Prototype Features

The currently installed `RowPlus` build is a safe prototype. It provides:

- custom launcher shell
- two internal user profiles:
  - `You`
  - `Wife`
- local history storage
- a mock `Row+` session screen
- Browser launch
- Spotify launch if installed
- hidden admin actions by long-pressing the app title

Admin actions currently include:

- open Hydrow app
- open Android Settings
- open home app settings

## Current Limitations

- Live rowing hardware metrics are **not** connected yet
- The `Row+` session screen currently records mock / simulated values only
- No real sensor, Bluetooth, USB, or rowing data path has been wired into the custom app yet
- Hydrow's private app data is still not directly readable because:
  - root is unavailable
  - `run-as` for the Hydrow package is not available

## Best Current Interpretation

The custom launcher layer is working.

The remaining major engineering task is metrics integration.

The most likely path is:

- inspect `device_apks/crew-base.apk`
- identify the code paths inside `com.truerowing.crew` responsible for Bluetooth / USB rowing data
- either:
  - replicate that hardware access directly in `RowPlus`, or
  - reverse engineer enough of Hydrow's integration path to consume the same signals

## Safe Rollback / Reversal

If you want to restore the stock Hydrow launcher as the default home app:

```powershell
adb shell cmd package set-home-activity com.truerowing.hydrowlauncher/.MainActivity
```

If you want to uninstall the custom launcher app:

```powershell
adb uninstall com.listinglab.rowplus
```

Recommended rollback order:

1. Set the stock Hydrow launcher back as default home
2. Verify Home resolves correctly
3. Then uninstall `com.listinglab.rowplus` if desired

## Useful Verification Commands

Check the connected device:

```powershell
adb devices -l
```

Check the current default home activity:

```powershell
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Check the custom launcher package:

```powershell
adb shell dumpsys package com.listinglab.rowplus | findstr /i "versionName versionCode codePath dataDir"
```

## Recommended Next Steps

1. Test the current `RowPlus` UI directly on the Hydrow:
   - profile switching
   - mock row save
   - history
   - Browser launch
   - admin actions
2. Reverse engineer `device_apks/crew-base.apk` to identify the real metrics pipeline.
3. Replace mock metrics in `SessionActivity` with real rowing telemetry.
4. Add stronger admin protection:
   - passcode
   - hidden gesture
5. Only consider disabling the stock Hydrow launcher after the replacement app is fully proven.

