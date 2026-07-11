import { Composio } from '@composio/core';
import { GoogleProvider } from '@composio/google';
import * as dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '.env') });

async function main() {
  const composio = new Composio({
    apiKey: process.env.COMPOSIO_API_KEY,
    provider: new GoogleProvider(),
  });

  const userId = '9caaffef-3c5a-45f5-b178-299abaaf31a9';

  try {
    const provider = composio.provider as any;
    const result = await provider.executeToolCall(userId, {
      name: 'GMAIL_FETCH_EMAILS',
      args: {},
    });
    console.log("Success:", result);
  } catch (error: any) {
    console.error("Error message:", error.message);
    if (error.response) {
      console.error("Response data:", error.response.data);
    } else if (error.cause) {
      console.error("Cause:", error.cause);
    }
  }
}

main().catch(console.error);
