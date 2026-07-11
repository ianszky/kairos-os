import { SupabaseClient } from '@supabase/supabase-js';
import { Database } from '@/types/database.types';

export async function getUser(supabase: SupabaseClient<any>, userId: string) {
  const { data, error } = await supabase
    .from('users')
    .select('*')
    .eq('id', userId)
    .single();

  if (error) throw error;
  return data;
}

export async function updateUser(supabase: SupabaseClient<any>, userId: string, updates: any) {
  const { data, error } = await supabase
    .from('users')
    .update(updates)
    .eq('id', userId)
    .select()
    .single();

  if (error) throw error;
  return data;
}
