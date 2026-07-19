package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.kairos.os.data.db.LocalNotificationDao
import com.kairos.os.domain.models.KairosResponse
import com.kairos.os.domain.models.WidgetPayload
import com.kairos.os.domain.models.WidgetItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDigestGenerator @Inject constructor(
    private val localNotificationDao: LocalNotificationDao,
    private val localLlmClient: LocalLlmClient
) {
    private val TAG = "LocalDigestGenerator"

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

        val engine = localLlmClient.getEngine()
        if (engine != null) {
            try {
                val notifListText = notifications.joinToString("\n") {
                    "- App: ${it.packageName}, Title: ${it.title}, Content: ${it.text}"
                }

                // Prompt requesting JSON formatted output
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

                Log.i(TAG, "🤖 Running LiteRT-LM Gemma to summarize ${notifications.size} notifications...")
                val responseText = withContext(Dispatchers.IO) {
                    val conversation = engine.createConversation()
                    try {
                        val response = conversation.sendMessage(prompt)
                        response.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("\n") { it.text }
                            .trim()
                    } finally {
                        conversation.close()
                    }
                }
                Log.i(TAG, "🤖 LiteRT-LM Gemma digest response text: '$responseText'")

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
                Log.e(TAG, "Error generating local digest with LiteRT-LM. Falling back to rules.", e)
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
