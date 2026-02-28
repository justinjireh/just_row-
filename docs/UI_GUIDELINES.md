# RowPlus UI Guidelines

This document is the visual and interaction baseline for future `RowPlus` UI work.

Use it as the source of truth when refining the launcher dashboard, workout browsing, and premium appliance feel of the Hydrow replacement experience.

## Visual Style

- Dark-mode primary interface with deep charcoal and near-black backgrounds
- High-contrast white typography with subtle gray hierarchy
- Refined sans-serif type direction similar to SF Pro, Inter, or Neue Haas Grotesk
- Clean, minimal UI chrome
- Large full-bleed hero rowing video background with water, mist, and sunrise lighting
- Subtle glassmorphism panels for metrics overlays using frosted dark blur with roughly 8-12% transparency
- Thin white divider lines at low opacity, roughly 10-15%
- Soft ambient gradient glows using cool blue and muted teal accents
- No clutter; preserve generous negative space

## Layout Direction

## Main Dashboard Screen

The primary launcher screen should present:

- Live rowing video background
- Overlay performance metrics: split time, stroke rate, distance, and watts
- Progress bar timeline at the bottom
- Minimal top navigation with profile icon and settings access

The main call to action should remain visually dominant:

- `Start Row+` should be the clearest action on screen
- Secondary actions should stay in a lighter-weight dock or rail

## Secondary Screen

The secondary browsing surface should present:

- Workout library with cinematic thumbnails
- Horizontal category scrolling
- Clean filter chips

This screen should feel curated and premium, not like a generic streaming app clone or an Android app drawer.

## Interaction Design

- Smooth microinteractions
- Subtle motion-blur style transitions
- Elegant progress animations
- Premium tactile button states

## Product-Level Interpretation

These guidelines should result in an interface that feels like:

- a premium fitness appliance
- visually immersive but operationally simple
- bold enough to use while standing and preparing to row
- calm and minimal when idle

## Implementation Notes

- Prefer native Android implementation over a `WebView` for production UI
- Use the existing HTML mockup only as visual reference, not as the long-term implementation
- Preserve large tap targets and low-friction navigation
- Keep admin and recovery actions accessible but visually de-emphasized
- Do not let Browser or Spotify overwhelm the primary rowing workflow

## Applies To

These guidelines should directly influence:

- `MainActivity` launcher/dashboard work
- future workout library screens
- `SessionActivity` visual refinement
- future transitions between launcher, session, history, and media surfaces
