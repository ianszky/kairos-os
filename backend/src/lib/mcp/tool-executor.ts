import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';

export async function executeComplexIntent(
  prompt: string, 
  appTarget: string, 
  userId: string,
  history: Array<{ role: string; content: string }>,
  userMemory: Record<string, any> | null
) {
  const toolkits = ["googlesuper"]; // Requested by user to start with this

  // Fetch tools
  const tools = await composio.tools.get(userId, {
    toolkits: toolkits,
    limit: 20, // Reasonable limit
  });

  // We manually map tools to Google GenAI FunctionDeclarations since @composio/google's wrapTools 
  // sometimes strips the name, and Gemini rejects non-standard JSON schema keys like 'examples'.
  const cleanSchema = (obj: any): any => {
    if (Array.isArray(obj)) return obj.map(cleanSchema);
    if (obj !== null && typeof obj === 'object') {
      const newObj: any = {};
      for (const key of Object.keys(obj)) {
        if (!['examples', 'title', 'default'].includes(key)) {
          newObj[key] = cleanSchema(obj[key]);
        }
      }
      return newObj;
    }
    return obj;
  };

  const functionDeclarations = tools.map((t: any) => {
    const func = t.function || t;
    return {
      name: func.name,
      description: func.description || 'No description',
      parameters: cleanSchema(func.parameters),
    };
  });

  const provider = composio.provider as any; 

  const chat = ai.chats.create({
    model: 'gemini-3.5-flash',
    config: {
      tools: [{ functionDeclarations }],
      systemInstruction: `You are the KAIROS OS agent. You fulfill the user's intent by calling the necessary tools. Return a clear and concise summary of what you did or found.
Conversation History: ${JSON.stringify(history)}
User Memory: ${JSON.stringify(userMemory)}`
    }
  });

  let response = await chat.sendMessage({ message: prompt });

  // Handle agentic loop
  while (response.functionCalls && response.functionCalls.length > 0) {
    const parts = [];
    
    for (const fc of response.functionCalls) {
      try {
        const result = await provider.executeToolCall(userId, {
          name: fc.name,
          args: fc.args,
        });

        parts.push({
          functionResponse: { 
            name: fc.name, 
            response: typeof result === 'string' ? JSON.parse(result) : result 
          }
        });
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : 'Unknown error';
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

  return response.text;
}
