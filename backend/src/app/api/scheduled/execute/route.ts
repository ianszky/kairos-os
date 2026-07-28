import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';
import { processIntent } from '@/lib/router/intent-router';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { createScheduledTaskRun, updateScheduledTaskRun } from '@/lib/db/scheduled-tasks';

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get('Authorization');
    const token = authHeader?.replace('Bearer ', '');

    const supabase = token
      ? createSupabaseClient(
          process.env.NEXT_PUBLIC_SUPABASE_URL!,
          process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
          {
            global: {
              headers: { Authorization: `Bearer ${token}` },
            },
          }
        )
      : await createServerClient();

    const {
      data: { user },
    } = token ? await supabase.auth.getUser(token) : await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json(
        {
          type: 'ERROR',
          text: 'Unauthorized',
        } as KairosResponse,
        { status: 401 }
      );
    }

    const body = await request.json();
    const taskId = body.taskId;

    if (!taskId) {
      return NextResponse.json({ error: 'taskId is required' }, { status: 400 });
    }

    // 1. Fetch task details
    const { data: task, error: taskError } = await supabase
      .from('scheduled_tasks')
      .select('*')
      .eq('id', taskId)
      .eq('user_id', user.id)
      .single();

    if (taskError || !task) {
      return NextResponse.json({ error: 'Scheduled task not found' }, { status: 404 });
    }

    // 2. Create execution run entry
    const runRecord = await createScheduledTaskRun(supabase, task.id, null, 'running');

    // 3. Create a dedicated conversation tagged with source = 'scheduled'
    const { data: convData, error: convErr } = await supabase
      .from('conversations')
      .insert({
        user_id: user.id,
        title: task.title ? `🔄 ${task.title}` : `🔄 ${task.prompt.slice(0, 30)}`,
        source: 'scheduled',
        scheduled_task_id: task.id,
      })
      .select('id')
      .single();

    if (convErr || !convData) {
      await updateScheduledTaskRun(supabase, runRecord.id, 'failed', convErr?.message || 'Failed to create conversation');
      throw new Error('Failed to create scheduled conversation: ' + (convErr?.message || 'Unknown error'));
    }

    const conversationId = convData.id;

    // 4. Insert initial user prompt message into conversation
    const { data: msgData, error: msgErr } = await supabase
      .from('messages')
      .insert({
        conversation_id: conversationId,
        user_id: user.id,
        role: 'user',
        content: task.prompt,
        app_target: task.app_target,
      })
      .select('id')
      .single();

    if (msgErr || !msgData) {
      await updateScheduledTaskRun(supabase, runRecord.id, 'failed', msgErr?.message || 'Failed to insert message', conversationId);
      throw new Error('Failed to insert scheduled task message: ' + (msgErr?.message || 'Unknown error'));
    }

    // 5. Execute prompt through intent router
    try {
      const responsePayload = await processIntent(
        task.prompt,
        task.app_target,
        user.id,
        conversationId,
        token || '',
        []
      );

      // Update run record to completed
      await updateScheduledTaskRun(supabase, runRecord.id, 'completed', undefined, conversationId);

      return NextResponse.json({
        ...responsePayload,
        meta: {
          ...responsePayload.meta,
          conversationId,
          taskId: task.id,
          runId: runRecord.id,
        },
      });
    } catch (execError: any) {
      console.error('[API/Scheduled/Execute] processIntent error:', execError);
      await updateScheduledTaskRun(
        supabase,
        runRecord.id,
        'failed',
        execError.message || 'Execution error',
        conversationId
      );
      return NextResponse.json({
        type: 'ERROR',
        text: `Execution failed: ${execError.message || 'Unknown error'}`,
        meta: {
          conversationId,
          timestamp: new Date().toISOString(),
          model: 'system',
        },
      } as KairosResponse, { status: 500 });
    }
  } catch (error: any) {
    console.error('[API/Scheduled/Execute] Global error:', error);
    return NextResponse.json(
      {
        type: 'ERROR',
        text: `Failed to process scheduled task: ${error.message || 'Unknown error'}`,
      } as KairosResponse,
      { status: 500 }
    );
  }
}
