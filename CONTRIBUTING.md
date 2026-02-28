# Contributing

This repository is set up for iterative development of the `RowPlus` launcher and rowing app for a Hydrow tablet.

The project is intentionally split between:

- source code that belongs in Git
- local tooling and device-specific assets that do not

## Start Here

Read these files before making changes:

1. [README.md](./README.md)
2. [HYDROW_CUSTOM_APP_BRIEF.md](./HYDROW_CUSTOM_APP_BRIEF.md)
3. [HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](./HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)
4. [device_recon/HYDROW_PACKAGE_RECON.md](./device_recon/HYDROW_PACKAGE_RECON.md)
5. The relevant handoff prompt for your task in [docs/README.md](./docs/README.md)

## Project Scope

The active Android app project is:

- [hydrow-rowplus-launcher](./hydrow-rowplus-launcher)

Current package name:

- `com.listinglab.rowplus`

Current intended role:

- default Android `HOME` app on the Hydrow tablet

## Local Setup

You need:

- JDK 17+
- Android SDK installed locally
- a valid `local.properties` file in `hydrow-rowplus-launcher`

Example `local.properties`:

```properties
sdk.dir=C:/Path/To/Your/Android/Sdk
```

Do not commit `local.properties`.

## Build

From the project root:

```powershell
cd hydrow-rowplus-launcher
.\gradlew.bat assembleDebug
```

Expected output:

- `hydrow-rowplus-launcher/app/build/outputs/apk/debug/app-debug.apk`

## Install To Device

From the repository root:

```powershell
adb install -r hydrow-rowplus-launcher\app\build\outputs\apk\debug\app-debug.apk
```

## Make RowPlus The Home App

```powershell
adb shell cmd package set-home-activity com.listinglab.rowplus/.MainActivity
```

Verify:

```powershell
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Restore Stock Hydrow Launcher

```powershell
adb shell cmd package set-home-activity com.truerowing.hydrowlauncher/.MainActivity
```

Optional uninstall after restoring stock home:

```powershell
adb uninstall com.listinglab.rowplus
```

## Current Device Safety Rules

- Do not factory reset the device.
- Do not assume root access.
- Do not disable stock Hydrow packages as a default step.
- Always preserve a path back to:
  - Android Settings
  - the stock Hydrow app
  - the stock Hydrow launcher

If a change affects launcher behavior, include rollback steps in your notes or PR description.

## What Belongs In Git

Commit:

- Android app source in `hydrow-rowplus-launcher`
- docs and handoff prompts
- small helper scripts
- core reverse-engineering inputs already tracked intentionally
  - `device_apks/HydrowLauncher.apk`
  - `device_apks/crew-base.apk`

Do not commit:

- local SDK/toolchain installs
- generated build outputs
- IDE state
- machine-specific config
- large third-party utilities unless intentionally approved

## Pull Requests / Change Sets

When you make changes, keep them easy to reason about:

- keep launcher safety intact
- make incremental changes
- explain what files changed and why
- include device test steps
- include rollback steps if behavior changes on boot/home flow

## Preferred Work Order

1. Stabilize and improve the current launcher app.
2. Replace temporary storage with Room / SQLite.
3. Harden admin access.
4. Reverse engineer real telemetry from `device_apks/crew-base.apk`.
5. Replace mock rowing metrics with live data.

