const { Composio } = require('@composio/core');
const { GoogleProvider } = require('@composio/google');

async function main() {
  const composio = new Composio({
    apiKey: 'ak_czB9ykx4EMDR53ki-3iX',
  });

  const rawUserId = '9caaffef-3c5a-45f5-b178-299abaaf31a9';
  
  // Try 1: replace dashes
  const userId = `u-${rawUserId.replace(/-/g, '')}`;

  console.log("Using entity ID:", userId);
  try {
    const connectedAccounts = await composio.connectedAccounts.get(userId);
    console.log("Connected accounts:", connectedAccounts);
  } catch (error) {
    console.error("Error fetching connected accounts:", error.response ? error.response.data : error.message);
  }
}

main().catch(console.error);
