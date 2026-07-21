package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.kairos.os.data.db.LocalConversationDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTitleGenerator @Inject constructor(
    private val localLlmClient: LocalLlmClient,
    private val supabaseClient: SupabaseClient,
    private val localConversationDao: LocalConversationDao
) {
    private val TAG = "LocalTitleGenerator"

    suspend fun generateAndSaveTitle(conversationId: String, firstPrompt: String, isLocal: Boolean = false) {
        val engine = localLlmClient.getEngine()

        var titleResult = ""

        if (engine != null) {
            val prompt = """
                Summarize the following user message into a short, 3 to 5 words title for a conversation:
                "$firstPrompt"
                
                Respond with ONLY the title. Do not wrap in quotes or write any other text.
            """.trimIndent()

            try {
                titleResult = withContext(Dispatchers.IO) {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error in Gemma LLM title generation", e)
            }
        }

        // Fallback title generation if LLM was unavailable or produced empty output
        if (titleResult.isBlank()) {
            val words = firstPrompt.trim().split("\\s+".toRegex()).take(4)
            titleResult = words.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }

        if (titleResult.isNotBlank()) {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
            Log.i(TAG, "Generated title: '$titleResult' (isLocal=$isLocal). Saving...")

            withContext(Dispatchers.IO) {
                try {
                    if (isLocal) {
                        localConversationDao.updateTitle(conversationId, titleResult, nowIso)
                        Log.d(TAG, "Local conversation title saved to Room DB.")
                    } else {
                        supabaseClient.postgrest["conversations"].update(
                            mapOf("title" to titleResult)
                        ) {
                            filter {
                                eq("id", conversationId)
                            }
                        }
                        Log.d(TAG, "Cloud conversation title saved to Supabase DB.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving title to database", e)
                }
            }
        }
    }
}
