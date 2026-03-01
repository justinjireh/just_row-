# Hydrow Serial Protocol — Decoded

Date: 2026-02-28

## Summary

The Hydrow rowing machine communicates with its tablet over a UART serial link. The stock app (`com.truerowing.crew`) reads raw bytes from a serial port, assembles CR/LF-terminated ASCII lines, and dispatches them to type-specific parsers. This document fully decodes the wire protocol from static analysis of `crew-base.apk` and its bundled workout log files.

---

## Transport Layer

### Serial Port

| Parameter | Value |
|-----------|-------|
| Baud rate (normal) | **921600** |
| Baud rate (flash) | 115200 |
| Line terminator | `\r\n` (CR + LF) |
| Max line length | 500 bytes |
| Device nodes (tried in order) | `/dev/ttyACM0`, `/dev/ttyMT1`, `/dev/ttyUSB0` |
| Native library | `libserial_port.so` (arm64-v8a, armeabi-v7a, x86) |
| JNI class | `android_serialport_api.SerialPort` |

**Device status on our Hydrow:**
- `/dev/ttyMT1` exists (symlink → `/dev/ttyS1`)
- `/dev/ttyACM0` does not exist
- `/dev/ttyUSB0` does not exist

### Handshake Sequence

The `CrewSerialManager` performs this handshake before entering data mode:

```
→ "?"                     (TEDDY_SYNC_MSG)
← "OK"                    (TEDDY_SYNC_OK_RESPONSE)
→ "A 0\r\n"               (TEDDY_ECHO_OFF_MSG — disable echo)
→ "J\r\n"                 (TEDDY_PART_ID_CONFIRMATION_MSG)
→ "U 23130\r\n"           (TEDDY_UNLOCK_CODE_MSG)
→ "T\r\n"                 (TEDDY_REBOOT_TO_NORMAL_MSG — optional)
```

Handshake retry: every 250ms (`HANDSHAKE_RETRY_PERIOD_MILLIS`).

### Data Modes

| Command | Response | Meaning |
|---------|----------|---------|
| `Cm 1\r` | `Rm 1` | Enable continuous data streaming |
| `Cm 0\r` | `Rm 0` | Disable continuous data streaming |

---

## Packet Architecture

```
Raw serial bytes
    │
    ▼
ResponsePacketeer          — Assembles CR/LF lines from byte stream
    │
    ▼
getPacketFromString()      — Routes line to correct parser by prefix
    │
    ├── Di2PacketParser    — High-frequency interval data
    ├── Ds2PacketParser    — Per-stroke summary data
    ├── ResistanceLevelPacketParser  — Drag factor response
    ├── SerialNumberPacketParser     — Serial number response
    ├── VersionPacketParser          — Firmware/hardware version
    ├── HealthPacketParser           — Health/diagnostic data
    ├── StrokeBrakePacketParser      — Brake controller stroke data
    ├── InstantaneousBrakePacketParser — Instantaneous brake data
    ├── RebootPacketParser           — Reboot reason/status
    ├── ContinuousResponsePacketParser — Cm mode confirmation
    ├── DebugResponsePacketParser     — Debug output
    ├── ErrorPacketParser            — Error responses
    ├── ErrorMessagePacketParser     — Error detail messages
    ├── CommandDataResponsePacket     — Generic command response
    └── RebootToBootloaderResponsePacketParser
    │
    ▼
PacketListener             — Dispatches ResponsePacket to service layer
    │
    ▼
CrewDataService            — Computes workout metrics from raw packets
```

---

## Packet Formats

All packets are ASCII, space-delimited. The prefix character(s) identify the type.

### Outgoing Commands (App → Rower)

Prefixed with `>` in log files. Not part of wire format — the `>` is a log convention.

