package com.kairos.os.domain.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kairos.os.services.AppSessionTimerService
import com.kairos.os.services.SessionExpiryReceiver
import com.kairos.os.services.SessionNotificationHelper
import com.kairos.os.ui.LauncherActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: AppSessionStore
) {
    val activeSession: StateFlow<AppSession?> = sessionStore.activeSession

    private val endLock = Any()

    fun startSession(
        packageName: String,
        displayName: String,
        appSlug: String,
        minutes: Int
    ) {
        endSession(SessionEndReason.REPLACED)
        val now = System.currentTimeMillis()
        val session = AppSession(
            packageName = packageName,
            displayName = displayName,
            appSlug = appSlug,
            grantedAtMs = now,
            expiresAtMs = now + minutes * 60_000L,
            minutes = minutes
        )
        sessionStore.saveSession(session)
        scheduleExpiryAlarm(session.expiresAtMs)
        startTimerService()
    }

    fun getActiveSession(): AppSession? = sessionStore.getSession()

    fun hasValidGrant(packageName: String): Boolean {
        val session = sessionStore.getSession() ?: return false
        return !session.isExpired && session.packageName == packageName
    }

    fun endSession(reason: SessionEndReason) {
        synchronized(endLock) {
            val session = sessionStore.getSession() ?: return
            cancelExpiryAlarm()
            stopTimerService()
            SessionNotificationHelper.cancelTimerNotification(context)
            sessionStore.clearSession()
            if (reason == SessionEndReason.EXPIRED) {
                goHome()
                SessionNotificationHelper.showExpiredNotification(context, session)
            }
        }
    }

    fun restoreSessionIfNeeded() {
        val session = sessionStore.getSession() ?: return
        if (session.isExpired) {
            sessionStore.clearSession()
            return
        }
        scheduleExpiryAlarm(session.expiresAtMs)
        startTimerService()
    }

    private fun scheduleExpiryAlarm(expiresAtMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SessionExpiryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAtMs, pendingIntent)
    }

    private fun cancelExpiryAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SessionExpiryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun startTimerService() {
        val intent = Intent(context, AppSessionTimerService::class.java).apply {
            action = AppSessionTimerService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    private fun stopTimerService() {
        val intent = Intent(context, AppSessionTimerService::class.java).apply {
            action = AppSessionTimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun goHome() {
        val homeIntent = Intent(context, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(homeIntent)
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 7001
    }
}
