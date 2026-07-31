import { describe, it, expect, vi, beforeEach } from 'vitest';
import { processIntent } from './intent-router';
import { classifyIntent } from '../ai/intent-classifier';
import { executeComplexIntent } from '../mcp/tool-executor';
import { buildResponse } from '../response/response-builder';
import { getConversationContext } from '../ai/context-manager';
import { getUserMemory, updateUserMemoryAsync } from '../ai/user-memory';

vi.mock('../ai/intent-classifier');
vi.mock('../mcp/tool-executor');
vi.mock('../response/response-builder');
vi.mock('../ai/context-manager');
vi.mock('../ai/user-memory');
vi.mock('../mcp/connection-manager', () => ({
  getConnectionStatus: vi.fn().mockResolvedValue({ connected: true }),
  initiateConnection: vi.fn(),
  mapAppTargetToToolkitSlug: (target: string) => target,
}));

describe('processIntent tier routing', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getConversationContext).mockResolvedValue([]);
    vi.mocked(getUserMemory).mockResolvedValue(null);
    vi.mocked(updateUserMemoryAsync).mockResolvedValue(undefined);
    vi.mocked(buildResponse).mockResolvedValue({
      type: 'RESPONSE',
      text: 'Built response',
      meta: {
        conversationId: 'conv-1',
        timestamp: new Date().toISOString(),
        model: 'gemini',
      },
    });
  });

  it('forces COMPLEX routing for explicit browser target even when classifier returns SIMPLE', async () => {
    vi.mocked(classifyIntent).mockResolvedValue({
      tier: 'SIMPLE',
      appTarget: 'generic',
      taskType: 'search',
      inferredDetails: '',
      reason: 'Looks like a simple question',
    });
    vi.mocked(executeComplexIntent).mockResolvedValue('Search results for chicken breast recipes');

    await processIntent(
      '@browser some chicken breast recipes',
      'browser',
      'user-1',
      'conv-1',
      'token-1'
    );

    expect(executeComplexIntent).toHaveBeenCalledWith(
      '@browser some chicken breast recipes',
      expect.arrayContaining(['browser']),
      'user-1',
      [],
      null,
      'search',
      '',
      []
    );
  });

  it('keeps SIMPLE routing when no explicit integration target is provided', async () => {
    vi.mocked(classifyIntent).mockResolvedValue({
      tier: 'SIMPLE',
      appTarget: 'generic',
      taskType: 'general',
      inferredDetails: '',
      reason: 'Greeting',
    });

    await processIntent('hello there', null, 'user-1', 'conv-1', 'token-1');

    expect(executeComplexIntent).not.toHaveBeenCalled();
    expect(buildResponse).toHaveBeenCalledWith(
      'hello there',
      expect.stringContaining('Intent was classified as simple'),
      'generic',
      'conv-1',
      'token-1',
      [],
      null
    );
  });
});
