# KAIROS OS — Technical Implementation Document

> Technical architecture, stack specifications, and implementation plan for the KAIROS OS agentic Android launcher.

---

## 1. Architecture Overview

KAIROS OS follows a **thin-client / fat-backend** architecture. The Android launcher is a lightweight presentation layer that captures user input and renders structured responses. All AI inference, API integration, and business logic lives on the backend.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID CLIENT                              │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────────┐  │
│  │ Command  │  │ Widget       │  │ Floating │  │ Notification   │  │
│  │ Input    │  │ Renderer     │  │ Bubble   │  │ Listener       │  │
│  └────┬─────┘  └──────▲───────┘  └──────────┘  └───────┬────────┘  │
│       │               │                                 │           │
│       ▼               │                                 ▼           │
│  ┌────────────────────┴─────────────────────────────────────────┐   │
│  │                     HTTP / WebSocket Client                   │   │
│  └──────────────────────────┬────────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        NEXT.JS BACKEND                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Intent       │  │ AI Model     │  │ MCP Server               │  │
│  │ Router       │  │ Orchestrator │  │ Registry                 │  │
│  │              │  │              │  │                          │  │
│  │ • /open →    │  │ • Flash-Lite │  │ • Gmail MCP              │  │
│  │   Instant    │  │   (Simple)   │  │ • Calendar MCP           │  │
│  │ • @app →     │  │ • Flash      │  │ • Sheets MCP             │  │
│  │   Classify   │  │   (Complex)  │  │ • Spotify MCP            │  │
│  │              │  │ • Nano       │  │ • Notes MCP (Custom)     │  │
│  │              │  │   (Notif.)   │  │ • Browser MCP (Custom)   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                 │                      │                  │
│         ▼                 ▼                      ▼                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Response Builder                          │    │
│  │  Generates: Widget JSON | Deep Link URI | Text Response     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Database (PostgreSQL / SQLite)                              │    │
│  │  • User sessions   • Conversation context                   │    │
│  │  • App configs      • Notification queue                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Tech Stack Specification

### 2.1 Android Client

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Language** | Kotlin | **2.4.0** (June 2026) | Stable context parameters, explicit backing fields, UUID API stabilization |
| **UI Framework** | Jetpack Compose | **BOM 2026.06.00** (Compose 1.11.3) | Strong Skipping Mode (default), K2 compiler integration |
| **Design System** | Material Design 3 | Latest via Compose BOM | Material You dynamic color, minimal dark theme |
| **Architecture** | MVVM + Clean Architecture | — | `ui` / `domain` / `data` layers |
| **DI** | Hilt | Latest stable | Inject system services (`PackageManager`, `NotificationManager`) |
| **Async** | Kotlin Coroutines + Flow | Bundled with Kotlin 2.4 | `StateFlow` / `SharedFlow` for reactive state |
| **Networking** | Retrofit + OkHttp | Latest stable | HTTP client for backend communication |
| **Startup Perf** | Baseline Profiles | — | Pre-compile critical code paths for launcher cold start |
| **On-Device AI** | AICore / ML Kit | GenAI Prompt API | Runs Gemma 4 E4B natively on device |
| **Min SDK** | Android 10 (API 29) | — | Covers ~95% of active devices |
| **Target SDK** | Android 16 (API 36) | — | Latest platform target |

**Key Kotlin 2.4.0 Features Leveraged:**
- **Context parameters** — Cleaner DI patterns for composables
- **K2 Compiler** — Compose compiler ships inside the Kotlin repo; no manual version matching
- **Strong Skipping Mode** — Default in Compose 1.11+; skips recomposition for unchanged parameters (critical for launcher performance)

