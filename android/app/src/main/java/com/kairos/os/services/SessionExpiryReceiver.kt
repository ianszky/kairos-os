package com.kairos.os.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kairos.os.domain.session.AppSessionManager
import com.kairos.os.domain.session.SessionEndReason
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionExpiryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionManager: AppSessionManager

    override fun onReceive(context: Context, intent: Intent?) {
        sessionManager.endSession(SessionEndReason.EXPIRED)
        context.stopService(Intent(context, AppSessionTimerService::class.java))
    }
}
