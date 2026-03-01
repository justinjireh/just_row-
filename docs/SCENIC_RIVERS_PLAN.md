# Scenic Rivers Plan

This document defines the first practical version of `Scenic Rivers` for RowPlus.

## Product Goal

Create an immersive rowing mode that combines:

- scenic background media for atmosphere
- route-style progress for structure
- existing RowPlus workout metrics for performance feedback

The first version should feel polished without depending on live streaming or full hardware reverse engineering.

## V1 Scope

`Scenic Rivers` should ship as a local, asset-driven mode:

- locally defined route metadata
- local placeholder background media references
- route progress based on session distance
- a simple progress UI layered onto the existing rowing session screen

V1 does not need:

- network streaming
- real map tile rendering
- true GPS
- dynamic scenery downloads
- pace-reactive video speed

## Core User Experience

1. User opens the workout library.
2. User selects `Scenic Rivers`.
3. User chooses a route preset.
4. Pre-row screen shows route name, location, target distance, and a short description.
5. Row session starts with:
   - scenic background media
   - standard rowing metrics
   - route progress bar
   - remaining distance
   - percent complete
6. Post-row summary shows route completion or partial progress.

## Data Model

Add a simple route model, for example:

- `id`
- `name`
- `location`
- `targetDistanceMeters`
- `optionalTargetDurationSeconds`
- `description`
- `thumbnailAsset`
- `backgroundMediaAsset`
- `difficulty`
- `tags`

Keep the model independent from the telemetry source.

## Built-In V1 Routes

Use a small starter set:

1. `Calm River 2K`
2. `Sunrise Lake 5K`
3. `Urban Canal 20 Min`
4. `Evening Glide 30 Min`
5. `Endurance River 10K`

These can initially use placeholder local media references and static route metadata.

## Progress Logic

The first route-progress implementation should use distance-based progress:

- `distanceRowed / targetDistanceMeters`
- clamp the result between `0` and `100%`
- compute remaining distance from the same source

If the selected route is time-based, use:

- `elapsedTime / targetDurationSeconds`

If live telemetry is not ready, the current replay telemetry may temporarily drive these values.

## Session UI Requirements

`Scenic Rivers` should re-use the existing session flow.

Add to the session UI:

- scenic background layer
- dark overlay for readability
- route title
- route progress bar
- remaining distance/time
- optional small route thumbnail or mini-map placeholder

Do not bury the rowing metrics. The user should still be able to read:

- split
- stroke rate
- distance
- watts
- time

## Media Handling

V1 should avoid network dependency.

Use:

- locally bundled sample media files, or
- local asset references / placeholders until real media is added

Behavior rules:

- if media loads, play it behind the session UI
- if media fails, fall back to a static scenic background
- do not block the workout if media is missing

## Future Upgrades

After V1 is stable, extend `Scenic Rivers` with:

1. More route packs
2. Real mini-map or route-line rendering
3. Pace-reactive media progression
4. Curated “real river” themed collections
5. Downloadable media packs

## Engineering Constraints

- Keep the implementation local and incremental
- Do not require root
- Do not create a second disconnected session system
- Extend the current workout/session architecture so live telemetry can slot in later
