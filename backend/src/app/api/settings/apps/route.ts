import { NextRequest, NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { getUserAppConfigs, toggleAppClassification } from '@/lib/db/app-configs';

async function getAuthUser(request: NextRequest) {
  const authHeader = request.headers.get('Authorization');
  const token = authHeader?.replace('Bearer ', '');

  const supabase = token
    ? createSupabaseClient(
        process.env.NEXT_PUBLIC_SUPABASE_URL!,
        process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
        { global: { headers: { Authorization: `Bearer ${token}` } } }
      )
    : await createServerClient();

  const { data: { user } } = token
    ? await supabase.auth.getUser(token)
    : await supabase.auth.getUser();

  return { supabase, user };
}

export async function GET(request: NextRequest) {
  try {
    const { supabase, user } = await getAuthUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const configs = await getUserAppConfigs(supabase, user.id);
    const formatted = configs.map(c => ({
      appIdentifier: c.app_identifier,
      category: c.category,
      pendingCategory: c.pending_category,
      pendingChangeEffectiveAt: c.pending_change_effective_at,
      intentGateEnabled: c.intent_gate_enabled,
    }));

    return NextResponse.json({ configs: formatted });
  } catch (error: any) {
    console.error('[API/settings/apps] GET Error:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}

export async function PUT(request: NextRequest) {
  try {
    const { supabase, user } = await getAuthUser(request);
    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { appId, appIdentifier, isDistracting } = body;
    const targetAppId = appId || appIdentifier;

    if (!targetAppId || typeof isDistracting !== 'boolean') {
      return NextResponse.json({ error: 'Invalid appIdentifier or isDistracting boolean' }, { status: 400 });
    }

    const result = await toggleAppClassification(supabase, user.id, targetAppId, isDistracting);

    return NextResponse.json({
      status: result.status,
      message: result.message,
      effectiveAt: result.effectiveAt,
      config: {
        appIdentifier: result.config.app_identifier,
        category: result.config.category,
        pendingCategory: result.config.pending_category,
        pendingChangeEffectiveAt: result.config.pending_change_effective_at,
        intentGateEnabled: result.config.intent_gate_enabled,
      }
    });
  } catch (error: any) {
    console.error('[API/settings/apps] PUT Error:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
