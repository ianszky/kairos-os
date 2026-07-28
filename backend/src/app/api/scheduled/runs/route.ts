import { NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { getScheduledTaskRuns } from '@/lib/db/scheduled-tasks';

export async function GET(request: Request) {
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
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const url = new URL(request.url);
    const taskId = url.searchParams.get('taskId') || undefined;
    const limit = parseInt(url.searchParams.get('limit') || '50', 10);

    const runs = await getScheduledTaskRuns(supabase, user.id, taskId, limit);
    return NextResponse.json(runs);
  } catch (error: any) {
    console.error('[API/Scheduled/Runs] GET error:', error);
    return NextResponse.json(
      { error: error.message || 'Failed to fetch scheduled task runs' },
      { status: 500 }
    );
  }
}
