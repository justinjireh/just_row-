# Release Notes: v0.1-prototype

This document captures the repository and device state represented by the `v0.1-prototype` tag.

## Release Summary

`v0.1-prototype` is the first working Hydrow `RowPlus` prototype. It establishes device access, a custom launcher path, and a safe base for follow-on engineering work.

## What Is Included

- A working Android app project at `hydrow-rowplus-launcher/`
- A compiled and installable debug build of `com.listinglab.rowplus`
- A default `HOME` launcher implementation that can replace the stock Hydrow launcher in normal use
- Internal profile switching for household use
- A mock `Row+` session flow with local session persistence
- Launcher actions for Browser, Spotify, Hydrow, and Android Settings
- Project briefs, LLM handoff prompts, and contributor docs
- Pulled Hydrow APKs for reverse engineering (`HydrowLauncher.apk` and `crew-base.apk`)

## Verified Device State

The following state was verified on the live Hydrow tablet before this release tag:

- Device: `foenix_h`
- Android version: `10`
- `adb` connectivity is working
- `adb root` is blocked on the production build
- Stock launcher package: `com.truerowing.hydrowlauncher`
- Stock rowing app package: `com.truerowing.crew`
- Custom launcher package: `com.listinglab.rowplus`
- Default `HOME` activity set to `com.listinglab.rowplus/.MainActivity`

## Important Safety Properties

- Stock Hydrow system apps remain installed
- The stock Hydrow launcher was not removed
- The prototype is intended to be reversible
- Hidden admin actions still provide a path back to Hydrow and Android Settings

## Known Limitations

- The current row session screen is still a prototype and does not yet consume real rowing telemetry
- Metrics integration with `com.truerowing.crew` is not implemented
- The app currently uses a lightweight local persistence approach and still needs a stronger data layer
- The installed build is a debug build, not a signed release build

## Primary Next Steps

1. Replace the prototype persistence layer with Room or SQLite.
2. Add admin protection and a cleaner household UX.
3. Reverse engineer the telemetry path in `crew-base.apk`.
4. Replace mock session values with real live rowing metrics.

## Reference Docs

- [../HYDROW_CUSTOM_APP_BRIEF.md](../HYDROW_CUSTOM_APP_BRIEF.md)
- [../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md](../HYDROW_IMPLEMENTATION_STATUS_BRIEF.md)
- [../NEXT_STEPS.md](../NEXT_STEPS.md)
