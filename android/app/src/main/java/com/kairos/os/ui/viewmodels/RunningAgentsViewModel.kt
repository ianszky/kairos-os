package com.kairos.os.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.data.db.RunningAgentDao
import com.kairos.os.data.db.RunningAgentEntity
import com.kairos.os.data.db.toDomain
import com.kairos.os.domain.models.AgentStatus
import com.kairos.os.domain.models.KairosResponse
import com.kairos.os.domain.models.RunningAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class RunningAgentsViewModel @Inject constructor(
    private val runningAgentDao: RunningAgentDao
) : ViewModel() {

    val agents: StateFlow<List<RunningAgent>> = runningAgentDao.getAllAgentsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = agents
        .map { list -> list.count { it.status == AgentStatus.PROCESSING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun dispatch(conversationId: String, prompt: String, isLocal: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val entity = RunningAgentEntity(
                    id = conversationId,
                    prompt = prompt,
                    status = AgentStatus.PROCESSING.name,
                    isLocal = isLocal,
                    createdAt = System.currentTimeMillis()
                )
                runningAgentDao.insert(entity)
            }
        }
    }

    fun updateTitle(id: String, title: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runningAgentDao.updateTitle(id, title)
            }
        }
    }

    fun complete(id: String, response: KairosResponse) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val json = runCatching { Json.encodeToString(KairosResponse.serializer(), response) }.getOrNull()
                runningAgentDao.updateStatusAndResponse(id, AgentStatus.COMPLETE.name, json)
            }
        }
    }

    fun markError(id: String, errorMessage: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val errorResponse = KairosResponse(type = "ERROR", text = errorMessage)
                val json = runCatching { Json.encodeToString(KairosResponse.serializer(), errorResponse) }.getOrNull()
                runningAgentDao.updateStatusAndResponse(id, AgentStatus.ERROR.name, json)
            }
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runningAgentDao.delete(id)
            }
        }
    }

    fun cleanupStale() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                runningAgentDao.deleteStale(cutoff)
            }
        }
    }
}
