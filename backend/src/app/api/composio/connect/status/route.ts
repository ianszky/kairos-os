import { NextResponse } from 'next/server';
import { getConnectionStatus } from '@/lib/mcp/connection-manager';
import { createClient } from '@/lib/supabase/server';

export async function GET(request: Request) {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const result = await getConnectionStatus(user.id);
    return NextResponse.json(result);
  } catch (error) {
    console.error('Error in /api/composio/connect/status:', error);
    return NextResponse.json({ error: 'Failed to get connection status' }, { status: 500 });
  }
}
