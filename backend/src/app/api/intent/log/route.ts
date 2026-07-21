import { NextRequest, NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { logIntent, getTodayUsageMinutes } from '@/lib/db/intent-logs';
import { getUserSettings } from '@/lib/db/user-settings';

export async function POST(request: NextRequest) {
  try {
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

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { appIdentifier, appDisplayName, reason, timeLimitMinutes, aiApproved } = body;

    if (!appIdentifier || !reason || typeof timeLimitMinutes !== 'number') {
      return NextResponse.json({ error: 'Invalid parameters' }, { status: 400 });
    }

    // Check user settings and today's usage
    const settings = await getUserSettings(supabase, user.id);
    const todayUsed = await getTodayUsageMinutes(supabase, user.id);
    const dailyLimit = settings.daily_leisure_minutes;

    if (todayUsed + timeLimitMinutes > dailyLimit) {
      const remaining = Math.max(0, dailyLimit - todayUsed);
      return NextResponse.json({
        logged: false,
        budgetExceeded: true,
        remainingMinutes: remaining,
        message: `Daily leisure budget exceeded. You have ${remaining} minute(s) remaining today.`
      });
    }

    const loggedRecord = await logIntent(supabase, user.id, {
      appIdentifier,
      appDisplayName,
      reason,
      timeLimitMinutes,
      aiApproved: aiApproved ?? true,
    });

    const newRemaining = Math.max(0, dailyLimit - (todayUsed + timeLimitMinutes));

    return NextResponse.json({
      logged: true,
      budgetExceeded: false,
      remainingMinutes: newRemaining,
      intentLogId: loggedRecord.id,
    });
  } catch (error: any) {
    console.error('[API/intent/log] Error:', error);
    return NextResponse.json({ error: error.message || 'Internal server error' }, { status: 500 });
  }
}
