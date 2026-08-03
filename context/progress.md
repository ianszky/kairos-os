# KAIROS OS Hackathon Progress & Ticketing

This file serves as the permanent memory for the automated developer and evaluator agents. 
Every open task must have **Context**, **Technical Requirements**, and **Acceptance Criteria** so agents can execute them autonomously without needing to constantly re-read the core PRD.

---

## 🟢 COMPLETED TASKS

### 33. Async Prompt Jobs (Phase 2)
**Status:** DONE
**Branch:** `feature/async-prompt-jobs`
**Summary:** Long-running Composio prompts no longer block the HTTP request or hit the Android 60s timeout:
- **`prompt_runs` table** + migration SQL for run lifecycle tracking
- **POST /api/prompt** returns 202 + `runId`; `processIntent` runs in Next.js `after()`
- **GET /api/prompt/status** for polling completed/failed responses
- **Android `postPromptAndAwait()`** polls every 2s up to 5 min while Running Agent stays Processing
- **Tool executor cap**: max 5 successful calls per tool name per request
- **Verification**: apply `context/supabase_prompt_runs_migration.sql`, run backend vitest, retry bulk calendar prompt

### 32. Tool Executor Default Tool Scoping Fix
**Status:** DONE
**Branch:** `fix-tool-scoping-loop`
**Summary:** Fixed Google Calendar (and other integrations) failing on multi-step prompts when classifier emitted a single `taskType`:
- **`resolveToolSlugs`**: Always loads `COMPOSIO_ACTION_MAP[app].default`; `taskType` is prompt context only.
- **Loop safety**: `MAX_ITERATIONS` raised 5 → 8; clearer logging when loop exits with pending function calls; keeps `lastToolResult` text fallback (no forced JSON turn).
- **Verification**: `npm test -- --run src/lib/mcp/tool-executor.test.ts` in backend.

### 31. On-Device Gemma Voice Input (STT)
**Status:** DONE
**Branch:** `feature-gemma-voice-stt`
**Summary:** Replaced platform-only mic STT with Gemma 4 E2B on-device ASR via LiteRT-LM, with Android SpeechRecognizer / system voice UI fallback:
- **`LocalLlmClient`**: Enables `audioBackend = Backend.CPU()` with GPU→CPU→text-only init cascade; exposes `isAudioReady()`.
- **`AudioRecorder` + `GemmaSttClient`**: Captures 16 kHz mono WAV (≤28s), runs Gemma ASR via `Content.AudioBytes`.
- **`LauncherActivity`**: Mic uses Gemma when model audio backend is ready; falls back to `SpeechRecognizer` and `RecognizerIntent` (including `ERROR_CLIENT`).
- **Verification**: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL.

### 30. Running Agents ↔ Android Notification Two-Way Sync
**Status:** DONE
**Branch:** `feature/agent-notification-sync`
**Summary:** Two-way sync between home Running Agents stack and Android status-bar notifications:
- **Agent → Android**: `AgentNotificationSync` observes Room `running_agents` and posts system notifications mirroring card title/status. Processing uses low-importance ongoing channel; complete/fail re-posts on high-importance channel with heads-up alert. Swipe-dismiss on card or clearing shade notification deletes the Room row.
- **Session → Home**: Active intentional-app grants appear as `AppGrantCard` in the Running Agents stack with live countdown. Swipe hides in-app card only (`SessionCardHideStore`); Android timer FGS and access grant keep running.
- **Deep links**: Tapping agent notifications opens the conversation in chat via `AgentNotificationNavigationStore`.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in ~3m.

### 29. Agent Card ID Parity & Kai Apps Hybrid UI/UX Overhaul
**Status:** DONE
**Branch:** `feature/agent-card-id-sync-fix`
**Summary:** Resolved 2 core architectural issues with Agent Cards and Kai Apps prompt routing:
- **Conversation ID Parity**: Updated cloud fallback in `LauncherActivity.kt` to reuse `dispatchConvId` when creating/upserting Supabase conversations (`mapOf("id" to targetConvId, ...)`). This ensures 1:1 ID parity across agent cards, Room DB, Supabase DB, and sidebar history, resolving the bug where cards remained stuck on "Processing...".
- **Kai Apps vs Standard Apps Routing**: Standard apps (`@spotify`, `@youtube`) always launch native packages and display `OpenInNew`. Kai Apps (`@kaicalendar`, `@kainotes`, `@kaiclock`) act as hybrids: without prompt text, they display `OpenInNew` and open the app screen; when prompt text is typed, the icon switches to `Send` and dispatches an agent prompt with a running card.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 47s.

### 28. Agent Widget UX, Title Synchronization & Chat Loading Fixes
**Status:** DONE
**Branch:** `feature/agent-widget-ux-fixes`
**Summary:** Resolved 3 Running Agent Widget UX and state synchronization issues:
- **Direct Title Synchronization**: Updated `LocalTitleGenerator.generateAndSaveTitle` to return the generated title string directly, updating `RunningAgentsViewModel.updateTitle` instantly without relying on stale async state.
- **Chatbar Auto-Clear & Keyboard Dismissal**: Prompt submissions now automatically clear text input, reset attachments, clear focus via `LocalFocusManager`, and hide the software keyboard via `LocalSoftwareKeyboardController`.
- **Card Entrance Animation**: Wrapped `CollapsedAgentStack` in `AnimatedVisibility` for a smooth slide-up entrance animation above the chatbar when a new agent task starts.
- **Restored Three-Dots Indicator**: `onViewAgent` now appends `Interaction.Loading()` when navigating to an active agent in `PROCESSING` status, displaying the three-dots loading bubble in `ChatView`.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 47s.

