import { classifyIntent } from '../ai/intent-classifier';
import { executeComplexIntent } from '../mcp/tool-executor';
import { buildResponse } from '../response/response-builder';
import { getConversationContext, checkAndSummarizeIfNeeded } from '../ai/context-manager';
import { getUserMemory, updateUserMemoryAsync } from '../ai/user-memory';
import { KairosResponse } from '@/types/kairos';
import { createClient as createSupabaseClient } from '@supabase/supabase-js';
import { ai } from '../ai/gemini-client';

async function fetchAttachmentParts(attachments: any[], token: string) {
  const supabase = createSupabaseClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      global: {
        headers: { Authorization: `Bearer ${token}` }
      }
    }
  );

  const parts = [];
  for (const att of attachments) {
    try {
      const { data, error } = await supabase.storage.from('attachments').download(att.filePath);
      if (error) {
        console.error(`[IntentRouter] Error downloading attachment ${att.filePath}:`, error);
        continue;
      }
      const buffer = Buffer.from(await data.arrayBuffer());
      const base64Data = buffer.toString('base64');
      parts.push({
        inlineData: {
          mimeType: att.mimeType,
          data: base64Data
        }
      });
      console.log(`[IntentRouter] Prepared attachment: ${att.fileName} (${att.mimeType})`);
    } catch (err) {
      console.error(`[IntentRouter] Exception downloading attachment:`, err);
    }
  }
  return parts;
}

