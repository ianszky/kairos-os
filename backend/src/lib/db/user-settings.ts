import { SupabaseClient } from '@supabase/supabase-js';

export interface UserSettingsData {
  daily_leisure_minutes: number;
  daily_leisure_minutes_pending: number | null;
  pending_change_effective_at: string | null;
}

/** Tomorrow 00:00:00.000 — same day boundary as intent-log usage totals. */
function getNextDayStart(): Date {
  const next = new Date();
  next.setHours(0, 0, 0, 0);
  next.setDate(next.getDate() + 1);
  return next;
}

export async function getUserSettings(
  supabase: SupabaseClient<any>,
  userId: string
): Promise<UserSettingsData> {
  const { data, error } = await supabase
    .from('user_settings')
    .select('*')
    .eq('user_id', userId)
    .single();

  if (error && error.code === 'PGRST116') {
    const { data: newSettings, error: insertError } = await supabase
      .from('user_settings')
      .insert({
        user_id: userId,
        daily_leisure_minutes: 60,
      })
      .select()
      .single();

    if (insertError) throw insertError;
    return newSettings;
  }

  if (error) throw error;

  if (
    data.daily_leisure_minutes_pending !== null &&
    data.pending_change_effective_at &&
    new Date(data.pending_change_effective_at) <= new Date()
  ) {
    const maturedValue = data.daily_leisure_minutes_pending;
    const { data: updated, error: updateErr } = await supabase
      .from('user_settings')
      .update({
        daily_leisure_minutes: maturedValue,
        daily_leisure_minutes_pending: null,
        pending_change_effective_at: null,
        updated_at: new Date().toISOString(),
      })
      .eq('user_id', userId)
      .select()
      .single();

    if (!updateErr && updated) {
      return updated;
    }
  }

  return data;
}

export async function updateDailyLeisureTime(
  supabase: SupabaseClient<any>,
  userId: string,
  newMinutes: number
): Promise<{ status: 'PENDING'; message: string; effectiveAt: string; settings: UserSettingsData }> {
  await getUserSettings(supabase, userId);

  const effectiveAtDate = getNextDayStart();
  const effectiveAtIso = effectiveAtDate.toISOString();

  const { data: updated, error } = await supabase
    .from('user_settings')
    .update({
      daily_leisure_minutes_pending: newMinutes,
      pending_change_effective_at: effectiveAtIso,
      updated_at: new Date().toISOString(),
    })
    .eq('user_id', userId)
    .select()
    .single();

  if (error) throw error;
  return {
    status: 'PENDING',
    message: 'Daily leisure limit change will take effect tomorrow.',
    effectiveAt: effectiveAtIso,
    settings: updated,
  };
}
