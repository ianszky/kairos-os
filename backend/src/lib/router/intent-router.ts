import { classifyIntent } from '../ai/intent-classifier';
import { executeComplexIntent } from '../mcp/tool-executor';
import { buildResponse } from '../response/response-builder';
import { getConversationContext, checkAndSummarizeIfNeeded } from '../ai/context-manager';
import { getUserMemory, updateUserMemoryAsync } from '../ai/user-memory';
import { KairosResponse } from '@/types/kairos';

export async function processIntent(prompt: string, explicitAppTarget: string | null, userId: string, conversationId: string, token: string): Promise<KairosResponse> {
  // 1. Load conversation context
  const history = await getConversationContext(conversationId, token);

  // 2. Load user memory
  const userMemory = await getUserMemory(userId, token);

  // 3. Classify the intent
  const classification = await classifyIntent(prompt, explicitAppTarget);

  let rawResponseText = "";

  // 4. Route based on tier
  if (classification.tier === 'SIMPLE') {
    rawResponseText = `User asked for a simple task. Intent was classified as simple. Action: ${prompt}`;
  } else {
    // 5. Complex intent requiring Composio tool execution
    try {
      rawResponseText = await executeComplexIntent(prompt, classification.appTarget, userId, history, userMemory) || "";
    } catch (err: unknown) {
      console.error("Error executing complex intent:", err);
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      rawResponseText = `Failed to execute intent using Composio: ${errorMessage}`;
    }
  }

  // 6. Build and return the structured UI widget
  const response = await buildResponse(prompt, rawResponseText, classification.appTarget, conversationId, token, history, userMemory);

  // 7. Fire-and-forget memory updates
  updateUserMemoryAsync(userId, prompt, response.text || "", userMemory, token);
  checkAndSummarizeIfNeeded(conversationId, token);

  return response;
}
