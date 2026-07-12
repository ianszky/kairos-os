import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';

export async function executeComplexIntent(
  prompt: string, 
  appTarget: string, 
  userId: string,
  history: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null
) {
  // Fallback mapping for generic terms and unified connections
  const appTargetMap: Record<string, string[]> = {
    'generic': ['search'],
    'browser': ['search', 'browser'],
    'clock': [], // Handled natively without composio
    'none': ['search'],
    
    // Map Google Workspace apps to googlesuper since they share a single connection
    'gmail': ['googlesuper'],
    'googlecalendar': ['googlesuper'],
    'googlesheets': ['googlesuper'],
    'googledocs': ['googlesuper'],
    'googledrive': ['googlesuper'],
    'googlecontacts': ['googlesuper'],
    'googleforms': ['googlesuper'],
    'googletasks': ['googlesuper'],
    'googlemaps': ['googlesuper'],
    'googlesuper': ['googlesuper'],
    'googlechat': ['googlesuper'],
    'googleclassroom': ['googlesuper'],
    'googleslides': ['googlesuper'],
    'googlephotos': ['googlesuper'],
    'googlemeet': ['googlesuper'],
    
    // Map Android target keys to exact Composio slugs
    'microsoftteams': ['microsoft_teams'],
    'teams': ['microsoft_teams'],
    'onedrive': ['one_drive']
  };

  const normalizedTarget = appTarget.toLowerCase();
  const toolkits = appTargetMap[normalizedTarget] || [normalizedTarget];

  console.log("[ToolExecutor] Starting executeComplexIntent for user:", userId);
  console.log("[ToolExecutor] Fetching tools for toolkits:", toolkits);

  let tools: any[] = [];
  if (toolkits.length > 0) {
    try {
      const filters: any = {
        toolkits: toolkits,
        limit: 50,
      };

      // If we are querying the unified googlesuper toolkit, search for the specific sub-app's tools dynamically
      if (toolkits.includes('googlesuper') && normalizedTarget !== 'googlesuper') {
        const searchTerms: Record<string, string> = {
          'gmail': 'gmail',
          'googlecalendar': 'calendar',
          'googlesheets': 'sheets',
          'googledocs': 'docs',
          'googledrive': 'drive',
          'googlecontacts': 'contacts',
          'googleforms': 'forms',
          'googletasks': 'tasks',
          'googlemaps': 'maps',
          'googlechat': 'chat',
          'googleclassroom': 'classroom',
          'googleslides': 'slides',
          'googlephotos': 'photos',
          'googlemeet': 'meet'
        };
        const search = searchTerms[normalizedTarget] || normalizedTarget;
        filters.search = search;
        filters.limit = 100; // Increase limit to ensure we capture all relevant tools in this category
      }

      tools = await composio.tools.get(userId, filters);
    } catch (err) {
      console.error("[ToolExecutor] Error fetching tools:", err);
    }
  }

  // We manually map tools to Google GenAI FunctionDeclarations since @composio/google's wrapTools 
  // sometimes doesn't strip out non-standard JSON schema keys like 'examples' or 'file_uploadable', 
  // which causes the Gemini API to throw HTTP 400.
  const cleanSchema = (obj: any, isPropertiesObject: boolean = false): any => {
    if (Array.isArray(obj)) {
      return obj.map(item => cleanSchema(item, false));
    }
    if (obj !== null && typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        if (isPropertiesObject) {
          // If this is a properties mapping, we preserve all keys (since they are parameter names),
          // but we clean their schema values recursively.
          newObj[key] = cleanSchema(obj[key], false);
        } else {
          // If this is a schema definition, we strip unsupported schema validation keywords.
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

  const provider = composio.provider as any; 

  const chat = ai.chats.create({
    model: 'gemini-3-flash-preview',
    config: {
      tools: functionDeclarations.length > 0 ? [{ functionDeclarations }] : [],
      systemInstruction: `You are the KAIROS OS agent. You fulfill the user's intent by calling the necessary tools. Return a clear and concise summary of what you did or found.
Conversation History: ${JSON.stringify(history)}
User Memory: ${JSON.stringify(userMemory)}`
    }
  });

  console.log("[ToolExecutor] Sending initial prompt to LLM...");
  let response = await chat.sendMessage({ message: prompt });

  // Handle agentic loop
  while (response.functionCalls && response.functionCalls.length > 0) {
    const parts = [];
    
    for (const fc of response.functionCalls) {
      const toolName = fc.name;
      if (!toolName) continue;

      console.log(`[ToolExecutor] Model requested function call: ${toolName}`);
      try {
        const result = await provider.executeToolCall(userId, {
          name: toolName,
          args: fc.args,
        });

        console.log(`[ToolExecutor] Successfully executed tool: ${toolName}`);
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
          if ((err as any).response?.data) {
            errorMessage += ` - Details: ${JSON.stringify((err as any).response.data)}`;
          } else if ((err as any).error) {
             errorMessage += ` - Details: ${JSON.stringify((err as any).error)}`;
          }
        }
        console.log(`[ToolExecutor] Error executing tool ${fc.name}:`, errorMessage);
        parts.push({
          functionResponse: {
            name: fc.name,
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

  console.log("[ToolExecutor] Completed agentic loop. Returning final text.");
  return response.text;
}
