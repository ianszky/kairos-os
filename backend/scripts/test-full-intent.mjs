import fs from 'fs';
import path from 'path';

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

async function runTest() {
  const { processIntent } = await import('../src/lib/router/intent-router.ts');
  const userId = '9caaffef-3c5a-45f5-b178-299abaaf31a9';
  const convId = 'f7475141-86c4-4b9b-9c32-a5e2d6345091'; // example UUID
  const dummyToken = 'dummy-jwt-token-for-test';

  console.log("\n=================== TEST 1: @gmail fetch my emails ===================");
  const t1Start = Date.now();
  try {
    const res1 = await processIntent("@gmail fetch my emails", null, userId, convId, dummyToken);
    console.log(`[Test 1 Complete in ${Date.now() - t1Start}ms] Result:`, JSON.stringify(res1, null, 2));
  } catch (err) {
    console.error(`[Test 1 Error in ${Date.now() - t1Start}ms]:`, err);
  }

  console.log("\n=================== TEST 2: @youtube search video recommendations ===================");
  const t2Start = Date.now();
  try {
    const res2 = await processIntent("@youtube search video recommendations", null, userId, convId, dummyToken);
    console.log(`[Test 2 Complete in ${Date.now() - t2Start}ms] Result:`, JSON.stringify(res2, null, 2));
  } catch (err) {
    console.error(`[Test 2 Error in ${Date.now() - t2Start}ms]:`, err);
  }
}

runTest();
