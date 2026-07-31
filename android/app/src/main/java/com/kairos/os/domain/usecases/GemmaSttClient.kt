package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaSttClient @Inject constructor(
    private val localLlmClient: LocalLlmClient
) {
    private val TAG = "GemmaSttClient"
    private val inferenceMutex = Mutex()

    private val asrPrompt = """
        Transcribe the following speech segment in its original language. Follow these specific instructions for formatting the answer:
        * Only output the transcription, with no newlines.
        * When transcribing numbers, write the digits, i.e. write 1.7 and not one point seven, and write 3 instead of three.
    """.trimIndent()

    suspend fun transcribe(wavBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        if (!localLlmClient.isAudioReady()) {
            Log.w(TAG, "Gemma audio backend not ready")
            return@withContext null
        }

        val engine = localLlmClient.getEngine() ?: return@withContext null

        inferenceMutex.withLock {
            val conversationConfig = ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 1,
                    topP = 0.95,
                    temperature = 0.1
                )
            )

            val conversation = engine.createConversation(conversationConfig)
            try {
                Log.i(TAG, "Running Gemma on-device ASR (${wavBytes.size} bytes)")
                val result = conversation.sendMessage(
                    Contents.of(
                        Content.Text(asrPrompt),
                        Content.AudioBytes(wavBytes)
                    )
                )
                val text = result.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(" ") { it.text }
                    .trim()
                    .replace("\n", " ")
                    .trim()

                Log.i(TAG, "Gemma ASR result: '$text'")
                text.ifBlank { null }
            } catch (e: Exception) {
                Log.e(TAG, "Gemma ASR failed", e)
                null
            } finally {
                try {
                    conversation.close()
                } catch (closeEx: Throwable) {
                    Log.w(TAG, "Ignored error closing LiteRT-LM conversation: ${closeEx.message}")
                }
            }
        }
    }
}
