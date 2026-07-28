# KAIROS OS Project State

## Current Task: Investigating and Fixing `/api/prompt` Early 200 Returns for `@gmail` and `@youtube` Google Tools

### Identified Root Causes & Bug Fixes:
1. **Google GenAI `functionResponse` Protobuf Struct Requirement**:
   - `provider.executeToolCall` in Composio returns JSON strings or objects. When passed as a raw JSON string to `@google/genai`'s `functionResponse`, the Google API rejected it with `400 Bad Request: Invalid value at contents[2].parts[0].function_response.response (type.googleapis.com/google.protobuf.Struct)`.
   - The backend's top-level catch block swallowed this API error and returned a status 200 JSON payload with an error string and `widget: null`, causing the client app to receive 200 early in ~4 seconds with no data.
   - **Fix**: Added `formatFunctionResponse` helper in [tool-executor.ts](file:///C:/Dev/kairos-os/backend/src/lib/mcp/tool-executor.ts) to parse JSON strings and convert them into plain objects suitable for Google GenAI's protobuf Struct serializer.

2. **Composio `executeToolCall` Parameter Structure**:
   - `provider.executeToolCall` in Composio expected `arguments: fc.args` (and/or `args: fc.args`). Passing only `args: fc.args` caused `TypeError: Cannot read properties of undefined (reading 'arguments')`.
   - **Fix**: Updated `provider.executeToolCall` invocation in [tool-executor.ts](file:///C:/Dev/kairos-os/backend/src/lib/mcp/tool-executor.ts) to include `arguments: fc.args`.

3. **`youtube` Toolkit & Target Mapping**:
   - `mapAppTargetToToolkitSlug` in [connection-manager.ts](file:///C:/Dev/kairos-os/backend/src/lib/mcp/connection-manager.ts) did not include `youtube`, causing `@youtube` requests to check for an unconnected separate toolkit and return a connection card instead of executing Google tools.
   - **Fix**: Added `'youtube'` to `mapAppTargetToToolkitSlug` array in `connection-manager.ts`.

4. **Action Map Coverage**:
   - Added task types (`'list'`, `'fetch'`, `'search'`, `'read'`) for `'gmail'` and `'youtube'` in [action-map.ts](file:///C:/Dev/kairos-os/backend/src/lib/mcp/action-map.ts) so intent router resolves the correct Composio tool slugs regardless of classified task type.

### Verification:
- Ran vitest unit tests across all backend suites (`npx vitest run`) - 100% passing.
- Verified real Composio + Gemini end-to-end execution for `@gmail fetch my emails` - successfully retrieved real emails and returned structured `EMAIL_LIST` widget JSON.