### 2.2 Backend Server

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Framework** | Next.js | **16.2.9** (June 2026) | App Router, Route Handlers, `proxy.ts` middleware |
| **Runtime** | Node.js | 22 LTS | — |
| **Language** | TypeScript | 5.x | Strict mode enabled |
| **AI SDK** | `@google/genai` | **~2.9.0** | Unified SDK for Gemini 2.0+; replaces `@google/generative-ai` |
| **MCP SDK** | `@modelcontextprotocol/sdk` | **1.29.0** | Production-stable; use v1.x (v2.x is pre-alpha) |
| **Schema Validation** | Zod | Latest stable | Input validation for MCP tool definitions |
| **Database** | PostgreSQL (prod) / SQLite (dev) | — | Session state, conversation context, user config |
| **ORM** | Prisma | Latest stable | Type-safe database access |
| **Auth** | OAuth 2.0 (Google) | — | Backend-managed; tokens never touch the device |
| **Deployment** | Vercel / Railway / VPS | — | Auto-scaling; edge functions for proxy |

**Key Next.js 16.x Changes:**
- **`middleware.ts` → `proxy.ts`** — Middleware file convention has been renamed
- **Turbopack** — ~400% faster dev startup; default dev bundler
- **Route Handlers** — Standard HTTP method handlers in `app/api/` for mobile backend endpoints
- **Server Actions** — For any admin/web dashboard mutations (not used by mobile client directly)

### 2.3 AI Model Tier (Hybrid Architecture)

| Tier | Location | Model | Use Case | Expected Latency |
|---|---|---|---|---|
| **Tier 0 (Instant)** | Phone | No model | `/open` commands → direct Android intent dispatch | <200ms |
| **Tier 1 (Local)** | Phone | Gemma 4 E4B | Simple intent classification, alarm setting, note-taking, offline Q&A | <500ms (NPU) |
| **Tier 2 (Cloud)** | Backend | Gemini 3.5 Flash | Complex tool calling (Gmail queries, Calendar operations, Composio flows) | 2–5s |
| **Tier 3 (Cloud)** | Backend | Gemini 3.1 Pro | Deep reasoning tasks, complex summarization | 5–10s |

**Key Details:**
- **On-Device:** Tier 1 runs completely offline using the Snapdragon 8 Gen 5 NPU via Android's AICore and the GenAI Prompt API.
- **Cloud:** Tiers 2 and 3 run on the Next.js backend using `@google/genai` to connect to Composio for execution.
- **Context Windows:** Flash = 1M tokens, Pro = 2M tokens — ample for conversation context
- **Security:** As of June 2026, Gemini API requires restricted API keys or Google Cloud credentials (no unrestricted keys)

```typescript
import { GoogleGenAI } from '@google/genai';

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// Tier 1: Lightweight intent classification
const classifyResponse = await ai.models.generateContent({
  model: 'gemini-3.1-flash-lite',
  contents: userPrompt,
  config: { tools: intentClassificationTools }
});

// Tier 2: Complex tool calling via Composio
const toolResponse = await ai.models.generateContent({
  model: 'gemini-3.5-flash',
  contents: enrichedPrompt,
  config: { tools: [{ functionDeclarations: await session.tools() }] }
});
```

### 2.4 Composio Integration Layer (Replaces Custom MCPs)

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Core SDK** | `@composio/core` | Latest | Manages sessions, toolkits, and connected accounts |
| **AI Provider** | `@composio/google` | Latest | Native Gemini integration; automatic function schema generation |
| **Auth** | Composio Managed Auth | — | Handles full OAuth2 flow, token storage, and refresh |

