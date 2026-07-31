package com.kairos.os.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kairos.os.data.db.RunningAgentDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AgentNotificationDismissReceiver : BroadcastReceiver() {

    @Inject
    lateinit var runningAgentDao: RunningAgentDao

    override fun onReceive(context: Context, intent: Intent) {
        val agentId = intent.getStringExtra(AgentNotificationHelper.EXTRA_AGENT_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runningAgentDao.delete(agentId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
