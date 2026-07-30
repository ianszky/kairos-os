# KAIROS OS: Scheduled Tasks & CRON Jobs Handoff Document

**Date**: July 30, 2026  
**Repository Path**: `C:\Dev\kairos-os`  
**Current Git Branch**: `feature/scheduled-cron-tasks`  
**Latest Commit**: `6a15eec` (`fix(scheduled): remove non-existent user_id property from messages table insert in execute API route`)

---

## 1. Executive Summary & Objective

The goal of this task was to implement the **Scheduled Tasks / CRON Jobs** subsystem across the KAIROS OS backend (Next.js & Supabase) and native Android client (Jetpack Compose). 

All requested features, UI overhauls, database schema migrations, and API execution fixes have been fully built, tested, and verified with clean builds (`./gradlew assembleDebug` and `npx next build`).

---

## 2. Architecture & Completed Features

### A. Next.js & Supabase Backend
- **Database Tables**:
  - `scheduled_tasks`: Stores task prompt, app target, frequency (`daily`, `weekly`), selected days of week, execution time, and active state (`is_active`).
  - `scheduled_task_runs`: Execution logs tracking status (`pending`, `running`, `completed`, `failed`), timestamps, and resulting `conversation_id`.
  - `conversations.source`: Tagged with `'scheduled'` and linked to `scheduled_task_id`.
  - Migration script located at: [context/supabase_scheduled_migration.sql](file:///C:/Dev/kairos-os/context/supabase_scheduled_migration.sql).
- **API Endpoints**:
  - `GET /api/scheduled`, `POST /api/scheduled`, `PUT /api/scheduled`, `DELETE /api/scheduled`: Task CRUD management.
  - `POST /api/scheduled/execute`: Executes scheduled prompts, creates tagged conversations, inserts user prompt message (fixed `user_id` schema mismatch), and dispatches agent intents.
  - `GET /api/scheduled/runs`: Fetches process execution history.

### B. Android Native Client (Jetpack Compose)
- **Two-Tab Interface (`RUNS` & `JOBS`)**: Styled according to KaiClock design patterns.
- **Native Chatbox & App Drawer Integration**:
  - Reuses the native Home Screen chatbox input container complete with `@` App Drawer autocomplete, file attachments, voice input, and `.imePadding()` keyboard sliding.
  - Restricts `@` App Drawer on the Scheduled screen to agentic apps (`Kai Notes`, `Kai Calendar`, `Kai Clock`, and Composio integrations).
- **Schedule Configuration Panel**:
  - Expands directly below the bottom text input field when an app tag/mention (`@`) is selected.
  - Frequency toggle (`Daily` vs `Weekly`), day-of-week chips (preselected to Monday for Weekly), and Material 3 `TimePicker`.
  - Primary CTA button: **`ACTIVATE`**.
- **Manual Task Runs & Home Screen Agent Cards**:
  - Job cards feature an explicit **Run** button that displays a confirmation modal and dispatches a `RunningAgentCard` on the Home Screen.
  - Completed scheduled task runs appear as agent cards on the main screen and are tagged with a `🔄` icon in the sidebar conversation history.

---

## 3. Key Files & Structure

- **Backend**:
  - [backend/src/app/api/scheduled/route.ts](file:///C:/Dev/kairos-os/backend/src/app/api/scheduled/route.ts)
  - [backend/src/app/api/scheduled/execute/route.ts](file:///C:/Dev/kairos-os/backend/src/app/api/scheduled/execute/route.ts)
  - [backend/src/app/api/scheduled/runs/route.ts](file:///C:/Dev/kairos-os/backend/src/app/api/scheduled/runs/route.ts)
  - [backend/src/lib/db/scheduled-tasks.ts](file:///C:/Dev/kairos-os/backend/src/lib/db/scheduled-tasks.ts)
  - [backend/src/types/database.types.ts](file:///C:/Dev/kairos-os/backend/src/types/database.types.ts)
- **Android**:
  - [android/app/src/main/java/com/kairos/os/ui/screens/ScheduledScreen.kt](file:///C:/Dev/kairos-os/android/app/src/main/java/com/kairos/os/ui/screens/ScheduledScreen.kt)
  - [android/app/src/main/java/com/kairos/os/ui/LauncherActivity.kt](file:///C:/Dev/kairos-os/android/app/src/main/java/com/kairos/os/ui/LauncherActivity.kt)
  - [android/app/src/main/java/com/kairos/os/ui/viewmodels/ScheduledViewModel.kt](file:///C:/Dev/kairos-os/android/app/src/main/java/com/kairos/os/ui/viewmodels/ScheduledViewModel.kt)
  - [android/app/src/main/java/com/kairos/os/data/db/ScheduledTaskDao.kt](file:///C:/Dev/kairos-os/android/app/src/main/java/com/kairos/os/data/db/ScheduledTaskDao.kt)

---

## 4. Suggested Skills for Incoming Agents

1. **`kairos-architecture`**: Core architectural rules for thin-client presentation and server-driven UI in KAIROS OS (`C:\Dev\kairos-os\.agents\skills\kairos-architecture\SKILL.md`).
2. **`mobile-android-design`**: Guidelines for Material Design 3 and Jetpack Compose (`C:\Dev\kairos-os\.agents\skills\mobile-android-design\SKILL.md`).
3. **`supabase`**: Supabase database management, RLS policies, and client integration (`C:\Users\Ian\.gemini\antigravity-cli\skills\supabase\SKILL.md`).

---

## 5. Next Steps for Next Session

- Run [context/supabase_scheduled_migration.sql](file:///C:/Dev/kairos-os/context/supabase_scheduled_migration.sql) on the production Supabase database instance.
- Deploy the Next.js backend and install/run the Android APK (`./gradlew installDebug`).
