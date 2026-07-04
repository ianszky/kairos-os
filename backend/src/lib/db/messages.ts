import { SupabaseClient } from '@supabase/supabase-js';
import { Database } from '@/types/database.types';

export async function addMessage(
  supabase: SupabaseClient<any>,
  conversationId: string,
  userId: string,
  role: 'user' | 'assistant',
  content: string
) {
  const { data, error } = await supabase
    .from('messages')
    .insert({
      conversation_id: conversationId,
      user_id: userId,
      role,
      content,
    })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getMessages(supabase: SupabaseClient<any>, conversationId: string) {
  const { data, error } = await supabase
    .from('messages')
    .select('*')
    .eq('conversation_id', conversationId)
    .order('created_at', { ascending: true });

  if (error) throw error;
  return data;
}
