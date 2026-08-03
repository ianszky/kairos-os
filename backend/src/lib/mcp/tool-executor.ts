import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';
import { COMPOSIO_ACTION_MAP } from './action-map';

/** Always load the curated default tool set for an app — not taskType subsets. */
export function resolveToolSlugs(mapKey: string): string[] {
  const appMapping = COMPOSIO_ACTION_MAP[mapKey];
  return appMapping?.['default'] || [];
}

export async function executeComplexIntent(
  prompt: string, 
  appTargets: string | string[], 
  userId: string,
  history: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null,
  taskType: string = 'default',
  inferredDetails: string = '',
  attachmentParts: any[] = []
) {
  const targets = Array.isArray(appTargets) ? appTargets : [appTargets];
  
  // Normalize app targets to correct map keys
  const targetMap: Record<string, string> = {
    'teams': 'microsoftteams',
    'microsoft_teams': 'microsoftteams',
    'microsoftteams': 'microsoftteams',
    'onedrive': 'onedrive',
    'one_drive': 'onedrive',
    'googlecalendar': 'googlecalendar',
    'google_calendar': 'googlecalendar',
    'googlesheets': 'googlesheets',
    'google_sheets': 'googlesheets',
    'googledocs': 'googledocs',
    'google_docs': 'googledocs',
    'googledrive': 'googledrive',
    'google_drive': 'googledrive',
    'googletasks': 'googletasks',
    'google_tasks': 'googletasks',
    'googlemaps': 'googlemaps',
    'google_maps': 'googlemaps',
    'googlecontacts': 'googlecontacts',
    'google_contacts': 'googlecontacts',
    'googleforms': 'googleforms',
    'google_forms': 'googleforms',
    'googlesuper': 'googlesuper',
    'google_super': 'googlesuper',
    'googlechat': 'googlechat',
    'google_chat': 'googlechat',
    'googleclassroom': 'googleclassroom',
    'google_classroom': 'googleclassroom',
    'googleslides': 'googleslides',
    'google_slides': 'googleslides',
    'googlephotos': 'googlephotos',
    'google_photos': 'googlephotos',
    'googlemeet': 'googlemeet',
    'google_meet': 'googlemeet',
    'gmail': 'gmail',
    'slack': 'slack',
    'slackbot': 'slackbot',
    'notion': 'notion',
    'todoist': 'todoist',
    'spotify': 'spotify',
    'github': 'github',
    'browser': 'composio_search',
    'generic': 'composio_search',
    'search': 'composio_search',
    'composio_search': 'composio_search',
    'supabase': 'supabase',
    'outlook': 'outlook',
    'twitter': 'twitter',
    'x': 'twitter',
    'hubspot': 'hubspot',
    'linear': 'linear',
    'airtable': 'airtable',
    'jira': 'jira',
    'youtube': 'youtube',
    'canvas': 'canvas',
    'bitbucket': 'bitbucket',
    'discord': 'discord',
    'figma': 'figma',
    'reddit': 'reddit',
    'hackernews': 'hackernews',
    'hacker_news': 'hackernews',
    'asana': 'asana',
    'shopify': 'shopify',
    'linkedin': 'linkedin',
    'docusign': 'docusign',
    'discordbot': 'discordbot',
    'salesforce': 'salesforce',
    'calendly': 'calendly',
    'trello': 'trello',
    'dropbox': 'dropbox'
  };

  const functionDeclarations: any[] = [];
  const validToolNames = new Set<string>();

  // Schema cleanup utility to satisfy Gemini API constraints.
  // Gemini function schemas are an OpenAPI 3.0 subset — keywords like `const`
  // (common in Composio/Notion discriminated unions) are rejected with 400.
  const cleanSchema = (obj: any, isPropertiesObject: boolean = false): any => {
    if (Array.isArray(obj)) {
      return obj.map(item => cleanSchema(item, false));
    }
    if (obj !== null && typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        if (isPropertiesObject) {
          newObj[key] = cleanSchema(obj[key], false);
          continue;
        }

        // `const` is not supported; preserve the constraint via single-value enum.
        if (key === 'const') {
          if (newObj.enum === undefined) {
            newObj.enum = [obj[key]];
          }
          continue;
        }

        // Gemini documents anyOf support; remap oneOf to avoid another rejection.
        if (key === 'oneOf') {
          newObj.anyOf = cleanSchema(obj[key], false);
          continue;
        }

        const forbiddenKeywords = [
          'examples', 'title', 'default', 'file_uploadable',
          'exclusiveMinimum', 'exclusiveMaximum', 'format',
          'minLength', 'maxLength', 'pattern', 'minimum', 'maximum',
          '$schema', '$id', '$comment', 'not', 'if', 'then', 'else',
          'dependentRequired', 'dependentSchemas', 'unevaluatedProperties',
          'propertyNames', 'uniqueItems', 'contentMediaType', 'contentEncoding',
          'deprecated', 'readOnly', 'writeOnly', 'additionalProperties',
        ];
        if (!forbiddenKeywords.includes(key)) {
          newObj[key] = cleanSchema(obj[key], key === 'properties');
        }
      }
      return newObj;
    }
    return obj;
  };

  for (const appTarget of targets) {
    const normalizedTarget = appTarget.toLowerCase();
    const mapKey = targetMap[normalizedTarget] || normalizedTarget;
    
    const slugs = resolveToolSlugs(mapKey);

    console.log(`[ToolExecutor] Resolved slugs for target ${mapKey} (taskType hint: ${taskType}):`, slugs);

    let tools: any[] = [];
    if (slugs.length > 0) {
      try {
        tools = await composio.tools.get(userId, { tools: slugs });
      } catch (err) {
        console.error(`[ToolExecutor] Error fetching tools by slug for ${mapKey}:`, err);
      }
    } else {
      // Fallback: search-based filtering with a tight limit to prevent bloat
      try {
        tools = await composio.tools.get(userId, {
          toolkits: [mapKey],
          search: prompt.substring(0, 100),
          limit: 15,
        });
      } catch (err) {
        console.error(`[ToolExecutor] Fallback search tools retrieval failed for ${mapKey}:`, err);
      }
    }

    if (tools.length > 0) {
      const currentDeclarations = tools.map((t: any) => {
        const func = t.function || t;
        return {
          name: func.name,
          description: func.description || 'No description',
          parameters: cleanSchema(func.parameters),
        };
      });
      functionDeclarations.push(...currentDeclarations);
      currentDeclarations.forEach(fd => validToolNames.add(fd.name));
    }
  }

  const provider = composio.provider as any; 

  const systemInstruction = `# KAIROS OS Agent

## Role
You are the backend agent for KAIROS OS. You fulfill the user's intent by calling the necessary tools.
Return a structured JSON object containing your final response.

## Critical Rules
- You are STRICTLY limited to the tools provided. Never invent or guess tool names.
- If a tool call fails, report the error. Do not retry with hallucinated names.
- If the user's request is vague, make reasonable assumptions and proceed.
- For multi-step tasks (e.g. logging to sheets), check/search for the resource first, create if missing, then write.

## Response Format
You must return your final output matching this exact JSON schema:
{
  "text": "Human-readable summary of what you did. Be detailed and helpful. Typical 2-4 sentences.",
  "widget": { // Optional: include ONLY if you have structured details
    "widgetType": "EMAIL_LIST|CALENDAR_EVENT|ALARM_CONFIRM|NOTE_CARD|MUSIC_CARD|SEARCH_RESULTS|GENERIC_CARD",
    "title": "Title of the widget card",
    "items": [{"id": "unique_id", "primary": "Main content", "secondary": "Subtitle info"}],
    "actions": [{"label": "Action label", "actionType": "DEEP_LINK", "target": "deep link URL"}]
  }
}

## Context
Classified task type (hint only — you have the full default tool set): ${taskType}
Inferred task details: ${inferredDetails}
Conversation History Context: ${JSON.stringify(history)}
User Memory Context: ${JSON.stringify(userMemory)}`;

  const jsonSchema = {
    type: "object",
    properties: {
      text: { type: "string" },
      widget: {
        type: "object",
        properties: {
          widgetType: { type: "string" },
          title: { type: "string" },
          items: {
            type: "array",
            items: {
              type: "object",
              properties: {
                id: { type: "string" },
                primary: { type: "string" },
                secondary: { type: "string" }
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
                actionType: { type: "string" },
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

  const chatConfig: any = {
    systemInstruction: systemInstruction,
    temperature: 0.2,
  };

  if (functionDeclarations.length > 0) {
    chatConfig.tools = [{ functionDeclarations }];
  } else {
    chatConfig.responseMimeType = "application/json";
    chatConfig.responseSchema = jsonSchema;
  }

  const chat = ai.chats.create({
    model: 'gemini-3.5-flash',
    config: chatConfig
  });

  console.log("[ToolExecutor] Sending prompt to Gemini 2.5 Flash...");
  let messageContent: any = prompt;
  if (attachmentParts && attachmentParts.length > 0) {
    messageContent = [
      prompt,
      ...attachmentParts
    ];
  }
  let response = await chat.sendMessage({ message: messageContent });

  const MAX_ITERATIONS = 8;
  let iterations = 0;
  const executedCalls = new Set<string>();
  let lastToolResult: any = null;

  while (response.functionCalls && response.functionCalls.length > 0 && iterations < MAX_ITERATIONS) {
    iterations++;
    const parts = [];
    
    for (const fc of response.functionCalls) {
      const toolName = fc.name;
      if (!toolName) continue;

      const callSignature = `${toolName}:${JSON.stringify(fc.args || {})}`;
      if (executedCalls.has(callSignature)) {
        console.warn(`[ToolExecutor] Prevented duplicate execution for ${toolName}`);
        parts.push({
          functionResponse: {
            name: toolName,
            response: {
              notice: `Tool "${toolName}" with these arguments has already been executed successfully. Do not invoke it again. Return your final JSON response now.`
            }
          }
        });
        continue;
      }

      console.log(`[ToolExecutor] Model requested function call: ${toolName}`);

      // VALIDATION GATE: Catch hallucinated tool names
      if (!validToolNames.has(toolName)) {
        console.warn(`[ToolExecutor] Blocked hallucinated tool call: ${toolName}`);
        parts.push({
          functionResponse: { 
            name: toolName, 
            response: { 
              error: `Tool "${toolName}" is not available in your toolset. Available tools: ${Array.from(validToolNames).join(', ')}. Select only from these.`
            } 
          }
        });
        continue;
      }

      try {
        const result = await provider.executeToolCall(userId, {
          name: toolName,
          arguments: fc.args,
          args: fc.args,
        });

        executedCalls.add(callSignature);
        const parsedResult = typeof result === 'string' ? (runCatchingJson(result) || result) : result;
        lastToolResult = parsedResult;

        const safeFunctionResponse = formatFunctionResponse(parsedResult);

        console.log(`[ToolExecutor] Executed tool: ${toolName}`);
        parts.push({
          functionResponse: { 
            name: toolName, 
            response: safeFunctionResponse
          }
        });
      } catch (err: unknown) {
        let errorMessage = 'Unknown error';
        if (err instanceof Error) {
          errorMessage = err.message;
          const cause = (err as Error & { cause?: unknown }).cause;
          if (cause instanceof Error) {
            errorMessage += ` (cause: ${cause.message})`;
          } else if (cause != null) {
            errorMessage += ` (cause: ${String(cause)})`;
          }
        }
        console.log(`[ToolExecutor] Tool execution error (${toolName}):`, errorMessage);
        parts.push({
          functionResponse: {
            name: toolName,
            response: { error: errorMessage }
          }
        });
      }
    }
    
    if (parts.length > 0) {
      try {
        response = await chat.sendMessage({ message: parts });
      } catch (sendErr: unknown) {
        console.error("[ToolExecutor] Error sending function responses to Gemini:", sendErr);
        break;
      }
    } else {
      break;
    }
  }

  const pendingCalls = response.functionCalls?.length ?? 0;
  if (pendingCalls > 0) {
    console.warn(
      `[ToolExecutor] Exited tool loop after ${iterations} iteration(s) (max ${MAX_ITERATIONS}) with ${pendingCalls} pending function call(s). Falling back to last tool result or generic text.`
    );
  } else {
    console.log("[ToolExecutor] Completed loop. Returning final text payload.");
  }

  const responseText = response.text?.trim() || "";
  if (responseText.length > 0) {
    return responseText;
  }

  if (lastToolResult) {
    console.log("[ToolExecutor] No final text from model; using lastToolResult fallback.");
    return JSON.stringify({
      text: `Successfully executed requested action(s).`,
      widget: lastToolResult?.widget || null
    });
  }

  return JSON.stringify({
    text: "Task execution complete."
  });
}

function runCatchingJson(str: string) {
  try {
    return JSON.parse(str);
  } catch {
    return null;
  }
}

function formatFunctionResponse(result: any): Record<string, any> {
  let parsed = result;
  if (typeof result === 'string') {
    try {
      parsed = JSON.parse(result);
    } catch {
      parsed = { result: result };
    }
  }

  if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return { result: parsed };
  }

  return parsed;
}


