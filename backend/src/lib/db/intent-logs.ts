import { SupabaseClient } from '@supabase/supabase-js';

export interface LogIntentParams {
  appIdentifier: string;
  appDisplayName?: string;
  reason: string;
  timeLimitMinutes: number;
  aiApproved?: boolean;
}

export async function logIntent(
  supabase: SupabaseClient<any>,
  userId: string,
  params: LogIntentParams
) {
  const { data, error } = await supabase
    .from('intent_logs')
    .insert({
      user_id: userId,
      app_identifier: params.appIdentifier,
      app_display_name: params.appDisplayName || params.appIdentifier,
      reason: params.reason,
      time_limit_minutes: params.timeLimitMinutes,
      ai_approved: params.aiApproved ?? true,
    })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getTodayUsageMinutes(
  supabase: SupabaseClient<any>,
  userId: string
): Promise<number> {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);

  const { data, error } = await supabase
    .from('intent_logs')
    .select('time_limit_minutes')
    .eq('user_id', userId)
    .gte('opened_at', startOfDay.toISOString());

  if (error) {
    console.error('[intent-logs] Failed to calculate today usage:', error);
    return 0;
  }

  return (data || []).reduce((acc: number, item: { time_limit_minutes: number }) => acc + (item.time_limit_minutes || 0), 0);
}

export async function getIntentHistory(
  supabase: SupabaseClient<any>,
  userId: string,
  limit: number = 20
) {
  const { data, error } = await supabase
    .from('intent_logs')
    .select('*')
    .eq('user_id', userId)
    .order('opened_at', { ascending: false })
    .limit(limit);

  if (error) throw error;
  return data;
}
