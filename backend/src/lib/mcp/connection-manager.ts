import { composio } from './composio-client';

const NO_MANAGED_AUTH_TOOLKITS = new Set(['twitter']);

const CUSTOM_AUTH_ENV: Record<string, { clientIdKeys: string[]; clientSecretKeys: string[]; displayName: string }> = {
  twitter: {
    clientIdKeys: ['TWITTER_CLIENT_ID', 'X_CLIENT_ID'],
    clientSecretKeys: ['TWITTER_CLIENT_SECRET', 'X_CLIENT_SECRET'],
    displayName: 'X (Twitter)',
  },
};

const COMPOSIO_OAUTH_REDIRECT_URI = 'https://backend.composio.dev/api/v3.1/toolkits/auth/callback';

export class ConnectionSetupRequiredError extends Error {
  constructor(
    public readonly appTarget: string,
    public readonly displayName: string,
    public readonly reason: 'custom_credentials_required' | 'connection_failed',
    message: string
  ) {
    super(message);
    this.name = 'ConnectionSetupRequiredError';
  }
}

export function mapAppTargetToToolkitSlug(appTarget: string): string {
  const target = appTarget.toLowerCase();
  
  // Google Workspace apps map to 'googlesuper'
  if (
    target.startsWith('google') || 
    ['gmail', 'drive', 'calendar', 'sheets', 'docs', 'contacts', 'forms', 'tasks', 'maps', 'chat', 'classroom', 'slides', 'photos', 'meet', 'youtube'].includes(target)
  ) {
    return 'googlesuper';
  }
  
  // Mapping for known apps to their exact Composio slugs
  const mappings: Record<string, string> = {
    'microsoftteams': 'microsoft_teams',
    'teams': 'microsoft_teams',
    'onedrive': 'one_drive',
    'x': 'twitter',
    'browser': 'composio_search',
    'search': 'composio_search',
  };
  
  return mappings[target] || target;
}

function readEnvValue(keys: string[]): string | undefined {
  for (const key of keys) {
    const value = process.env[key]?.trim();
    if (value) return value;
  }
  return undefined;
}

export function getCustomAuthCredentials(toolkitSlug: string): { clientId: string; clientSecret: string; displayName: string } | null {
  const config = CUSTOM_AUTH_ENV[toolkitSlug];
  if (!config) return null;

  const clientId = readEnvValue(config.clientIdKeys);
  const clientSecret = readEnvValue(config.clientSecretKeys);
  if (!clientId || !clientSecret) return null;

  return { clientId, clientSecret, displayName: config.displayName };
}

export function getIntegrationDisplayName(appTarget: string): string {
  const normalized = appTarget.toLowerCase();
  const displayNameMap: Record<string, string> = {
    'googlesuper': 'Google',
    'googlecalendar': 'Google Calendar',
    'googlesheets': 'Google Sheets',
    'googledocs': 'Google Docs',
    'googledrive': 'Google Drive',
    'microsoftteams': 'Microsoft Teams',
    'slackbot': 'Slackbot',
    'hackernews': 'Hacker News',
    'discordbot': 'Discord Bot',
    'x': 'X',
    'twitter': 'X',
  };

  return displayNameMap[normalized] ||
    CUSTOM_AUTH_ENV[mapAppTargetToToolkitSlug(normalized)]?.displayName ||
    (normalized.charAt(0).toUpperCase() + normalized.slice(1));
}

async function findExistingAuthConfigId(toolkitSlug: string): Promise<string | null> {
  const configs = await composio.authConfigs.list({ toolkit: toolkitSlug });
  const matchingConfig = configs.items?.find((item: any) => {
    const slug = (typeof item.toolkit === 'string' ? item.toolkit : item.toolkit?.slug || '').toLowerCase();
    return slug === toolkitSlug;
  });
  return matchingConfig?.id ?? null;
}

async function createAuthConfigId(toolkitSlug: string): Promise<string> {
  const existingConfigId = await findExistingAuthConfigId(toolkitSlug);
  if (existingConfigId) {
    console.log(`[ConnectionManager] Reusing existing config for ${toolkitSlug}: ${existingConfigId}`);
    return existingConfigId;
  }

  if (NO_MANAGED_AUTH_TOOLKITS.has(toolkitSlug)) {
    const customCredentials = getCustomAuthCredentials(toolkitSlug);
    if (!customCredentials) {
      throw new ConnectionSetupRequiredError(
        toolkitSlug,
        getIntegrationDisplayName(toolkitSlug),
        'custom_credentials_required',
        `${getIntegrationDisplayName(toolkitSlug)} requires your own X Developer OAuth credentials. Add TWITTER_CLIENT_ID and TWITTER_CLIENT_SECRET to the backend environment, then retry connecting.`
      );
    }

    console.log(`[ConnectionManager] Creating custom OAuth config for ${toolkitSlug}`);
    const newConfig = await composio.authConfigs.create(toolkitSlug, {
      type: 'use_custom_auth',
      authScheme: 'OAUTH2',
      name: customCredentials.displayName,
      credentials: {
        client_id: customCredentials.clientId,
        client_secret: customCredentials.clientSecret,
        oauth_redirect_uri: COMPOSIO_OAUTH_REDIRECT_URI,
      },
    });
    return newConfig.id;
  }

  console.log(`[ConnectionManager] No config found for ${toolkitSlug}. Creating managed config...`);
  const newConfig = await composio.authConfigs.create(toolkitSlug, {
    type: 'use_composio_managed_auth',
    name: `${toolkitSlug.toUpperCase()} Managed`,
  });
  return newConfig.id;
}

export async function getConnectionStatus(
  userId: string,
  appTarget: string = 'googlesuper'
): Promise<{
  connected: boolean;
  connectionId?: string;
}> {
  try {
    const toolkitSlug = mapAppTargetToToolkitSlug(appTarget);
    
    // Non-composio targets don't need authentication
    if (['clock', 'generic', 'browser', 'search', 'composio_search', 'none'].includes(toolkitSlug)) {
      return { connected: true };
    }

    const accounts = await composio.connectedAccounts.list({
      userIds: [userId],
      statuses: ['ACTIVE'],
    });

    const activeAccount = accounts.items?.find((acc: any) => {
      const slug = acc.toolkit?.slug?.toLowerCase();
      if (toolkitSlug === 'googlesuper') {
        return slug === 'googlesuper' || slug === 'google' || acc.authConfig?.id === 'ac_C16YuokUJota';
      }
      return slug === toolkitSlug;
    });

    if (activeAccount) {
      return { connected: true, connectionId: activeAccount.id };
    }

    return { connected: false };
  } catch (error) {
    console.error(`Error fetching Composio connection status for ${appTarget}:`, error);
    return { connected: false };
  }
}

export async function initiateConnection(
  userId: string,
  appTarget: string = 'googlesuper'
): Promise<{
  connectUrl: string;
}> {
  try {
    const toolkitSlug = mapAppTargetToToolkitSlug(appTarget);
    const authConfigId = await createAuthConfigId(toolkitSlug);
    const connection = await composio.connectedAccounts.link(userId, authConfigId);
    return { connectUrl: connection.redirectUrl! };
  } catch (error) {
    if (error instanceof ConnectionSetupRequiredError) {
      throw error;
    }
    console.error(`Error initiating Composio connection for ${appTarget}:`, error);
    throw new ConnectionSetupRequiredError(
      appTarget,
      getIntegrationDisplayName(appTarget),
      'connection_failed',
      `Failed to initiate connection for ${getIntegrationDisplayName(appTarget)}.`
    );
  }
}
