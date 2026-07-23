package com.kairos.os.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kairos.os.data.db.LocalNotification
import com.kairos.os.data.db.LocalNotificationDao
import com.kairos.os.domain.usecases.ClassificationTier
import com.kairos.os.domain.usecases.LocalNotificationClassifier
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
        Log.i(TAG, "🔔 KairosNotificationListener: Notification listener connected successfully to Android System Service.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // 1. Ignore notifications from KAIROS OS itself to prevent infinite feedback loops
        if (sbn.packageName == packageName) {
            return
        }

        // 2. Ignore non-clearable or ongoing system notifications (e.g. active call, media player, navigation, foreground service)
        if (!sbn.isClearable || sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(TAG, "⏩ Skipping non-clearable / ongoing notification from ${sbn.packageName}")
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        var text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
        val category = notification.category

        // Extract full MessagingStyle chat text (WhatsApp, Google Messages, Signal, etc.)
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

                when (tier) {
                    ClassificationTier.CRITICAL -> {
                        Log.i(TAG, "✅ PASS-THROUGH: Notification from ${sbn.packageName} classified as CRITICAL. Allowed on device.")
                    }

                    ClassificationTier.BLOCKED -> {
                        // Suppress: Dismiss notification and drop silently (do NOT save to Room DB)
                        cancelNotification(sbn.key)
                        Log.i(TAG, "🚫 BLACKLISTED (BLOCKED): Dismissed notification from ${sbn.packageName} without saving to digest DB.")
                    }

                    ClassificationTier.DIGEST -> {
                        // Suppress: Dismiss notification so it doesn't alert the user, and store in Room DB
                        cancelNotification(sbn.key)
                        Log.i(TAG, "🔕 DIGEST: Dismissed notification from ${sbn.packageName}. Saving to local Room DB...")

                        val localNotif = LocalNotification(
                            packageName = sbn.packageName,
                            title = title.ifBlank { "Notification" },
                            text = text.ifBlank { "No content" },
                            timestamp = sbn.postTime
                        )

                        localNotificationDao.insert(localNotif)
                        Log.i(TAG, "💾 Saved digest notification from ${sbn.packageName} into local Room DB.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling notification in interceptor listener", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
