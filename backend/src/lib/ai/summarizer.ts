import { ai } from './gemini-client';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';

export async function summarizeConversation(
  conversationId: string, 
  messages: Array<{ role: string; content: string }>,
  token: string
): Promise<void> {
  const summaryPrompt = `Summarize this conversation concisely. Preserve:
- Key facts discussed
- User preferences revealed
- Actions taken (tools called, results)
- Open threads / unresolved items

Messages:
${messages.map(m => `${m.role}: ${m.content}`).join('\n')}`;

  try {
    const result = await ai.models.generateContent({
      model: 'gemini-3.1-flash-lite',
      contents: summaryPrompt,
      config: { temperature: 0.1 }
    });

    const summary = result.text || "";

    const supabase = token
      ? createSupabaseClient(
          process.env.NEXT_PUBLIC_SUPABASE_URL!,
          process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
          { global: { headers: { Authorization: `Bearer ${token}` } } }
        )
      : await createServerClient();

    await supabase.from('conversation_summaries').insert({
      conversation_id: conversationId,
      summary: summary,
      message_count: messages.length
    });
  } catch (e) {
    console.error("Failed to summarize conversation:", e);
  }
}
