import { NextRequest, NextResponse } from 'next/server';
import { createClient as createServerClient } from '@/lib/supabase/server';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { ai } from '@/lib/ai/gemini-client';

export async function GET(req: NextRequest) {
  try {
    const authHeader = req.headers.get('Authorization');
    const token = authHeader?.replace('Bearer ', '');

    // Initialize Supabase Client
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

    // Get Authenticated User
    const { data: { user }, error: authError } = token
      ? await supabase.auth.getUser(token)
      : await supabase.auth.getUser();

    if (!user) {
      return NextResponse.json({
        type: 'ERROR',
        text: 'Unauthorized. Please sign in.',
        meta: {
          conversationId: 'digest-auth',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      }, { status: 401 });
    }

    // Fetch Unread Notifications
    const { data: notifications, error: dbError } = await supabase
      .from('notifications')
      .select('*')
      .eq('user_id', user.id)
      .eq('is_read', false)
      .order('created_at', { ascending: false });

    if (dbError) {
      console.error('[Digest] Database error:', dbError);
      throw dbError;
    }

    if (!notifications || notifications.length === 0) {
      return NextResponse.json({
        type: 'TEXT',
        text: 'You have no new notifications. Enjoy your peace.',
        meta: {
          conversationId: 'digest-empty',
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      });
    }

    // Prompt Gemini for summary
    const prompt = `You are a notifications summarizer. Please summarize the following list of raw notification messages into a concise daily digest for KAIROS OS.
Group them by application, sender, or category (e.g. Social, Work, Finance, System).
For each group, provide a clear title (primary) and a brief summary of the key messages/updates (secondaries).

Respond with a JSON object adhering strictly to this schema:
{
  "items": [
    {
      "id": "string (unique identifier, e.g. 'social_instagram')",
      "primary": "string (group name + count, e.g. 'Instagram (3)')",
      "secondary": "string (brief summary of notifications in this group)",
      "icon": "string (one of: 'social', 'mail', 'calendar', 'notification')"
    }
  ]
}

Raw Notifications:
${JSON.stringify(notifications)}

Respond ONLY with valid JSON.`;

    const result = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
      }
    });

    const responseText = result.text || '{}';
    let parsedDigest;
    try {
      parsedDigest = JSON.parse(responseText);
    } catch (e) {
      console.error('[Digest] Failed to parse Gemini response as JSON. Response:', responseText);
      // Fallback if parsing fails
      parsedDigest = {
        items: [
          {
            id: 'fallback_summary',
            primary: `Unread Notifications (${notifications.length})`,
            secondary: notifications.map(n => `${n.title}: ${n.body}`).join(' | '),
            icon: 'notification'
          }
        ]
      };
    }

    // Mark notifications as read in Supabase
    const notificationIds = notifications.map(n => n.id);
    const { error: updateError } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .in('id', notificationIds);

    if (updateError) {
      console.error('[Digest] Error updating notifications status:', updateError);
    }

    return NextResponse.json({
      type: "WIDGET",
      widget: {
        widgetType: "DIGEST_SUMMARY",
        title: `Daily Digest — ${notifications.length} notification${notifications.length > 1 ? 's' : ''}`,
        items: parsedDigest.items || []
      },
      meta: {
        conversationId: 'digest-success',
        timestamp: new Date().toISOString(),
        model: 'gemini-2.5-flash'
      }
    });

  } catch (error: any) {
    console.error('[Digest] Route Error:', error);
    return NextResponse.json({
      type: 'ERROR',
      text: `Failed to load digest: ${error.message || 'Unknown error'}`
    }, { status: 500 });
  }
}

