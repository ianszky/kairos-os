import fs from 'fs';
import path from 'path';
import { Composio } from '@composio/core';
import { GoogleProvider } from '@composio/google';
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

const composio = new Composio({ 
  apiKey: process.env.COMPOSIO_API_KEY,
  provider: new GoogleProvider()
});
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const userId = '9caaffef-3c5a-45f5-b178-299abaaf31a9';

const cleanSchema = (obj, isPropertiesObject = false) => {
  if (Array.isArray(obj)) return obj.map(item => cleanSchema(item, false));
  if (obj !== null && typeof obj === 'object') {
    const newObj = {};
    for (const key of Object.keys(obj)) {
      if (isPropertiesObject) {
        newObj[key] = cleanSchema(obj[key], false);
      } else {
        const forbiddenKeywords = [
          'examples', 'title', 'default', 'file_uploadable', 
          'exclusiveMinimum', 'exclusiveMaximum', 'format',
          'minLength', 'maxLength', 'pattern', 'minimum', 'maximum'
        ];
        if (!forbiddenKeywords.includes(key)) {
          newObj[key] = cleanSchema(obj[key], key === 'properties');
        }
      }
    }
    return newObj;
  }
  return obj;
};

// Helper function to safely format tool execution response for Google GenAI protobuf Struct
function formatFunctionResponse(result) {
  let parsed;
  if (typeof result === 'string') {
    try {
      parsed = JSON.parse(result);
    } catch {
      parsed = { result: result };
    }
  } else {
    parsed = result;
  }

  // Ensure parsed is a plain non-null object (not array or primitive)
  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return { result: parsed };
  }

  return parsed;
}

async function testGmailWithSafeResponse() {
  console.log("\n=================== TESTING GMAIL WITH SAFE RESPONSE ===================");
  const slugs = ['GOOGLESUPER_FETCH_EMAILS', 'GOOGLESUPER_GET_DRAFT', 'GOOGLESUPER_LIST_DRAFTS'];
  const tools = await composio.tools.get(userId, { tools: slugs });

  const functionDeclarations = tools.map(t => {
    const func = t.function || t;
    return {
      name: func.name,
      description: func.description || 'No description',
      parameters: cleanSchema(func.parameters),
    };
  });

  const chat = ai.chats.create({
    model: 'gemini-2.5-flash',
    config: {
      systemInstruction: `# KAIROS OS Agent
Summarize the emails found and return a JSON object with:
{
  "text": "Summary of emails",
  "widget": {
    "widgetType": "EMAIL_LIST",
    "title": "Recent Emails",
    "items": [{"id": "1", "primary": "Subject", "secondary": "Sender"}]
  }
}`,
      tools: [{ functionDeclarations }],
      temperature: 0.2,
    }
  });

  let response = await chat.sendMessage({ message: "fetch my emails" });
  if (response.functionCalls && response.functionCalls.length > 0) {
    for (const fc of response.functionCalls) {
      console.log(`Executing tool ${fc.name} with args:`, fc.args);
      const provider = composio.provider;
      
      const rawResult = await provider.executeToolCall(userId, {
        name: fc.name,
        arguments: fc.args,
        args: fc.args
      });
      console.log("Raw tool result successfully fetched!");

      const safeResponse = formatFunctionResponse(rawResult);
      console.log("Sending safeResponse to Gemini...");

      const followUp = await chat.sendMessage({
        message: [
          {
            functionResponse: {
              name: fc.name,
              response: safeResponse
            }
          }
        ]
      });
      console.log("\nSUCCESS! Gemini Follow-up Response:\n", followUp.text);
    }
  }
}

testGmailWithSafeResponse().catch(console.error);
