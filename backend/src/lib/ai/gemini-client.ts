import { GoogleGenAI } from '@google/genai';

if (!process.env.GEMINI_API_KEY) {
  console.warn("Missing GEMINI_API_KEY environment variable. Using dummy API key for build checks.");
}

export const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY || "dummy_api_key",
});
