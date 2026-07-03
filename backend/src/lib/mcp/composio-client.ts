import { Composio } from '@composio/core';
import { GoogleProvider } from '@composio/google';

if (!process.env.COMPOSIO_API_KEY) {
  console.warn("Missing COMPOSIO_API_KEY environment variable. Using dummy API key for build checks.");
}

export const composio = new Composio({
  apiKey: process.env.COMPOSIO_API_KEY || "dummy_api_key",
  provider: new GoogleProvider(),
});

// We hardcode the user ID as requested by the user
export const KAIROS_MOCK_USER_ID = 'kairos_mock_user_123';
