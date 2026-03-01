# Hydrow Metrics Pipeline Recon

Date: 2026-02-28

## Scope

This note documents the concrete telemetry path discovered from static inspection of:

- [crew-base.apk](E:/LISTING%20LAB%20DOWNLOADS/platform-tools-latest-windows/device_apks/crew-base.apk)
- extracted dex dump: [classes10.dexdump.txt](E:/LISTING%20LAB%20DOWNLOADS/platform-tools-latest-windows/device_recon/classes10.dexdump.txt)

The goal is to answer whether `RowPlus` can access the same core rowing metrics the stock Hydrow app uses.

## Bottom Line

Yes, the stock app has a concrete internal telemetry pipeline for the core rowing metrics, and it is not just UI state.

The strongest evidence shows:

- `com.truerowing.crew` uses a real serial/UART path to talk to the rowing hardware.
- That path is managed by `CrewSerialManager` through `android_serialport_api.SerialPort`.
- The service layer converts that low-level stream into structured Kotlin data models.
- Those models already contain the same core metrics a replacement app would want.

What is **not** proven yet:

- The exact packet format on the wire.
- Whether a normal third-party app can open the same UART device nodes without additional privileges.
- Whether every Hydrow-derived metric is raw or partially computed in app logic.

## Hardware Transport Evidence

`crew-base.apk` requests permissions and features consistent with direct hardware access:

