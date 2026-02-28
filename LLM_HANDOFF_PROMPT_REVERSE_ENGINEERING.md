# Reverse Engineering LLM Handoff Prompt

Use this prompt when handing the project to an LLM whose job is to reverse engineer the Hydrow APKs and map the telemetry path.

```text
You are continuing reverse engineering work on a Hydrow rowing machine Android tablet.

Your job is to map the real rowing metrics pipeline so the existing custom app can replace its mock metrics with live telemetry.

## Primary Goal

Analyze the pulled Hydrow APKs in this workspace and identify how the Hydrow system receives, processes, and exposes rowing telemetry.

You are not building the full UI. You are performing technical reconnaissance and producing actionable implementation guidance for the existing `RowPlus` app.

## APKs Available Locally

Use these as the primary analysis targets:

- `device_apks/crew-base.apk`
- `device_apks/HydrowLauncher.apk`

Treat `crew-base.apk` as the primary telemetry target.

## Ground Truth

These are already confirmed:

- Device codename: `foenix_h`
- Android family: Android 10
- `adb` works
- `adb root` does not work
- Stock rowing app package: `com.truerowing.crew`
- Stock launcher package: `com.truerowing.hydrowlauncher`
- Confirmed receiver in `com.truerowing.crew`:
  - `com.truerowing.crew/.service.receivers.UsbStateReceiver`
- `com.truerowing.crew` has Bluetooth-related permissions:
  - `BLUETOOTH`
  - `BLUETOOTH_ADMIN`
  - `BLUETOOTH_PRIVILEGED`
  - `BLUETOOTH_SCAN`
  - `BLUETOOTH_CONNECT`
  - `BLUETOOTH_ADVERTISE`

This strongly suggests that rowing telemetry likely enters through Bluetooth and/or USB-related services inside `com.truerowing.crew`.

## Files To Read First

Read these before starting analysis:

1. `HYDROW_CUSTOM_APP_BRIEF.md`
2. `HYDROW_IMPLEMENTATION_STATUS_BRIEF.md`
3. `device_recon/HYDROW_PACKAGE_RECON.md`

## Analysis Priorities

Work in this order:

1. Manifest-level mapping
   - activities
   - services
   - receivers
   - providers
   - permissions
2. Search for telemetry-relevant components
   - Bluetooth stack usage
   - USB stack usage
   - serial / HID / device listeners
   - background services
   - metrics models / DTOs
3. Identify the path from hardware input to UI data
4. Identify whether the custom app can:
   - talk to hardware directly
   - or needs to mimic / reuse a Hydrow internal path

## What To Look For

Specifically search for:

- classes involving rower hardware communication
- `BluetoothGatt`, `BluetoothDevice`, scan callbacks, socket usage
- USB permission handling, USB accessory/device APIs
- services that stay alive during workouts
- model classes for:
  - stroke rate
  - split
  - pace
  - watts
  - distance
  - duration
  - heart rate
- repositories / managers / transport adapters
- event buses, observers, reactive streams, or callback chains

Also identify:

- any native libraries
- any protobuf / JSON schema classes
- any obvious BLE UUIDs or device names
- any references to rowing machine state transitions

## Constraints

- Do not assume root-only runtime inspection is available
- Prefer static analysis of the pulled APKs first
- Avoid vague conclusions
- Produce concrete class names, package names, and likely execution paths

## Expected Output

Your output should include:

1. A likely end-to-end telemetry path map
2. The specific classes / packages most likely involved
3. The strongest evidence for Bluetooth vs USB vs other hardware transport
4. The easiest integration strategy for `com.listinglab.rowplus`
5. A ranked list of next concrete experiments

## Strong Preference

Do not just say “decompile the APK.”

Actually inspect the APK structure and produce a specific, evidence-based map that an engineer can use to implement real telemetry in the existing app.
```

