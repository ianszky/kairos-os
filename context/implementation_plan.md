# KAIROS OS — Database & Auth Implementation Plan

> **Goal:** Wire up a production-ready database and authentication layer that works seamlessly across the **Kotlin/Compose Android frontend**, the **Next.js 16 backend**, and **Composio's managed tool-calling services** — without breaking any existing code.

---

## User Review Required

> [!IMPORTANT]
> **Supabase replaces the original Prisma plan.** The TID mentions Prisma as the ORM, but Prisma has zero presence in the current codebase (no `schema.prisma`, no dependency in `package.json`). This plan proposes **Supabase** as the unified database + auth platform. This is a deliberate deviation — read the rationale below before approving.

> [!WARNING]
> **Composio handles its OWN OAuth for third-party services (Gmail, Calendar, Spotify, etc.).** Supabase Auth is for **KAIROS user identity** only — it does NOT replace Composio's managed auth for Google Workspace or other toolkits. These are two separate auth concerns that must coexist cleanly.

---

## Open Questions

> [!IMPORTANT]
> **Q1: Google Cloud Project.** Do you already have a Google Cloud project with OAuth 2.0 credentials configured (Client ID + Client Secret)? Supabase needs these to enable "Sign in with Google." If not, we'll need to create one during implementation.

> [!IMPORTANT]  
> **Q2: Deployment target for Supabase.** Should we use **Supabase Cloud** (hosted, free tier available, zero ops) or **self-hosted Supabase** via Docker? For a 1-month hackathon, Cloud is strongly recommended.

> [!IMPORTANT]
> **Q3: Android auth flow preference.** The plan proposes **Google One Tap → Supabase** on Android, which is the fastest UX. Are you okay with Google-only auth for the MVP, or do you also want email/password as a fallback?

---

## Rationale: Why Supabase Over Prisma + Raw PostgreSQL

| Criterion | Prisma + Raw PG | Supabase |
|---|---|---|
| **Auth out-of-the-box** | ❌ Must build from scratch (JWT, sessions, OAuth flows) | ✅ Built-in Auth with Google OAuth, JWT issuance, token refresh |
| **Kotlin Android SDK** | ❌ None. Must use Retrofit to hit custom auth endpoints | ✅ `supabase-kt` — official KMP SDK with `Auth`, `Postgrest`, Hilt-injectable |
| **Next.js SSR support** | ✅ Prisma works great with Server Components | ✅ `@supabase/ssr` — purpose-built for Next.js App Router middleware |
| **Row Level Security** | ❌ Must implement authorization in application code | ✅ RLS at the database level — policies enforce access even if app logic has bugs |
| **Hackathon speed** | 🟡 Slower — schema migrations, custom auth, manual JWT handling | ✅ Fastest path to a working multi-user system |
| **Composio compatibility** | ✅ Compatible (just stores user IDs) | ✅ Compatible — Composio uses its own user ID mapping, we just pass `supabase.auth.uid()` |
| **Type safety** | ✅ Prisma Client generates types | ✅ `supabase gen types` generates TypeScript types from your schema |
| **Real-time** | ❌ Must add WebSocket layer | ✅ Built-in Realtime subscriptions (useful for notification queue) |

**Bottom line:** Supabase gives us Auth + Database + RLS + Realtime + Kotlin SDK + Next.js middleware in a single dependency. For a 1-month hackathon, this is the pragmatic choice.

---

