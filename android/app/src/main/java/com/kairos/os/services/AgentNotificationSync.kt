package com.kairos.os.services

import android.content.Context
import com.kairos.os.data.db.RunningAgentDao
import com.kairos.os.data.db.toDomain
import com.kairos.os.domain.models.RunningAgent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentNotificationSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runningAgentDao: RunningAgentDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var previousAgents: Map<String, RunningAgent> = emptyMap()

    fun start() {
        scope.launch {
            runningAgentDao.getAllAgentsFlow()
                .map { entities -> entities.map { it.toDomain() } }
                .collect { agents -> syncAgents(agents) }
        }
    }

    private fun syncAgents(agents: List<RunningAgent>) {
        val currentMap = agents.associateBy { it.id }

        for (removedId in previousAgents.keys - currentMap.keys) {
            AgentNotificationHelper.cancel(context, removedId)
        }

        for (agent in agents) {
            val previous = previousAgents[agent.id]
            if (previous == null || previous != agent) {
                AgentNotificationHelper.showOrUpdate(
                    context = context,
                    agent = agent,
                    previousStatus = previous?.status
                )
            }
        }

        previousAgents = currentMap
    }
}
