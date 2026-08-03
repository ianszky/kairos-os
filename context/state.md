# KAIROS OS Project State

## Current Task: Async Prompt Jobs (Phase 2) — IN PROGRESS on `feature/async-prompt-jobs`

### Status: IMPLEMENTATION COMPLETE (requires Supabase migration + device verification)

### Async prompt pipeline:
- **POST /api/prompt** returns **202 ACCEPTED** with `runId`; Composio work runs in Next.js `after()`
- **GET /api/prompt/status?runId=** returns `running` / `completed` / `failed` + final `KairosResponse`
- **`prompt_runs` table** tracks lifecycle (migration: `context/supabase_prompt_runs_migration.sql`)
- **Android** uses `postPromptAndAwait()` — short POST + poll up to 5 min; Running Agent stays Processing
- **Tool cap**: max 5 successful executions per tool name per request

### Prior: Tool scoping fix — on `fix-tool-scoping-loop`
- **Search screen** replaces dummy sidebar page: search bar + result cards (title + highlighted match snippet).
- **Dual-source search**: Room (local titles + messages) and Supabase (`ilike` on cloud titles + message content).
- **Navigation**: tapping a result opens the conversation via `ChatViewModel.selectConversation` (same as Scheduled / HISTORY).
- **Files**: `SearchChatScreen.kt`, `ChatSearchResult.kt`, `ChatSearchHelper.kt`, DAO search queries, `ChatViewModel.searchChats()`.

### Verification:
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**
- Manual: local title hit, local message hit + highlight, cloud title/message hit (signed in), tap opens chat, empty states

### Prior: Running Agents ↔ Android Notification Sync — on `feature/agent-notification-sync`
- **Agent → Android**: Room-backed running agents mirror to status-bar notifications.
- **Session → Home**: Intentional app session timer appears as in-app grant card in Running Agents stack.
- **Two-way dismiss**: Agent card swipe or shade clear deletes Room row; session card swipe hides in-app only.

### Prior: Leisure Budget UI + Cloud Authority — on `feature/app-session-timer`
