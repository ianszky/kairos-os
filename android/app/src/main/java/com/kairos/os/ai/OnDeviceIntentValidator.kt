package com.kairos.os.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.*
import com.kairos.os.domain.usecases.LocalLlmClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class IntentValidationResult(
    val approved: Boolean,
    val feedback: String? = null
)

@Singleton
class OnDeviceIntentValidator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localLlmClient: LocalLlmClient
) {
    private val TAG = "OnDeviceIntentValidator"

    suspend fun validateReason(reason: String, appName: String): IntentValidationResult {
        val trimmedReason = reason.trim()
        if (trimmedReason.length < 3) {
            return IntentValidationResult(
                approved = false,
                feedback = "Please provide a specific, intentional reason."
            )
        }

        val engine = localLlmClient.getEngine()

        if (engine != null) {
            try {
                Log.i(TAG, "Running Gemma on-device intent validation for reason: '$trimmedReason'")
                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of(
                        """You are a digital wellbeing guardian for Kairos OS. A user wants to open the app "$appName" and must provide a specific, deliberate task or goal.

REJECT vague, passive, aimless reasons such as:
- "bored", "just checking", "scrolling", "watch videos", "fun", "relax", "photos", "idk", "feed", "funny clips", "entertainment", "curious", "boredom".
- Single words or short vague phrases without a specific action or objective.

APPROVE specific, intentional tasks such as:
- "reply to John's message about dinner"
- "check calculus assignment on blackboard"
- "watch tutorial on guitar chords"
- "post photo for mom's birthday"

Respond strictly in ONE of these two formats:
APPROVED
REJECTED|<brief actionable suggestion for a specific reason>""".trimIndent()
                    ),
                    samplerConfig = SamplerConfig(
                        topK = 1,
                        topP = 0.95,
                        temperature = 0.1
                    )
                )

                engine.createConversation(conversationConfig).use { conversation ->
                    val chunks = mutableListOf<String>()
                    conversation.sendMessageAsync("App: $appName. User's intent reason: \"$trimmedReason\"")
                        .collect { chunks.add(it.toString()) }

                    val response = chunks.joinToString("").trim()
                    Log.i(TAG, "Gemma response: '$response'")

                    if (response.contains("APPROVED", ignoreCase = true) && !response.contains("REJECTED", ignoreCase = true)) {
                        return IntentValidationResult(approved = true)
                    } else {
                        val rawFeedback = if (response.contains("|")) {
                            response.substringAfter("|").trim()
                        } else {
                            response.replace("REJECTED", "", ignoreCase = true).trim()
                        }
                        val feedback = rawFeedback.ifEmpty { "Please specify a clear, deliberate task or goal." }
                        return IntentValidationResult(approved = false, feedback = feedback)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemma model inference error, falling back to rule-based intent evaluation", e)
            }
        }

        // Fallback or model-absent intelligent intent evaluation
        return evaluateRuleBasedIntent(trimmedReason, appName)
    }

    private fun evaluateRuleBasedIntent(reason: String, appName: String): IntentValidationResult {
        val lowerReason = reason.lowercase()

        // 1. Block known vague or passive keywords
        val vagueKeywords = listOf(
            "bored", "idk", "nothing", "just checking", "want to scroll", "scroll", "scrolling", 
            "fun", "relax", "relaxing", "pass time", "kill time", "whatever", "stuff", "feed", 
            "reels", "shorts", "tiktok", "memes", "meme", "curious", "why not", "no reason", 
            "don't know", "boredom", "waste time", "see what's up", "checking", "just because", 
            "socialize", "browse", "browsing", "funny", "random", "entertainment", "check app",
            "open app", "video", "videos", "photos", "photo", "pic", "pics", "messages", "app", 
            "cat", "dog", "news", "music", "game", "games", "clips", "story", "stories"
        )

        val words = lowerReason.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Exact match with any vague keyword or single word from vague list
        if (vagueKeywords.contains(lowerReason) || (words.size == 1 && vagueKeywords.contains(words[0]))) {
            return IntentValidationResult(
                approved = false,
                feedback = "Please specify a clear, deliberate task (e.g. 'watch tutorial on guitar' or 'reply to DM from Alex')."
            )
        }

        // Action verbs indicating a deliberate task
        val actionVerbs = listOf(
            "watch", "reply", "check", "message", "send", "post", "search", "read", "email", 
            "call", "study", "look", "order", "buy", "pay", "review", "find", "listen", "work", 
            "do", "write", "create", "calculate", "solve", "learn", "upload", "download", 
            "schedule", "meet", "practice", "contact", "text", "ask", "answer"
        )

        val hasActionVerb = words.any { word -> actionVerbs.contains(word) }

        // Short phrases (under 3 words) without action verbs are rejected as vague
        if (words.size < 3 && !hasActionVerb) {
            return IntentValidationResult(
                approved = false,
                feedback = "Please specify a concrete task or goal (e.g. 'watch tutorial on guitar' or 'reply to DM from Alex')."
            )
        }

        // Short 2-word phrases with vague target objects like "watch videos" or "check feed"
        if (words.size == 2 && hasActionVerb) {
            val targetObject = words.firstOrNull { !actionVerbs.contains(it) } ?: ""
            val vagueTargets = listOf("video", "videos", "photos", "feed", "app", "reels", "shorts", "stuff", "media", "posts")
            if (vagueTargets.contains(targetObject)) {
                return IntentValidationResult(
                    approved = false,
                    feedback = "Please specify what $targetObject you intend to ${words.firstOrNull { actionVerbs.contains(it) } ?: "check"}."
                )
            }
        }

        return IntentValidationResult(approved = true)
    }
}
