package com.kairos.os.ai

import android.content.Context
import com.google.ai.edge.litertlm.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class IntentValidationResult(
    val approved: Boolean,
    val feedback: String? = null
)

@Singleton
class OnDeviceIntentValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null

    private suspend fun getEngine(): Engine? {
        if (engine == null) {
            withContext(Dispatchers.IO) {
                try {
                    val modelFile = java.io.File(context.filesDir, "models/gemma-4-e2b.litertlm")
                    if (modelFile.exists()) {
                        val config = EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = Backend.GPU(),
                            cacheDir = context.cacheDir.path
                        )
                        engine = Engine(config).also { it.initialize() }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    engine = null
                }
            }
        }
        return engine
    }

    suspend fun validateReason(reason: String, appName: String): IntentValidationResult {
        val trimmedReason = reason.trim()
        if (trimmedReason.length < 3) {
            return IntentValidationResult(
                approved = false,
                feedback = "Please provide a specific reason."
            )
        }

        // Fast rule-based validation fallback if model is not yet loaded/downloaded
        val lowerReason = trimmedReason.lowercase()
        val rejectedVaguePhrases = listOf("bored", "idk", "nothing", "just checking", "want to scroll", "scroll", "fun", "relax")
        if (rejectedVaguePhrases.contains(lowerReason) || lowerReason.length < 4) {
            return IntentValidationResult(
                approved = false,
                feedback = "Please specify a clear, deliberate task or goal."
            )
        }

        return try {
            val eng = getEngine() ?: return IntentValidationResult(approved = true)

            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(
                    """You are a digital wellbeing guardian. A user wants to open a distracting app and must provide a specific, intentional reason.

APPROVE specific, intentional reasons like:
- "reply to friend's DM about dinner plans"
- "check assignment deadline for calculus"
- "post photo for mom's birthday"
- "watch tutorial on guitar chords"

REJECT vague or passive reasons like:
- "just checking", "bored", "nothing specific"
- "want to scroll", "idk", "entertainment"
- Single words like "fun" or "relax"

Respond with ONLY: APPROVED or REJECTED|<brief feedback>
Example: REJECTED|Try specifying what exactly you need to do.""".trimIndent()
                ),
                samplerConfig = SamplerConfig(
                    topK = 1,
                    topP = 0.95,
                    temperature = 0.1
                )
            )

            eng.createConversation(conversationConfig).use { conversation ->
                val chunks = mutableListOf<String>()
                conversation.sendMessageAsync("App: $appName. Reason: $trimmedReason")
                    .collect { chunks.add(it.toString()) }

                val response = chunks.joinToString("").trim()

                if (response.startsWith("APPROVED")) {
                    IntentValidationResult(approved = true)
                } else {
                    val feedback = response
                        .removePrefix("REJECTED")
                        .removePrefix("|")
                        .trim()
                        .ifEmpty { "Please be more specific about your intention." }
                    IntentValidationResult(approved = false, feedback = feedback)
                }
            }
        } catch (e: Exception) {
            // Fail-open: if the model errors or isn't present, don't block user
            IntentValidationResult(approved = true)
        }
    }

    fun release() {
        try {
            engine?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        engine = null
    }
}
