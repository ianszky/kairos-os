package com.kairos.os.domain.usecases

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.kairos.os.data.db.LocalNotificationDao
import com.kairos.os.domain.models.KairosResponse
import com.kairos.os.domain.models.WidgetPayload
import com.kairos.os.domain.models.WidgetItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDigestGenerator @Inject constructor(
    private val localNotificationDao: LocalNotificationDao,
    @ApplicationContext private val context: Context
) {
    private val TAG = "LocalDigestGenerator"
    private val MODEL_PATH = "/data/local/tmp/llm/gemma.bin"
    
    private var llmInference: LlmInference? = null

    init {
        initializeLlm()
    }

    private fun initializeLlm() {
        try {
            val modelFile = File(MODEL_PATH)
            if (modelFile.exists() && modelFile.canRead()) {
                Log.d(TAG, "Initializing MediaPipe LlmInference with model at $MODEL_PATH...")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(MODEL_PATH)
                    .setMaxTokens(512)
                    .setTemperature(0.2f)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "MediaPipe LlmInference successfully initialized.")
            } else {
                Log.w(TAG, "Gemma model file not found or unreadable at $MODEL_PATH. Fallback summary will be used.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LlmInference client", e)
        }
    }

    suspend fun generateDigest(): KairosResponse {
        Log.d(TAG, "Generating daily digest from local notifications database...")
        
        val notifications = withContext(Dispatchers.IO) {
            localNotificationDao.getUnreadNotifications()
        }

        if (notifications.isEmpty()) {
            return KairosResponse(
                type = "TEXT",
                text = "You have no new notifications. Enjoy your peace."
            )
        }

        if (llmInference == null) {
            initializeLlm()
        }

        val inference = llmInference
        if (inference != null) {
            try {
                val notifListText = notifications.joinToString("\n") {
                    "- App: ${it.packageName}, Title: ${it.title}, Content: ${it.text}"
                }

                // Configured prompt requesting JSON formatted output
                val prompt = """
                    You are a notifications summarizer. Please summarize the following notification messages into a structured daily digest.
                    Group them by application, sender, or category (e.g. Social, Work, Finance, System).
                    
                    Format your response strictly as a JSON object matching this schema:
                    {
                      "items": [
                        {
                          "id": "unique string key, e.g. 'social_instagram'",
                          "primary": "group name + count, e.g. 'Instagram (3)'",
                          "secondary": "brief summary sentence of notifications in this group",
                          "icon": "one of: 'social', 'mail', 'calendar', 'notification'"
                        }
                      ]
                    }

                    Notifications:
                    $notifListText

                    Return ONLY the JSON. Do not include markdown code block formatting (like ```json).
                """.trimIndent()

                Log.d(TAG, "Sending prompt to on-device Gemma via MediaPipe...")
                val responseText = withContext(Dispatchers.IO) {
                    inference.generateResponse(prompt).trim()
                }
                Log.d(TAG, "On-device MediaPipe Gemma response: $responseText")

                // Extract JSON if model wraps it in markdown blocks despite prompt instruction
                val cleanJson = if (responseText.startsWith("```json")) {
                    responseText.substringAfter("```json").substringBeforeLast("```").trim()
                } else if (responseText.startsWith("```")) {
                    responseText.substringAfter("```").substringBeforeLast("```").trim()
                } else {
                    responseText
                }

                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(cleanJson).jsonObject
                val itemsArray = root["items"]?.jsonArray
                
                val items = itemsArray?.map { element ->
                    val obj = element.jsonObject
                    WidgetItem(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        primary = obj["primary"]?.jsonPrimitive?.content ?: "",
                        secondary = obj["secondary"]?.jsonPrimitive?.content,
                        icon = obj["icon"]?.jsonPrimitive?.content ?: "notification"
                    )
                } ?: emptyList()

                val notificationIds = notifications.map { it.id }
                withContext(Dispatchers.IO) {
                    localNotificationDao.markAsRead(notificationIds)
                }

                return KairosResponse(
                    type = "WIDGET",
                    widget = WidgetPayload(
                        widgetType = "DIGEST_SUMMARY",
                        title = "Daily Digest — ${notifications.size} notifications",
                        items = items
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error generating local digest with MediaPipe. Falling back to rules.", e)
            }
        }

        // Graceful Fallback: Local rule-based summary
        val grouped = notifications.groupBy { it.packageName }
        val items = grouped.map { (pkg, list) ->
            WidgetItem(
                id = pkg,
                primary = "${pkg.substringAfterLast(".")}: ${list.size} updates",
                secondary = list.joinToString(", ") { it.title },
                icon = "notification"
            )
        }

        val notificationIds = notifications.map { it.id }
        withContext(Dispatchers.IO) {
            localNotificationDao.markAsRead(notificationIds)
        }

        return KairosResponse(
            type = "WIDGET",
            widget = WidgetPayload(
                widgetType = "DIGEST_SUMMARY",
                title = "Daily Digest — ${notifications.size} notifications",
                items = items
            )
        )
    }
}
