# Hydrow Package Recon

## Local APK Pulls

- `device_apks/HydrowLauncher.apk`
- `device_apks/crew-base.apk`

## Resolved Home Activity

- Default home: `com.truerowing.hydrowlauncher/.MainActivity`

## Hydrow Launcher

- Package: `com.truerowing.hydrowlauncher`
- APK path: `/system/priv-app/HydrowLauncher/HydrowLauncher.apk`
- Version: `1.0.20231`
- Version code: `20231`
- Data dir: `/data/user/0/com.truerowing.hydrowlauncher`
- Role: stock `HOME` app on boot

## Hydrow Crew App

- Package: `com.truerowing.crew`
- APK path: `/data/app/com.truerowing.crew-wingf9EvAEX-lNACWNXfjQ==/base.apk`
- Version: `1.0.0.26765`
- Version code: `26765`
- ABI: `arm64-v8a`
- Data dir: `/data/user/0/com.truerowing.crew`

## Signals Relevant To Metrics

- Declares `com.truerowing.crew/.service.receivers.UsbStateReceiver`
- Has Bluetooth permissions:
  - `android.permission.BLUETOOTH`
  - `android.permission.BLUETOOTH_ADMIN`
  - `android.permission.BLUETOOTH_PRIVILEGED`
  - `android.permission.BLUETOOTH_SCAN`
  - `android.permission.BLUETOOTH_CONNECT`
  - `android.permission.BLUETOOTH_ADVERTISE`
- Has:
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_FINE_LOCATION`

## Interpretation

- The launcher replacement path is clean: build a new `HOME` app and set it as the default home activity.
- The likely rowing metrics integration path is inside `com.truerowing.crew`, probably via Bluetooth and/or USB state listeners.
- Root is still blocked, so the next analysis step is APK inspection or runtime observation, not direct private data access.

