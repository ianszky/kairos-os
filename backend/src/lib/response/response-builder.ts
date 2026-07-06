import { ai } from '../ai/gemini-client';
import { KairosResponse } from '@/types/kairos';

export async function buildResponseWidget(text: string, appTarget: string): Promise<KairosResponse> {
  const schema = {
    type: "object",
    properties: {
      type: {
        type: "string",
        enum: ["WIDGET", "TEXT", "ANDROID_INTENT", "DEEP_LINK", "ERROR"]
      },
      text: { type: "string" },
      widget: {
        type: "object",
        properties: {
          widgetType: {
            type: "string",
            enum: ['EMAIL_LIST', 'CALENDAR_EVENT', 'ALARM_CONFIRM', 'NOTE_CARD', 'MUSIC_CARD', 'SEARCH_RESULTS', 'DIGEST_SUMMARY', 'GENERIC_CARD']
          },
          title: { type: "string" },
          items: {
            type: "array",
            items: {
              type: "object",
              properties: {
                id: { type: "string" },
                primary: { type: "string" },
                secondary: { type: "string" },
                icon: { type: "string" },
                metadata: {
                  type: "object",
                  additionalProperties: { type: "string" }
                }
              },
              required: ["id", "primary"]
            }
          },
          actions: {
            type: "array",
            items: {
              type: "object",
              properties: {
                label: { type: "string" },
                actionType: { type: "string", enum: ["DEEP_LINK", "CALLBACK", "DISMISS"] },
                target: { type: "string" }
              },
              required: ["label", "actionType", "target"]
            }
          }
        },
        required: ["widgetType", "items"]
      }
    },
    required: ["type"]
  };

  const response = await ai.models.generateContent({
    model: 'gemini-3.5-flash',
    contents: `Convert the following text into a structured KAIROS OS widget payload for the appTarget "${appTarget}". Text:\n\n${text}`,
    config: {
      responseMimeType: "application/json",
      responseSchema: schema,
      temperature: 0.1,
    }
  });

  try {
    const rawResult = response.text || "{}";
    const result = JSON.parse(rawResult);

    return {
      ...result,
      meta: {
        conversationId: 'mock-session-123',
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash'
      }
    };
  } catch (err) {
    return {
      type: 'TEXT',
      text: text,
      meta: {
        conversationId: 'mock-session-123',
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash'
      }
    };
  }
}
