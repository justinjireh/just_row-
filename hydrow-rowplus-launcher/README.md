# RowPlus Launcher Prototype

This is a minimal Android launcher prototype for the Hydrow tablet.

It is designed to:

- act as the default `HOME` app
- replace the stock Hydrow launcher in normal use
- keep two internal user profiles
- store local row session history
- launch Browser and Spotify
- keep hidden admin access to Hydrow and Android Settings

## What It Is

This prototype is intentionally simple:

- `MainActivity` is the launcher shell
- `SessionActivity` is a local-only mock "Row+" session screen
- session history is stored in `SharedPreferences` as JSON

This does not read live Hydrow hardware metrics yet. It provides the shell and local persistence first.

## Build

Use Android Studio or a local Android SDK/Gradle setup.

Expected environment:

- Android Studio recent version
- JDK 17+
- Android SDK platform 34

## Install

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Set As Default Home App

After install, set it as the active launcher:

```powershell
adb shell cmd package set-home-activity com.listinglab.rowplus/.MainActivity
```

To verify:

```powershell
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Safety

- Do not disable `com.truerowing.hydrowlauncher` until this launcher is installed and verified.
- Keep the hidden admin actions available so you can still open Hydrow and Android Settings.
