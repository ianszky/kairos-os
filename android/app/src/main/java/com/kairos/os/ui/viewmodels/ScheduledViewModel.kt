package com.kairos.os.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.data.api.KairosApiClient
import com.kairos.os.data.db.RunningAgentDao
import com.kairos.os.data.db.RunningAgentEntity
import com.kairos.os.data.db.ScheduledTaskDao
import com.kairos.os.data.db.toDomain
import com.kairos.os.data.db.toEntity
import com.kairos.os.domain.models.CreateScheduledTaskRequest
import com.kairos.os.domain.models.ScheduledTask
import com.kairos.os.domain.models.ScheduledTaskRun
import com.kairos.os.domain.models.UpdateScheduledTaskRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ScheduledViewModel"

@HiltViewModel
class ScheduledViewModel @Inject constructor(
    private val apiClient: KairosApiClient,
    private val scheduledTaskDao: ScheduledTaskDao,
    private val runningAgentDao: RunningAgentDao
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<ScheduledTask>>(emptyList())
    val tasks: StateFlow<List<ScheduledTask>> = _tasks.asStateFlow()

    private val _runs = MutableStateFlow<List<ScheduledTaskRun>>(emptyList())
    val runs: StateFlow<List<ScheduledTaskRun>> = _runs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    init {
        // Observe local Room cache
        viewModelScope.launch {
            scheduledTaskDao.getAllTasksFlow().collectLatest { entities ->
                _tasks.value = entities.map { it.toDomain() }
            }
        }
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Fetch remote tasks
                val remoteTasks = apiClient.getScheduledTasks()
                if (remoteTasks.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        scheduledTaskDao.deleteAll()
                        scheduledTaskDao.insertAll(remoteTasks.map { it.toEntity() })
                    }
                }
                // 2. Fetch remote execution runs
                val remoteRuns = apiClient.getScheduledTaskRuns()
                _runs.value = remoteRuns
            } catch (e: Exception) {
                Log.e(TAG, "refreshAll failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createScheduledTask(
        prompt: String,
        appTarget: String?,
        frequency: String,
        daysOfWeek: List<Int>,
        timeOfDay: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val created = apiClient.createScheduledTask(
                    CreateScheduledTaskRequest(
                        prompt = prompt,
                        appTarget = appTarget,
                        frequency = frequency,
                        daysOfWeek = daysOfWeek,
                        timeOfDay = timeOfDay
                    )
                )
                if (created != null) {
                    withContext(Dispatchers.IO) {
                        scheduledTaskDao.insert(created.toEntity())
                    }
                    _actionFeedback.value = "Scheduled task created successfully!"
                    onSuccess()
                } else {
                    _actionFeedback.value = "Failed to create scheduled task."
                }
            } catch (e: Exception) {
                Log.e(TAG, "createScheduledTask error", e)
                _actionFeedback.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateScheduledTask(
        taskId: String,
        prompt: String? = null,
        appTarget: String? = null,
        title: String? = null,
        frequency: String? = null,
        daysOfWeek: List<Int>? = null,
        timeOfDay: String? = null,
        isActive: Boolean? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updated = apiClient.updateScheduledTask(
                    UpdateScheduledTaskRequest(
                        id = taskId,
                        prompt = prompt,
                        appTarget = appTarget,
                        title = title,
                        frequency = frequency,
                        daysOfWeek = daysOfWeek,
                        timeOfDay = timeOfDay,
                        isActive = isActive
                    )
                )
                if (updated != null) {
                    withContext(Dispatchers.IO) {
                        scheduledTaskDao.insert(updated.toEntity())
                    }
                    _actionFeedback.value = "Task updated."
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateScheduledTask error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleTaskActive(task: ScheduledTask) {
        updateScheduledTask(
            taskId = task.id,
            isActive = !task.isActive
        )
    }

    fun deleteScheduledTask(taskId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = apiClient.deleteScheduledTask(taskId)
                if (success) {
                    withContext(Dispatchers.IO) {
                        scheduledTaskDao.delete(taskId)
                    }
                    _actionFeedback.value = "Task deleted."
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteScheduledTask error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Executes a CRON job manually.
     * Inserts a RunningAgent card on the Home Screen as requested by the user,
     * calls backend /api/scheduled/execute, and updates the agent status to COMPLETE.
     */
    fun runTaskManually(task: ScheduledTask, onDispatched: (String) -> Unit = {}) {
        val agentId = UUID.randomUUID().toString()
        val titleText = task.title ?: task.prompt.take(45)

        viewModelScope.launch {
            // 1. Immediately insert RunningAgentEntity with status PROCESSING to pop off Home Screen agent card
            withContext(Dispatchers.IO) {
                runningAgentDao.insert(
                    RunningAgentEntity(
                        id = agentId,
                        prompt = task.prompt,
                        title = "🔄 $titleText",
                        status = "PROCESSING",
                        isLocal = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            onDispatched(agentId)

            // 2. Call backend /api/scheduled/execute
            try {
                val response = apiClient.executeScheduledTask(task.id)
                val isError = response.type == "ERROR"

                // 3. Update agent card status to COMPLETE or ERROR
                withContext(Dispatchers.IO) {
                    val responseJsonStr = runCatching {
                        kotlinx.serialization.json.Json.encodeToString(
                            com.kairos.os.domain.models.KairosResponse.serializer(),
                            response
                        )
                    }.getOrNull()

                    runningAgentDao.updateStatusAndResponse(
                        id = agentId,
                        status = if (isError) "ERROR" else "COMPLETE",
                        responseJson = responseJsonStr
                    )
                }

                // Refresh runs list
                val updatedRuns = apiClient.getScheduledTaskRuns()
                _runs.value = updatedRuns

            } catch (e: Exception) {
                Log.e(TAG, "runTaskManually failed", e)
                withContext(Dispatchers.IO) {
                    runningAgentDao.updateStatusAndResponse(
                        id = agentId,
                        status = "ERROR",
                        responseJson = null
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }
}
