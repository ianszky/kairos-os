import { composio } from './composio-client';

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
    
    // 1. Fetch all existing configs and filter manually by toolkit slug
    const configs = await composio.authConfigs.list({ toolkit: toolkitSlug });
    
    const matchingConfig = configs.items?.find((item: any) => {
      const slug = (typeof item.toolkit === 'string' ? item.toolkit : item.toolkit?.slug || '').toLowerCase();
      return slug === toolkitSlug;
    });

    let authConfigId = '';
    
    if (matchingConfig) {
      authConfigId = matchingConfig.id;
      console.log(`[ConnectionManager] Reusing existing config for ${toolkitSlug}: ${authConfigId}`);
    } else {
      // 2. Create managed config if none exists for this specific toolkit
      console.log(`[ConnectionManager] No config found for ${toolkitSlug}. Creating managed config...`);
      const newConfig = await composio.authConfigs.create(toolkitSlug, {
        type: 'use_composio_managed_auth',
        name: `${toolkitSlug.toUpperCase()} Managed`,
      });
      authConfigId = newConfig.id;
    }

    // 3. Generate redirect URL
    const connection = await composio.connectedAccounts.link(userId, authConfigId);
    return { connectUrl: connection.redirectUrl! };
  } catch (error) {
    console.error(`Error initiating Composio connection for ${appTarget}:`, error);
    throw new Error(`Failed to initiate connection for ${appTarget}`);
  }
}