## Proposed Changes

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    ANDROID CLIENT (Kotlin)                       │
│                                                                 │
│  supabase-kt (Auth)     Retrofit (API calls to Next.js)        │
│  ┌──────────────┐       ┌──────────────────────────────┐       │
│  │ Google One   │       │ POST /api/prompt             │       │
│  │ Tap → Auth   │       │ Authorization: Bearer <jwt>  │       │
│  └──────┬───────┘       └──────────────┬───────────────┘       │
│         │ JWT token                     │                       │
│         └──────────────────┬────────────┘                       │
└────────────────────────────┼────────────────────────────────────┘
                             │ HTTPS + JWT
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NEXT.JS BACKEND                               │
│                                                                 │
│  proxy.ts (middleware)                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ @supabase/ssr → createServerClient()                     │   │
│  │ Validates JWT on every request, refreshes tokens         │   │
│  │ Attaches authenticated user to request context           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                             │                                   │
│  Route Handlers (API)       │                                   │
│  ┌──────────────────────────▼──────────────────────────────┐   │
│  │ /api/prompt/route.ts     → uses user.id for Composio    │   │
│  │ /api/notifications/*     → user-scoped notification ops │   │
│  │ /api/config/apps/route.ts→ user-scoped app configs      │   │
│  │ /api/auth/callback/route.ts → OAuth callback handler    │   │
│  └──────────────────────────┬──────────────────────────────┘   │
│                             │                                   │
│  ┌──────────────────────────▼──────────────────────────────┐   │
│  │ Supabase Client (server)  │  Composio SDK               │   │
│  │ @supabase/supabase-js     │  @composio/core              │   │
│  │ DB reads/writes with RLS  │  Tool calls with managed     │   │
│  │                           │  OAuth (separate auth!)      │   │
│  └──────────────────────────┬──────────────────────────────┘   │
└──────────────────────────────┼──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SUPABASE (Cloud)                              │
│                                                                 │
│  ┌───────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ Auth (GoTrue)  │  │ PostgreSQL     │  │ Realtime         │  │
│  │ Google OAuth   │  │ + RLS Policies │  │ (notifications)  │  │
│  └───────────────┘  └────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

### Component 1: Supabase Project & Database Schema

#### [NEW] Supabase Cloud Project Setup

- Create a new Supabase project
- Enable Google OAuth provider in Supabase Auth dashboard
- Configure redirect URLs for both Android deep links and Next.js callback

#### [NEW] SQL Migration: Core Tables

The database schema from the [TECHNICAL_IMPLEMENTATION_DOCUMENT.md](file:///C:/Dev/kairos-os/context/TECHNICAL_IMPLEMENTATION_DOCUMENT.md#L493-L541) is preserved with minimal changes. The key difference is that `users` is now managed by Supabase Auth (`auth.users`), and all tables reference `auth.users.id` instead of a custom `users` table.

```sql
-- profiles: extends Supabase auth.users with app-specific data
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    strict_mode BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- conversations, messages, context_summaries, notification_queue, user_app_configs
-- (same schema as TID §3.4, with user_id referencing auth.users)
```

#### [NEW] SQL Migration: Row Level Security Policies

```sql
-- Every table gets RLS enabled
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
-- ... etc for all tables

-- Users can only access their own data
CREATE POLICY "Users read own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);
-- Similar policies for all tables
```

---

### Component 2: Next.js Backend — Auth & Database Integration

#### [NEW] `backend/src/lib/supabase/server.ts`

Server-side Supabase client factory for use in Route Handlers. Uses `@supabase/ssr` with the `cookies()` API.

```typescript
import { createServerClient } from '@supabase/ssr';
import { cookies } from 'next/headers';

export async function createClient() {
  const cookieStore = await cookies();
  return createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll: () => cookieStore.getAll(),
        setAll: (cookiesToSet) => {
          cookiesToSet.forEach(({ name, value, options }) =>
            cookieStore.set(name, value, options)
          );
        },
      },
    }
  );
}
```

#### [NEW] `backend/src/lib/supabase/admin.ts`

Service-role client for backend operations that bypass RLS (e.g., notification classification writing).

```typescript
import { createClient } from '@supabase/supabase-js';

export const supabaseAdmin = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!, // server-only, never exposed
);
```

#### [NEW] `backend/src/proxy.ts` (Next.js 16 proxy convention)

Proxy function that validates the JWT on every API request and refreshes tokens.

```typescript
// proxy.ts — Next.js 16 proxy convention
import { createServerClient } from '@supabase/ssr';
import { NextResponse, type NextRequest } from 'next/server';

export async function proxy(request: NextRequest) {
  let response = NextResponse.next({ request });
  const supabase = createServerClient(/* ... cookie handlers ... */);
  const { data: { user } } = await supabase.auth.getUser();
  
  // Protect API routes
  if (request.nextUrl.pathname.startsWith('/api/') && !user) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }
  return response;
}

export const config = {
  matcher: ['/api/:path*'],
};
```

#### [NEW] `backend/src/app/api/auth/callback/route.ts`

OAuth callback handler for the PKCE flow from Android.

#### [MODIFY] [route.ts](file:///C:/Dev/kairos-os/backend/src/app/api/prompt/route.ts)

Update the existing prompt route to:
1. Extract the authenticated user from the Supabase session
2. Pass `user.id` to Composio as the `userId` parameter
3. Store conversation messages in Supabase (instead of the current `mock-session-123` placeholder)

#### [MODIFY] [.env.local](file:///C:/Dev/kairos-os/backend/.env.local)

Add Supabase environment variables alongside existing Composio and Gemini keys:
```
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon-key>
SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
```

---

### Component 3: Kotlin Android — Auth Integration

#### [NEW] Supabase Dependencies in `build.gradle.kts`

Add to [build.gradle.kts](file:///C:/Dev/kairos-os/android/app/build.gradle.kts):

```kotlin
// Supabase BOM
implementation(platform("io.github.jan-tennert.supabase:bom:3.7.0"))
implementation("io.github.jan-tennert.supabase:auth-kt")       // GoTrue auth
implementation("io.github.jan-tennert.supabase:postgrest-kt")   // Optional: direct DB access

// Ktor (required HTTP engine for supabase-kt)
implementation("io.ktor:ktor-client-android:3.4.3")

// Kotlin Serialization (required by supabase-kt)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
```

Also add the serialization plugin:
```kotlin
plugins {
    kotlin("plugin.serialization")
}
```

#### [NEW] `di/SupabaseModule.kt`

Hilt module providing the `SupabaseClient` as a singleton, consistent with the existing Hilt DI setup:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "kairos"    // deep link scheme
                host = "login"      // deep link host
            }
        }
    }

    @Provides @Singleton
    fun provideAuth(client: SupabaseClient): Auth = client.auth
}
```