**Composio + Gemini Execution Flow:**
```typescript
import { Composio } from '@composio/core';
import { GoogleProvider } from '@composio/google';
import { GoogleGenAI } from '@google/genai';

const composio = new Composio({
  apiKey: process.env.COMPOSIO_API_KEY,
  provider: new GoogleProvider(),
});
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// 1. Fetch user's connected tools
const session = await composio.create(userId, {
  toolkits: ["googlesuper", "spotify", "todoist"],
});
const tools = await session.tools();

// 2. Feed directly into Gemini
const chat = ai.chats.create({
  model: 'gemini-3.5-flash',
  config: { tools: [{ functionDeclarations: tools }] },
});

// 3. Agentic Loop
let response = await chat.sendMessage({ message: userPrompt });

while (response.functionCalls?.length > 0) {
  const parts = [];
  for (const fc of response.functionCalls) {
    // 4. Execute the tool against the 3rd party API automatically
    const result = await composio.provider.executeToolCall(userId, {
      name: fc.name,
      args: fc.args,
    });
    parts.push({
      functionResponse: { id: fc.id, name: fc.name, response: JSON.parse(result) }
    });
  }
  response = await chat.sendMessage({ message: parts });
}
```

---

## 3. Detailed Component Specifications

### 3.1 Intent Router (Hybrid Two-Stage Architecture)

The Intent Router uses a hybrid approach to maximize privacy and speed while retaining cloud intelligence for complex tasks.

```
User Input (On-Device)
        │
        ▼
   Stage 1: Phone (Gemma 4 E4B / AICore)
   Classifies prompt locally without network
        │
        ├───────────────┬──────────────────┐
        │               │                  │
    /open cmd        Simple             Complex
    (Tier 0)        (Tier 1)           (Tier 2)
        │               │                  │
        ▼               ▼                  ▼
     Execute         Execute           Send to Cloud
     Locally         Locally          (Next.js Backend)
                                           │
                                           ▼
                                    Composio + Gemini
                                    3.5 Flash Tool Call
```

**Implementation (Android Client - Stage 1):**

```kotlin
class OnDeviceIntentClassifier {
    private val model = GenerativeModel.getClient(
        generationConfig {
            modelConfig = ModelConfig {
                releaseTrack = ModelReleaseTrack.PREVIEW
                preference = ModelPreference.FULL  // Gemma 4 E4B
            }
        }
    )

    suspend fun classify(prompt: String): IntentResult {
        // Runs entirely on the Snapdragon 8 Gen 5 NPU
        val response = model.generateContent("Classify this prompt into simple/complex...")
        return parseIntentResult(response.text)
    }
}
```

**Implementation (Next.js Backend - Stage 2):**

```typescript
// app/api/prompt/route.ts (Only called if Tier 2)
import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
  const { prompt, sessionId, appTarget, userId } = await req.json();

  // Tier 2: Complex tool calling via Composio
  // Session handles auth and fetches schemas dynamically based on the appTarget

  // Tier 2: Complex tool calling via Composio
  // Session handles auth and fetches schemas dynamically based on the appTarget
  const session = await composio.create(userId, { toolkits: [appTarget] });
  const result = await handleComplexIntent(prompt, session);
  return NextResponse.json(result);
}
```

### 3.2 Server-Driven UI Protocol

The backend returns structured JSON that the Android client renders into native Compose widgets. This eliminates the "chatbot wall of text" problem.

**Response Schema:**

```typescript
interface KairosResponse {
  type: 'WIDGET' | 'TEXT' | 'ANDROID_INTENT' | 'DEEP_LINK' | 'ERROR';
  widget?: WidgetPayload;
  text?: string;
  intent?: AndroidIntentPayload;
  deepLink?: string;
  meta?: {
    conversationId: string;
    timestamp: string;
    model: string; // which AI tier handled this
  };
}

interface WidgetPayload {
  widgetType: 'EMAIL_LIST' | 'CALENDAR_EVENT' | 'ALARM_CONFIRM'
            | 'NOTE_CARD' | 'MUSIC_CARD' | 'SEARCH_RESULTS'
            | 'DIGEST_SUMMARY' | 'GENERIC_CARD';
  title?: string;
  items: WidgetItem[];
  actions?: WidgetAction[];
}

interface WidgetItem {
  id: string;
  primary: string;   // main text
  secondary?: string; // subtitle
  icon?: string;      // icon name or URL
  metadata?: Record<string, string>;
}

interface WidgetAction {
  label: string;
  actionType: 'DEEP_LINK' | 'CALLBACK' | 'DISMISS';
  target: string; // deep link URI or callback endpoint
}
```