### 27. 2-Pass Synthesis Engine for KaiNotes & Local Tools (Strategy 1)
**Status:** DONE
**Branch:** `main`
**Summary:** Implemented an on-device 2-Pass Synthesis Engine in `LocalAgentEngine.kt` so local tools can perform cognitive tasks (summarization, analysis, key point extraction, Q&A) on notes and calendar events:
- **Pass 1 LLM-Driven Synthesis Gate**: Updated Pass 1 tool selection system prompt to include `"needs_synthesis": true / false` in Gemma's JSON output.
- **Fast Path for Direct Retrieval**: Simple view/open commands (`"needs_synthesis": false`) skip Pass 2 and return raw widget cards with zero extra latency.
- **Pass 2 Synthesis Pipeline (`runSynthesis`)**: When `"needs_synthesis": true`, retrieved note contents or calendar event details are formatted and passed back to Gemma 4 E2B to generate a tailored natural language summary or analysis.
- **Unified Output**: Returns `KairosResponse` containing both the AI-generated synthesis text and the underlying source widget card (`NOTE_CARD` or `CALENDAR_EVENT`).
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 24s.

### 26. Global Live Date & Time System Prompt Injection
**Status:** DONE
**Branch:** `main`
**Summary:** Updated system prompt construction across all prompt execution tiers (simple conversational prompts, local intent classifier, local tools agent, and cloud backend requests) to inject live client date and time context (`yyyy-MM-dd HH:mm EEEE`):
- **Simple Local Prompts (`handleSimplePrompt`)**: Injected system prompt header containing `Current date and time: $currentDateStr` so general conversational queries know the exact date/time context on-device.
- **Intent Classifier (`classifyPrompt`)**: Added `Current date and time: $currentDateStr` into `classificationPrompt` so relative time expressions (e.g. "tomorrow", "next Monday") are accurately classified into local/cloud tiers.
- **Cloud API Client (`KairosApiClient.kt`)**: Added `currentDate` field to `PromptRequest` and auto-populated client-side formatted timestamp so cloud requests carry the exact local date & time.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 24s.

### 25. KaiCalendar Visual Polish & Event Edit/Delete Dialog
**Status:** DONE
**Branch:** `main`
**Summary:** Refined KaiCalendar UI aesthetics and interaction capabilities based on user feedback:
- **Warm Orange Accent Palette**: Replaced blue accent with KAIROS OS's signature brand warm accent (`Color(0xFFFF6B00)`) for selected date highlights, event indicator dots, FAB button, time labels, and side accent bars.
- **Borderless Card Clean Layout**: Removed borders on week view and month view cards (`border = null`) for a sleek, modern container layout.
- **Dynamic Header Color Hierarchy**: Collapsed / non-toggled day headers display muted gray text (`Color(0xFF888888)`), transitioning to white (`MaterialTheme.colorScheme.onSurface`) when expanded/toggled, and warm orange (`Color(0xFFFF6B00)`) on Today.
- **Event Edit & Delete Dialog**: Tapping any event card opens `EditCalendarEventDialog` pre-filled with the event's data (Title, Description, Date, Start/End Times, All-Day toggle, Sync toggle) with a prominent red **DELETE** button on the bottom left and a **SAVE** button on the bottom right.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 24s.

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
### 26. Running Agents Widget & Home Screen Central Operations Hub Overhaul
**Status:** DONE
**Branch:** `main`
**Summary:** Overhauled the home screen experience to make KAIROS OS feel like a true agentic central hub of operations:
- **No-Redirect Prompt Dispatch**: Dispatching a prompt from the home screen command bar no longer forces navigation away to the full chat screen. The home screen remains active.
- **Running Agent Cards (`RunningAgentCard.kt`)**: Implemented translucent notification-style cards displaying the task title (generated locally via Gemma 4), real-time status ("Processing...", "Complete", "Failed", "Cancelled"), pulsing orange indicator dot, and a primary "View" action button.
- **Swipe-to-Dismiss Gesture**: Removed X icon button; added horizontal drag (swipe right) to dismiss cards from view without cancelling background processes.
- **Stacked Card Container (`RunningAgentsWidget.kt`)**: Positions cards directly above the chat input bar. Stacked cards feature a 20dp vertical peek offset so cards layered underneath are prominently visible.
- **Expanded Full-Screen View (`ExpandedAgentList`)**: Expanded view uses a separate backdrop blur layer (`hazeChild`), keeping expanded agent cards and header 100% crisp and unblurred on top.
- **Concurrent Prompt Isolation**: Every home dispatch generates a distinct `conversationId` and maintains isolated messages, preventing multi-prompt overwrites.
- **In-Chat Stop Button**: Send button transforms into a Stop button when viewing an actively generating conversation, allowing users to abort the process.
- **Room Database Persistence**: Created `RunningAgentEntity` and `RunningAgentDao` in Room DB for state persistence across app restarts, with automatic 24-hour cleanup of completed/cancelled tasks.
- **Verification**: `./gradlew assembleDebug` — BUILD SUCCESSFUL in 47s.

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