- `android.permission.MANAGE_USB`
- `android.permission.BLUETOOTH`
- `android.permission.BLUETOOTH_ADMIN`
- `android.permission.BLUETOOTH_PRIVILEGED`
- `android.permission.BLUETOOTH_SCAN`
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.BLUETOOTH_ADVERTISE`
- feature `android.hardware.usb.host`
- feature `android.hardware.bluetooth`

The APK also ships native serial code:

- `lib/arm64-v8a/libserial_port.so`
- `lib/armeabi-v7a/libserial_port.so`
- `lib/x86/libserial_port.so`

String extraction from `libserial_port.so` shows:

- `Java_android_1serialport_1api_SerialPort_open`
- `Java_android_1serialport_1api_SerialPort_close`
- `Configuring serial port`
- `Opening serial port %s with flags 0x%x`

This is direct evidence that the stock app includes a JNI-backed serial port bridge.

## Core Serial Classes

### `CrewSerialManager`

Class:

- `com.truerowing.crew.service.readers.CrewSerialManager`

This is the main low-level serial manager.

Key findings from dexdump:

- Uses `android_serialport_api.SerialPort`
- Holds `uartPaths`
- Holds `brakeControllerSerialNumber`
- Exposes `setListener(SerialInterface)`
- Exposes `write(byte[])`
- Contains handshake/reconnect logic

Important constants:

- `FIRMWARE_BAUD_RATE = 921600`
- `FLASH_BAUD_RATE = 115200`
- `HANDSHAKE_RETRY_PERIOD_MILLIS = 250`

Important handshake strings:

- `TEDDY_SYNC_MSG = "?"`
- `TEDDY_SYNC_OK_RESPONSE = "OK"`
- `TEDDY_ECHO_OFF_MSG = "A 0\r\n"`
- `TEDDY_PART_ID_CONFIRMATION_MSG = "J\r\n"`
- `TEDDY_UNLOCK_CODE_MSG = "U 23130\r\n"`
- `TEDDY_REBOOT_TO_NORMAL_MSG = "T\r\n"`
- `TURN_OFF_CONTINUOUS_MODE_MSG = "Cm 0\r"`

Concrete UART paths found in `classes10.dex` string scan:

- `/dev/ttyACM0`
- `/dev/ttyMT1`
- `/dev/ttyUSB0`

This is the clearest indication that Hydrow is reading live machine telemetry over serial/UART, not merely from a cloud or UI abstraction.

### `SerialInterface`

Class:

- `com.truerowing.crew.service.readers.SerialInterface`

This is the callback contract used by the serial layer.

Methods:

- `action(String)`
- `action(String, String)`
- `connected()`
- `data(byte[])`
- `error(Exception)`
- `flashReady()`
- `flashRequired(String)`
- `serialState(CrewSerialManager.State)`

This confirms the serial manager emits raw byte payloads upward through `data(byte[])`.

### `UsbStateReceiver`

Class:

- `com.truerowing.crew.service.receivers.UsbStateReceiver`

Listener methods:

- `usbDeviceAttached(UsbDevice)`
- `usbDeviceDetached(UsbDevice)`

This supports the overall picture that the app monitors direct device connectivity state and reacts to hardware presence.

## Service Layer

### `CrewDataService`

Class:

- `com.truerowing.crew.service.CrewDataService`

This is the central service that appears to bridge transport and app-level workout telemetry.

It implements:

- `SerialInterface`
- `WorkoutDataListener`
- `StatusDataListener`
- `DataDumpFileReaderListener`
- firmware/flasher listener interfaces
- `ICrewDataServiceSendCommand`

Important fields:

- `mCrewSerialManager`
- `mMockSerialManager`
- `mDataDumpFileReader`
- `mDataListeners`
- `mPacketeer`
- `mPacketLogger`
- `commandQueueManager`
- `driveStrokeRatioProcessor`
- `forceCurveProcessor`

Important method names seen in dexdump/string scans:

- `connected()`
- `data(byte[])`
- `serialState(CrewSerialManager.State)`
- `initializeObjects()`
- `getResistanceFromStatusProcessor()`
- `getSummaryStats()`
- `logSerialStatus()`

This strongly suggests the service receives raw serial bytes, packetizes/parses them, and then emits structured workout/status updates to higher layers.

## Structured Metric Models

### `HandleData`

Class:

- `com.truerowing.crew.service.data.HandleData`

Fields:

- `startHandlePosition : BigDecimal`
- `endHandlePosition : BigDecimal`
- `strokes : BigDecimal`

This indicates the stock app tracks handle travel / stroke geometry, not just summary workout numbers.

### `PreliminaryRowingData`

Class:

- `com.truerowing.crew.service.data.PreliminaryRowingData`

Fields:

- `forceCurveData : Map`
- `driveStrokeRatio : Double`

This indicates the app computes or exposes early-stroke analytics such as force curve and drive-to-recovery relationship.

### `RealTimeWorkoutData`

Class:

- `com.truerowing.crew.service.data.RealTimeWorkoutData`

Fields:

- `averageWatts : BigDecimal`
- `heartRate : BigDecimal`
- `remainingMeters : BigDecimal`
- `strokes : BigDecimal`
- `timeSeconds : BigDecimal`
- `totalCalories : BigDecimal`
- `totalMeters : BigDecimal`

It also stores formatted UI string versions for each metric.

This is evidence that the stock app has a normalized live workout model for the major session metrics.

### `StrokeData`

Class:

- `com.truerowing.crew.service.data.StrokeData`

Fields:

- `averageSplitSeconds : BigDecimal`
- `averageWatts : BigDecimal`
- `currentSplitSeconds : BigDecimal`
- `currentStrokesPerMinute : BigDecimal`
- `currentWatts : BigDecimal`
- `strokes : BigDecimal`
- `timeSeconds : BigDecimal`
- `totalCalories : BigDecimal`
- `totalMeters : BigDecimal`

It also stores formatted UI string versions.

This is the strongest direct evidence that the stock app has the exact core metrics a `RowPlus` replacement would care about:

- split
- stroke rate
- watts
- stroke count
- elapsed time
- calories
- distance

### `WorkoutDataListener`

Class:

- `com.truerowing.crew.service.processors.WorkoutDataListener`

Methods:

- `error(CrewException)`
- `onSendPreliminaryRowingData()`
- `receive(HandleData)`
- `receive(RealTimeWorkoutData)`
- `receive(StrokeData)`

This shows the stock app explicitly publishes those structured data objects through a listener pipeline.

## Preliminary Data Processors

Two processor classes indicate Hydrow intentionally triggers "preliminary rowing data" based on workout mode:

- `DistanceRowSendPreliminaryDataProcessor`
- `TimedRowSendPreliminaryDataProcessor`

`TimedRowSendPreliminaryDataProcessor` includes:

- `elapsedWorkoutBodyTimeToSendInitialRowingData`
- `workoutDataListener`
- `processData(RealTimeWorkoutData, Continuation)`
- `onBodyDataUpdate(...)`

This implies the app has workout-mode-specific logic for when to emit the early force-curve / drive-stroke analytics.

## What `RowPlus` Can Likely Reproduce

Based on static evidence, `RowPlus` should be able to reproduce the important live workout metrics **if** it can access or replicate the same serial transport:

- current split
- average split
- current SPM
- stroke count
- current watts
- average watts
- elapsed time
- distance
- calories
- heart rate (if fed into the same service path)
- force curve data
- drive/stroke ratio
- handle position data

## What Is Still Blocking a Clean Replacement

Even though the metrics clearly exist, several implementation questions remain:

1. `RowPlus` is a normal app, not a privileged system app.
   - We do not yet know whether it can open `/dev/ttyACM0`, `/dev/ttyMT1`, or `/dev/ttyUSB0` directly.

2. We do not yet know the packet framing/protocol.
   - `CrewDataService` references packetizing machinery (`mPacketeer`, `mPacketLogger`), but the wire format has not been decoded yet.

3. The stock app may compute some derived values.
   - For example, calories may be partially computed in-app rather than delivered as a raw sensor value.

4. `adb root` is blocked.
   - We cannot simply inspect the stock app's private runtime state or database from the outside.

## Practical Answer

If the question is "Can a custom app access the same core rowing metrics Hydrow uses?" then the answer is:

- **Very likely yes for the important live workout metrics**
- **Not yet proven for every derived metric or every implementation path**

If the question is "Can `RowPlus` read them right now?" then the answer is:

- **No, not yet**
- The current `RowPlus` build still uses mock data
- The next engineering step is to either:
  - reverse engineer the serial protocol enough to open and parse the same UART path, or
  - hook into the stock app's exposed behavior if a usable IPC surface exists

## Recommended Next Reverse-Engineering Steps

1. Inspect packet-related classes in `classes10.dex`
   - likely `PacketType`, `BrakePacketType`, packet parser/packeteer classes

2. Trace how `CrewDataService.data(byte[])` routes incoming serial bytes
   - identify the parse boundary from raw bytes to `RealTimeWorkoutData` / `StrokeData`

3. Confirm whether the UART device nodes are accessible to a normal app on this device
   - if not, pivot to a privileged/helper or alternate hook strategy

4. Inspect the mock path
   - `MockSerialManager` plus bundled workout logs may be useful for reconstructing the expected packet/data flow without live hardware capture

## Immediate Product Impact

For `RowPlus`, this means the project should move from "Can we get the metrics?" to "How do we replicate the serial protocol safely?"

The design and data model can proceed now with confidence that the target metric set is real and known:

- split
- SPM
- watts
- distance
- calories
- elapsed time
- heart rate
- force curve / drive ratio extensions

