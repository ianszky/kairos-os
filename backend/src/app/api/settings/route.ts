import { NextRequest, NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { getUserSettings, updateDailyLeisureTime } from '@/lib/db/user-settings';
import { getTodayUsageMinutes } from '@/lib/db/intent-logs';

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

    const settings = await getUserSettings(supabase, user.id);
    const todayUsed = await getTodayUsageMinutes(supabase, user.id);
    const remainingMinutes = Math.max(0, settings.daily_leisure_minutes - todayUsed);

    return NextResponse.json({
      dailyLeisureMinutes: settings.daily_leisure_minutes,
      pendingLeisureMinutes: settings.daily_leisure_minutes_pending,
      pendingChangeEffectiveAt: settings.pending_change_effective_at,
      todayUsedMinutes: todayUsed,
      remainingLeisureMinutes: remainingMinutes,
    });
  } catch (error: any) {
    console.error('[API/settings] GET Error:', error);
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
    const { dailyLeisureMinutes } = body;

    if (typeof dailyLeisureMinutes !== 'number' || dailyLeisureMinutes < 0) {
      return NextResponse.json({ error: 'Invalid dailyLeisureMinutes value' }, { status: 400 });
    }

    const result = await updateDailyLeisureTime(supabase, user.id, dailyLeisureMinutes);

    return NextResponse.json({
      status: result.status,
      message: result.message,
      effectiveAt: result.effectiveAt,
      settings: {
        dailyLeisureMinutes: result.settings.daily_leisure_minutes,
        pendingLeisureMinutes: result.settings.daily_leisure_minutes_pending,
        pendingChangeEffectiveAt: result.settings.pending_change_effective_at,
      }
    });
  } catch (error: any) {
    console.error('[API/settings] PUT Error:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