| Command | Meaning |
|---------|---------|
| `Cv` | Query firmware/hardware version |
| `Cs` | Query serial number |
| `Ql` | Query resistance/drag level |
| `Cm 1` | Start continuous data mode |
| `Cm 0` | Stop continuous data mode |
| `?` | Sync/handshake probe |
| `A 0\r\n` | Disable echo |
| `J\r\n` | Part ID confirmation |
| `U 23130\r\n` | Unlock code |

### Incoming Responses (Rower → App)

#### `Di2` — Data Interval v2 (High-Frequency Telemetry)

**Rate:** ~100-125 samples/second (8-10ms between samples)

**Format:** `Di2 <distance> <handleForce> <handlePosition> <power> <sequence>`

| Field | Type | Unit | Description |
|-------|------|------|-------------|
| distance | BigDecimal | meters (delta) | Incremental distance since last sample |
| handleForce | BigDecimal | Newtons | Force on handle (0 during recovery) |
| handlePosition | BigDecimal | (unit TBD) | Handle position / travel |
| power | BigDecimal | (accumulated) | Accumulated metric (likely total distance in km or time) |
| sequence | Int | counter | Monotonic sample counter |

**Example (recovery phase — no force):**
```
Di2 0.016745 0.000 0.000 0.549 10206
```

**Example (drive phase — force applied):**
```
Di2 0.020338 90.961 94.536 0.751 10283
```

**Key observations from log analysis:**
- `distance` ranges ~0.000444 to ~0.021 per sample (delta distance in meters)
- `handleForce` is 0.0 during recovery, peaks at 100-200+ during drive phase
- `handlePosition` correlates with handleForce; represents the instantaneous force curve
- `power` accumulates slowly across the session (likely total distance in km)
- ~120 Di2 samples per stroke at 22-28 SPM

#### `Di` — Data Interval v1 (Legacy Format)

**Format:** `Di <field1> <field2> <field3> <field4> <counter>`

| Field | Type | Likely meaning |
|-------|------|---------------|
| field1 | Int | Split time or flywheel period (decreases as speed increases) |
| field2 | Int | Related to force/torque (8-19 range) |
| field3 | Int | Always 0 in observed data |
| field4 | Int | Countdown or distance-related (decreases over workout) |
| counter | Int | Sample counter |

**Example:**
```
Di 5981 8 0 946 33361
```

Note: Di2 is the current format on modern firmware. Di appears in older logs.

#### `Ds2` — Data Stroke v2 (Per-Stroke Summary)

**Emitted:** Once per complete stroke cycle (drive + recovery)

**Format:** `Ds2 <averagePower> <distance> <endHandlePos> <startHandlePos> <endOfDriveSeq> <endOfRecoverySeq> <sequence>`

| Field | Type | Description |
|-------|------|-------------|
| averagePower | BigDecimal | Average watts for this stroke |
| distance | BigDecimal | Total accumulated distance |
| endHandlePosition | BigDecimal | Handle position at end of drive |
| startHandlePosition | BigDecimal | Handle position at start of drive |
| endOfDriveSequence | Int | Di2 sequence number at end of drive phase |
| endOfRecoverySequence | Int | Di2 sequence number at end of recovery phase |
| sequence | Int | Stroke sequence counter |

**Example:**
```
Ds2 0.464 1.573 43.047 3.092 5185 9562 39163
```

#### `Rv` — Version Response

**Format:** `Rv F<firmware>H<hardware>`

**Example:** `Rv F00035H00256`
- Firmware version: 00035
- Hardware version: 00256

#### `Rs` — Serial Number Response

**Format:** `Rs <serial_string>`

**Example:** `Rs 203335463553501200490019`

#### `Rl` — Resistance Level Response

**Format:** `Rl <drag_factor>`

**Example:** `Rl 104` (drag factor = 104)

Correlates with filenames like `22spm_fan_4_drag_104.log`.

#### `Rm` — Mode Confirmation

**Format:** `Rm <mode>`

**Example:** `Rm 1` (continuous mode enabled)

---

## Data Flow: From Packets to Workout Metrics

