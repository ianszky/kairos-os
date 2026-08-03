import { POST } from './route';
import { NextRequest } from 'next/server';
import { describe, it, expect, vi, beforeEach } from 'vitest';

const processIntentMock = vi.fn();
const afterTasks: Array<() => Promise<void>> = [];

vi.mock('next/headers', () => ({
  cookies: () => ({
    getAll: () => [],
    setAll: () => {},
  }),
}));

vi.mock('next/server', async (importOriginal) => {
  const actual = await importOriginal<typeof import('next/server')>();
  return {
    ...actual,
    after: (task: () => Promise<void>) => {
      afterTasks.push(task);
    },
  };
});

vi.mock('@/lib/db/prompt-runs', () => ({
  createPromptRun: vi.fn().mockResolvedValue({ id: 'test-run-id' }),
  updatePromptRun: vi.fn().mockResolvedValue({ id: 'test-run-id' }),
}));

vi.mock('@/lib/supabase/server', () => ({
  createClient: () => ({
    auth: {
      getUser: () => ({ data: { user: { id: 'test-user-id' } }, error: null }),
    },
    from: () => ({
      insert: () => ({
        select: () => ({
          single: () => ({
            data: { id: 'test-id' },
            error: null,
          }),
        }),
      }),
      update: () => ({
        eq: () => ({ data: {}, error: null }),
      }),
    }),
  }),
}));

vi.mock('@/lib/router/intent-router', () => ({
  processIntent: (...args: unknown[]) => processIntentMock(...args),
}));

describe('POST /api/prompt', () => {
  beforeEach(() => {
    afterTasks.length = 0;
    processIntentMock.mockReset();
    processIntentMock.mockResolvedValue({
      type: 'RESPONSE',
      text: 'Task completed',
      meta: {
        conversationId: 'test-conversation-id',
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash',
      },
    });
  });

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
  });

  it('should return 202 ACCEPTED immediately without awaiting processIntent', async () => {
    const req = createRequest({ intent: 'Update my calendar' });
    const res = await POST(req);
    const json = await res.json();

    expect(res.status).toBe(202);
    expect(json.type).toBe('ACCEPTED');
    expect(json.meta.runId).toBe('test-run-id');
    expect(json.meta.status).toBe('running');
    expect(processIntentMock).not.toHaveBeenCalled();

    expect(afterTasks).toHaveLength(1);
    await afterTasks[0]();
    expect(processIntentMock).toHaveBeenCalledTimes(1);
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
