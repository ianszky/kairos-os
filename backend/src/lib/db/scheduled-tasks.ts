import { SupabaseClient } from '@supabase/supabase-js';

export function buildCronExpression(
  frequency: 'daily' | 'weekly' | 'specific_days',
  timeOfDay: string, // "HH:mm" or "HH:mm:ss"
  daysOfWeek: number[] = [] // 0=Sun, 1=Mon, ..., 6=Sat
): string {
  const [hourStr, minuteStr] = timeOfDay.split(':');
  const hour = parseInt(hourStr || '0', 10);
  const minute = parseInt(minuteStr || '0', 10);

  if (frequency === 'daily') {
    return `${minute} ${hour} * * *`;
  }

  if (frequency === 'weekly' || frequency === 'specific_days') {
    const daysStr = daysOfWeek.length > 0 ? daysOfWeek.sort().join(',') : '*';
    return `${minute} ${hour} * * ${daysStr}`;
  }

  return `${minute} ${hour} * * *`;
}

export async function createScheduledTask(
  supabase: SupabaseClient<any>,
  userId: string,
  data: {
    prompt: string;
    appTarget?: string | null;
    title?: string | null;
    frequency: 'daily' | 'weekly' | 'specific_days';
    daysOfWeek?: number[];
    timeOfDay: string;
    timezone?: string;
    startsAt?: string;
  }
) {
  const cronExpr = buildCronExpression(data.frequency, data.timeOfDay, data.daysOfWeek);

  const { data: task, error } = await supabase
    .from('scheduled_tasks')
    .insert({
      user_id: userId,
      prompt: data.prompt,
      app_target: data.appTarget || null,
      title: data.title || data.prompt.slice(0, 40),
      frequency: data.frequency,
      days_of_week: data.daysOfWeek || [],
      time_of_day: data.timeOfDay,
      timezone: data.timezone || 'Asia/Manila',
      cron_expression: cronExpr,
      is_active: true,
      starts_at: data.startsAt || new Date().toISOString(),
    })
    .select()
    .single();

  if (error) throw error;
  return task;
}

export async function getScheduledTasks(supabase: SupabaseClient<any>, userId: string) {
  const { data, error } = await supabase
    .from('scheduled_tasks')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}

export async function updateScheduledTask(
  supabase: SupabaseClient<any>,
  userId: string,
  taskId: string,
  updates: Partial<{
    prompt: string;
    appTarget: string | null;
    title: string;
    frequency: 'daily' | 'weekly' | 'specific_days';
    daysOfWeek: number[];
    timeOfDay: string;
    isActive: boolean;
  }>
) {
  const dbUpdates: Record<string, any> = {
    updated_at: new Date().toISOString(),
  };

  if (updates.prompt !== undefined) dbUpdates.prompt = updates.prompt;
  if (updates.appTarget !== undefined) dbUpdates.app_target = updates.appTarget;
  if (updates.title !== undefined) dbUpdates.title = updates.title;
  if (updates.frequency !== undefined) dbUpdates.frequency = updates.frequency;
  if (updates.daysOfWeek !== undefined) dbUpdates.days_of_week = updates.daysOfWeek;
  if (updates.timeOfDay !== undefined) dbUpdates.time_of_day = updates.timeOfDay;
  if (updates.isActive !== undefined) dbUpdates.is_active = updates.isActive;

  if (updates.frequency || updates.timeOfDay || updates.daysOfWeek) {
    const existing = await supabase.from('scheduled_tasks').select('*').eq('id', taskId).single();
    if (existing.data) {
      const freq = updates.frequency || existing.data.frequency;
      const time = updates.timeOfDay || existing.data.time_of_day;
      const days = updates.daysOfWeek || existing.data.days_of_week;
      dbUpdates.cron_expression = buildCronExpression(freq, time, days);
    }
  }

  const { data, error } = await supabase
    .from('scheduled_tasks')
    .update(dbUpdates)
    .eq('id', taskId)
    .eq('user_id', userId)
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function deleteScheduledTask(
  supabase: SupabaseClient<any>,
  userId: string,
  taskId: string
) {
  const { error } = await supabase
    .from('scheduled_tasks')
    .delete()
    .eq('id', taskId)
    .eq('user_id', userId);

  if (error) throw error;
  return true;
}

export async function createScheduledTaskRun(
  supabase: SupabaseClient<any>,
  taskId: string,
  conversationId: string | null = null,
  status: 'pending' | 'running' | 'completed' | 'failed' = 'running'
) {
  const { data, error } = await supabase
    .from('scheduled_task_runs')
    .insert({
      task_id: taskId,
      conversation_id: conversationId,
      status: status,
      started_at: new Date().toISOString(),
    })
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function updateScheduledTaskRun(
  supabase: SupabaseClient<any>,
  runId: string,
  status: 'completed' | 'failed',
  errorMessage?: string,
  conversationId?: string
) {
  const updates: Record<string, any> = {
    status,
    completed_at: new Date().toISOString(),
  };
  if (errorMessage) updates.error_message = errorMessage;
  if (conversationId) updates.conversation_id = conversationId;

  const { data, error } = await supabase
    .from('scheduled_task_runs')
    .update(updates)
    .eq('id', runId)
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function getScheduledTaskRuns(
  supabase: SupabaseClient<any>,
  userId: string,
  taskId?: string,
  limit: number = 50
) {
  let query = supabase
    .from('scheduled_task_runs')
    .select('*, scheduled_tasks!inner(user_id, title, prompt), conversations(title)')
    .eq('scheduled_tasks.user_id', userId)
    .order('started_at', { ascending: false })
    .limit(limit);

  if (taskId) {
    query = query.eq('task_id', taskId);
  }

  const { data, error } = await query;
  if (error) throw error;
  return data;
}
