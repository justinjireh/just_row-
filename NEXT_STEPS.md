# Next Steps

This is the prioritized engineering backlog for the `RowPlus` project.

## Priority 0: Keep The Device Safe

- Preserve `RowPlus` as a working `HOME` app.
- Keep the stock Hydrow launcher available as a fallback.
- Do not introduce changes that can strand the tablet without a launcher.
- Keep admin access to Android Settings and the stock Hydrow app.

## Priority 1: Production-ize The App Foundation

- Refactor the current prototype into clearer layers:
  - UI
  - data
  - domain
  - integrations
- Keep the package name stable:
  - `com.listinglab.rowplus`
- Preserve installability after each change.

Definition of done:

- source structure is easier to extend
- launcher behavior remains unchanged
- code is no longer centered around prototype-only classes

## Priority 2: Replace Temporary Storage

- Replace `SharedPreferences` JSON storage in `SessionStore`
- Introduce Room / SQLite
- Create persistent entities for:
  - user profiles
  - row sessions
  - optional splits / snapshots
  - aggregate user stats

Definition of done:

- session data survives app restarts and schema evolution cleanly
- history no longer depends on ad hoc JSON serialization

## Priority 3: Harden Admin Access

- Replace the current simple long-press admin trigger
- Add:
  - hidden gesture
  - passcode gate
  - or both

Admin panel must still allow:

- launch stock Hydrow app
- open Android Settings
- manage home-app recovery path

Definition of done:

- normal users do not accidentally access admin functions
- recovery path remains available

## Priority 4: Improve The Launcher Dashboard

- Upgrade `MainActivity` from prototype to a polished appliance dashboard
- Improve:
  - visual hierarchy
  - user profile visibility
  - recent row summaries
  - quick actions

Definition of done:

- the home screen feels like a dedicated rower UI
- the main action is clearly `Start Row+`

## Priority 5: Reverse Engineer Real Telemetry

- Analyze `device_apks/crew-base.apk`
- Map likely classes and services related to:
  - Bluetooth
  - USB
  - rower hardware events
  - workout metric models

Strong clues already known:

- package: `com.truerowing.crew`
- receiver: `com.truerowing.crew/.service.receivers.UsbStateReceiver`

Definition of done:

- a documented likely telemetry path exists
- the next code integration point is clear

## Priority 6: Replace Mock Metrics

- Remove simulated metrics from `SessionActivity`
- Introduce a pluggable telemetry adapter
- Feed real data into the `Row+` session flow

Definition of done:

- distance, duration, split, and stroke rate use live hardware input
- local session history records real values

## Priority 7: Session And History Enhancements

- Add better per-user summaries
- Add aggregate totals and PRs
- Add richer history detail
- Consider export options later

Definition of done:

- each user has meaningful persistent progress tracking

## Priority 8: Secondary App Experience

- Improve Browser and Spotify launch flow
- Detect whether Spotify is installed
- Add friendlier fallback behavior if it is not

Definition of done:

- auxiliary apps feel integrated but secondary

## Suggested Work Sequence

1. Refactor app structure
2. Add Room
3. Harden admin access
4. Improve launcher UI
5. Reverse engineer telemetry
6. Integrate live metrics
7. Improve history and stats

## Success Criteria

The project is in a strong state when:

- `RowPlus` is a stable full-time launcher
- the app stores real rowing sessions per user
- the stock Hydrow apps remain recoverable
- telemetry is no longer mocked
- contributors can continue the project from GitHub alone

