import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const intent = typeof body.intent === 'string' ? body.intent.toLowerCase() : '';

    let responsePayload: KairosResponse;

    if (intent.includes('alarm')) {
      responsePayload = {
        type: 'WIDGET',
        widget: {
          widgetType: 'ALARM_CONFIRM',
          title: 'Open Clock',
          items: [
            {
              id: 'clock_1',
              primary: 'Opening the clock app',
              secondary: 'Set your alarm',
              icon: 'alarm'
            }
          ],
          actions: [
            {
              label: 'Open Clock',
              actionType: 'DEEP_LINK',
              target: 'clock://open'
            }
          ]
        },
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'mock-router'
        }
      };
    } else {
      responsePayload = {
        type: 'WIDGET',
        widget: {
          widgetType: 'GENERIC_CARD',
          title: 'Text Widget',
          items: [
            {
              id: 'text_1',
              primary: 'Generic text widget',
              secondary: `You said: ${body.intent || 'nothing'}`,
            }
          ]
        },
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'mock-router'
        }
      };
    }

    return NextResponse.json(responsePayload);
  } catch (error) {
    const errorResponse: KairosResponse = {
      type: 'ERROR',
      text: 'Failed to process request',
      meta: {
        conversationId: 'mock-session-123',
        timestamp: new Date().toISOString(),
        model: 'mock-router'
      }
    };
    return NextResponse.json(errorResponse, { status: 400 });
  }
}
