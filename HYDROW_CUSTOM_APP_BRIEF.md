# Hydrow Custom Launcher + Row+ App Brief

## Objective

Build a custom Android app for a Hydrow rowing machine display that:

- Boots automatically as the top-level user experience.
- Hides or bypasses the normal Hydrow launcher in day-to-day use.
- Provides an enhanced "Just Row" style experience without paid Hydrow subscription features.
- Tracks rowing metrics and persists session history locally.
- Supports two internal user profiles (for example: user + spouse).
- Provides quick access to additional apps such as Browser and Spotify.

This brief is written for another LLM or engineer to continue implementation on the actual device.

## Current Device State

- Device codename/model seen by Android: `foenix_h`
- Build fingerprint: `alps/foenix_h/foenix_h:10/QP1A.190711.020/1725505377:user/release-keys`
- Android version family: Android 10
- `adb` is working and authorized
- `fastboot` is not the active transport in normal operation
- The device is a production build; `adb root` is blocked

## Confirmed Access

- `adb` connectivity is active from Windows
- Current `adb devices -l` result:
  - serial: `I22302124000662`
  - product: `foenix_h`
  - model: `foenix_h`
  - device: `foenix_h`
- Normal Android Settings UI is reachable on the tablet
- Developer options were enabled
- USB debugging was enabled
- The tablet can switch into recovery manually with button combos

## USB / Driver Notes

- Normal working ADB mode enumerated as:
  - `USB\\VID_0E8D&PID_201D`
  - composite device with:
  - MTP interface
  - ADB interface
- Prior non-working states observed:
  - `PID_2008` vendor/MTP-like mode
  - `PID_201C` fastboot-class mode
- On the Windows host, `adb` required MediaTek vendor ID `0x0E8D` added to:
  - `C:\\Users\\15086\\.android\\adb_usb.ini`

## Installed Hydrow Packages

- Hydrow launcher package:
  - `com.truerowing.hydrowlauncher`
- Hydrow launcher main activity:
  - `com.truerowing.hydrowlauncher/.MainActivity`
- Hydrow launcher APK path:
  - `/system/priv-app/HydrowLauncher/HydrowLauncher.apk`
- Hydrow rowing/subscription app:
  - `com.truerowing.crew`
- Hydrow crew APK path:
  - `/data/app/com.truerowing.crew-wingf9EvAEX-lNACWNXfjQ==/base.apk`
- Existing browser package:
  - `com.android.browser`

## Package Role Summary

- `com.truerowing.hydrowlauncher` is the default Android `HOME` app right now.
- `com.truerowing.crew` appears to be the primary rowing/subscription app.
- `com.truerowing.crew` has Bluetooth and USB-related receivers and permissions, suggesting it likely owns the rowing hardware integration path.

## Key Constraints

- The device is not rootable from normal `adb`; `adbd cannot run as root in production builds`.
- The available shell user is `uid=2000(shell)`.
- Direct access to other apps' private data is restricted.
- `run-as com.truerowing.crew` failed because the package is not debuggable / not available for `run-as`.
- Hydrow launcher is a privileged system app; removing or disabling it before a replacement launcher is proven is risky.

## Recommended Product Architecture

Do not try to patch Hydrow's UI directly first.

The recommended design is:

- Build a standalone Android app that is also a launcher (`HOME` app).
- Make that app the default home screen.
- Keep Hydrow installed but hidden from normal user flow.
- Treat the app as the primary shell for the machine.

This custom app should provide:

- A custom home screen / launcher UI
- A "Row+" workflow (enhanced Just Row)
- Internal profile selection for two users
- Local persistent storage for session history
- Buttons to launch Browser
- Buttons to launch Spotify if installed
- A hidden admin/maintenance path back to:
  - Android Settings
  - Hydrow app(s), if needed

## Why This Architecture

- It avoids modifying Hydrow's APKs at the start.
- It is more stable than trying to inject a button into Hydrow's UI.
- It gives full control over the boot experience.
- It allows features to be added incrementally.
- It reduces the chance of bricking or locking out the device.

## What "Top-Level App" Should Mean Here

On boot:

- Android launches the custom launcher instead of `com.truerowing.hydrowlauncher`.
- The user lands in a simple, large-button control surface.
- Hydrow subscription screens do not appear unless deliberately launched from a hidden admin path.

Suggested primary buttons:

- `Start Row+`
- `History`
- `User: You`
- `User: Wife`
- `Spotify`
- `Browser`
- `Settings` (optional, can be hidden behind admin gesture)

## User Model

Do not depend on Android multi-user.

Implement profiles inside the custom app:

- Profile A: primary user
- Profile B: spouse

