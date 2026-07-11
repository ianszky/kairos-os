import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';

export async function executeComplexIntent(
  prompt: string, 
  appTarget: string, 
  userId: string,
  history: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null
) {
  // Fallback mapping for generic terms
  const appTargetMap: Record<string, string[]> = {
    'generic': ['SEARCH'],
    'browser': ['SEARCH', 'BROWSER'],
    'clock': [], // Handled natively without composio
    'none': ['SEARCH']
  };

  const normalizedTarget = appTarget.toLowerCase();
  const toolkits = appTargetMap[normalizedTarget] || [normalizedTarget.toUpperCase()];

  console.log("[ToolExecutor] Starting executeComplexIntent for user:", userId);
  console.log("[ToolExecutor] Fetching tools for toolkits:", toolkits);

  let tools: any[] = [];
  if (toolkits.length > 0) {
    try {
      tools = await composio.tools.get(userId, {
        toolkits: toolkits,
        limit: 20,
      });
    } catch (err) {
      console.error("[ToolExecutor] Error fetching tools:", err);
    }
  }

  // We manually map tools to Google GenAI FunctionDeclarations since @composio/google's wrapTools 
  // sometimes doesn't strip out non-standard JSON schema keys like 'examples' or 'file_uploadable', 
  // which causes the Gemini API to throw HTTP 400.
  const cleanSchema = (obj: any): any => {
    if (Array.isArray(obj)) return obj.map(cleanSchema);
    if (obj !== null && typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        if (!['examples', 'title', 'default', 'file_uploadable'].includes(key)) {
          newObj[key] = cleanSchema(obj[key]);
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
      console.log(`[ToolExecutor] Model requested function call: ${fc.name}`);
      try {
        const result = await provider.executeToolCall(userId, {
          name: fc.name,
          args: fc.args,
        });

        console.log(`[ToolExecutor] Successfully executed tool: ${fc.name}`);
        parts.push({
          functionResponse: { 
            name: fc.name, 
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
