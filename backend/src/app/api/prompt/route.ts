import { NextResponse } from 'next/server';
import { KairosResponse } from '@/types/kairos';
import { processIntent } from '@/lib/router/intent-router';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';

export async function POST(request: Request) {
  try {
    const authHeader = request.headers.get('Authorization');
    const token = authHeader?.replace('Bearer ', '');
    
    // Create client depending on whether we received a token or cookies
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
    
    console.log('[API/Prompt] Auth Header present:', !!authHeader);
    console.log('[API/Prompt] Token length:', token?.length);

    const { data: { user }, error: authError } = token 
      ? await supabase.auth.getUser(token) 
      : await supabase.auth.getUser();

    if (!user) {
      console.log('[API/Prompt] User not found. Error:', authError);
      return NextResponse.json({
        type: 'ERROR',
        text: 'Unauthorized. Please sign in again.',
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse, { status: 401 });
    }

    const body = await request.json();
    const prompt = typeof body.intent === 'string' ? body.intent : '';
    const appTarget = typeof body.appTarget === 'string' ? body.appTarget : null;

    if (!prompt) {
      return NextResponse.json({
        type: 'ERROR',
        text: 'Prompt is required',
        meta: {
          conversationId: 'mock-session-123',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse, { status: 400 });
    }


    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    let conversationId = typeof body.sessionId === 'string' && uuidRegex.test(body.sessionId) ? body.sessionId : null;
    let isNewConversation = false;

    if (!conversationId) {
      const { data: convData, error: convErr } = await supabase
        .from('conversations')
        .insert({ user_id: user.id, title: 'New Conversation' })
        .select('id')
        .single();
      
      if (convErr || !convData) {
        throw new Error('Failed to create conversation: ' + (convErr?.message || 'Unknown error'));
      }
      conversationId = convData.id;
      isNewConversation = true;
    }

    const attachments = Array.isArray(body.attachments) ? body.attachments : [];

    // Insert user's message and select the generated ID
    const { data: msgData, error: msgErr } = await supabase
      .from('messages')
      .insert({
        conversation_id: conversationId,
        role: 'user',
        content: prompt,
        app_target: appTarget
      })
      .select('id')
      .single();

    if (msgErr || !msgData) {
      throw new Error('Failed to insert user message: ' + (msgErr?.message || 'Unknown error'));
    }

    const messageId = msgData.id;

    // Save attachments to public.message_attachments
    if (attachments.length > 0) {
      const attachmentsToInsert = attachments.map((att: any) => ({
        message_id: messageId,
        file_path: att.filePath,
        file_name: att.fileName,
        mime_type: att.mimeType,
        file_size: att.fileSize
      }));

      const { error: attachErr } = await supabase
        .from('message_attachments')
        .insert(attachmentsToInsert);

      if (attachErr) {
        console.error('[API/Prompt] Error saving message attachments:', attachErr);
      }
    }

    // Touch updated_at so sidebar ordering stays current
    await supabase.from('conversations')
      .update({ updated_at: new Date().toISOString() })
      .eq('id', conversationId);

    if (isNewConversation) {
      // Fire and forget title generation
      import('@/lib/ai/gemini-client').then(({ ai }) => {
        ai.models.generateContent({
          model: 'gemini-3.1-flash-lite',
          contents: `Generate a short, 3 to 5 words title for a conversation that starts with this message: "${prompt}". Respond with ONLY the title and nothing else.`
        }).then(async (res) => {
          const generatedTitle = res.text?.trim().replace(/["']/g, '');
          if (generatedTitle) {
            await supabase.from('conversations').update({ title: generatedTitle }).eq('id', conversationId);
          }
        }).catch(e => console.error("Title generation error:", e));
      });
    }

    // Bridge: pass the Supabase user ID and conversationId into the intent processing logic
    const responsePayload = await processIntent(prompt, appTarget, user.id, conversationId, token || '', attachments);
    return NextResponse.json(responsePayload);

  } catch (error: unknown) {
    console.error("API Route Error:", error);
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    const errorResponse: KairosResponse = {
      type: 'ERROR',
      text: `Failed to process request: ${errorMessage}`,
      meta: {
        conversationId: 'mock-session-123',
        timestamp: new Date().toISOString(),
        model: 'system'
      }
    };
    return NextResponse.json(errorResponse, { status: 500 });
  }
}
