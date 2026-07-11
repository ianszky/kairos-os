import { ai } from '../ai/gemini-client';
import { KairosResponse } from '@/types/kairos';

import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';

export async function buildResponse(
  prompt: string,
  rawToolOutput: string,
  appTarget: string,
  conversationId: string,
  token: string,
  conversationHistory: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null
): Promise<KairosResponse> {
  const jsonSchema = {
    type: "object",
    properties: {
      text: { type: "string", description: "The natural language response addressing the user's prompt directly." },
      widget: {
        type: "object",
        description: "Optional. Include only if structured data needs to be displayed.",
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
    required: ["text"]
  };

  const systemContext = `
You are KAIROS OS. Your goal is to respond to the user concisely.
Conversation History Context: ${JSON.stringify(conversationHistory)}
User Memory Context: ${JSON.stringify(userMemory)}

Generate a response for the user's prompt based on the provided tool output (if any).
Provide a conversational "text" response. 
If the tool output contains structured data (like a list of emails, events, or actionable items), ALSO populate the "widget" field.
`;

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: `System Context:\n${systemContext}\n\nUser Prompt:\n${prompt}\n\nTool Output:\n${rawToolOutput}`,
      config: {
        responseMimeType: "application/json",
        responseJsonSchema: jsonSchema,
        temperature: 0.1,
      }
    });

    const rawResult = response.text || "{}";
    const result = JSON.parse(rawResult);

    const payload: KairosResponse = {
      type: 'RESPONSE',
      text: result.text || rawToolOutput || prompt,
      widget: result.widget,
      meta: {
        conversationId: conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-3-flash-preview'
      }
    };

    // Insert assistant message into Supabase
    const supabase = token
      ? createSupabaseClient(
          process.env.NEXT_PUBLIC_SUPABASE_URL!,
          process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
          {
            global: {
              headers: { Authorization: `Bearer ${token}` }
            }
          }
        )
      : await createServerClient();
      
    await supabase.from('messages').insert({
      conversation_id: conversationId,
      role: 'assistant',
      content: payload.text || "",
      app_target: appTarget,
      widget_payload: payload.widget || null,
      model_tier: 'flash'
    });

    return payload;
  } catch (err) {
    console.error("Response builder error:", err);
    return {
      type: 'ERROR',
      text: "Failed to generate response.",
      meta: {
        conversationId: conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-3-flash-preview'
      }
    };
  }
}
