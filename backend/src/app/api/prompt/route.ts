import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';
import { processIntent } from '@/lib/router/intent-router';
import { createClient } from '@/lib/supabase/server';

export async function POST(request: Request) {
  try {
    const supabase = await createClient();
    const authHeader = request.headers.get('Authorization');
    const token = authHeader?.replace('Bearer ', '');
    const { data: { user } } = token 
      ? await supabase.auth.getUser(token) 
      : await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({
        type: 'ERROR',
        text: 'Unauthorized. Please sign in again.',
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse, { status: 401 });
    }

    const body = await request.json();
    const prompt = typeof body.intent === 'string' ? body.intent : '';
    const appTarget = typeof body.appTarget === 'string' ? body.appTarget : null;

    if (!prompt) {
      return NextResponse.json({
        type: 'ERROR',
        text: 'Prompt is required',
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse, { status: 400 });
    }

    // Pre-flight connection check for Composio
    const { getConnectionStatus } = await import('@/lib/mcp/connection-manager');
    const connectionStatus = await getConnectionStatus(user.id);
    
    if (!connectionStatus.connected) {
      const { initiateConnection } = await import('@/lib/mcp/connection-manager');
      const connectData = await initiateConnection(user.id);

      return NextResponse.json({
        type: 'WIDGET',
        text: 'Please connect your Google account to use this feature.',
        widget: {
          widgetType: 'GENERIC_CARD',
          title: 'Connection Required',
          items: [
            { id: 'auth_msg', primary: 'KAIROS OS needs access to your Google account (Gmail, Calendar, etc.) to perform this action.' }
          ],
          actions: [
            { label: 'Connect Google', actionType: 'DEEP_LINK', target: connectData.connectUrl }
          ]
        },
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse, { status: 403 });
    }

    // Bridge: pass the Supabase user ID into the intent processing logic
    // where Composio will use it to create a session
    const responsePayload = await processIntent(prompt, appTarget, user.id);
    return NextResponse.json(responsePayload);

  } catch (error: unknown) {
    console.error("API Route Error:", error);
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    const errorResponse: KairosResponse = {
      type: 'ERROR',
      text: `Failed to process request: ${errorMessage}`,
      meta: {
        conversationId: 'mock-session-123',
        timestamp: new Date().toISOString(),
        model: 'system'
      }
    };
    return NextResponse.json(errorResponse, { status: 500 });
  }
}