#### [NEW] `ui/screens/LoginScreen.kt`

Minimal login screen using Google One Tap via supabase-kt:

```kotlin
@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel()) {
    // Blank canvas with KAIROS branding + "Sign in with Google" button
    // On success → navigate to HomeScreen
}
```

#### [MODIFY] `LauncherActivity.kt`

- Handle deep link callbacks from Supabase OAuth: `supabaseClient.handleDeeplinks(intent)`
- Add auth state check: if not authenticated, show `LoginScreen`; otherwise show `KairosHomeScreen`

#### [MODIFY] `data/api/KairosApiClient.kt`

Add the Supabase JWT as a `Bearer` token in the `Authorization` header for all Retrofit calls to the Next.js backend.

#### [MODIFY] `AndroidManifest.xml`

Add deep link intent filter for the OAuth callback:
```xml
<activity android:name=".ui.LauncherActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="kairos" android:host="login" />
    </intent-filter>
</activity>
```

---

### Component 4: Composio ↔ Supabase User ID Bridge

This is the critical integration point. Composio identifies users by a string `userId`. Supabase identifies users by a UUID `auth.uid()`. They must be bridged.

#### Strategy: Use Supabase `auth.uid()` as Composio's `userId`

```typescript
// In /api/prompt/route.ts
const supabase = await createClient();
const { data: { user } } = await supabase.auth.getUser();

// Bridge: Supabase user ID → Composio user ID
const session = await composio.create(user.id, {
  toolkits: [appTarget],
});
```

This means:
- When a user first connects a Google Workspace account via Composio, the `connected_account` is stored under their Supabase UUID
- All subsequent tool calls are scoped to that user
- No separate user management system needed

---

### Component 5: Database Access Layer

#### [NEW] `backend/src/lib/db/` directory

| File | Purpose |
|---|---|
| `conversations.ts` | CRUD for conversations & messages using Supabase client |
| `notifications.ts` | Queue/dequeue notifications, digest generation |
| `app-configs.ts` | User app configuration (Utility/Trap, time limits) |
| `profiles.ts` | User profile management |

All database access uses the **Supabase JS client** (not Prisma), with RLS automatically enforcing user-scoped access.

#### Type Generation

```bash
npx supabase gen types typescript --project-id <ref> > src/types/database.ts
```

This generates full TypeScript types from the Supabase schema — equivalent to Prisma's generated types but without the ORM overhead.

---

## File Summary

| Action | File | Layer |
|---|---|---|
| **[NEW]** | Supabase project + SQL migrations | Infrastructure |
| **[NEW]** | `backend/src/lib/supabase/server.ts` | Backend |
| **[NEW]** | `backend/src/lib/supabase/admin.ts` | Backend |
| **[NEW]** | `backend/src/proxy.ts` | Backend |
| **[NEW]** | `backend/src/app/api/auth/callback/route.ts` | Backend |
| **[NEW]** | `backend/src/lib/db/conversations.ts` | Backend |
| **[NEW]** | `backend/src/lib/db/notifications.ts` | Backend |
| **[NEW]** | `backend/src/lib/db/app-configs.ts` | Backend |
| **[NEW]** | `backend/src/lib/db/profiles.ts` | Backend |
| **[NEW]** | `backend/src/types/database.ts` (generated) | Backend |
| **[NEW]** | `android/.../di/SupabaseModule.kt` | Android |
| **[NEW]** | `android/.../ui/screens/LoginScreen.kt` | Android |
| **[NEW]** | `android/.../ui/viewmodels/AuthViewModel.kt` | Android |
| **[MODIFY]** | `backend/.env.local` | Backend |
| **[MODIFY]** | `backend/package.json` (+`@supabase/supabase-js`, `@supabase/ssr`) | Backend |
| **[MODIFY]** | `backend/src/app/api/prompt/route.ts` | Backend |
| **[MODIFY]** | `android/app/build.gradle.kts` (+supabase-kt, ktor, serialization) | Android |
| **[MODIFY]** | `android/.../ui/LauncherActivity.kt` | Android |
| **[MODIFY]** | `android/.../data/api/KairosApiClient.kt` | Android |
| **[MODIFY]** | `android/app/src/main/AndroidManifest.xml` | Android |

---

## Verification Plan

### Automated Tests
- `npm run test` — existing vitest suite still passes after backend changes
- New test: `POST /api/prompt` without auth header → returns 401
- New test: `POST /api/prompt` with valid JWT → returns `KairosResponse`
- Supabase migration dry-run: `supabase db reset` succeeds without errors
- Type generation: `npx supabase gen types` produces valid TypeScript

### Manual Verification
- **Android:** Install APK → Google One Tap sign-in → JWT stored → API calls authenticated
- **Backend:** Verify Composio tool calls work with Supabase user ID as the entity ID
- **Database:** Verify RLS policies block cross-user data access via the Supabase dashboard SQL editor
- **End-to-end:** Submit `@gmail show me my emails` from Android → Backend authenticates user → Composio executes with user's connected Google account → Widget response rendered on Android
