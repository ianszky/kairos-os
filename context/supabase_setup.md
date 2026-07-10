# Supabase SQL Schema and Scripts

Here are the SQL scripts you can run in your Supabase SQL Editor to set up your KAIROS OS database.

## 1. Schema Generation

This script creates the `public.users` table and the tables defined in the technical implementation document. It also sets up some basic Row Level Security (RLS) so users can only access their own data.

```sql
-- 1. Create the Users table
CREATE TABLE public.users (
  id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT,
  full_name TEXT,
  avatar_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()),
  
  PRIMARY KEY (id)
);

-- 2. Create the remaining tables based on the Technical Implementation Document
CREATE TABLE public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    app_target TEXT,           -- e.g., 'gmail', 'calendar'
    model_tier TEXT,           -- 'flash-lite', 'flash', 'pro'
    widget_payload JSONB,      -- stored widget response
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.context_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    summary JSONB NOT NULL,    -- condensed context JSON
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    title TEXT,
    body TEXT,
    tier TEXT NOT NULL CHECK (tier IN ('CRITICAL', 'DIGEST')),
    is_read BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.user_app_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    app_identifier TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('UTILITY', 'TRAP')),
    intent_gate_enabled BOOLEAN DEFAULT FALSE,
    default_time_limit INTEGER,  -- minutes
    vip_contact BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, app_identifier)
);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.context_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_app_configs ENABLE ROW LEVEL SECURITY;

-- 4. Create RLS Policies (Users can only see/modify their own data)
CREATE POLICY "Users can manage their own profile" ON public.users
  FOR ALL USING (auth.uid() = id);

CREATE POLICY "Users can manage their own conversations" ON public.conversations
  FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own messages" ON public.messages
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.conversations 
      WHERE conversations.id = messages.conversation_id AND conversations.user_id = auth.uid()
    )
  );

CREATE POLICY "Users can manage their own summaries" ON public.context_summaries
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.conversations 
      WHERE conversations.id = context_summaries.conversation_id AND conversations.user_id = auth.uid()
    )
  );

CREATE POLICY "Users can manage their own notifications" ON public.notification_queue
  FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own configs" ON public.user_app_configs
  FOR ALL USING (auth.uid() = user_id);
```

---

## 2. Automatic Transfer on Google Sign Up (Trigger & Function)

This script creates a Postgres function and a trigger. Whenever a user authenticates via Google (or any auth method) and a new row is created in `auth.users`, it will automatically create a corresponding row in `public.users` with their email and metadata.

```sql
-- 1. Create the function
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.users (id, email, full_name, avatar_url)
  VALUES (
    NEW.id,
    NEW.email,
    NEW.raw_user_meta_data->>'full_name',
    NEW.raw_user_meta_data->>'avatar_url'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Create the trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();
```

---

## 3. Backfill Existing Users

If you already have existing users who authenticated with Google before this trigger was created, run this script to manually transfer those existing `auth.users` over to `public.users`.

```sql
INSERT INTO public.users (id, email, full_name, avatar_url)
SELECT 
  id, 
  email, 
  raw_user_meta_data->>'full_name', 
  raw_user_meta_data->>'avatar_url'
FROM auth.users
ON CONFLICT (id) DO NOTHING;
```
