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
  if (explicitAppTarget) {
    console.log(`[IntentRouter] Overriding classified appTarget '${classification.appTarget}' with explicit target '${explicitAppTarget}'`);
    classification.appTarget = explicitAppTarget;
  }

  let rawResponseText = "";

  // 4. Route based on tier
  if (classification.tier === 'SIMPLE') {
    rawResponseText = `User asked for a simple task. Intent was classified as simple. Action: ${prompt}`;
  } else {
    // 5. Check connection status for the target toolkit slug
    const { getConnectionStatus, initiateConnection } = await import('../mcp/connection-manager');
    const connStatus = await getConnectionStatus(userId, classification.appTarget);
    
    if (!connStatus.connected) {
      const connectData = await initiateConnection(userId, classification.appTarget);
      
      const displayNameMap: Record<string, string> = {
        'googlesuper': 'Google',
        'googlecalendar': 'Google Calendar',
        'googlesheets': 'Google Sheets',
        'googledocs': 'Google Docs',
        'googledrive': 'Google Drive',
        'microsoftteams': 'Microsoft Teams',
        'slackbot': 'Slackbot',
        'hackernews': 'Hacker News',
        'discordbot': 'Discord Bot',
      };
      const displayName = displayNameMap[classification.appTarget.toLowerCase()] || 
                          (classification.appTarget.charAt(0).toUpperCase() + classification.appTarget.slice(1));
      
      return {
        type: 'WIDGET',
        text: `Please connect your ${displayName} account to use this feature.`,
        widget: {
          widgetType: 'GENERIC_CARD',
          title: 'Connection Required',
          items: [
            { id: 'auth_msg', primary: `KAIROS OS needs access to your ${displayName} account to perform this action.` }
          ],
          actions: [
            { label: `Connect ${displayName}`, actionType: 'DEEP_LINK', target: connectData.connectUrl }
          ]
        },
        meta: {
          conversationId,
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse;
    }

    // 6. Complex intent requiring Composio tool execution
    console.log(`[Router] Intent classified as COMPLEX. Routing to Composio for appTarget: ${classification.appTarget}`);
    try {
      rawResponseText = await executeComplexIntent(
        prompt, 
        classification.appTarget, 
        userId, 
        history, 
        userMemory,
        classification.taskType,
        classification.inferredDetails
      ) || "";
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
