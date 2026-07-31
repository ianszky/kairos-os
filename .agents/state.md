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
    - **Running Agents ↔ Android notification sync** (`feature/agent-notification-sync`): agents mirror to status-bar notifications with complete/fail alerts; intentional app session grants appear in the home Running Agents stack with hide-only dismiss.
    - Renamed Twitter integration mention to `@x` (Composio toolkit remains `twitter`); installed X app friction only triggers on `@app:x`, not bare `@x`.
    - Mapped `@browser` to Composio Search (`composio_search` toolkit: `COMPOSIO_SEARCH_WEB`, etc.) instead of stale EXA tool slugs.
    - X/Twitter integration requires custom OAuth (`TWITTER_CLIENT_ID` / `TWITTER_CLIENT_SECRET`) because Composio removed managed credentials; backend returns a setup widget instead of 500 when missing.
    - Implemented on-device voice input in the chat bar using Gemma 4 E2B ASR via LiteRT-LM (`GemmaSttClient`, `AudioRecorder`), with Android SpeechRecognizer / system voice UI fallback when the local model is unavailable.

## Next Steps
- Implement end-to-end testing between the Android Auth flow, proxy server, and Supabase cloud.
- Continue implementing the rest of the OS architecture.
