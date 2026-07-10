import { ai } from '../ai/gemini-client';
import { KairosResponse } from '@/types/kairos';

import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';

export async function buildResponseWidget(text: string, appTarget: string, conversationId: string, token: string): Promise<KairosResponse> {
  const jsonSchema = {
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
    required: ["type"],
    propertyOrdering: ["type", "text", "widget"]
  };

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: `Convert the following text into a structured KAIROS OS widget payload for the appTarget "${appTarget}". Text:\n\n${text}`,
      config: {
        responseMimeType: "application/json",
        responseJsonSchema: jsonSchema,
        temperature: 0.1,
      }
    });

    const rawResult = response.text || "{}";
    const result = JSON.parse(rawResult);

    // Ensure type is always present — spread result AFTER defaults
    const payload: KairosResponse = {
      type: 'TEXT',
      text: text,
      ...result,
      meta: {
        conversationId: conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash'
      }
    };

    // Final safety net: if type is somehow still missing, force it
    if (!payload.type) {
      payload.type = 'TEXT';
    }

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
      content: payload.text || text,
      app_target: appTarget,
      widget_payload: payload.widget || null,
      model_tier: 'flash'
    });

    return payload;
  } catch (err) {
    console.error("Response builder error:", err);
    return {
      type: 'TEXT',
      text: text,
      meta: {
        conversationId: conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-3.5-flash'
      }
    };
  }
}
