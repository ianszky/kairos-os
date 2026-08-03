# KAIROS OS Project State

## Current Task: Tool scoping fix — IN PROGRESS on `fix-tool-scoping-loop`

### Status: IMPLEMENTATION COMPLETE (pending merge)

### Tool executor scoping fix:
- **Problem**: Classifier `taskType: create` narrowed Google Calendar to `GOOGLESUPER_CREATE_EVENT` only; multi-step create+read prompts failed when model tried to list events.
- **Fix**: Always load each app's curated `COMPOSIO_ACTION_MAP[app].default` tool set; keep `taskType` as prompt hint only.
- **Loop**: Raised `MAX_ITERATIONS` 5 → 8; improved logging when loop exits with pending function calls; no forced JSON final turn.
- **Files**: `backend/src/lib/mcp/tool-executor.ts`, `backend/src/lib/mcp/tool-executor.test.ts`

### Prior: Search Chat (Android) — COMPLETE on `main`
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
