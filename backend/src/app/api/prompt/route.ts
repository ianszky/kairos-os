import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';
import { processIntent } from '@/lib/router/intent-router';

export async function POST(request: Request) {
  try {
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

    const responsePayload = await processIntent(prompt, appTarget);
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
