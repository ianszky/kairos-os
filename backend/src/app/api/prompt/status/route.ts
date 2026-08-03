import { NextResponse } from 'next/server';
import { PromptRunStatusResponse } from '@/types/kairos';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { getPromptRun } from '@/lib/db/prompt-runs';

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

    const { data: { user } } = token
      ? await supabase.auth.getUser(token)
      : await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const runId = searchParams.get('runId');

    if (!runId) {
      return NextResponse.json({ error: 'runId is required' }, { status: 400 });
    }

    const run = await getPromptRun(supabase, runId, user.id);
    if (!run) {
      return NextResponse.json({ error: 'Prompt run not found' }, { status: 404 });
    }

    const payload: PromptRunStatusResponse = {
      status: run.status,
      runId: run.id,
      conversationId: run.conversation_id,
    };

    if (run.status === 'completed' && run.response_payload) {
      payload.response = run.response_payload as PromptRunStatusResponse['response'];
    }

    if (run.status === 'failed') {
      payload.error = run.error_message || 'Prompt execution failed';
      if (run.response_payload) {
        payload.response = run.response_payload as PromptRunStatusResponse['response'];
      }
    }

    return NextResponse.json(payload);
  } catch (error: unknown) {
    console.error('[API/Prompt/Status] Error:', error);
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ error: errorMessage }, { status: 500 });
  }
}
