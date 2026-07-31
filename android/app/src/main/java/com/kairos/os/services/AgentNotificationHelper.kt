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
import com.kairos.os.domain.models.AgentStatus
import com.kairos.os.domain.models.RunningAgent
import com.kairos.os.ui.LauncherActivity

object AgentNotificationHelper {

    const val CHANNEL_RUNNING = "agent_running"
    const val CHANNEL_COMPLETE = "agent_complete"
    const val GROUP_KEY = "kairos_running_agents"
    const val NOTIFICATION_ID_BASE = 2000

    const val EXTRA_OPEN_AGENT_ID = "open_agent_id"
    const val EXTRA_AGENT_ID = "agent_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val runningChannel = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.agent_running_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.agent_running_channel_desc)
            setShowBadge(false)
        }

        val completeChannel = NotificationChannel(
            CHANNEL_COMPLETE,
            context.getString(R.string.agent_complete_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.agent_complete_channel_desc)
        }

        manager.createNotificationChannel(runningChannel)
        manager.createNotificationChannel(completeChannel)
    }

    fun notificationIdFor(agentId: String): Int {
        return NOTIFICATION_ID_BASE + (agentId.hashCode() and 0x7FFF)
    }

    fun showOrUpdate(
        context: Context,
        agent: RunningAgent,
        previousStatus: AgentStatus?
    ) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val notificationId = notificationIdFor(agent.id)

        when (agent.status) {
            AgentStatus.PROCESSING -> {
                manager.notify(
                    notificationId,
                    buildRunningNotification(context, agent)
                )
            }
            AgentStatus.COMPLETE, AgentStatus.ERROR -> {
                val alert = previousStatus == AgentStatus.PROCESSING || previousStatus == null
                manager.notify(
                    notificationId,
                    buildCompleteNotification(context, agent, alert = alert)
                )
            }
            AgentStatus.CANCELLED -> cancel(context, agent.id)
        }
    }

    fun cancel(context: Context, agentId: String) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationIdFor(agentId))
    }

    private fun buildRunningNotification(context: Context, agent: RunningAgent): Notification {
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setContentTitle(displayTitle(agent))
            .setContentText(context.getString(R.string.agent_status_processing))
            .setSmallIcon(R.drawable.ic_agent_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context, agent.id))
            .setDeleteIntent(dismissIntent(context, agent.id))
            .setGroup(GROUP_KEY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun buildCompleteNotification(
        context: Context,
        agent: RunningAgent,
        alert: Boolean
    ): Notification {
        val channel = if (alert) CHANNEL_COMPLETE else CHANNEL_RUNNING
        val statusText = when (agent.status) {
            AgentStatus.COMPLETE -> context.getString(R.string.agent_status_complete)
            AgentStatus.ERROR -> context.getString(R.string.agent_status_failed)
            else -> context.getString(R.string.agent_status_processing)
        }
        return NotificationCompat.Builder(context, channel)
            .setContentTitle(displayTitle(agent))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_agent_notification)
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(contentIntent(context, agent.id))
            .setDeleteIntent(dismissIntent(context, agent.id))
            .setGroup(GROUP_KEY)
            .build()
    }

    private fun displayTitle(agent: RunningAgent): String {
        return agent.title ?: agent.prompt.take(45)
    }

    private fun contentIntent(context: Context, agentId: String): PendingIntent {
        val intent = Intent(context, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_AGENT_ID, agentId)
        }
        return PendingIntent.getActivity(
            context,
            agentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissIntent(context: Context, agentId: String): PendingIntent {
        val intent = Intent(context, AgentNotificationDismissReceiver::class.java).apply {
            putExtra(EXTRA_AGENT_ID, agentId)
        }
        return PendingIntent.getBroadcast(
            context,
            agentId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
