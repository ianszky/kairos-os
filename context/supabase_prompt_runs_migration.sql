-- KAIROS OS: Async Prompt Runs SQL Migration
-- Run this in your Supabase SQL Editor

CREATE TABLE IF NOT EXISTS public.prompt_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_message_id UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    status TEXT NOT NULL CHECK (status IN ('pending', 'running', 'completed', 'failed')),
    error_message TEXT,
    response_payload JSONB,
    started_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

ALTER TABLE public.prompt_runs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own prompt runs" ON public.prompt_runs;
CREATE POLICY "Users can manage their own prompt runs"
    ON public.prompt_runs FOR ALL USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_prompt_runs_user_started ON public.prompt_runs(user_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_prompt_runs_conversation ON public.prompt_runs(conversation_id, started_at DESC);
