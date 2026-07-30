# KAIROS OS Project State

## Current Task: Scheduled Tasks UI Bug Fixes (feature/scheduled-cron-tasks)

### Fixes Applied:
1. **Duplicate header on Scheduled screen**
   - Hid the global launcher overlay header when `activeScreen == "scheduled"` in `LauncherActivity.kt`. `ScheduledScreen` now owns the sole top bar (title, back, refresh).

2. **Tab labels**
   - Removed run/job counts from tab names; tabs now read `RUNS` and `JOBS`.

3. **Schedule config panel visibility**
   - Daily/Weekly, time picker, and ACTIVATE only appear after a valid app/integration mention is selected from the app drawer — not on bare `@`.

4. **Run timestamp timezone**
   - `ScheduledRunCard` parses ISO `started_at` and formats it in the device timezone via `ZoneId.systemDefault()`.

5. **Edit modal & job card polish**
   - Active switch: orange thumb only, neutral track.
   - Job card spacing increased between status, title, prompt, and schedule time.
   - Edit modal DELETE uses error color with delete icon for clearer destructive affordance.

### Verification:
- Android debug build (`./gradlew assembleDebug`) verified clean compilation.
