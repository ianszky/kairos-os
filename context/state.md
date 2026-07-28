# KAIROS OS Project State

## Current Task: Chatbox App Mentions, Local Agent Intent Routing, & IME Keyboard Session Overhaul

### Status: COMPLETED & MERGED TO `main`

### Summary of Completed Fixes:
1. **Local KAI App Prompt Routing & Classification**:
   - `onSendPrompt()` in `LauncherActivity.kt` exempted KAI local apps (`isKaiApp`) from launcher opening logic when prompt text is present.
   - Updated `LocalAgentEngine.kt` to strip `app:` from `appTarget` (`cleanTarget`) and added a deterministic pre-classifier (`localTargets`) for all KAI local apps (`kainotes`, `kaicalendar`, `kaiclock`, `kairos`, `kai`) so local prompts never fall through to `CLOUD_AGENT`.
   - Strengthened LiteRT-LM classifier system prompt rules.

2. **Cursor Session App Drawer Re-Triggering & Leftover Space Fix**:
   - Updated drawer `.clickable` item listener to update `termInput` and `textFieldValue` synchronously, clear `searchQuery`, and request focus, keeping Android IME active composition sessions intact.
   - Fixed regex mention pill deletion to remove the trailing space along with `@app:<slug>`, eliminating leftover spaces that previously suppressed the drawer or prepended spaces to new queries.
   - Added `termInput.trimStart()` handling in `parsedActiveApp`, `parsedActiveIntegration`, and the drawer `LaunchedEffect`.

3. **Input Blocker & IME Composition Clearing**:
   - Enforced input blocking on installed non-KAI apps (`!isKaiApp(parsedActiveApp)`), preventing users from typing prompt text after `@app:installed_app` mentions while leaving KAI local apps unblocked.
   - Added `composition = null` to `TextFieldValue` on mention deletion to clear Android soft keyboard composition buffers (`finishComposingText()`), eliminating the bug where the first character after a mention pill was left behind upon backspacing.

### Verification:
- `./gradlew assembleDebug`: **`BUILD SUCCESSFUL in 2m 31s`** (0 errors).
- All changes merged cleanly into `main` branch.

