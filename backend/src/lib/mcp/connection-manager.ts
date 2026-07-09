import { composio } from './composio-client';

export async function getConnectionStatus(userId: string): Promise<{
  connected: boolean;
  connectionId?: string;
}> {
  try {
    const accounts = await composio.connectedAccounts.list({
      userIds: [userId],
      statuses: ['ACTIVE'],
    });

    const activeAccount = accounts.items?.find(
      (acc: any) => acc.providerId === 'googlesuper' || acc.providerId === 'google' || acc.appId === 'googlesuper'
    );

    if (activeAccount) {
      return { connected: true, connectionId: activeAccount.id };
    }

    return { connected: false };
  } catch (error) {
    console.error('Error fetching Composio connection status:', error);
    return { connected: false };
  }
}

export async function initiateConnection(userId: string): Promise<{
  connectUrl: string;
}> {
  try {
    const connection = await composio.connectedAccounts.initiate(
      userId,
      'googlesuper', // Target integration name/ID
      {}
    );

    return { connectUrl: connection.redirectUrl! };
  } catch (error) {
    console.error('Error initiating Composio connection:', error);
    throw new Error('Failed to initiate connection');
  }
}
