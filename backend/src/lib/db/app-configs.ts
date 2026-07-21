import { SupabaseClient } from '@supabase/supabase-js';

export interface UserAppConfigItem {
  id: string;
  app_identifier: string;
  category: 'UTILITY' | 'TRAP';
  pending_category: 'UTILITY' | 'TRAP' | null;
  pending_change_effective_at: string | null;
  intent_gate_enabled: boolean;
  default_time_limit: number | null;
}

export async function getAppConfig(supabase: SupabaseClient<any>, appTarget: string) {
  const { data, error } = await supabase
    .from('app_configs')
    .select('*')
    .eq('app_target', appTarget)
    .single();

  if (error) return null;
  return data;
}

export async function getUserAppConfigs(
  supabase: SupabaseClient<any>,
  userId: string
): Promise<UserAppConfigItem[]> {
  const { data, error } = await supabase
    .from('user_app_configs')
    .select('*')
    .eq('user_id', userId);

  if (error) {
    console.error('[app-configs] Failed to fetch user_app_configs:', error);
    return [];
  }

  const now = new Date();
  const result: UserAppConfigItem[] = [];

  for (const item of data || []) {
    // Check if pending change has matured
    if (
      item.pending_category &&
      item.pending_change_effective_at &&
      new Date(item.pending_change_effective_at) <= now
    ) {
      const maturedCategory = item.pending_category;
      const { data: updated } = await supabase
        .from('user_app_configs')
        .update({
          category: maturedCategory,
          pending_category: null,
          pending_change_effective_at: null,
        })
        .eq('id', item.id)
        .select()
        .single();

      if (updated) {
        result.push(updated);
        continue;
      }
    }
    result.push(item);
  }

  return result;
}

export async function toggleAppClassification(
  supabase: SupabaseClient<any>,
  userId: string,
  appIdentifier: string,
  isDistracting: boolean
): Promise<{ status: 'APPLIED' | 'PENDING'; message: string; effectiveAt?: string; config: UserAppConfigItem }> {
  const newCategory: 'UTILITY' | 'TRAP' = isDistracting ? 'TRAP' : 'UTILITY';

  // Fetch existing config for this app if any
  const { data: existing } = await supabase
    .from('user_app_configs')
    .select('*')
    .eq('user_id', userId)
    .eq('app_identifier', appIdentifier)
    .maybeSingle();

  // If making an app distracting (UTILITY -> TRAP), apply immediately
  // If removing distraction tag (TRAP -> UTILITY), apply 12-hour cooling-off delay
  const requiresCoolingOff = existing && existing.category === 'TRAP' && newCategory === 'UTILITY';

  if (!requiresCoolingOff) {
    // Immediate apply
    if (existing) {
      const { data: updated, error } = await supabase
        .from('user_app_configs')
        .update({
          category: newCategory,
          intent_gate_enabled: isDistracting,
          pending_category: null,
          pending_change_effective_at: null,
        })
        .eq('id', existing.id)
        .select()
        .single();

      if (error) throw error;
      return {
        status: 'APPLIED',
        message: `App ${appIdentifier} updated to ${newCategory} immediately.`,
        config: updated,
      };
    } else {
      const { data: inserted, error } = await supabase
        .from('user_app_configs')
        .insert({
          user_id: userId,
          app_identifier: appIdentifier,
          category: newCategory,
          intent_gate_enabled: isDistracting,
        })
        .select()
        .single();

      if (error) throw error;
      return {
        status: 'APPLIED',
        message: `App ${appIdentifier} updated to ${newCategory} immediately.`,
        config: inserted,
      };
    }
  } else {
    // 12-hour cooling off period
    const effectiveAtDate = new Date(Date.now() + 12 * 60 * 60 * 1000);
    const effectiveAtIso = effectiveAtDate.toISOString();

    const { data: updated, error } = await supabase
      .from('user_app_configs')
      .update({
        pending_category: newCategory,
        pending_change_effective_at: effectiveAtIso,
      })
      .eq('id', existing.id)
      .select()
      .single();

    if (error) throw error;
    return {
      status: 'PENDING',
      message: `Changing ${appIdentifier} to UTILITY will take effect in 12 hours (cooling-off period).`,
      effectiveAt: effectiveAtIso,
      config: updated,
    };
  }
}
