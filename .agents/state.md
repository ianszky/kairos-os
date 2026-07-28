# KAIROS OS State

## Current Status
- **Backend (Next.js Proxy)**: Implemented authentication logic and Supabase DAOs for proxying requests.
- **Android App**: 
  - Integrated Supabase Auth (`supabase-kt` 3.6.0).
  - Resolved Gradle/Hilt/Kotlin metadata compatibility hell by migrating to KSP 2.3.9 natively on AGP 9.0.0 and Gradle 9.1.0, paired with Hilt 2.60.
  - Successfully compiled `assembleDebug` with Hilt generation successfully working.
  - `LoginScreen` and `AuthViewModel` configured.
  - Implemented UI/UX adjustments:
    - Integrated Google Sans font for the majority of the app while keeping Doto for clock/date.
    - Built premium Compose screens (`LocalNotesScreen`, `LocalCalendarScreen`, `LocalClockScreen`) and prioritized `@kainotes`, `@kaiclock`, and `@kaicalendar` in the App Drawer list with unified navigation.
    - Implemented direct screen opening when selecting local apps in the App Drawer or submitting bare mentions, alongside on-device Gemma 4 tool execution for tagged prompts.
    - Implemented `AlarmReceiver` and fullscreen overlay activity `AlarmAlertActivity` to handle exact alarm wakeup over lockscreens.
    - Integrated on-device conversation summary/title generator (`LocalTitleGenerator`) using Gemma 4, saving to Supabase.
    - Added visually matching badge chips indicating if the assistant's response was powered by Gemma 4 offline (NPU) or Gemini (Cloud).

    - Implemented strict separation of entities: Installed/Local Apps use `@app:<slug>` (e.g. `@app:youtube`, `@app:kainotes`) and Composio Integrations use `@<slug>` (e.g. `@youtube`, `@gmail`).
    - Fixed Intent Friction Layer execution so selecting YouTube from the **App** tab inserts `@app:youtube` and triggers app usage limits / launcher UI, while `@youtube` from the **Integrations** tab is strictly reserved for AI prompt tasks.

## Next Steps
- Implement end-to-end testing between the Android Auth flow, proxy server, and Supabase cloud.
- Continue implementing the rest of the OS architecture.
