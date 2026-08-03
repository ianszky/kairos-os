import { SupabaseClient } from '@supabase/supabase-js';
import { KairosResponse } from '@/types/kairos';

export type PromptRunStatus = 'pending' | 'running' | 'completed' | 'failed';

export async function createPromptRun(
  supabase: SupabaseClient<any>,
  userId: string,
  conversationId: string,
  userMessageId: string | null,
  status: PromptRunStatus = 'running'
) {
  const { data, error } = await supabase
    .from('prompt_runs')
    .insert({
      user_id: userId,
      conversation_id: conversationId,
      user_message_id: userMessageId,
      status,
      started_at: new Date().toISOString(),
    })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function updatePromptRun(
  supabase: SupabaseClient<any>,
  runId: string,
  status: 'completed' | 'failed',
  options?: {
    errorMessage?: string;
    responsePayload?: KairosResponse;
  }
) {
  const updates: Record<string, unknown> = {
    status,
    completed_at: new Date().toISOString(),
  };
  if (options?.errorMessage) updates.error_message = options.errorMessage;
  if (options?.responsePayload) updates.response_payload = options.responsePayload;

  const { data, error } = await supabase
    .from('prompt_runs')
    .update(updates)
    .eq('id', runId)
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getPromptRun(
  supabase: SupabaseClient<any>,
  runId: string,
  userId: string
) {
  const { data, error } = await supabase
    .from('prompt_runs')
    .select('*')
    .eq('id', runId)
    .eq('user_id', userId)
    .single();

  if (error) return null;
  return data;
}
