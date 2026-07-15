import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';
import { COMPOSIO_ACTION_MAP } from './action-map';

export async function executeComplexIntent(
  prompt: string, 
  appTarget: string, 
  userId: string,
  history: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null,
  taskType: string = 'default',
  inferredDetails: string = ''
) {
  const normalizedTarget = appTarget.toLowerCase();
  
  // Normalize app targets to correct map keys
  const targetMap: Record<string, string> = {
    'teams': 'microsoftteams',
    'microsoft_teams': 'microsoftteams',
    'onedrive': 'onedrive',
    'one_drive': 'onedrive',
    'googlecalendar': 'googlecalendar',
    'googlesheets': 'googlesheets',
    'googledrive': 'googledrive',
    'googletasks': 'googletasks',
    'gmail': 'gmail',
    'slack': 'slack',
    'slackbot': 'slackbot',
    'notion': 'notion',
    'todoist': 'todoist',
    'spotify': 'spotify',
    'github': 'github',
    'browser': 'search',
    'generic': 'search'
  };

  const mapKey = targetMap[normalizedTarget] || normalizedTarget;
  
  // Resolve slugs from actionMap
  const appMapping = COMPOSIO_ACTION_MAP[mapKey];
  const slugs = appMapping?.[taskType] || appMapping?.['default'] || [];

  console.log(`[ToolExecutor] Resolved slugs for target ${mapKey} (intent: ${taskType}):`, slugs);

  let tools: any[] = [];
  if (slugs.length > 0) {
    try {
      tools = await composio.tools.get(userId, { tools: slugs });
    } catch (err) {
      console.error("[ToolExecutor] Error fetching tools by slug:", err);
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
      console.error("[ToolExecutor] Fallback search tools retrieval failed:", err);
    }
  }

  // Schema cleanup utility to satisfy Gemini API constraints
  const cleanSchema = (obj: any, isPropertiesObject: boolean = false): any => {
    if (Array.isArray(obj)) {
      return obj.map(item => cleanSchema(item, false));
    }
    if (obj !== null && typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        if (isPropertiesObject) {
          newObj[key] = cleanSchema(obj[key], false);
        } else {
          const forbiddenKeywords = [
            'examples', 'title', 'default', 'file_uploadable', 
            'exclusiveMinimum', 'exclusiveMaximum', 'format',
            'minLength', 'maxLength', 'pattern', 'minimum', 'maximum'
          ];
          if (!forbiddenKeywords.includes(key)) {
            newObj[key] = cleanSchema(obj[key], key === 'properties');
          }
        }
      }
      return newObj;
    }
    return obj;
  };

  let functionDeclarations: any[] = [];
  if (tools.length > 0) {
    functionDeclarations = tools.map((t: any) => {
      const func = t.function || t;
      return {
        name: func.name,
        description: func.description || 'No description',
        parameters: cleanSchema(func.parameters),
      };
    });
  }

  const validToolNames = new Set(functionDeclarations.map(fd => fd.name));
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

  const chat = ai.chats.create({
    model: 'gemini-3.5-flash',
    config: {
      tools: functionDeclarations.length > 0 ? [{ functionDeclarations }] : [],
      systemInstruction: systemInstruction,
      responseMimeType: "application/json",
      responseSchema: jsonSchema,
      temperature: 0.2, // Lower temperature to avoid creative guesses
    }
  });

  console.log("[ToolExecutor] Sending prompt to Gemini 2.5 Flash...");
  let response = await chat.sendMessage({ message: prompt });

  const MAX_ITERATIONS = 5;
  let iterations = 0;

  while (response.functionCalls && response.functionCalls.length > 0 && iterations < MAX_ITERATIONS) {
    iterations++;
    const parts = [];
    
    for (const fc of response.functionCalls) {
      const toolName = fc.name;
      if (!toolName) continue;

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
          args: fc.args,
        });

        console.log(`[ToolExecutor] Executed tool: ${toolName}`);
        parts.push({
          functionResponse: { 
            name: toolName, 
            response: typeof result === 'string' ? JSON.parse(result) : result 
          }
        });
      } catch (err: unknown) {
        let errorMessage = 'Unknown error';
        if (err instanceof Error) {
          errorMessage = err.message;
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
      response = await chat.sendMessage({ message: parts });
    } else {
      break;
    }
  }

  console.log("[ToolExecutor] Completed loop. Returning final text payload.");
  return response.text;
}
