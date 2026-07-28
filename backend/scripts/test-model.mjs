import fs from 'fs';
import path from 'path';
import { GoogleGenAI } from '@google/genai';

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

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

async function testModel() {
  console.log("Testing model gemini-3.5-flash...");
  try {
    const chat = ai.chats.create({ model: 'gemini-3.5-flash' });
    const res = await chat.sendMessage({ message: "Hello" });
    console.log("Success with gemini-3.5-flash! Response:", res.text);
  } catch (err) {
    console.error("Error with gemini-3.5-flash:", err.message);
  }

  console.log("\nTesting model gemini-2.5-flash...");
  try {
    const chat = ai.chats.create({ model: 'gemini-2.5-flash' });
    const res = await chat.sendMessage({ message: "Hello" });
    console.log("Success with gemini-2.5-flash! Response:", res.text);
  } catch (err) {
    console.error("Error with gemini-2.5-flash:", err.message);
  }
}

testModel();
