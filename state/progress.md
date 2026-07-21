# KAIROS OS Hackathon Progress & Ticketing

This file serves as the permanent memory for the automated developer and evaluator agents. 
Every open task must have **Context**, **Technical Requirements**, and **Acceptance Criteria** so agents can execute them autonomously without needing to constantly re-read the core PRD.

---

## 🟢 COMPLETED TASKS

### 16. Fix Supabase Realtime WebSocket Ktor OkHttp Engine & Sidebar Sync
**Status:** DONE
**Summary:** Replaced `ktor-client-android` with `ktor-client-okhttp:3.4.3` in `android/app/build.gradle.kts` and configured `httpEngine = OkHttp.create()` in `SupabaseModule.kt` and `HttpClient(OkHttp)` in `KairosApiClient.kt`. This resolves `Engine doesn't support WebSocketCapability` and enables Supabase Realtime WebSocket live updates. Added `chatViewModel.onPromptResponse()` triggers post local title generation and execution in `LauncherActivity.kt` to ensure local Gemini conversations reliably sync to the sidebar.

### 13. Chatbox Interface & Multimodal Integration Overhaul
**Status:** DONE
**Summary:** Implemented encircled "+" menu (Add App, Add Files, Add Images), restricted file picker MIME types to Gemini-compatible formats (PDF, TXT, CSV, HTML, RTF), added local attachment caching & background upload to Supabase, implemented native SpeechRecognizer + RMS volume-driven fluid WaveformView replacing text input, supported custom VisualTransformation + OffsetMapping to render app logos on canvas behind `@app` mentions, and upgraded ChatBubble to render inline app logos using Compose `inlineContent`.

### 10. Optimize Prompt-Response Pipeline
**Status:** DONE
**Summary:** Merged Response Builder LLM call into Tool Executor, migrated all cloud inference to stable Gemini 2.5 series, added COMPOSIO_ACTION_MAP for 13 toolkits to avoid schema bloat, added tool validation gate & retry loop, and updated test suite.

### 9. Fix Composio Auth Config Mismatch for other integrations
**Status:** DONE
**Summary:** Fixed the auth configuration lookup in connection-manager.ts to ensure microsoftteams/slackbot and other integrations do not mismatch or redirect to GitHub.

### 8. Implement Dynamic Composio Authentication & Hyphen Bug Fix
**Status:** DONE
**Summary:** Fixed the authentication layer for all Composio toolkits to generate connection prompts dynamically post-intent classification, and resolved the user ID identity split bug (retained hyphens across all tool executions).

### 7. Fix Conversation Persistence & Session Continuity
**Status:** DONE
**Summary:** Fixed three bugs causing every message to create a new conversation. Send button now passes conversationId, back button uses startNewConversation(), and sidebar refreshes after every response via onPromptResponse().

### 6. Implement Chat History Management
**Status:** DONE
**Summary:** Replaced placeholder logs with Supabase-backed conversation tracking. Next.js creates conversations and generates titles, and the Android client uses ChatViewModel to render histories in the sidebar.

### 5. Translate Main Design Prototype to Compose Components
**Status:** DONE
**Summary:** Implemented Jetpack Compose components for standard widgets (EmailListWidget, etc.) and domain models.

### 4. Redesign Android Home Screen to match mockup
**Status:** DONE
**Summary:** Redesigned LauncherActivity.kt layout to match the blinking cursor minimal mockup.

### 3. Setup Next.js Intent Router Endpoint (`/api/prompt/route.ts`)
**Status:** DONE
**Summary:** Implemented dynamic auth checks, integrated intent classification, and routed complex intents to Composio tool execution. Fully tested and verified.

### 2. Implement Android Home Screen (Blinking Cursor UI)
**Status:** DONE
**Summary:** Scaffolded LauncherActivity.kt with Jetpack Compose blank screen and text field capturing user intents.

