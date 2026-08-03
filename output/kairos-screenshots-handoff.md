# Handoff: Kairos OS Screenshot Capture

**Date:** 2026-07-31  
**Repo:** `C:\Dev\kairos-os`  
**Status:** Paused by user. First ScreenshotWhale pass completed earlier; integration-widget capture stopped mid-flight.

## Goal for next session

Finish **raw PNG** captures of major integration screens (`@gmail`, `@googlecalendar`, `@slack`, `@spotify`, `@notion`, `@github`, `@googledrive`, `@digest`) using temporary hardcoded demo responses. **Do not** upload to ScreenshotWhale. Save under `screenshots/integrations/` in the repo. Then remove demo mocks / free the worktree.

## Already done

### Store-style ScreenshotWhale project (earlier in conversation)
- Captured 8 major product screens from physical device via ADB.
- Uploaded ScreenshotWhale project (claim URL was returned in-session).
- Local raw PNGs for that batch were deleted after upload per the generate-screenshots skill cleanup.

### Integration demo scaffolding (in progress, paused)
- Worktree: `.worktrees/integration-screenshots` on branch `feature/integration-screenshots` (branched from `main` at `6bbf752`).
- Temporary mock catalog:  
  `.worktrees/integration-screenshots/android/app/src/main/java/com/kairos/os/ui/ScreenshotDemoCatalog.kt`  
  (`ENABLED = true`; intercepts tags and returns hardcoded `KairosResponse` widgets).
- Hooked in  
  `.worktrees/integration-screenshots/android/app/src/main/java/com/kairos/os/ui/LauncherActivity.kt`  
  (before digest/local/cloud routing: `ScreenshotDemoCatalog.responseFor(...)`).
- Demo APK was built/installed on the connected device (`com.kairos.os`); last package update around 2026-07-31 ~10:31 local.
- Partial agent runs already completed on device (prompts in Running Agents may show a single missing character from the expand-state input bug — not truncation). Widget chat screens were **not** cleanly saved as a final PNG set.

### Plans / skills (reference, do not duplicate)
- Plan: `C:\Users\Ian\.cursor\plans\Kairos Screenshot Capture-30930440.plan.md`
- Skill used earlier: `.agents/skills/generate-screenshots.md/SKILL.md` (ScreenshotWhale) — **not** for the remaining raw-PNG work unless user asks again.
- Architecture / Compose: `.agents/skills/kairos-architecture/SKILL.md`, `.agents/skills/mobile-android-design/SKILL.md`
- Agent rules: `.agents/AGENTS.md` (worktree + free branch when done)

## Device / tooling

- Physical device connected via ADB (was `V2536A`, 1080×2358, Android 16).
- SDK: `C:\Users\Ian\AppData\Local\Android\Sdk` (see `android/local.properties` for `sdk.dir`).
- Also installed on device: `com.oceanwing.soundcore` — automation accidentally launched this when using Add Context → App drawer; **avoid App drawer**.
- Prefer `adb shell monkey -p com.kairos.os -c android.intent.category.LAUNCHER 1` to foreground Kairos (plain `am start` sometimes lost focus to `com.bbk.launcher2`).

## Hard lessons (must follow)

1. **Keep long prompts.** User wants natural full sentences (e.g. `@gmail show my important emails`), not ultra-short tags-only. Demo catalog still matches on the `@tag` / target, so slightly mangled text is OK for routing — but capture UX should use the intended long copy.
2. **Input bug (not truncation):** When the command input transitions to its expanded state, **one character is dropped** roughly around the **6th–8th character** of the sentence. Workaround: clear/retype the full prompt (or type it again) and verify the EditText text before Send. Do **not** “fix” this by shortening prompts.
3. **No Add Context / App tab:** Selecting installed apps launches real packages (Soundcore). Prefer typing `@integration …` in the command box. Avoid App drawer automation.
4. **No ScreenshotWhale** for this continuation — user explicitly wants raw PNGs saved in-repo.
5. **Stop spiraling:** If ADB is flaky, retry the same long prompt once; don’t redesign the whole capture strategy mid-run.

## Suggested capture sequence

1. Confirm worktree + mock still present; reinstall APK if needed:  
   `cd .worktrees/integration-screenshots/android && .\gradlew.bat :app:assembleDebug` then `adb install -r app\build\outputs\apk\debug\app-debug.apk`
2. Force-stop Soundcore; monkey-launch Kairos; ensure dark theme.
3. For each integration: New chat → type the **full long prompt** → if a char is missing near positions 6–8 after expand, **type the prompt again** (or clear + retype) → confirm text in UI dump → Send → wait for widget → `adb exec-out screencap -p` →  
   `C:\Dev\kairos-os\screenshots\integrations\0N-*.png`
4. Visually verify each PNG (widget visible, not agents list / not wrong app).
5. Cleanup: remove `ScreenshotDemoCatalog` hook + file (or set `ENABLED = false`), reinstall non-demo build if desired, `git worktree remove` / free `feature/integration-screenshots` per AGENTS.md. Prefer **not** committing mock code unless user asks.

Helper script (may be stale/flaky; rewrite smaller if needed):  
`C:\Users\Ian\AppData\Local\Temp\capture-integrations.py`

## Widget needles + suggested long prompts (from mock catalog)

| File | Prompt to type (retry if 1 char drops) | Expect in UI |
|------|----------------------------------------|----------------|
| `01-gmail.png` | `@gmail show my important emails` | Important Emails / Open in Gmail |
| `02-googlecalendar.png` | `@googlecalendar show todays schedule` | Today's Schedule / Open Calendar |
| `03-slack.png` | `@slack summarize unread highlights` | Slack Digest / Open Slack |
| `04-spotify.png` | `@spotify play a deep focus playlist` | Now Playing / Open Spotify |
| `05-notion.png` | `@notion find Kairos launch checklist` | Notion Results / Open Notion |
| `06-github.png` | `@github show PRs needing review` | GitHub Updates / Open GitHub |
| `07-googledrive.png` | `@googledrive recent Kairos files` | Google Drive / Open Drive |
| `08-digest.png` | `@digest daily digest` | Daily Digest |

Widget types rendered in `android/.../ui/components/WidgetRenderer.kt` (`EMAIL_LIST`, `CALENDAR_EVENT`, `GENERIC_CARD`, `MUSIC_CARD`, `DIGEST_SUMMARY`).

## Do not

- Commit Supabase keys or secrets from `local.properties`.
- Force-push / amend unrelated history.
- Re-open App drawer automation.
- Upload to ScreenshotWhale unless user asks.

## Suggested skills

1. **`.agents/skills/kairos-architecture/SKILL.md`** — thin-client / response-widget conventions if mocks need adjustment.
2. **`.agents/skills/mobile-android-design/SKILL.md`** — Compose UI context when validating screenshots.
3. **`.agents/skills/generate-screenshots.md/SKILL.md`** — only if user later wants another ScreenshotWhale project from the new raw PNGs.
4. **`C:\Users\Ian\.agents\skills\handoff\SKILL.md`** — if pausing again mid-task.
5. **`find-docs`** (`C:\Users\Ian\.claude\skills\find-docs\SKILL.md`) — if verifying current `adb` / emulator CLI behavior.

## User preferences from this thread

- Name: Ian; prefers direct code iteration.
- Integration deliverable: **raw PNGs in repo**, not ScreenshotWhale.
- **Keep long input prompts**; do not switch to short tags to dodge the input bug.
- Input expand bug drops one character ~positions 6–8 — workaround is retype, not shorten.
- Explicitly asked to **stop** before cleanup/capture finished — resume only when asked.
