import { POST } from './route';
import { NextRequest } from 'next/server';
import { describe, it, expect, vi } from 'vitest';

// Mock cookies and supabase client
vi.mock('next/headers', () => ({
  cookies: () => ({
    getAll: () => [],
    setAll: () => {},
  }),
}));

vi.mock('@/lib/supabase/server', () => ({
  createClient: () => ({
    auth: {
      getUser: () => ({ data: { user: { id: 'test-user-id' } }, error: null }),
    },
    from: (table: string) => ({
      insert: () => ({
        select: () => ({
          single: () => ({ data: { id: 'test-conversation-id' }, error: null }),
        }),
      }),
      update: () => ({
        eq: () => ({ data: {}, error: null }),
      }),
    }),
  }),
}));

// Mock processIntent
vi.mock('@/lib/router/intent-router', () => ({
  processIntent: vi.fn().mockImplementation(async (prompt, appTarget) => {
    if (prompt.toLowerCase().includes('alarm')) {
      return {
        type: 'RESPONSE',
        text: 'Alarm set successfully',
        widget: {
          widgetType: 'ALARM_CONFIRM',
          items: [{ id: 'alarm_1', primary: '7:00 AM' }],
        },
        meta: {
          conversationId: 'test-conversation-id',
          timestamp: new Date().toISOString(),
          model: 'gemini-3.5-flash',
        },
      };
    }
    return {
      type: 'RESPONSE',
      text: 'Other task completed',
      widget: {
        widgetType: 'GENERIC_CARD',
        items: [{ id: 'generic_1', primary: prompt.toLowerCase() }],
      },
      meta: {
        conversationId: 'test-conversation-id',
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash',
      },
    };
  }),
}));

describe('POST /api/prompt', () => {
  const createRequest = (body: unknown) => {
    return new NextRequest('http://localhost/api/prompt', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  };

  it('should return error if no prompt/intent provided', async () => {
    const req = createRequest({});
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(400);
    expect(json.type).toBe('ERROR');
    expect(json.text).toBe('Prompt is required');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return ALARM_CONFIRM widget for alarm intent', async () => {
    const req = createRequest({ intent: 'Set an alarm for 7 AM' });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(200);
    expect(json.type).toBe('RESPONSE');
    expect(json.widget.widgetType).toBe('ALARM_CONFIRM');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return GENERIC_CARD widget for other intents', async () => {
    const req = createRequest({ intent: 'Turn on the lights' });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(200);
    expect(json.type).toBe('RESPONSE');
    expect(json.widget.widgetType).toBe('GENERIC_CARD');
    expect(json.widget.items[0].primary).toBe('turn on the lights');
    expect(json.meta).toHaveProperty('timestamp');
  });

  it('should return 500 for invalid JSON body', async () => {
    const req = new NextRequest('http://localhost/api/prompt', {
      method: 'POST',
      body: 'invalid-json',
    });
    const res = await POST(req);
    const json = await res.json();
    
    expect(res.status).toBe(500);
    expect(json.type).toBe('ERROR');
    expect(json.text).toContain('Failed to process request');
  });
});
