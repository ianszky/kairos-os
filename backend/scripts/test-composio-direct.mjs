import fs from 'fs';
import path from 'path';
import { Composio } from '@composio/core';

// Load .env.local
const envPath = path.resolve(process.cwd(), '.env.local');
if (fs.existsSync(envPath)) {
  const envConfig = fs.readFileSync(envPath, 'utf8');
  for (const line of envConfig.split('\n')) {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith('#')) {
      const [key, ...valueParts] = trimmed.split('=');
      const val = valueParts.join('=').trim();
      process.env[key.trim()] = val;
    }
  }
}

console.log("COMPOSIO_API_KEY:", process.env.COMPOSIO_API_KEY ? "Present" : "Missing");

const composio = new Composio({
  apiKey: process.env.COMPOSIO_API_KEY,
});

async function test() {
  const userId = '9caaffef-3c5a-45f5-b178-299abaaf31a9'; // mock/real test userId

  console.log("\n--- Checking connected accounts ---");
  try {
    const accounts = await composio.connectedAccounts.list({ userIds: [userId], statuses: ['ACTIVE'] });
    console.log("Connected accounts count:", accounts.items?.length || 0);
    accounts.items?.forEach(acc => {
      console.log(`- Account ID: ${acc.id}, Toolkit: ${acc.toolkit?.slug || acc.toolkit}, Status: ${acc.status}`);
    });
  } catch (err) {
    console.error("Error fetching connected accounts:", err.message);
  }

  console.log("\n--- Testing composio.tools.get with GOOGLESUPER_FETCH_EMAILS ---");
  try {
    const tools = await composio.tools.get(userId, { tools: ['GOOGLESUPER_FETCH_EMAILS'] });
    console.log("Tools returned for GOOGLESUPER_FETCH_EMAILS:", tools.length);
    if (tools.length > 0) {
      console.log("Tool 0 name:", tools[0].function?.name || tools[0].name);
    }
  } catch (err) {
    console.error("Error fetching tools for GOOGLESUPER_FETCH_EMAILS:", err.message);
  }

  console.log("\n--- Testing composio.tools.get with GMAIL_FETCH_EMAILS ---");
  try {
    const tools = await composio.tools.get(userId, { tools: ['GMAIL_FETCH_EMAILS'] });
    console.log("Tools returned for GMAIL_FETCH_EMAILS:", tools.length);
    if (tools.length > 0) {
      console.log("Tool 0 name:", tools[0].function?.name || tools[0].name);
    }
  } catch (err) {
    console.error("Error fetching tools for GMAIL_FETCH_EMAILS:", err.message);
  }

  console.log("\n--- Testing composio.tools.get with youtube target ---");
  try {
    const tools = await composio.tools.get(userId, { tools: ['YOUTUBE_CREATE_PLAYLIST'] });
    console.log("Tools returned for YOUTUBE_CREATE_PLAYLIST:", tools.length);
  } catch (err) {
    console.error("Error fetching tools for YOUTUBE_CREATE_PLAYLIST:", err.message);
  }
}

test().catch(console.error);
