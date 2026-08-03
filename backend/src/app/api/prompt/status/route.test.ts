import { GET } from './route';
import { NextRequest } from 'next/server';
import { describe, it, expect, vi } from 'vitest';

vi.mock('@/lib/supabase/server', () => ({
  createClient: () => ({
    auth: {
      getUser: () => ({ data: { user: { id: 'test-user-id' } }, error: null }),
    },
  }),
}));

vi.mock('@/lib/db/prompt-runs', () => ({
  getPromptRun: vi.fn(),
}));

import { getPromptRun } from '@/lib/db/prompt-runs';

describe('GET /api/prompt/status', () => {
  it('returns 400 when runId is missing', async () => {
    const req = new NextRequest('http://localhost/api/prompt/status');
    const res = await GET(req);
    expect(res.status).toBe(400);
  });

  it('returns 404 when run is not found', async () => {
    vi.mocked(getPromptRun).mockResolvedValue(null);
    const req = new NextRequest('http://localhost/api/prompt/status?runId=missing');
    const res = await GET(req);
    expect(res.status).toBe(404);
  });

  it('returns completed response payload', async () => {
    vi.mocked(getPromptRun).mockResolvedValue({
      id: 'run-1',
      user_id: 'test-user-id',
      conversation_id: 'conv-1',
      user_message_id: 'msg-1',
      status: 'completed',
      error_message: null,
      response_payload: {
        type: 'RESPONSE',
        text: 'All done',
        meta: {
          conversationId: 'conv-1',
          timestamp: new Date().toISOString(),
          model: 'gemini-3.5-flash',
        },
      },
      started_at: new Date().toISOString(),
      completed_at: new Date().toISOString(),
    });

    const req = new NextRequest('http://localhost/api/prompt/status?runId=run-1');
    const res = await GET(req);
    const json = await res.json();

    expect(res.status).toBe(200);
    expect(json.status).toBe('completed');
    expect(json.response.text).toBe('All done');
  });

  it('returns failed status with error message', async () => {
    vi.mocked(getPromptRun).mockResolvedValue({
      id: 'run-2',
      user_id: 'test-user-id',
      conversation_id: 'conv-1',
      user_message_id: 'msg-1',
      status: 'failed',
      error_message: 'Tool execution failed',
      response_payload: null,
      started_at: new Date().toISOString(),
      completed_at: new Date().toISOString(),
    });

    const req = new NextRequest('http://localhost/api/prompt/status?runId=run-2');
    const res = await GET(req);
    const json = await res.json();

    expect(res.status).toBe(200);
    expect(json.status).toBe('failed');
    expect(json.error).toBe('Tool execution failed');
  });
});
