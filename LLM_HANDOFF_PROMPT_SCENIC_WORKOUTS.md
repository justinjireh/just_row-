# Scenic Workouts Handoff Prompt

Use this prompt when handing the repository to an LLM that should build the workout library, scenic river modes, and related session UX.

```text
You are continuing development of the Android app in this repo: hydrow-rowplus-launcher.

Task:
Expand RowPlus from a single-session prototype into a structured workout product with preset modes, interval logic, and a Scenic Rivers experience.

Project context:
- Repo already contains the live Hydrow launcher prototype and session flow
- Android project root: hydrow-rowplus-launcher
- Package: com.listinglab.rowplus
- The app already has:
  - custom launcher
  - profile setup flow
  - pre-row, session, and post-row screens
  - replay-based telemetry prototype
- The app is already installed on the Hydrow and can be built locally with:
  - ./gradlew.bat assembleDebug

Primary objective:
Build a real workout-mode system that supports:
- Free Row
- goal-based rows
- intervals
- a workout library
- Scenic Rivers sessions with video-backed immersion and route progress

What to implement:
1. Add a workout mode model and selection system
   - centralize the available row modes in code
   - separate workout definition from session rendering
2. Build a workout library surface
   - browse workouts by category
   - support presets and future custom workouts
3. Add core workout modes first:
   - Free Row
   - Time Goal
   - Distance Goal
   - Intervals
   - Scenic Rivers
4. Add Scenic Rivers support
   - route metadata model
   - built-in sample route catalog
   - scenic background media support
   - route progress UI (percent complete, remaining distance, progress bar)
5. Keep telemetry integration flexible
   - current replay telemetry can drive route progress for now
   - do not hardcode the implementation so live telemetry cannot replace it later
6. Preserve current functionality
   - do not break launcher behavior
   - do not break profile storage
   - do not break session save flow
   - do not break pre-row/post-row navigation

Design and UX requirements:
- Follow docs/UI_GUIDELINES.md
- Keep the UI premium, dark, uncluttered, and appliance-like
- Scenic mode should use media as background, but metrics must remain readable
- Use the session screen as the base; extend it instead of creating a disconnected parallel app

Repo context to read first:
1. docs/START_HERE.md
2. docs/UI_GUIDELINES.md
3. docs/SCENIC_RIVERS_PLAN.md
4. docs/WORKOUT_MODES_CATALOG.md
5. LLM_HANDOFF_PROMPT_CODING.md

Implementation guidance:
- Introduce small, maintainable models such as:
  - WorkoutMode
  - WorkoutDefinition
  - ScenicRoute
- Prefer asset-backed sample content for v1
- Use local placeholder media references rather than requiring network playback
- Add clear extension points for future route packs and custom workout creation

Acceptance criteria:
- The app builds with ./gradlew.bat assembleDebug
- Users can choose from multiple workout modes instead of only the current basic path
- Scenic Rivers has a visible sample implementation with at least a few built-in routes
- Current installed functionality still works after the changes

Deliverables:
- Actual code changes in the existing Android project
- New models/resources/assets as needed
- Short summary of changed files
- Clear test steps for the Hydrow tablet

Important:
- Make code changes directly
- Do not only write planning notes
- Keep the work incremental and safe
```
