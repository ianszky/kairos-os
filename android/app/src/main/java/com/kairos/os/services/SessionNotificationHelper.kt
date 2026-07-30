package com.kairos.os.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kairos.os.R
import com.kairos.os.domain.session.AppSession
import com.kairos.os.ui.LauncherActivity
import java.text.DateFormat
import java.util.Date

object SessionNotificationHelper {

    const val CHANNEL_TIMER = "app_session_timer"
    const val CHANNEL_EXPIRED = "app_session_expired"
    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_EXPIRED = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.session_timer_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.session_timer_channel_desc)
            setShowBadge(false)
        }

        val expiredChannel = NotificationChannel(
            CHANNEL_EXPIRED,
            context.getString(R.string.session_expired_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.session_expired_channel_desc)
        }

        manager.createNotificationChannel(timerChannel)
        manager.createNotificationChannel(expiredChannel)
    }

    fun buildTimerNotification(context: Context, session: AppSession): Notification {
        ensureChannels(context)
        val endTimeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(session.expiresAtMs))
        val launchIntent = Intent(context, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setContentTitle(session.displayName)
            .setContentText(context.getString(R.string.session_timer_until, endTimeText))
            .setSubText(context.getString(R.string.session_timer_subtitle, session.minutes))
            .setSmallIcon(R.drawable.ic_session_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(session.expiresAtMs)
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun showExpiredNotification(context: Context, session: AppSession) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val launchIntent = Intent(context, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SESSION_EXPIRED, true)
            putExtra(EXTRA_EXPIRED_APP_NAME, session.displayName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRED)
            .setContentTitle(context.getString(R.string.session_expired_title))
            .setContentText(context.getString(R.string.session_expired_body, session.displayName))
            .setSmallIcon(R.drawable.ic_session_timer)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID_EXPIRED, notification)
    }

    fun cancelTimerNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID_TIMER)
    }

    const val EXTRA_SESSION_EXPIRED = "session_expired"
    const val EXTRA_EXPIRED_APP_NAME = "expired_app_name"
}