```
Di2 stream ──┐
             ├──▶ CrewDataService ──▶ WorkoutDataListener
Ds2 stream ──┘         │
                       ├──▶ receive(StrokeData)
                       ├──▶ receive(RealTimeWorkoutData)
                       ├──▶ receive(HandleData)
                       └──▶ onSendPreliminaryRowingData()
                                   │
                       ┌───────────┘
                       ▼
              forceCurveProcessor
              driveStrokeRatioProcessor
```

### Computed Metrics (from Di2 + Ds2)

| Metric | Source | Computation |
|--------|--------|------------|
| Total distance | Di2.distance (sum) | Accumulate delta distances |
| Split time (/500m) | Di2.distance | 500 / instantaneous_speed |
| Stroke rate (SPM) | Ds2 timestamps | 60 / (stroke_duration_seconds) |
| Watts | Ds2.averagePower | Direct from stroke packet |
| Calories | Derived | Standard rowing calorie formula from watts |
| Stroke count | Ds2 sequence | Count of Ds2 packets |
| Elapsed time | Timestamps | Current time - workout start time |
| Force curve | Di2.handleForce | Collect force samples during drive phase |
| Drive:recovery ratio | Di2/Ds2 | Drive samples / recovery samples |
| Handle travel | Ds2 | endHandlePosition - startHandlePosition |

---

## Log File Format

The bundled `.log` files in `assets/` use this format for mock serial playback:

```
<epoch_ms> <packet_line>
```

Or with metadata:
```
<epoch_ms> ## Date: YYYY_MM_DD_HHMMSS
<epoch_ms> ## Serial-Number: <serial>
<epoch_ms> >Cm 1                          (outgoing command, > prefix)
<epoch_ms> Rm 1                           (incoming response)
<epoch_ms> Di2 0.016745 0.000 0.000 0.549 10206
```

Older format uses a simple counter instead of epoch timestamp:
```
<counter> Di <field1> <field2> <field3> <field4> <seq>
```

---

## Bundled Test Logs

| File pattern | Content |
|-------------|---------|
| `0_499m_distance_workout.log` | 499m distance workout with Di2 + Ds2 |
| `0_499m_distance_workout_unittest.log` | Stripped version for testing |
| `0_54min.log` | 54-minute timed workout |
| `0_di2_26_104.log` | Di2 data at 26 SPM, drag 104 |
| `{spm}_fan_{n}_drag_{d}.log` | Calibration logs at specific SPM/drag combos |
| `2019_*_*spm_drag_*.log` | 2019 calibration data at various SPM/drag combos |
| `0_playback_v2_45min.log` | 45-minute playback test |

These files are gold — they contain real hardware packet captures that can be used to test a parser without a live connection.

---

## Implementation Notes for RowPlus

### What RowPlus Needs to Do

1. **Open serial port** — `/dev/ttyMT1` at 921600 baud (or try all 3 paths)
2. **Run handshake** — Send `?`, wait for `OK`, send echo-off / unlock sequence
3. **Enable continuous mode** — Send `Cm 1\r`
4. **Assemble lines** — Buffer incoming bytes, split on `\r\n`
5. **Route by prefix** — Parse `Di2`, `Ds2`, `Rv`, `Rs`, `Rl` lines
6. **Compute metrics** — Accumulate distance from Di2 deltas, compute split from speed, extract watts/SPM from Ds2
7. **Update UI** — Push metrics to session display at ~1 Hz

### Permission Concern

The stock app ships `libserial_port.so` which calls native `open()` on the device node. Whether a non-system app can open `/dev/ttyMT1` depends on the device node permissions. This must be tested on the live device.

### Fallback Strategy

If `/dev/ttyMT1` is not accessible to a normal app:
- Option A: Use `su` (requires root — blocked on this device)
- Option B: Use USB host API if the rower appears as a USB serial device
- Option C: Use a companion helper service with elevated permissions
- Option D: Log replay from bundled `.log` files for demo/testing
