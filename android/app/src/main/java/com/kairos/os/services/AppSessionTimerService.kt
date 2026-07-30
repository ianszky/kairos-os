package com.kairos.os.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kairos.os.domain.session.AppSessionManager
import com.kairos.os.domain.session.SessionEndReason
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppSessionTimerService : Service() {

    @Inject
    lateinit var sessionManager: AppSessionManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var expiryWatchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val session = sessionManager.getActiveSession()
                if (session == null || session.isExpired) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = SessionNotificationHelper.buildTimerNotification(this, session)
                startForeground(SessionNotificationHelper.NOTIFICATION_ID_TIMER, notification)
                startExpiryWatch(session.expiresAtMs)
                return START_STICKY
            }
        }
    }

    private fun startExpiryWatch(expiresAtMs: Long) {
        expiryWatchJob?.cancel()
        expiryWatchJob = serviceScope.launch {
            val remaining = expiresAtMs - System.currentTimeMillis()
            if (remaining > 0) {
                delay(remaining)
            }
            if (isActive) {
                sessionManager.endSession(SessionEndReason.EXPIRED)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        expiryWatchJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.kairos.os.action.START_SESSION_TIMER"
        const val ACTION_STOP = "com.kairos.os.action.STOP_SESSION_TIMER"
    }
}
