# KAIROS OS Hackathon Progress & Ticketing

This file serves as the permanent memory for the automated developer and evaluator agents. 
Every open task must have **Context**, **Technical Requirements**, and **Acceptance Criteria** so agents can execute them autonomously without needing to constantly re-read the core PRD.

---

## 🟢 COMPLETED TASKS

### 24. KaiCalendar Overhaul (Week/Month Views, Rich Event Scheduler, 2-Way Google Sync & AI Tool Updates)
**Status:** DONE
**Branch:** `main`
**Summary:** Implemented requested overhaul for KaiCalendar:
- **Header View Mode Toggle**: Added a toggle button in the top action bar (beside theme toggle) to switch between **Week View** and **Month View**.
- **Week View (7-Day Collapsible Accordion)**: Renders Today + next 6 days as collapsible day sections. Header formatted as `"Sunday, July 26, 2026"`. Today is expanded by default; remaining days can be expanded/collapsed on tap with event badges.
- **Month View (Split View)**:
  - Top 50%: Interactive month grid with Month/Year title and `< >` navigation arrows. Preselects current date, displays primary-colored event indicators for days with events.
  - Bottom 50%: Displays a slimmed-down agenda list of events for the selected day.
- **Rich Schedule Event Dialog**: Removed single `Duration` input and replaced with native **Date Picker**, **Start & End Time Pickers**, **"All-Day Event" Checkbox** (hides time pickers when checked), and **"Sync with Google / Connected Calendars" Checkbox**.
- **2-Way Google & System Calendar Sync**: Listened via `ContentObserver` on `CalendarContract.Events.CONTENT_URI` with a 90-day rolling window query. Events created via KaiCalendar, Google Calendar, or local Gemma AI tools automatically refresh and display.
- **Local AI Agent Tool Compatibility**: Updated `LocalAgentEngine.kt` tool signature for `create_calendar_event` (`start_date`, `start_time`, `end_date`, `end_time`, `is_all_day`, `sync_google`) and dispatch logic.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 25s.

### 23. KaiNotes Context-Injected Semantic Note Search (Approach 1)
**Status:** DONE
**Branch:** `main`
**Summary:** Implemented Approach 1 for semantic note retrieval using Gemma 4 E2B on-device context injection:
- **Database Context Injection**: When `@kainotes` commands are processed in `LocalAgentEngine.kt`, all stored local notes (IDs, titles, and content snippets) are fetched from Room DB and dynamically injected into Gemma's system prompt.
- **`get_note_by_id` Tool**: Declared `get_note_by_id(id)` in Gemma's system prompt, allowing Gemma to evaluate semantic intent (e.g. "buying dinner") against note candidates (e.g. "Grocery List") and directly return the target note by ID.
- **Smart Fallback in `search_notes`**: If exact SQL `LIKE` keyword search returns 0 rows, `search_notes` automatically falls back to checking Gemma-provided IDs or soft fuzzy token matching in memory.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 43s.

### 21. Notification Interceptor Audit, Logcat Diagnostics, 0ms Instant Settings & Per-App Rules Overhaul
**Status:** DONE
**Branch:** `main`
**Summary:** Overhauled the Notification Interceptor and Settings architecture:
- **Logcat Diagnostic Logs**: Added explicit `KairosNotificationListener` & `KairosNotificationClassifier` Logcat output indicating LiteRT-LM (Gemma) model status, prompt inputs, and raw Gemma output.
- **Removed Hardcoded Tier 0 System Bypasses**: Removed system-level blanket overrides (`CATEGORY_MESSAGE`, package name checks for dialer/messaging) in favor of pure settings-driven policy & AI/heuristic message content evaluation.
- **Settings-Level Default Whitelisting**: Essential core native apps (Dialer, SMS, Clock, Calendar) automatically seed into Room DB as **Allowed (Whitelisted)** on initial launch.
- **Per-App Notification Rules Screen (`NotificationRulesScreen.kt`)**: Implemented a dedicated screen with search filtering and 3-way segmented toggles (**Allowed**, **Blocked**, **Kai Decides**), persisted in Room DB (`AppNotificationRuleEntity`, `AppNotificationRuleDao`).
- **0ms Instant Local-First Toggles**: App distraction toggles, leisure sliders, and notification rules update local `SharedPreferences` and Room DB immediately for 0ms UI latency, with background non-blocking cloud synchronization.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL.

### 20. Kai Local Apps UI Refinements, Markdown Editor & Header Save Status, Vertical Timer Slider, and Google Sans Typography
**Status:** DONE
**Branch:** `main`
**Summary:** Executed requested visual and functional refinements across Kai Notes, Kai Calendar, and Kai Clock/Alarm:
- **Kai Notes Main View:** Removed "Notes & Thoughts" header line. Made search bar full-width. Replaced top add button with a floating circular `+` button positioned at ~60% height from top of screen.
- **Kai Notes Editor:** Removed bottom `EDIT`, `PREVIEW`, `CANCEL`, `SAVE NOTE` buttons and `EDIT NOTE` text. Default view opens in Markdown Preview Mode; tapping/clicking content seamlessly switches into Edit Mode. Borderless title with 24.sp bold and low-opacity bottom indicator line.
- **Top Header Status Icon:** Integrated 3-state check/save status icon in `LauncherActivity.kt` header beside theme toggle: Gray Check (`#888888`) for stationary/unmodified, Orange Save (`#FF6B00`) when changes are made (tapping saves to DB), and Orange Check (`#FF6B00`) post-save. Resets to Gray Check on re-entry.
- **Kai Clock & Timer:** Removed "Scheduled Alarms" header text. Replaced static timer inputs and preset buttons (`+1m`, `+5m`, `+15m`) with a Vertical Timer Slider featuring top & bottom fading gradient overlays (`Brush.verticalGradient`). Replaced add alarm button with floating `+` circle button at ~60% height.
- **Kai Calendar:** Removed "Upcoming Agenda" header text. Replaced add event button with floating `+` circle button at ~60% height. Updated `LocalCalendarController.kt` to query system primary calendar ID (`CalendarContract.Calendars.CONTENT_URI`) for native Android calendar event sync.
- **Typography:** Changed action button fonts across all 3 apps to `googleSansFont`.
- **Verification:** `./gradlew assembleDebug` — BUILD SUCCESSFUL.

### 18. Intent Layer & Settings Overhaul (On-Device Gemma 4 Reason Validation + App Settings)
**Status:** DONE
**Summary:** Implemented the complete Intent Gate system and Settings Overhaul. Added Supabase database tables (`intent_logs`, `user_settings`, and cooling-off extensions to `user_app_configs`) and database helpers. Created Next.js API endpoints (`/api/intent/log`, `/api/settings`, `/api/settings/apps`). Built on-device intent reason validation using Gemma 4 via LiteRT-LM SDK (`OnDeviceIntentValidator.kt`). Added `IntentViewModel.kt` for state management. Wired `LauncherActivity.kt` to intercept `@app` mentions for distracting apps, clear chat input box on distracting mention, present 6 time pills (5m-1hr), 80-character reason limit, live character counter, validation feedback, and budget indicator. Disabled send button and made open app icon button grayed out until time + valid reason are set. Overhauled Settings drawer header to "Settings" and replaced boilerplate toggles with "App Settings" and "System Settings". Built `AppSettingsScreen.kt` with a daily leisure budget slider and per-app distraction toggles with 12-hour cooling-off period protection. Verified builds via Next.js `npm run build` and Android `gradlew assembleDebug`.

---

## 🔴 OPEN TASKS

None. All scheduled tasks are completed!
