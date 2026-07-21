import { SupabaseClient } from '@supabase/supabase-js';

export interface UserSettingsData {
  daily_leisure_minutes: number;
  daily_leisure_minutes_pending: number | null;
  pending_change_effective_at: string | null;
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
    // Record does not exist, insert default settings (60 minutes)
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

  // Check if a pending increase has matured (12 hour cooling-off period passed)
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
): Promise<{ status: 'APPLIED' | 'PENDING'; message: string; effectiveAt?: string; settings: UserSettingsData }> {
  const current = await getUserSettings(supabase, userId);

  // Decreasing or keeping budget same applies immediately (no bypass vulnerability)
  if (newMinutes <= current.daily_leisure_minutes) {
    const { data: updated, error } = await supabase
      .from('user_settings')
      .update({
        daily_leisure_minutes: newMinutes,
        daily_leisure_minutes_pending: null,
        pending_change_effective_at: null,
        updated_at: new Date().toISOString(),
      })
      .eq('user_id', userId)
      .select()
      .single();

    if (error) throw error;
    return {
      status: 'APPLIED',
      message: 'Daily leisure time updated immediately.',
      settings: updated,
    };
  } else {
    // Increasing budget requires a 12-hour cooling-off period
    const effectiveAtDate = new Date(Date.now() + 12 * 60 * 60 * 1000);
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
      message: 'Increase in daily leisure time will take effect in 12 hours (cooling-off period).',
      effectiveAt: effectiveAtIso,
      settings: updated,
    };
  }
}
