import { SupabaseClient } from '@supabase/supabase-js';
import { Database } from '@/types/database.types';

export async function getAppConfig(supabase: SupabaseClient<any>, appTarget: string) {
  const { data, error } = await supabase
    .from('app_configs')
    .select('*')
    .eq('app_target', appTarget)
    .single();

  if (error) throw error;
  return data;
}