Each profile should store:

- Session history
- Personal bests
- Aggregate totals
- Optional settings like preferred metrics

This avoids fighting the device's single-user appliance setup.

## Metrics Strategy

This is the hardest technical part.

There are two possible approaches:

### Preferred Approach

Have the custom app talk to the rower hardware directly.

Possible transport paths to investigate:

- Bluetooth
- USB
- Serial
- Local binder/service interface

If direct hardware access is possible, the custom app can own:

- Stroke rate
- Pace
- Distance
- Time
- Power / watts
- Calories
- Heart rate, if available

### Secondary Approach

Piggyback on Hydrow's app behavior.

This likely means:

- Reverse engineering `com.truerowing.crew`
- Inspecting its APK code/resources
- Discovering the service/class that receives rowing data
- Hooking or emulating that path

This is harder because:

- `adb root` is not available
- Direct access to Hydrow private storage is blocked
- Hydrow app updates can break assumptions

## What Is Likely Not Easy

- Reading Hydrow's private session database directly from a separate app
- Modifying Hydrow's packaged UI in place as a first step
- Permanently removing the system launcher safely before a replacement exists

## Immediate Development Plan

### Phase 1: Safe Recon

- Pull and inspect the Hydrow APKs
- Identify package manifests, activities, services, and receivers
- Identify code paths in `com.truerowing.crew` related to:
  - USB state
  - Bluetooth
  - rowing services
  - hardware listeners

### Phase 2: Minimal Custom Launcher

Build a launcher APK that:

- Declares `MAIN` + `HOME` intent filters
- Opens as the new home app
- Provides a simple navigation shell
- Can launch:
  - `com.android.browser`
  - `com.truerowing.crew` (hidden admin path only)
  - Spotify if installed

### Phase 3: Local Data Model

Add local persistence using a normal Android database:

- Room / SQLite recommended

Suggested entities:

- `UserProfile`
- `RowSession`
- `SessionSplit`
- `PersonalRecord`

### Phase 4: Metrics Integration

- Discover the hardware metrics transport
- Implement a direct reader if possible
- Fall back to reverse engineered app-to-service integration only if needed

### Phase 5: Make It the Default Experience

After the launcher is tested:

- Set it as the default home activity
- Keep a hidden escape hatch to Settings and Hydrow
- Only consider disabling/hiding Hydrow launcher after validation

## Safety Rules

- Do not disable `com.truerowing.hydrowlauncher` until the new launcher is installed and manually verified.
- Do not factory reset the device.
- Do not assume root access.
- Do not rely on `fastboot` for core workflow unless it becomes necessary.
- Preserve a path back to Android Settings at all times.

## Functional Requirements For The Custom App

- Must auto-launch on boot by acting as the `HOME` app
- Must present a clean appliance-like UI
- Must support at least two internal user profiles
- Must store history locally and persist across reboots
- Must work offline
- Must expose easy access to Browser and Spotify
- Must allow a hidden maintenance path back to stock Hydrow / Settings

## Suggested UX

Simple large-button launcher screen:

- Top area: current user and quick switch
- Center: `Start Row+`
- Secondary actions:
  - `History`
  - `Programs` (optional, future)
  - `Spotify`
  - `Browser`
- Hidden admin gesture:
  - long-press logo
  - 5-tap corner
  - passcode dialog

The app should feel like a dedicated appliance front-end, not a generic Android launcher.

## Technical Notes For Another LLM

- Treat `adb` as already solved; the connection is live.
- Focus on APK inspection and app build, not driver recovery.
- The main launcher to replace is `com.truerowing.hydrowlauncher`.
- The likely metrics owner is `com.truerowing.crew`.
- The system is Android 10 and uses a MediaTek-based build (`alps`, `foenix_h`).
- Root-only strategies are out unless a separate exploit path is found later.
- The best near-term win is a custom launcher with internal profiles and local history.
- The biggest unknown is the rowing data interface.

## Useful Existing Commands

- Check ADB:
  - `adb devices -l`
- Resolve current home activity:
  - `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`
- Set a new home activity:
  - `adb shell cmd package set-home-activity <your.package/.YourHomeActivity>`
- Show package path:
  - `adb shell pm path com.truerowing.hydrowlauncher`
  - `adb shell pm path com.truerowing.crew`
- List packages:
  - `adb shell pm list packages`

## Deliverable Recommendation

The first real deliverable should be:

- a minimal installable custom launcher APK

It should:

- become the default home screen
- launch at boot
- include profile switcher
- store simple fake/test row history first
- open Browser
- optionally open Hydrow and Settings via hidden admin actions

Only after that should work begin on live rowing metrics capture.

