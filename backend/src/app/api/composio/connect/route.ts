import { NextResponse } from 'next/server';
import { initiateConnection } from '@/lib/mcp/connection-manager';
import { createClient } from '@/lib/supabase/server';

export async function GET(request: Request) {
  try {
    const supabase = await createClient();
    const authHeader = request.headers.get('Authorization');
    const token = authHeader?.replace('Bearer ', '');
    const { data: { user } } = token 
      ? await supabase.auth.getUser(token) 
      : await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const result = await initiateConnection(user.id);
    return NextResponse.json(result);
  } catch (error) {
    console.error('Error in /api/composio/connect:', error);
    return NextResponse.json({ error: 'Failed to generate connect URL' }, { status: 500 });
  }
}
