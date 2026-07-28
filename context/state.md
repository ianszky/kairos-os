# KAIROS OS Project State

## Current Task: Fixing Chatbox App Mentions, KAI Local App Prompt Dispatching, & Friction Layer Persistence

### Identified Root Causes & Bug Fixes:
1. **Local KAI App Prompt Disregarding**:
   - `onSendPrompt()` in `LauncherActivity.kt` previously intercepted all prompts starting with `@app:` and attempted to launch a package manager intent, swallowing prompts intended for local KAI apps (e.g. `@app:kainotes create a note`).
   - **Fix**: Added `!isKaiApp(currentTarget)` check to the `@app:` opening branch in `onSendPrompt()`. Local KAI app mentions with prompt text after the tag are now exempted and proceed to dispatch to `LocalAgentEngine`.

2. **Installed App Open Icon & Friction Layer Disappearing on Re-Mention**:
   - `isFrictionMode` and `frictionTargetApp` relied on asynchronous `LaunchedEffect(parsedActiveApp)` side-effect mutations. Clearing the text and typing an `@app:` mention again caused a 1-frame state lag where `isFrictionMode` was false and `frictionTargetApp` was null during initial composition, causing the UI to fall back to the Send button.
   - **Fix**: Derived `currentApp` and `isFrictionMode` synchronously using `remember(parsedActiveApp, availableApps)` and `remember(currentApp, distractingAppIds)`. Synchronized action button logic across both Single-Line and Stacked input layouts.

3. **App Drawer Backspace & Re-triggering**:
   - Clearing an `@app:` or `@integration` mention via backspace cleared `termInput`, but left `searchQuery` and tab state unhandled, preventing typing `@` immediately after from opening the drawer.
   - **Fix**: Enhanced `LaunchedEffect(termInput)` to auto-switch drawer tabs to "App" when queries start with `app` or `app:`, and updated `filteredApps` to search both `it.id` and `it.id.removePrefix("app:")`.

### Verification:
- Android debug build (`./gradlew assembleDebug`) verified clean compilation.

