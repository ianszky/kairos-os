import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';
import { processIntent } from '@/lib/router/intent-router';
import { createClient } from '@/lib/supabase/server';

export async function POST(request: Request) {
  try {
    const supabase = await createClient();
    const { data: { user } } = await supabase.auth.getUser();

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
      return NextResponse.json({
        type: 'ERROR',
        text: 'Please connect your Google account first.',
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