export async function processIntent(
  prompt: string, 
  explicitAppTarget: string | null, 
  userId: string, 
  conversationId: string, 
  token: string,
  attachments: any[] = []
): Promise<KairosResponse> {
  // 1. Load conversation context
  const history = await getConversationContext(conversationId, token);

  // 2. Load user memory
  const userMemory = await getUserMemory(userId, token);

  // 3. Classify the intent
  const classification = await classifyIntent(prompt, explicitAppTarget);
  if (explicitAppTarget) {
    console.log(`[IntentRouter] Overriding classified appTarget '${classification.appTarget}' with explicit target '${explicitAppTarget}'`);
    classification.appTarget = explicitAppTarget;
  }

  // Find all app targets by scanning for @app tags in the prompt
  const mentionRegex = /@([a-zA-Z0-9\-]+)/g;
  const mentions = Array.from(prompt.matchAll(mentionRegex)).map(m => m[1].toLowerCase());
  
  const appTargetsSet = new Set<string>();
  if (classification.appTarget && classification.appTarget !== 'generic' && classification.appTarget !== 'search') {
    appTargetsSet.add(classification.appTarget.toLowerCase());
  }
  if (explicitAppTarget) {
    appTargetsSet.add(explicitAppTarget.toLowerCase());
  }
  mentions.forEach(m => appTargetsSet.add(m));
  
  const appTargets = appTargetsSet.size > 0 ? Array.from(appTargetsSet) : [classification.appTarget || 'generic'];

  let rawResponseText = "";

  // 3.5 Intercept local launcher digest command
  const isDigestPrompt = appTargets.includes('launcher') && (
    prompt.toLowerCase().includes('digest') || 
    prompt.toLowerCase().includes('notification') || 
    prompt.toLowerCase().includes('summary')
  );

  if (isDigestPrompt) {
    console.log('[IntentRouter] Intercepted launcher digest command');
    const supabase = createSupabaseClient(
      process.env.NEXT_PUBLIC_SUPABASE_URL!,
      process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
      {
        global: {
          headers: { Authorization: `Bearer ${token}` }
        }
      }
    );

    const { data: notifications, error: dbError } = await supabase
      .from('notifications')
      .select('*')
      .eq('user_id', userId)
      .eq('is_read', false)
      .order('created_at', { ascending: false });

    if (dbError) {
      console.error('[Digest] Database error:', dbError);
      throw dbError;
    }

    if (!notifications || notifications.length === 0) {
      return {
        type: 'TEXT',
        text: 'You have no new notifications. Enjoy your peace.',
        meta: {
          conversationId,
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse;
    }

    const digestPrompt = `You are a notifications summarizer. Please summarize the following list of raw notification messages into a concise daily digest for KAIROS OS.
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
      contents: digestPrompt,
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

    // Mark notifications as read
    const notificationIds = notifications.map(n => n.id);
    await supabase
      .from('notifications')
      .update({ is_read: true })
      .in('id', notificationIds);

    return {
      type: "WIDGET",
      widget: {
        widgetType: "DIGEST_SUMMARY",
        title: `Daily Digest — ${notifications.length} notification${notifications.length > 1 ? 's' : ''}`,
        items: parsedDigest.items || []
      },
      meta: {
        conversationId,
        timestamp: new Date().toISOString(),
        model: 'gemini-2.5-flash'
      }
    } as KairosResponse;
  }

  // 4. Route based on tier
  if (classification.tier === 'SIMPLE') {
    rawResponseText = `User asked for a simple task. Intent was classified as simple. Action: ${prompt}`;
  } else {
    // 5. Check connection status for all target toolkit slugs
    const { getConnectionStatus, initiateConnection } = await import('../mcp/connection-manager');
    
    const unconnectedTargets: string[] = [];
    for (const target of appTargets) {
      if (['alarm', 'system', 'launcher', 'installed', 'generic', 'search'].includes(target)) continue;
      const connStatus = await getConnectionStatus(userId, target);
      if (!connStatus.connected) {
        unconnectedTargets.push(target);
      }
    }
    
    if (unconnectedTargets.length > 0) {
      const targetToConnect = unconnectedTargets[0];
      const connectData = await initiateConnection(userId, targetToConnect);
      
      const displayNameMap: Record<string, string> = {
        'googlesuper': 'Google',
        'googlecalendar': 'Google Calendar',
        'googlesheets': 'Google Sheets',
        'googledocs': 'Google Docs',
        'googledrive': 'Google Drive',
        'microsoftteams': 'Microsoft Teams',
        'slackbot': 'Slackbot',
        'hackernews': 'Hacker News',
        'discordbot': 'Discord Bot',
      };
      const displayName = displayNameMap[targetToConnect.toLowerCase()] || 
                          (targetToConnect.charAt(0).toUpperCase() + targetToConnect.slice(1));
      
      return {
        type: 'WIDGET',
        text: `Please connect your ${displayName} account to use this feature.`,
        widget: {
          widgetType: 'GENERIC_CARD',
          title: 'Connection Required',
          items: [
            { id: 'auth_msg', primary: `KAIROS OS needs access to your ${displayName} account to perform this action.` }
          ],
          actions: [
            { label: `Connect ${displayName}`, actionType: 'DEEP_LINK', target: connectData.connectUrl }
          ]
        },
        meta: {
          conversationId,
          timestamp: new Date().toISOString(),
          model: 'system'
        }
      } as KairosResponse;
    }

    // Download attachments from Supabase and format them as Gemini parts
    const attachmentParts = attachments.length > 0 ? await fetchAttachmentParts(attachments, token) : [];

    // 6. Complex intent requiring Composio tool execution
    console.log(`[Router] Intent classified as COMPLEX. Routing to Composio for appTargets:`, appTargets);
    try {
      rawResponseText = await executeComplexIntent(
        prompt, 
        appTargets, 
        userId, 
        history, 
        userMemory,
        classification.taskType,
        classification.inferredDetails,
        attachmentParts
      ) || "";
    } catch (err: unknown) {
      console.error("Error executing complex intent:", err);
      const errorMessage = err instanceof Error ? err.message : 'Unknown error';
      rawResponseText = `Failed to execute intent using Composio: ${errorMessage}`;
    }
  }

  // 6. Build and return the structured UI widget
  const primaryTarget = appTargets[0] || classification.appTarget;
  const response = await buildResponse(prompt, rawResponseText, primaryTarget, conversationId, token, history, userMemory);

  // 7. Fire-and-forget memory updates
  updateUserMemoryAsync(userId, prompt, response.text || "", userMemory, token);
  checkAndSummarizeIfNeeded(conversationId, token);

  return response;
}