### 1. Setup project repositories (Android + Next.js)
**Status:** DONE
**Summary:** Kotlin 2.4.0 / Compose BOM 2026.06.00 frontend and Next.js 16.2.9 backend scaffolded and verified by the evaluator.

### 11. Complete Composio Action Map
**Status:** DONE
**Summary:** Mapped all remaining 34 Composio integrations present in the Android app drawer into COMPOSIO_ACTION_MAP on the backend, using verified tool names from the Composio SDK to prevent hallucinations.

### 12. Mobile UI/UX Adjustments & Animations
**Status:** DONE
**Summary:** Replaced Doto font with Google Sans for the majority of the app (retaining Doto for homepage clock/date and labels), toned down and bottom-aligned the radial orange gradient, removed orange border and offset on agent replies, and implemented custom markdown rendering support. Also removed orange border on typing loading indicator, added a staggered vertical bounce animation to the three dots, converted the main layout container of MindfulLauncherScreen to a Box to layer components (preventing scroll cutoffs), and applied fading top gradient to the header. Made the input box translucent with a 0.65f alpha background, applied a native BlurEffect (RenderEffect) background layer to solidify the glassmorphism look, added a translucent border, and left the margins transparent to let the orange gradient bleed through.

### 13. Mobile Conversation UI/UX Adjustments (Spacing, Bubble Widths & Typography)
**Status:** DONE
**Summary:** Restricted user typed prompt chat bubble width to a maximum of 60% of the screen width (retaining content wrap below that width). Increased default bodyLarge, bodyMedium, and bodySmall line heights and letter spacing globally. Upgraded message body font in chat bubbles to bodyLarge with a generous line spacing (24.sp) and padding. Increased vertical layout margins between consecutive interactions and widgets for a spacious, modern, and breathing layout.

### 14. Local Notification Interception & On-Device Digest (LiteRT-LM Migration)
**Status:** DONE
**Summary:** Swapped the cloud-based notification interception with Room-based local persistence database. Replaced MediaPipe GenAI with LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android:latest.release`) for on-device notification classification (into CRITICAL/DIGEST) and structured summary digest generation via local Gemma LLM models. Extracted text responses natively from the JNI-backed `Message.contents.contents` properties and verified it builds and compiles successfully.

### 15. Next.js 16 Configuration Allowed Dev Origins Hotfix
**Status:** DONE
**Summary:** Moved `allowedDevOrigins` configuration from the `experimental` block to the top level of the config in `next.config.ts` to support Next.js 16 schema changes. Additionally configured `turbopack.root` path resolution to resolve the multiple lockfiles workspace warning.

### 16. Local @kai Apps App Drawer & Tool Calling Integration
**Status:** DONE
**Summary:** Registered `@kai`, `@kainotes`, `@kaicalendar`, and `@kaiclock` as first-class local app connections in `LauncherActivity.kt`. Prioritized local apps at the top of the App Drawer and added "Local App" badges. Updated `LauncherActivity.kt` screen routing for bare mentions (`@kainotes`, `@kaicalendar`) and `/open` commands. Updated `LocalAgentEngine.kt` to recognize `@kai*` app targets (`kainotes`, `kaicalendar`, `kaiclock`, `kai`), whitelist local target routing, and expanded rule fallback handlers for notes, alarms, and calendar events.

### 17. Local Kai Apps Screen Overhaul & Navigation Alignment
**Status:** DONE
**Summary:** Overhauled `LocalNotesScreen.kt`, `LocalCalendarScreen.kt`, and created `LocalClockScreen.kt`. Unified header navigation in `LauncherActivity.kt` so non-home screens present a clean top back button and title header, while hiding the bottom prompt input box to avoid layout overlap. Built Material 3 search filtering, note editors, agenda cards, alarm managers, and empty state layouts. Verified clean compilation via `gradlew assembleDebug`.

---

None. All scheduled tasks are completed!

