-- KAIROS OS: Scheduled Tasks & CRON Jobs SQL Migration
-- Run this in your Supabase SQL Editor

-- 1. Create scheduled_tasks table
CREATE TABLE IF NOT EXISTS public.scheduled_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    prompt TEXT NOT NULL,
    app_target TEXT,
    title TEXT,
    frequency TEXT NOT NULL CHECK (frequency IN ('daily', 'weekly', 'specific_days')),
    days_of_week INTEGER[] DEFAULT '{}',
    time_of_day TIME NOT NULL,
    timezone TEXT NOT NULL DEFAULT 'Asia/Manila',
    cron_expression TEXT,
    pg_cron_job_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Create scheduled_task_runs table
CREATE TABLE IF NOT EXISTS public.scheduled_task_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES public.scheduled_tasks(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE SET NULL,
    status TEXT NOT NULL CHECK (status IN ('pending', 'running', 'completed', 'failed')),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

-- 3. Add source and scheduled_task_id columns to public.conversations
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS source TEXT DEFAULT 'chat';
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS scheduled_task_id UUID REFERENCES public.scheduled_tasks(id) ON DELETE SET NULL;

-- 4. Enable RLS
ALTER TABLE public.scheduled_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scheduled_task_runs ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies
DROP POLICY IF EXISTS "Users can manage their own scheduled tasks" ON public.scheduled_tasks;
CREATE POLICY "Users can manage their own scheduled tasks"
    ON public.scheduled_tasks FOR ALL USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can view their own task runs" ON public.scheduled_task_runs;
CREATE POLICY "Users can view their own task runs"
    ON public.scheduled_task_runs FOR ALL USING (
        EXISTS (
            SELECT 1 FROM public.scheduled_tasks
            WHERE scheduled_tasks.id = scheduled_task_runs.task_id
            AND scheduled_tasks.user_id = auth.uid()
        )
    );

-- 6. Indexes
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_user_active ON public.scheduled_tasks(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_runs_task ON public.scheduled_task_runs(task_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_source ON public.conversations(source);
