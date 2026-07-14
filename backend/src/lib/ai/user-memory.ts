import { ai } from './gemini-client';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { getUser, updateUser } from '../db/users';

export async function getUserMemory(userId: string, token: string): Promise<Record<string, any> | null> {
  const supabase = token
    ? createSupabaseClient(
        process.env.NEXT_PUBLIC_SUPABASE_URL!,
        process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
        { global: { headers: { Authorization: `Bearer ${token}` } } }
      )
    : await createServerClient();

  try {
    const user = await getUser(supabase, userId);
    return user?.memory_context || {};
  } catch (e) {
    console.error("Error fetching user memory:", e);
    return {};
  }
}

export async function updateUserMemoryAsync(
  userId: string, 
  prompt: string, 
  response: string,
  currentMemory: Record<string, any> | null,
  token: string
): Promise<void> {
  // Batch/Filter: Skip updates on simple conversational filler or non-preference queries
  const skipPatterns = [
    /^(hi|hello|hey|thanks|thank you|ok|sure|yes|no)/i,
    /^(what|how|when|where|why|who)\s/i,
  ];
  
  if (skipPatterns.some(p => p.test(prompt.trim()))) {
    return; // Skip LLM call
  }

  try {
    const updatePrompt = `Given this user interaction, extract any new facts about the user 
(preferences, habits, personal info, work context) that should be 
remembered for future interactions. Return ONLY a JSON object containing the new or updated information 
merged with the existing memory. If nothing new, just return the existing memory unchanged.

Existing memory: ${JSON.stringify(currentMemory || {})}
User said: ${prompt}
System responded: ${response}

Return ONLY valid JSON.`;

    const result = await ai.models.generateContent({
      model: 'gemini-2.5-flash-lite',
      contents: updatePrompt,
      config: { 
        temperature: 0.1,
        responseMimeType: "application/json"
      }
    });

    const newMemoryJsonStr = result.text || "{}";
    const newMemory = JSON.parse(newMemoryJsonStr);

    const supabase = token
      ? createSupabaseClient(
          process.env.NEXT_PUBLIC_SUPABASE_URL!,
          process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
          { global: { headers: { Authorization: `Bearer ${token}` } } }
        )
      : await createServerClient();

    await updateUser(supabase, userId, { memory_context: newMemory });
  } catch (e) {
    console.error("Error updating user memory:", e);
  }
}
