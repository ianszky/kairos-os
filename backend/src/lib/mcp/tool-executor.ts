import { composio } from './composio-client';
import { ai } from '../ai/gemini-client';

export async function executeComplexIntent(prompt: string, appTarget: string, userId: string) {
  const toolkits = ["googlesuper"]; // Requested by user to start with this

  // Fetch tools
  const tools = await composio.tools.get(userId, {
    toolkits: toolkits,
    limit: 20, // Reasonable limit
  });

  // The @composio/google provider has wrapTools
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const provider = composio.provider as any; 
  const wrappedTools = provider.wrapTools(tools);

  // According to @google/genai docs, tools should be structured like: [{ functionDeclarations: [...] }]
  // However, @composio/google might return the exact format needed for the array.
  // We will assume wrappedTools is an array of GoogleGenAITool objects and pass it to `tools` directly,
  // or wrapped inside { functionDeclarations: wrappedTools }.
  // The README for @composio/google says:
  // type GoogleGenAIToolCollection = GoogleTool[];
  // Let's pass it as { functionDeclarations: wrappedTools } as that is the standard for the older REST API, 
  // but for the new @google/genai SDK v2+, tools are just passed as an array of tool objects, or 
  // if they are function declarations, `{ functionDeclarations: [...] }`.

  // Let's try the newer @google/genai v2.9+ format which often accepts { functionDeclarations: wrappedTools }
  // or simply the tools directly if they are already formatted.
  
  // For safety, let's format it explicitly or check what wrapTools returns.
  // Actually, let's just use the ai.chats.create
  const chat = ai.chats.create({
    model: 'gemini-2.5-flash',
    config: {
      tools: [{ functionDeclarations: wrappedTools }],
      systemInstruction: "You are the KAIROS OS agent. You fulfill the user's intent by calling the necessary tools. Return a clear and concise summary of what you did or found."
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