**Example — Gmail Response:**

```json
{
  "type": "WIDGET",
  "widget": {
    "widgetType": "EMAIL_LIST",
    "title": "3 Important Emails",
    "items": [
      {
        "id": "email_001",
        "primary": "Q2 Revenue Report - Action Required",
        "secondary": "From: Ms. Tenorio • 2 hours ago",
        "icon": "mail_important",
        "metadata": { "threadId": "abc123" }
      },
      {
        "id": "email_002",
        "primary": "Team Standup Notes - June 25",
        "secondary": "From: DevOps Team • 5 hours ago",
        "icon": "mail",
        "metadata": { "threadId": "def456" }
      }
    ],
    "actions": [
      { "label": "Open in Gmail", "actionType": "DEEP_LINK", "target": "gmail://inbox" },
      { "label": "Dismiss", "actionType": "DISMISS", "target": "" }
    ]
  },
  "meta": {
    "conversationId": "sess_abc",
    "timestamp": "2026-06-25T15:00:00Z",
    "model": "gemini-3.5-flash"
  }
}
```

### 3.3 Android Launcher Components

#### 3.3.1 Launcher Activity (Home Screen)

The launcher must register as a home screen replacement in the `AndroidManifest.xml`:

```xml
<activity
    android:name=".ui.LauncherActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:stateNotNeeded="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

#### 3.3.2 Compose UI Structure

```kotlin
@Composable
fun KairosHomeScreen(viewModel: KairosViewModel = hiltViewModel()) {
    val commandStream by viewModel.commandStream.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Scrollable feed of interactions
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(commandStream) { interaction ->
                when (interaction) {
                    is Interaction.UserCommand -> UserCommandBubble(interaction)
                    is Interaction.WidgetResponse -> WidgetRenderer(interaction.widget)
                    is Interaction.TextResponse -> TextResponseCard(interaction)
                    is Interaction.Loading -> LoadingIndicator()
                }
            }
        }

        // The command input — the heart of KAIROS
        CommandInputBar(
            onSubmit = { prompt, appTarget ->
                viewModel.submitCommand(prompt, appTarget)
            },
            availableApps = viewModel.availableApps
        )
    }
}
```

#### 3.3.3 Widget Renderer

```kotlin
@Composable
fun WidgetRenderer(widget: WidgetPayload) {
    when (widget.widgetType) {
        "EMAIL_LIST" -> EmailListWidget(widget)
        "CALENDAR_EVENT" -> CalendarEventWidget(widget)
        "ALARM_CONFIRM" -> AlarmConfirmWidget(widget)
        "NOTE_CARD" -> NoteCardWidget(widget)
        "MUSIC_CARD" -> MusicCardWidget(widget)
        "SEARCH_RESULTS" -> SearchResultsWidget(widget)
        "DIGEST_SUMMARY" -> DigestSummaryWidget(widget)
        else -> GenericCardWidget(widget)
    }
}
```

#### 3.3.4 Notification Listener Service

```kotlin
class KairosNotificationListener : NotificationListenerService() {

    @Inject lateinit var notificationRepository: NotificationRepository

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val payload = NotificationPayload(
            packageName = sbn.packageName,
            title = extras.getString(Notification.EXTRA_TITLE) ?: "",
            text = extras.getString(Notification.EXTRA_TEXT) ?: "",
            timestamp = sbn.postTime,
            category = notification.category
        )

