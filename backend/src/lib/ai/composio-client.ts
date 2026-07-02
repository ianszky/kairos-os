import { Composio } from '@composio/core';
import { GoogleProvider } from '@composio/google';

export const composio = new Composio({
  apiKey: process.env.COMPOSIO_API_KEY,
  provider: new GoogleProvider(),
});
