package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTitleGenerator @Inject constructor(
    private val localLlmClient: LocalLlmClient,
    private val supabaseClient: SupabaseClient
) {
    private val TAG = "LocalTitleGenerator"

    suspend fun generateAndSaveTitle(conversationId: String, firstPrompt: String) {
        val engine = localLlmClient.getEngine()
        if (engine == null) {
            Log.w(TAG, "Local LLM engine not available. Skipping local title generation.")
            return
        }

        val prompt = """
            Summarize the following user message into a short, 3 to 5 words title for a conversation:
            "$firstPrompt"
            
            Respond with ONLY the title. Do not wrap in quotes or write any other text.
        """.trimIndent()

        try {
            val titleResult = withContext(Dispatchers.IO) {
                val conversation = engine.createConversation()
                try {
                    val response = conversation.sendMessage(prompt)
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("\n") { it.text }
                        .trim().replace("\"", "").replace("'", "")
                } finally {
                    conversation.close()
                }
            }

            if (titleResult.isNotBlank()) {
                Log.i(TAG, "Generated local title: '$titleResult'. Saving to Supabase...")
                withContext(Dispatchers.IO) {
                    supabaseClient.postgrest["conversations"].update(
                        mapOf("title" to titleResult)
                    ) {
                        filter {
                            eq("id", conversationId)
                        }
                    }
                }
                Log.d(TAG, "Local title successfully saved to Supabase.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in local title generation / database sync", e)
        }
    }
}
