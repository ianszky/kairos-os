import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  mapAppTargetToToolkitSlug,
  getCustomAuthCredentials,
  getIntegrationDisplayName,
  ConnectionSetupRequiredError,
  initiateConnection,
} from './connection-manager';

vi.mock('./composio-client', () => ({
  composio: {
    authConfigs: {
      list: vi.fn().mockResolvedValue({ items: [] }),
      create: vi.fn(),
    },
    connectedAccounts: {
      list: vi.fn().mockResolvedValue({ items: [] }),
      link: vi.fn(),
    },
  },
}));

describe('connection-manager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    delete process.env.TWITTER_CLIENT_ID;
    delete process.env.TWITTER_CLIENT_SECRET;
    delete process.env.X_CLIENT_ID;
    delete process.env.X_CLIENT_SECRET;
  });

  it('maps integration aliases to Composio toolkit slugs', () => {
    expect(mapAppTargetToToolkitSlug('x')).toBe('twitter');
    expect(mapAppTargetToToolkitSlug('browser')).toBe('composio_search');
    expect(mapAppTargetToToolkitSlug('search')).toBe('composio_search');
  });

  it('maps Google Workspace apps with GOOGLESUPER coverage to googlesuper', () => {
    expect(mapAppTargetToToolkitSlug('gmail')).toBe('googlesuper');
    expect(mapAppTargetToToolkitSlug('googledocs')).toBe('googlesuper');
    expect(mapAppTargetToToolkitSlug('googlemaps')).toBe('googlesuper');
    expect(mapAppTargetToToolkitSlug('googleslides')).toBe('googlesuper');
    expect(mapAppTargetToToolkitSlug('googlephotos')).toBe('googlesuper');
    expect(mapAppTargetToToolkitSlug('googlemeet')).toBe('googlesuper');
  });

  it('maps standalone Google toolkits to their own Composio toolkit slugs', () => {
    expect(mapAppTargetToToolkitSlug('googlecontacts')).toBe('googlecontacts');
    expect(mapAppTargetToToolkitSlug('googleforms')).toBe('googleforms');
    expect(mapAppTargetToToolkitSlug('googlechat')).toBe('google_chat');
    expect(mapAppTargetToToolkitSlug('googleclassroom')).toBe('google_classroom');
    expect(mapAppTargetToToolkitSlug('youtube')).toBe('youtube');
  });

  it('passes through known toolkit slugs unchanged', () => {
    expect(mapAppTargetToToolkitSlug('twitter')).toBe('twitter');
    expect(mapAppTargetToToolkitSlug('github')).toBe('github');
  });

  it('uses X display name for x and twitter targets', () => {
    expect(getIntegrationDisplayName('x')).toBe('X');
    expect(getIntegrationDisplayName('twitter')).toBe('X');
  });

  it('returns null custom auth credentials when env vars are missing', () => {
    expect(getCustomAuthCredentials('twitter')).toBeNull();
  });

  it('reads twitter custom auth credentials from env vars', () => {
    process.env.TWITTER_CLIENT_ID = 'client-id';
    process.env.TWITTER_CLIENT_SECRET = 'client-secret';
    expect(getCustomAuthCredentials('twitter')).toEqual({
      clientId: 'client-id',
      clientSecret: 'client-secret',
      displayName: 'X (Twitter)',
    });
  });

  it('throws setup-required error for twitter when custom credentials are missing', async () => {
    await expect(initiateConnection('user-1', 'x')).rejects.toMatchObject({
      name: 'ConnectionSetupRequiredError',
      reason: 'custom_credentials_required',
      displayName: 'X',
    } satisfies Partial<ConnectionSetupRequiredError>);
  });

  it('creates custom auth config for twitter when credentials exist', async () => {
    const { composio } = await import('./composio-client');
    process.env.TWITTER_CLIENT_ID = 'client-id';
    process.env.TWITTER_CLIENT_SECRET = 'client-secret';
    vi.mocked(composio.authConfigs.create).mockResolvedValue({ id: 'ac_twitter_custom' } as any);
    vi.mocked(composio.connectedAccounts.link).mockResolvedValue({ redirectUrl: 'https://connect.example/x' } as any);

    const result = await initiateConnection('user-1', 'x');

    expect(composio.authConfigs.create).toHaveBeenCalledWith('twitter', expect.objectContaining({
      type: 'use_custom_auth',
      authScheme: 'OAUTH2',
      credentials: expect.objectContaining({
        client_id: 'client-id',
        client_secret: 'client-secret',
      }),
    }));
    expect(result.connectUrl).toBe('https://connect.example/x');
  });
});
