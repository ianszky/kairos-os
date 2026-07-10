import { classifyIntent } from '../ai/intent-classifier';
import { executeComplexIntent } from '../mcp/tool-executor';
import { buildResponseWidget } from '../response/response-builder';
import { KairosResponse } from '@/types/kairos';

export async function processIntent(prompt: string, explicitAppTarget: string | null, userId: string, conversationId: string, token: string): Promise<KairosResponse> {
  // 1. Classify the intent
  const classification = await classifyIntent(prompt, explicitAppTarget);

  let rawResponseText = "";

  // 2. Route based on tier
  if (classification.tier === 'SIMPLE') {
    // For simple commands like alarms, we just build the widget directly
    // since we don't need real tool fetching in this MVP for simple stuff yet
    // Or we could run it through gemini to get a basic text response
    rawResponseText = `User asked for a simple task. Intent was classified as simple. Action: ${prompt}`;
  } else {
    // 3. Complex intent requiring Composio tool execution
    try {
      rawResponseText = await executeComplexIntent(prompt, classification.appTarget, userId) || "";
    } catch (err: unknown) {
      console.error("Error executing complex intent:", err);
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      rawResponseText = `Failed to execute intent using Composio: ${errorMessage}`;
    }
  }

  // 4. Build and return the structured UI widget
  return await buildResponseWidget(rawResponseText, classification.appTarget, conversationId, token);
}