        // Send to backend for classification
        coroutineScope.launch {
            val classification = apiClient.classifyNotification(payload)

            when (classification.tier) {
                "CRITICAL" -> {
                    // Allow passthrough — show custom minimal alert
                    showCriticalAlert(payload)
                }
                "DIGEST" -> {
                    // Suppress and queue for daily digest
                    notificationRepository.enqueue(payload)
                    cancelNotification(sbn.key)
                }
            }
        }
    }
}
```

#### 3.3.5 Floating Bubble Overlay

```kotlin
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        bubbleView = LayoutInflater.from(this)
            .inflate(R.layout.floating_bubble, null)

        bubbleView.setOnClickListener {
            // Open command overlay on top of current app
            openCommandOverlay()
        }

        windowManager.addView(bubbleView, params)
    }
}
```

### 3.4 Conversation Context Management

Each user session maintains a conversation context that enables follow-up commands.

**Database Schema:**

```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    app_target TEXT,           -- e.g., 'gmail', 'calendar'
    model_tier TEXT,           -- 'flash-lite', 'flash', 'pro'
    widget_payload JSONB,      -- stored widget response
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE context_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    summary JSONB NOT NULL,    -- condensed context JSON
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    package_name TEXT NOT NULL,
    title TEXT,
    body TEXT,
    tier TEXT NOT NULL CHECK (tier IN ('CRITICAL', 'DIGEST')),
    is_read BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_app_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    app_identifier TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('UTILITY', 'TRAP')),
    intent_gate_enabled BOOLEAN DEFAULT FALSE,
    default_time_limit INTEGER,  -- minutes
    vip_contact BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, app_identifier)
);
```

### 3.5 Intent Gate Enforcement

```kotlin
// IntentGateManager.kt
class IntentGateManager @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val overlayService: FloatingBubbleService
) {
    suspend fun requestAppLaunch(
        appId: String,
        reason: String?,
        timeLimitMinutes: Int?
    ): IntentGateResult {
        val config = appConfigRepository.getConfig(appId)

        return when (config.category) {
            AppCategory.UTILITY -> {
                // Immediate launch
                IntentGateResult.Approved(launchIntent = buildLaunchIntent(appId))
            }
            AppCategory.TRAP -> {
                if (reason.isNullOrBlank() || timeLimitMinutes == null) {
                    IntentGateResult.RequiresIntent(
                        message = "Specify intent and duration.\n" +
                            "Example: @${appId} /open --reason 'reply to DM' --time 5m"
                    )
                } else {
                    // Launch with timer
                    overlayService.startCountdown(timeLimitMinutes)
                    IntentGateResult.Approved(
                        launchIntent = buildLaunchIntent(appId),
                        timeLimit = timeLimitMinutes
                    )
                }
            }
        }
    }
}
```

---

## 4. Composio Toolkit Inventory (MVP)

> **Strategic Update:** To maximize our **Feasibility** score and ensure we can deliver a massively capable app in 1 month, KAIROS OS relies on **Composio** for 95% of its integrations. This eliminates months of OAuth and API development.

| Target App / Service | Composio Toolkit | Priority | Notes |
|---|---|---|---|
| **Google Workspace** | `GOOGLESUPER` | P0 | Single connection grants access to 442 tools across Gmail, Calendar, Drive, Docs, and Sheets. |
| **Spotify** | `SPOTIFY` | P1 | Managed OAuth for playback control and metadata fetching. |
| **Slack** | `SLACK` | P1 | Enterprise communication; critical for the "Deep-Work" persona. |
| **Microsoft 365** | `MICROSOFT` | P2 | Mail, Excel, Calendar via Graph API. |
| **Notion / Todoist** | `NOTION`, `TODOIST` | P1 | Personal productivity and task tracking. |
| **Web Search** | `COMPOSIO_SEARCH_TOOLS` | P1 | Native semantic search, web scraping, and Perplexity integration. |
| **Local Device Alarms** | *Custom MCP Server* | P0 | Built with `@modelcontextprotocol/sdk`. Returns Android Intent payload to the phone. The *only* custom tool integration needed. |

**Implementation Strategy:**
During onboarding, users connect their apps via Composio's managed OAuth flow. The KAIROS backend then uses the `@composio/google` provider to dynamically feed the user's authorized tool schemas into the Gemini Intent Router.

---

## 5. API Contract (Android ↔ Backend)

### 5.1 Submit Command

```
POST /api/prompt
```

**Request:**
```json
{
  "prompt": "@gmail show me my important emails",
  "appTarget": "gmail",
  "sessionId": "sess_abc123",
  "userId": "usr_xyz789"
}
```

**Response:** `KairosResponse` (see §3.2)

### 5.2 Classify Notification

```
POST /api/notifications/classify
```

**Request:**
```json
{
  "packageName": "com.instagram.android",
  "title": "@username liked your photo",
  "text": "",
  "category": "social"
}
```

**Response:**
```json
{
  "tier": "DIGEST",
  "reason": "Social media engagement notification — non-critical"
}
```

### 5.3 Get Daily Digest

```
GET /api/notifications/digest?userId=usr_xyz789
```

**Response:**
```json
{
  "type": "WIDGET",
  "widget": {
    "widgetType": "DIGEST_SUMMARY",
    "title": "Daily Digest — 23 notifications",
    "items": [
      {
        "id": "digest_social",
        "primary": "Instagram: 5 likes, 2 comments",
        "secondary": "Nothing requiring immediate action",
        "icon": "social"
      },
      {
        "id": "digest_promo",
        "primary": "3 promotional emails archived",
        "secondary": "Lazada, Shopee, GCash",
        "icon": "mail_promo"
      }
    ]
  }
}
```

### 5.4 Update App Configuration

```
PUT /api/config/apps
```

**Request:**
```json
{
  "userId": "usr_xyz789",
  "appId": "instagram",
  "category": "TRAP",
  "intentGateEnabled": true,
  "defaultTimeLimit": 10
}
```

**Response:**
```json
{
  "status": "PENDING",
  "message": "Configuration change will take effect in 12 hours (cooling-off period)",
  "effectiveAt": "2026-06-26T03:00:00Z"
}
```

---

## 6. Project Structure

```
kairos-os/
├── android/                          # Android Launcher (Kotlin + Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/kairos/os/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── LauncherActivity.kt
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── CommandInputBar.kt
│   │   │   │   │   │   ├── WidgetRenderer.kt
│   │   │   │   │   │   ├── EmailListWidget.kt
│   │   │   │   │   │   ├── CalendarEventWidget.kt
│   │   │   │   │   │   ├── AlarmConfirmWidget.kt
│   │   │   │   │   │   └── DigestSummaryWidget.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       └── Typography.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── KairosResponse.kt
│   │   │   │   │   │   ├── WidgetPayload.kt
│   │   │   │   │   │   ├── Interaction.kt
│   │   │   │   │   │   └── AppConfig.kt
│   │   │   │   │   └── usecases/
│   │   │   │   │       ├── SubmitCommandUseCase.kt
│   │   │   │   │       └── IntentGateUseCase.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   └── KairosApiClient.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── NotificationRepository.kt
│   │   │   │   │       └── AppConfigRepository.kt
│   │   │   │   ├── services/
│   │   │   │   │   ├── KairosNotificationListener.kt
│   │   │   │   │   ├── FloatingBubbleService.kt
│   │   │   │   │   └── IntentGateManager.kt
│   │   │   │   └── di/
│   │   │   │       └── AppModule.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── floating_bubble.xml
│   │   │   │   └── values/
│   │   │   │       └── strings.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                          # Next.js Backend
│   ├── src/
│   │   ├── app/
│   │   │   ├── api/
│   │   │   │   ├── prompt/
│   │   │   │   │   └── route.ts      # Main command endpoint
│   │   │   │   ├── notifications/
│   │   │   │   │   ├── classify/
│   │   │   │   │   │   └── route.ts
│   │   │   │   │   └── digest/
│   │   │   │   │       └── route.ts
│   │   │   │   └── config/
│   │   │   │       └── apps/
│   │   │   │           └── route.ts
│   │   │   ├── layout.tsx
│   │   │   └── page.tsx              # Admin dashboard (optional)
│   │   ├── lib/
│   │   │   ├── ai/
│   │   │   │   ├── gemini-client.ts   # @google/genai wrapper
│   │   │   │   ├── intent-classifier.ts
│   │   │   │   └── model-tiers.ts
│   │   │   ├── mcp/
│   │   │   │   ├── mcp-registry.ts    # MCP server discovery & routing
│   │   │   │   ├── servers/
│   │   │   │   │   ├── gmail-server.ts
│   │   │   │   │   ├── calendar-server.ts
│   │   │   │   │   ├── notes-server.ts
│   │   │   │   │   ├── alarm-server.ts
│   │   │   │   │   ├── browser-server.ts
│   │   │   │   │   └── sheets-server.ts
│   │   │   │   └── transport.ts
│   │   │   ├── router/
│   │   │   │   ├── intent-router.ts   # Tier 0/1/2 dispatch logic
│   │   │   │   └── app-resolver.ts    # @app → MCP server mapping
│   │   │   ├── response/
│   │   │   │   ├── response-builder.ts
│   │   │   │   └── widget-schemas.ts
│   │   │   └── db/
│   │   │       ├── prisma.ts
│   │   │       └── schema.prisma
│   │   └── proxy.ts                   # Edge middleware (auth, rate limit)
│   ├── package.json
│   ├── tsconfig.json
│   └── next.config.ts
│
├── context/                           # Project documentation
│   ├── CONTEXT.md
│   ├── PROJECT_REQUIREMENTS_DOCUMENT.md
│   └── TECHNICAL_IMPLEMENTATION_DOCUMENT.md
│
└── README.md
```

---

## 7. Development Phases (1-Month Sprint Plan)

### Phase 1: Foundation (Days 1–7)

| Task | Owner | Deliverable |
|---|---|---|
| Initialize Android project (Kotlin 2.4, Compose BOM 2026.06) | Android Dev | Compilable launcher shell |
| Initialize Next.js 16 backend with TypeScript | Backend Dev | Deployable server skeleton |
| Set up Prisma + PostgreSQL schema | Backend Dev | Database migration scripts |
| Implement basic `POST /api/prompt` route handler | Backend Dev | Echo endpoint |
| Build `CommandInputBar` + `LazyColumn` stream UI | Android Dev | Text input + response feed |
| Configure Gemini SDK (`@google/genai`) with function calling | Backend Dev | Working AI client |

### Phase 2: Core Loop (Days 8–14)

| Task | Owner | Deliverable |
|---|---|---|
| Implement Intent Router (Tier 0 + Tier 1 + Tier 2 dispatch) | Backend Dev | Smart prompt routing |
| Build Gmail MCP server (`kairos-gmail`) | Backend Dev | `list-important-emails` tool |
| Build Calendar MCP server (`kairos-calendar`) | Backend Dev | `create-event`, `list-events` tools |
| Build Alarm MCP server (returns Android intent payloads) | Backend Dev | `set-alarm` tool |
| Implement `WidgetRenderer` for Email, Calendar, Alarm widgets | Android Dev | Native Compose widgets |
| Implement `/open` command → Android intent dispatch | Android Dev | Direct app launching |
| End-to-end test: prompt → backend → widget response | Full Team | Working demo loop |

### Phase 3: Differentiation Features (Days 15–21)

| Task | Owner | Deliverable |
|---|---|---|
| Implement `NotificationListenerService` | Android Dev | Notification capture |
| Build notification classification endpoint | Backend Dev | Critical vs. Digest routing |
| Build daily digest endpoint + `DigestSummaryWidget` | Full Team | `@launcher daily digest` flow |
| Implement Intent Gate system (Trap app detection + reason prompt) | Full Team | `@instagram /open --reason --time` flow |
| Implement `FloatingBubbleService` overlay | Android Dev | Persistent bubble + countdown |
| Build Notes MCP server (in-house) | Backend Dev | `create-note`, `list-notes` tools |
| Conversation context management (session state, follow-up support) | Backend Dev | Contextual conversations |

### Phase 4: Polish & Demo (Days 22–30)

| Task | Owner | Deliverable |
|---|---|---|
| Onboarding flow (Compose screens: philosophy, Trap/Utility config, notification rules) | Android Dev | Complete onboarding |
| Cooling-off period enforcement for settings changes | Backend Dev | 12-hour delay logic |
| UI polish: animations, transitions, Material 3 theme, typography | Android Dev | Premium look and feel |
| Browser MCP server | Backend Dev | `@browser` search queries |
| Stress testing: latency benchmarks per tier | Full Team | Performance validation |
| Demo script preparation | Full Team | End-to-end demo flow |
| Bug fixes and edge case handling | Full Team | Stable build |

---

## 8. Security Considerations

| Concern | Mitigation |
|---|---|
| **OAuth tokens** | Stored exclusively on backend; never sent to Android client |
| **API keys** | Gemini API key stored as environment variable on server; restricted key required (June 2026 policy) |
| **User data** | Notification payloads processed and immediately discarded after classification; only summaries stored |
| **Transport** | All Android ↔ Backend communication over HTTPS |
| **MCP servers** | Run in sandboxed processes; no cross-server data access |
| **Overlay permissions** | `SYSTEM_ALERT_WINDOW` permission requested transparently during onboarding |

---

## 9. Deployment Architecture

```
┌──────────────┐     HTTPS      ┌────────────────────────┐
│   Android    │ ◄────────────► │   Vercel / Railway      │
│   Client     │                │                        │
│   (APK)      │                │  ┌──────────────────┐  │
│              │                │  │  Next.js 16 App  │  │
└──────────────┘                │  │  (Route Handlers) │  │
                                │  └────────┬─────────┘  │
                                │           │            │
                                │  ┌────────▼─────────┐  │
                                │  │  MCP Servers      │  │
                                │  │  (In-process)     │  │
                                │  └────────┬─────────┘  │
                                │           │            │
                                │  ┌────────▼─────────┐  │
                                │  │  PostgreSQL       │  │
                                │  │  (Managed DB)     │  │
                                │  └──────────────────┘  │
                                └────────────────────────┘
                                           │
                                     ┌─────┴──────┐
                                     │            │
                              ┌──────▼───┐  ┌────▼─────┐
                              │ Gemini   │  │ Google   │
                              │ API      │  │ OAuth    │
                              │ (AI)     │  │ (APIs)   │
                              └──────────┘  └──────────┘
