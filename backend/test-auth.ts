import { Composio } from '@composio/core';

// use local .env
import 'dotenv/config';

async function main() {
  const composio = new Composio({
    apiKey: process.env.COMPOSIO_API_KEY,
  });

  const userId = '9caaffef-3c5a-45f5-b178-299abaaf31a9';

  try {
    const connectedAccounts = await composio.connectedAccounts.get(userId);
    console.log("Connected accounts:", connectedAccounts);
  } catch (error: any) {
    console.error("Error fetching connected accounts:", error.message);
  }
}

main().catch(console.error);
