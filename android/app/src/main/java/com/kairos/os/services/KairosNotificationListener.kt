package com.kairos.os.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kairos.os.domain.usecases.LocalNotificationClassifier
import com.kairos.os.domain.usecases.ClassificationTier
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    @SerialName("is_read") val isRead: Boolean = false
)

@AndroidEntryPoint
class KairosNotificationListener : NotificationListenerService() {

    private val TAG = "KairosNotificationListener"

    @Inject
    lateinit var notificationClassifier: LocalNotificationClassifier

    @Inject
    lateinit var supabaseClient: SupabaseClient

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected successfully")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Ignore notifications from KAIROS OS itself to prevent infinite loops
        if (sbn.packageName == packageName) {
            return
        }

        // Ignore notifications that cannot be cleared (e.g. system foreground services)
        if (!sbn.isClearable) {
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
        val category = notification.category

        serviceScope.launch {
            try {
                val tier = notificationClassifier.classify(
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    category = category
                )

                Log.d(TAG, "Notification from ${sbn.packageName} classified as $tier")

                if (tier == ClassificationTier.DIGEST) {
                    // Suppress: Dismiss the notification so it doesn't alert the user
                    cancelNotification(sbn.key)
                    Log.d(TAG, "Suppressed notification from ${sbn.packageName}")

                    // Get current Supabase user
                    val user = supabaseClient.auth.currentUserOrNull()
                    if (user != null) {
                        Log.d(TAG, "Syncing suppressed notification to Supabase for user: ${user.id}")
                        
                        val insertPayload = NotificationInsert(
                            userId = user.id,
                            title = title.ifBlank { "Notification" },
                            body = text.ifBlank { "No content" }
                        )

                        supabaseClient.postgrest["notifications"].insert(insertPayload)
                        Log.d(TAG, "Successfully synced notification to Supabase")
                    } else {
                        Log.w(TAG, "No authenticated user. Suppressed notification was not synced.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification in interceptor", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
