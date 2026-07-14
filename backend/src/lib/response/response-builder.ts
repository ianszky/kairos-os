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
  try {
    // Parse the structured JSON response directly from the tool executor
    // The tool executor now outputs KairosResponse-compatible JSON directly
    const result = typeof rawToolOutput === 'string' ? JSON.parse(rawToolOutput) : rawToolOutput;

    const payload: KairosResponse = {
      type: 'RESPONSE',
      text: result.text || rawToolOutput || prompt,
      widget: result.widget,
      meta: {
        conversationId: conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-2.5-flash'
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
        model: 'gemini-2.5-flash'
      }
    };
  }
}
