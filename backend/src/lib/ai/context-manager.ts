import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { summarizeConversation } from './summarizer';
import { getMessages } from '../db/messages';

const MAX_CONTEXT_TOKENS = 100_000;
const CHARS_PER_TOKEN = 4;

function estimateTokens(text: string): number {
  return Math.ceil(text.length / CHARS_PER_TOKEN);
}

export async function getConversationContext(
  conversationId: string,
  token: string
): Promise<{ role: string; content: string }[]> {
  const supabase = token
    ? createSupabaseClient(
        process.env.NEXT_PUBLIC_SUPABASE_URL!,
        process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
        { global: { headers: { Authorization: `Bearer ${token}` } } }
      )
    : await createServerClient();

  try {
    // 1. Fetch messages
    const messagesData = await getMessages(supabase, conversationId);
    const formattedMessages = messagesData.map((m: any) => ({
      role: m.role,
      content: m.content
    }));

    // 2. Fetch latest summary
    const { data: summaryData } = await supabase
      .from('conversation_summaries')
      .select('*')
      .eq('conversation_id', conversationId)
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle();

    // 3. Estimate tokens
    const totalTokens = formattedMessages.reduce((sum: number, msg: any) => sum + estimateTokens(msg.content), 0);

    if (totalTokens > MAX_CONTEXT_TOKENS && summaryData) {
      // Return summary + last N messages (let's say 20)
      return [
        { role: 'system', content: `Previous conversation summary: ${summaryData.summary}` },
        ...formattedMessages.slice(-20)
      ];
    }

    return formattedMessages;
  } catch (e) {
    console.error("Error getting conversation context:", e);
    return [];
  }
}

export async function checkAndSummarizeIfNeeded(
  conversationId: string,
  token: string
): Promise<void> {
  try {
    const supabase = token
      ? createSupabaseClient(
          process.env.NEXT_PUBLIC_SUPABASE_URL!,
          process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
          { global: { headers: { Authorization: `Bearer ${token}` } } }
        )
      : await createServerClient();

    const messagesData = await getMessages(supabase, conversationId);
    
    const formattedMessages = messagesData.map((m: any) => ({
      role: m.role,
      content: m.content
    }));

    const totalTokens = formattedMessages.reduce((sum: number, msg: any) => sum + estimateTokens(msg.content), 0);

    // If context is large enough, trigger summary
    if (totalTokens > MAX_CONTEXT_TOKENS * 0.8) {
      const { data: summaryData } = await supabase
        .from('conversation_summaries')
        .select('message_count')
        .eq('conversation_id', conversationId)
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();
        
      // Only summarize if we have significantly more messages than the last summary (e.g. +10 messages)
      const lastSummaryCount = summaryData?.message_count || 0;
      if (formattedMessages.length > lastSummaryCount + 10) {
         await summarizeConversation(conversationId, formattedMessages, token);
      }
    }
  } catch(e) {
    console.error("Failed in checkAndSummarizeIfNeeded:", e);
  }
}
