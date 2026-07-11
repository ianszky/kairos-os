import { ai } from './gemini-client';

export type IntentClassification = {
  tier: 'SIMPLE' | 'COMPLEX';
  appTarget: string;
  reason: string;
};

export async function classifyIntent(prompt: string, defaultAppTarget: string | null): Promise<IntentClassification> {
  const schema = {
    type: "object",
    properties: {
      tier: {
        type: "string",
        enum: ["SIMPLE", "COMPLEX"],
        description: "SIMPLE if it's a basic request like setting an alarm or a generic question. COMPLEX if it requires fetching data from Gmail, Calendar, Drive, etc."
      },
      appTarget: {
        type: "string",
        description: "The targeted application, e.g., 'gmail', 'calendar', 'clock'. If unsure, default to 'browser' or 'generic'."
      },
      reason: {
        type: "string",
        description: "Reasoning for the classification."
      }
    },
    required: ["tier", "appTarget", "reason"]
  };

  const response = await ai.models.generateContent({
    model: 'gemini-3.1-flash-lite',
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
    reason: result.reason || ''
  };
}