```

---

## 10. Version Summary

| Technology | Version | Source |
|---|---|---|
| Kotlin | 2.4.0 | kotlinlang.org (June 2026) |
| Jetpack Compose BOM | 2026.06.00 (Compose 1.11.3) | developer.android.com |
| Material Design 3 | Via Compose BOM | developer.android.com |
| Android Gradle Plugin | 9.1.0+ | developer.android.com |
| Gradle | 8.7+ / 9.5.0 compatible | gradle.org |
| Next.js | 16.2.9 | nextjs.org (June 2026) |
| Node.js | 22 LTS | nodejs.org |
| TypeScript | 5.x | typescriptlang.org |
| `@google/genai` SDK | ~2.9.0 | npm (June 2026) |
| Gemini 3.5 Flash | `gemini-3.5-flash` | Google AI (May 2026 GA) |
| Gemini 3.1 Flash-Lite | `gemini-3.1-flash-lite` | Google AI (May 2026 GA) |
| `@modelcontextprotocol/sdk` | 1.29.0 | npm (June 2026) |
| MCP Spec | 2026-07-28 RC | modelcontextprotocol.io |
| Prisma | Latest stable | prisma.io |
| Zod | Latest stable | zod.dev |

---

*Document Version: 1.0*  
*Last Updated: June 25, 2026*  
*Authors: Ian Szky & Team KAIROS*
