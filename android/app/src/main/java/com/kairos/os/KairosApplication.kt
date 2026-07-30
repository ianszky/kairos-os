package com.kairos.os

import android.app.Application
import com.kairos.os.domain.session.AppSessionManager
import com.kairos.os.services.SessionNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KairosApplication : Application() {

    @Inject
    lateinit var appSessionManager: AppSessionManager

    override fun onCreate() {
        super.onCreate()
        SessionNotificationHelper.ensureChannels(this)
        appSessionManager.restoreSessionIfNeeded()
    }
}
