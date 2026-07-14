import { ai } from './gemini-client';

export type IntentClassification = {
  tier: 'SIMPLE' | 'COMPLEX';
  appTarget: string;
  taskType: 'create' | 'read' | 'update' | 'delete' | 'send' | 'search' | 'list' | 'general';
  inferredDetails: string;
  reason: string;
};

export async function classifyIntent(prompt: string, defaultAppTarget: string | null): Promise<IntentClassification> {
  const schema = {
    type: "object",
    properties: {
      tier: {
        type: "string",
        enum: ["SIMPLE", "COMPLEX"],
        description: "SIMPLE: basic greetings, generic text questions, alarms. COMPLEX: requires fetching or mutating data in Gmail, Calendar, Sheets, Notion, etc."
      },
      appTarget: {
        type: "string",
        description: "The targeted application slug (e.g. 'gmail', 'googlecalendar', 'googlesheets', 'spotify', 'todoist', 'notion', 'slackbot', 'microsoftteams', 'onedrive', 'github'). Default to 'search' or 'generic' if undefined."
      },
      taskType: {
        type: "string",
        enum: ["create", "read", "update", "delete", "send", "search", "list", "general"],
        description: "The type of operation the user is requesting."
      },
      inferredDetails: {
        type: "string",
        description: "Inferred context not explicitly stated in the prompt (e.g. for sheets logging, detail: 'wants to log values in a spreadsheet')."
      },
      reason: {
        type: "string",
        description: "Detailed classification rationale."
      }
    },
    required: ["tier", "appTarget", "taskType", "inferredDetails", "reason"]
  };

  const response = await ai.models.generateContent({
    model: 'gemini-2.5-flash-lite',
    contents: `Classify the following user command: "${prompt}"\n\nIf the user provided an @app tag implicitly or explicitly, use that as the basis for appTarget. Default app target given by system: ${defaultAppTarget || 'none'}.`,
    config: {
      responseMimeType: "application/json",
      responseSchema: schema,
      temperature: 0.1,
    }
  });

  const text = response.text || "{}";
  const result = JSON.parse(text);
  
  return {
    tier: result.tier || 'COMPLEX',
    appTarget: result.appTarget || defaultAppTarget || 'generic',
    taskType: result.taskType || 'general',
    inferredDetails: result.inferredDetails || '',
    reason: result.reason || ''
  };
}
