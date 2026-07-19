package com.kairos.os.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kairos.os.data.db.LocalNotification
import com.kairos.os.data.db.LocalNotificationDao
import com.kairos.os.domain.usecases.LocalNotificationClassifier
import com.kairos.os.domain.usecases.ClassificationTier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class KairosNotificationListener : NotificationListenerService() {

    private val TAG = "KairosNotificationListener"

    @Inject
    lateinit var notificationClassifier: LocalNotificationClassifier

    @Inject
    lateinit var localNotificationDao: LocalNotificationDao

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
        var text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
        val category = notification.category

        // If it's a MessagingStyle notification (e.g. WhatsApp, Messages), extract all message parts
        val messagingStyle = androidx.core.app.NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null) {
            val messages = messagingStyle.messages
            if (messages.isNotEmpty()) {
                text = messages.joinToString("\n") { message ->
                    val sender = message.person?.name ?: message.sender ?: ""
                    if (sender.isNotEmpty()) "$sender: ${message.text}" else "${message.text}"
                }
            }
        }

        serviceScope.launch {
            try {
                val tier = notificationClassifier.classify(
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    category = category
                )

                Log.i(TAG, "🔔 Intercepted notification from ${sbn.packageName} classified as: $tier")

                if (tier == ClassificationTier.DIGEST) {
                    // Suppress: Dismiss the notification so it doesn't alert the user
                    cancelNotification(sbn.key)
                    Log.i(TAG, "🔕 Suppressed and dismissed notification from ${sbn.packageName}")

                    // Store it in the local Room database
                    Log.i(TAG, "💾 Saving suppressed notification to local Room database...")
                    
                    val localNotif = LocalNotification(
                        packageName = sbn.packageName,
                        title = title.ifBlank { "Notification" },
                        text = text.ifBlank { "No content" },
                        timestamp = sbn.postTime
                    )

                    localNotificationDao.insert(localNotif)
                    Log.i(TAG, "✅ Successfully saved notification to local database.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling notification in interceptor", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
