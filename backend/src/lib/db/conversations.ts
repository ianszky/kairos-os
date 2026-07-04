import { SupabaseClient } from '@supabase/supabase-js';
import { Database } from '@/types/database.types';

export async function createConversation(supabase: SupabaseClient<any>, userId: string) {
  const { data, error } = await supabase
    .from('conversations')
    .insert({ user_id: userId })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getConversations(supabase: SupabaseClient<any>, userId: string) {
  const { data, error } = await supabase
    .from('conversations')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}
