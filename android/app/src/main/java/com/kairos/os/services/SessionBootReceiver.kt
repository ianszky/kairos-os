package com.kairos.os.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kairos.os.domain.session.AppSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionManager: AppSessionManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            sessionManager.restoreSessionIfNeeded()
        }
    }
}
