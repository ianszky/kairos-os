package com.kairos.os.domain.usecases

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
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
    private val localNotificationDao: LocalNotificationDao
) {
    private val TAG = "LocalDigestGenerator"

    private val generativeModel by lazy {
        try {
            Generation.getClient()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ML Kit GenAI client", e)
            null
        }
    }

    suspend fun generateDigest(): KairosResponse {
        Log.d(TAG, "Generating daily digest from local notifications database...")
        
        // Run database queries on IO thread
        val notifications = withContext(Dispatchers.IO) {
            localNotificationDao.getUnreadNotifications()
        }

        if (notifications.isEmpty()) {
            Log.d(TAG, "No unread notifications found. Returning text response.")
            return KairosResponse(
                type = "TEXT",
                text = "You have no new notifications. Enjoy your peace."
            )
        }

        Log.d(TAG, "Found ${notifications.size} unread notifications.")

        val model = generativeModel
        val status = model?.checkStatus()
        Log.d(TAG, "Local Gemma model status: $status (AVAILABLE = 3, DOWNLOADABLE = 1, DOWNLOADING = 2, UNAVAILABLE = 0)")

        if (model != null && status == FeatureStatus.AVAILABLE) {
            try {
                val notifListText = notifications.joinToString("\n") {
                    "- App: ${it.packageName}, Title: ${it.title}, Content: ${it.text}"
                }

                val prompt = """
                    You are a notifications summarizer. Please summarize these notification messages into a daily digest.
                    Group them by application or sender.
                    
                    Respond with a JSON object adhering strictly to this schema:
                    {
                      "items": [
                        {
                          "id": "string (unique identifier, e.g. 'social_instagram')",
                          "primary": "string (group name + count, e.g. 'Instagram (3)')",
                          "secondary": "string (brief summary of notifications in this group)",
                          "icon": "string (one of: 'social', 'mail', 'calendar', 'notification')"
                        }
                      ]
                    }

                    Notifications:
                    $notifListText

                    Respond ONLY with valid JSON.
                """.trimIndent()

                Log.d(TAG, "Sending prompt to on-device Gemma model...")
                val response = model.generateContent(prompt)
                val responseText = response.candidates.firstOrNull()?.text?.trim() ?: "{}"
                Log.d(TAG, "On-device Gemma model response: $responseText")

                // Parse the JSON array
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(responseText).jsonObject
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

                // Mark read locally on IO thread
                val notificationIds = notifications.map { it.id }
                withContext(Dispatchers.IO) {
                    localNotificationDao.markAsRead(notificationIds)
                }
                Log.d(TAG, "Successfully marked ${notifications.size} notifications as read locally.")

                return KairosResponse(
                    type = "WIDGET",
                    widget = WidgetPayload(
                        widgetType = "DIGEST_SUMMARY",
                        title = "Daily Digest — ${notifications.size} notifications",
                        items = items
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error generating local digest with Gemma. Falling back to rules.", e)
            }
        } else {
            Log.d(TAG, "Gemma model not ready or unavailable. Falling back to rule-based grouping.")
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

        // Mark read locally on IO thread
        val notificationIds = notifications.map { it.id }
        withContext(Dispatchers.IO) {
            localNotificationDao.markAsRead(notificationIds)
        }
        Log.d(TAG, "Fallback: successfully marked ${notifications.size} notifications as read locally.")

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
