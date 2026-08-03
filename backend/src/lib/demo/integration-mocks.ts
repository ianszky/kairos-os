import { KairosResponse, WidgetPayload } from '@/types/kairos';
import { buildResponse } from '../response/response-builder';

export type DemoIntegrationTag = 'slack' | 'github' | 'notion';

const DEMO_DELAYS_MS: Record<DemoIntegrationTag, number> = {
  slack: 3200,
  github: 2800,
  notion: 3500,
};

const DEMO_MOCKS: Record<DemoIntegrationTag, { text: string; widget: WidgetPayload }> = {
  slack: {
    text: 'I reviewed 12 unread messages across #dev, #product, and 2 DMs. Three items need your attention today.',
    widget: {
      widgetType: 'GENERIC_CARD',
      title: 'Slack Digest — 12 unread',
      items: [
        { id: 's1', primary: '#dev', secondary: 'PR #42 merged; mobile dashboard polish in progress' },
        { id: 's2', primary: 'DM from Alex', secondary: 'Can you review the demo script before recording?' },
        { id: 's3', primary: '#product', secondary: 'Helios launch checklist moved to Notion — 2 blockers flagged' },
      ],
      actions: [{ label: 'Open Slack', actionType: 'DEEP_LINK', target: 'slack://' }],
    },
  },
  github: {
    text: 'You have 2 pull requests waiting for review and 1 CI check that failed on main.',
    widget: {
      widgetType: 'GENERIC_CARD',
      title: 'GitHub Updates',
      items: [
        { id: 'g1', primary: 'PR #87 — feat: webhook retry logic', secondary: 'helios-mobile · waiting on your review · +124 −18' },
        { id: 'g2', primary: 'PR #85 — fix: dashboard loading state', secondary: 'helios-mobile · approved · merge when ready' },
        { id: 'g3', primary: 'CI failed on main', secondary: 'backend lint — 1 error in api-client.test.ts' },
      ],
      actions: [{ label: 'Open GitHub', actionType: 'DEEP_LINK', target: 'github://' }],
    },
  },
  notion: {
    text: 'Found the Helios launch checklist and 2 related pages. Two items are still open.',
    widget: {
      widgetType: 'GENERIC_CARD',
      title: 'Notion Results',
      items: [
        { id: 'n1', primary: 'Helios Launch Checklist', secondary: '8/10 complete · Demo video, store assets pending' },
        { id: 'n2', primary: 'Integration onboarding flow', secondary: 'OAuth connect UX — draft' },
        { id: 'n3', primary: 'Blocker: Slack workspace auth', secondary: 'Waiting on enterprise admin approval' },
      ],
      actions: [{ label: 'Open Notion', actionType: 'DEEP_LINK', target: 'notion://' }],
    },
  },
};

export function isDemoIntegrationsEnabled(): boolean {
  return process.env.KAIROS_DEMO_INTEGRATIONS === 'true';
}

export function matchDemoIntegration(prompt: string): DemoIntegrationTag | null {
  const lower = prompt.toLowerCase();
  if (lower.includes('@slack')) return 'slack';
  if (lower.includes('@github')) return 'github';
  if (lower.includes('@notion')) return 'notion';
  return null;
}

export function getDemoMockPayload(tag: DemoIntegrationTag): { text: string; widget: WidgetPayload } {
  return DEMO_MOCKS[tag];
}

export function getDemoDelayMs(tag: DemoIntegrationTag): number {
  return DEMO_DELAYS_MS[tag];
}

export async function simulateDemoDelay(tag: DemoIntegrationTag): Promise<void> {
  const ms = DEMO_DELAYS_MS[tag];
  console.log(`[DemoIntegrations] Simulating ${tag} fetch (${ms}ms)…`);
  await new Promise((resolve) => setTimeout(resolve, ms));
}

export async function buildDemoResponse(
  tag: DemoIntegrationTag,
  prompt: string,
  conversationId: string,
  token: string,
  conversationHistory: Array<{ role: string; content: string }>,
  userMemory: Record<string, unknown> | null
): Promise<KairosResponse> {
  await simulateDemoDelay(tag);
  const mock = getDemoMockPayload(tag);
  return buildResponse(
    prompt,
    JSON.stringify(mock),
    tag,
    conversationId,
    token,
    conversationHistory,
    userMemory
  );
}
