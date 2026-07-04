import { SupabaseClient } from '@supabase/supabase-js';
import { Database } from '@/types/database.types';

export async function createNotification(
  supabase: SupabaseClient<any>,
  userId: string,
  title: string,
  body: string
) {
  const { data, error } = await supabase
    .from('notifications')
    .insert({
      user_id: userId,
      title,
      body,
      is_read: false,
    })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getUnreadNotifications(supabase: SupabaseClient<any>, userId: string) {
  const { data, error } = await supabase
    .from('notifications')
    .select('*')
    .eq('user_id', userId)
    .eq('is_read', false)
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}

export async function markAsRead(supabase: SupabaseClient<any>, notificationId: string) {
  const { data, error } = await supabase
    .from('notifications')
    .update({ is_read: true })
    .eq('id', notificationId)
    .select()
    .single();

  if (error) throw error;
  return data;
}
