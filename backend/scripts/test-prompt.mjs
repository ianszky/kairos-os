import fetch from 'node-fetch';

async function testPrompt() {
  console.log("Sending simple prompt...");
  let res = await fetch('http://localhost:3000/api/prompt', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ intent: 'Set an alarm for 6am tomorrow' })
  });
  console.log("Response:", await res.json());

  console.log("\nSending complex prompt...");
  res = await fetch('http://localhost:3000/api/prompt', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ intent: 'Show me my last 2 emails from Ms. Tenorio' })
  });
  console.log("Response:", await res.json());
}

testPrompt();
