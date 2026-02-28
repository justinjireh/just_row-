# Product / UX LLM Handoff Prompt

Use this prompt when handing the project to an LLM whose job is to improve product design, UX flow, and interaction design for the existing `RowPlus` app.

Before using this prompt, also read `docs/UI_GUIDELINES.md`. That file is the visual baseline and should be treated as binding design direction unless a deliberate design revision is being proposed.

```text
You are continuing product and UX design work for an Android app already running as the default launcher on a Hydrow rowing machine tablet.

Your job is to improve the product, information architecture, and on-device experience of the existing app without breaking its role as the main HOME screen.

## Product Goal

Design and refine a custom launcher + rowing app called `RowPlus` that:

- boots as the first screen users see
- feels like a dedicated fitness appliance UI, not a generic Android launcher
- supports two internal user profiles
- gives fast access to rowing, history, Browser, and Spotify
- hides maintenance/admin functions behind an intentional gesture
- eventually becomes a superior replacement for basic “Just Row” flow

## Ground Truth

Already true on the live device:

- The custom app package is `com.listinglab.rowplus`
- It is already installed
- It is already the default HOME app
- The stock Hydrow launcher is still installed but no longer the default

Current prototype capabilities:

- profile switch (`You`, `Wife`)
- mock row session screen
- local session history
- Browser launch
- Spotify launch if installed
- hidden admin menu via long-press on the app title

## Design Constraints

- The app is used on a rowing machine display
- Users need large, reliable targets and minimal friction
- The app should feel fast, clear, and robust while physically exercising
- Do not design for tiny tap targets or dense settings-heavy views
- Keep launcher safety and admin escape hatches intact

## Visual Baseline

The app should align with these visual rules:

- dark-mode primary surface with deep charcoal and near-black backgrounds
- high-contrast white typography with restrained gray hierarchy
- refined sans-serif type direction
- large full-bleed rowing video background with water, mist, and sunrise lighting
- subtle frosted dark glass overlays for metrics
- thin low-opacity white dividers
- cool blue and muted teal ambient glows
- generous negative space

The intended motion language should include:

- smooth microinteractions
- subtle motion-blur transitions
- elegant progress animation
- premium tactile button feedback

## Required UX Direction

The experience should feel like:

- a dedicated home dashboard
- one-tap entry into rowing
- easy user switching
- quick access to recent sessions and totals
- optional media access without cluttering the main flow

Avoid:

- generic app-grid launcher design
- busy Android-style settings screens on the home surface
- hidden critical actions except for admin-only functions

## What To Improve

Focus on these areas:

1. Launcher Information Architecture
   - what appears first
   - what is primary vs secondary
   - how profiles are surfaced
2. Row Start Flow
   - make “Start Row+” the clearest primary action
   - reduce hesitation before a session begins
3. Session History
   - better summaries
   - clearer progress feedback
   - more motivating review of prior rows
4. Admin / Escape Hatch
   - hidden but recoverable
   - protected by gesture + passcode
5. Shared Household Use
   - obvious switch between two people
   - no confusion about whose history is active

## Strong Product Requirements

Your design recommendations should support:

- two-person household use
- local-only operation when offline
- quick transitions while standing at the machine
- a visible path to:
  - current user
  - start row
  - recent history
  - Browser / Spotify

## Deliverables

When you respond, provide:

1. A concrete revised home screen layout
2. A proposed row session UX flow
3. A clearer history UX
4. An admin access pattern
5. Specific implementation guidance that can be translated into Android layouts and screens

If you suggest UI changes, tie them back to the existing app:

- `MainActivity`
- `SessionActivity`
- the current launcher role of the app

Your recommendations should also stay consistent with:

- `docs/UI_GUIDELINES.md`

## Important

Do not treat this as a generic mobile app.

Design for a dedicated rowing machine appliance where the user is physically engaged, often mid-workout, and needs an interface that is bold, obvious, and low-friction.
```
