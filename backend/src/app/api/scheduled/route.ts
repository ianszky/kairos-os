import { NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import {
  createScheduledTask,
  getScheduledTasks,
  updateScheduledTask,
  deleteScheduledTask,
} from '@/lib/db/scheduled-tasks';

async function getAuthenticatedUser(request: Request) {
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

  return { supabase, user, token };
}

export async function GET(request: Request) {
  try {
    const { supabase, user } = await getAuthenticatedUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const tasks = await getScheduledTasks(supabase, user.id);
    return NextResponse.json(tasks);
  } catch (error: any) {
    console.error('[API/Scheduled] GET error:', error);
    return NextResponse.json({ error: error.message || 'Failed to fetch scheduled tasks' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const { supabase, user } = await getAuthenticatedUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { prompt, appTarget, frequency, daysOfWeek, timeOfDay, timezone, startsAt } = body;

    if (!prompt || !timeOfDay || !frequency) {
      return NextResponse.json(
        { error: 'Prompt, timeOfDay, and frequency are required' },
        { status: 400 }
      )
    }

    // Auto-generate title using Gemini 3.1 Flash-Lite (async fire-and-forget title enrichment if prompt is long)
    let generatedTitle = prompt.slice(0, 40);
    try {
      const { ai } = await import('@/lib/ai/gemini-client');
      const res = await ai.models.generateContent({
        model: 'gemini-3.1-flash-lite',
        contents: `Generate a short, 3 to 5 words title for a scheduled automated prompt: "${prompt}". Respond with ONLY the title and nothing else.`,
      });
      if (res.text) {
        generatedTitle = res.text.trim().replace(/["']/g, '');
      }
    } catch (err) {
      console.warn('[API/Scheduled] Title generation fallback:', err);
    }

    const task = await createScheduledTask(supabase, user.id, {
      prompt,
      appTarget,
      title: generatedTitle,
      frequency,
      daysOfWeek: Array.isArray(daysOfWeek) ? daysOfWeek : [],
      timeOfDay,
      timezone,
      startsAt,
    });

    return NextResponse.json(task, { status: 201 });
  } catch (error: any) {
    console.error('[API/Scheduled] POST error:', error);
    return NextResponse.json({ error: error.message || 'Failed to create scheduled task' }, { status: 500 });
  }
}

export async function PUT(request: Request) {
  try {
    const { supabase, user } = await getAuthenticatedUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { id, prompt, appTarget, title, frequency, daysOfWeek, timeOfDay, isActive } = body;

    if (!id) {
      return NextResponse.json({ error: 'Task ID is required' }, { status: 400 });
    }

    const updated = await updateScheduledTask(supabase, user.id, id, {
      prompt,
      appTarget,
      title,
      frequency,
      daysOfWeek,
      timeOfDay,
      isActive,
    });

    return NextResponse.json(updated);
  } catch (error: any) {
    console.error('[API/Scheduled] PUT error:', error);
    return NextResponse.json({ error: error.message || 'Failed to update scheduled task' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    const { supabase, user } = await getAuthenticatedUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const url = new URL(request.url);
    const taskId = url.searchParams.get('id');

    if (!taskId) {
      return NextResponse.json({ error: 'Task ID is required' }, { status: 400 });
    }

    await deleteScheduledTask(supabase, user.id, taskId);
    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('[API/Scheduled] DELETE error:', error);
    return NextResponse.json({ error: error.message || 'Failed to delete scheduled task' }, { status: 500 });
  }
}
