package com.kairos.os.domain.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentNotificationNavigationStore @Inject constructor() {

    private val _pendingAgentId = MutableStateFlow<String?>(null)
    val pendingAgentId: StateFlow<String?> = _pendingAgentId.asStateFlow()

    fun requestOpen(agentId: String) {
        _pendingAgentId.value = agentId
    }

    fun consumePending(): String? {
        val id = _pendingAgentId.value
        _pendingAgentId.value = null
        return id
    }
}
