import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  isDemoIntegrationsEnabled,
  matchDemoIntegration,
  getDemoMockPayload,
  getDemoDelayMs,
  simulateDemoDelay,
} from './integration-mocks';

describe('integration-mocks', () => {
  const originalEnv = process.env.KAIROS_DEMO_INTEGRATIONS;

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    if (originalEnv === undefined) {
      delete process.env.KAIROS_DEMO_INTEGRATIONS;
    } else {
      process.env.KAIROS_DEMO_INTEGRATIONS = originalEnv;
    }
    vi.useRealTimers();
  });

  describe('isDemoIntegrationsEnabled', () => {
    it('returns true when env is set to true', () => {
      process.env.KAIROS_DEMO_INTEGRATIONS = 'true';
      expect(isDemoIntegrationsEnabled()).toBe(true);
    });

    it('returns false when env is unset or false', () => {
      delete process.env.KAIROS_DEMO_INTEGRATIONS;
      expect(isDemoIntegrationsEnabled()).toBe(false);

      process.env.KAIROS_DEMO_INTEGRATIONS = 'false';
      expect(isDemoIntegrationsEnabled()).toBe(false);
    });
  });

  describe('matchDemoIntegration', () => {
    it('detects slack, github, and notion tags case-insensitively', () => {
      expect(matchDemoIntegration('@slack summarize unread highlights')).toBe('slack');
      expect(matchDemoIntegration('@GITHUB show PRs')).toBe('github');
      expect(matchDemoIntegration('@Notion find Helios launch checklist')).toBe('notion');
    });

    it('returns null for unrelated prompts', () => {
      expect(matchDemoIntegration('@gmail show my important emails')).toBeNull();
      expect(matchDemoIntegration('set an alarm for 7am')).toBeNull();
    });
  });

  describe('getDemoMockPayload', () => {
    it('uses fictional Helios branding and no Kairos references', () => {
      for (const tag of ['slack', 'github', 'notion'] as const) {
        const payload = getDemoMockPayload(tag);
        const serialized = JSON.stringify(payload).toLowerCase();
        expect(serialized).not.toContain('kairos');
        expect(serialized).not.toContain('kairos-os');
      }

      expect(getDemoMockPayload('notion').widget.items[0].primary).toBe('Helios Launch Checklist');
      expect(getDemoMockPayload('github').widget.items[0].secondary).toContain('helios-mobile');
    });
  });

  describe('simulateDemoDelay', () => {
    it('waits for the configured delay per integration', async () => {
      const slackPromise = simulateDemoDelay('slack');
      await vi.advanceTimersByTimeAsync(getDemoDelayMs('slack') - 1);
      let settled = false;
      slackPromise.then(() => {
        settled = true;
      });
      await Promise.resolve();
      expect(settled).toBe(false);

      await vi.advanceTimersByTimeAsync(1);
      await slackPromise;
    });
  });
});
