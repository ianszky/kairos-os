package com.kairos.os.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledTask(
    val id: String,
    @SerialName("user_id") val userId: String,
    val prompt: String,
    @SerialName("app_target") val appTarget: String? = null,
    val title: String? = null,
    val frequency: String, // "daily", "weekly", "specific_days"
    @SerialName("days_of_week") val daysOfWeek: List<Int> = emptyList(), // 0=Sun, 1=Mon...6=Sat
    @SerialName("time_of_day") val timeOfDay: String, // "HH:mm:ss"
    val timezone: String = "Asia/Manila",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ScheduledTaskRun(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("conversation_id") val conversationId: String? = null,
    val status: String, // "pending", "running", "completed", "failed"
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class CreateScheduledTaskRequest(
    val prompt: String,
    val appTarget: String? = null,
    val frequency: String,
    val daysOfWeek: List<Int> = emptyList(),
    val timeOfDay: String,
    val timezone: String = "Asia/Manila"
)

@Serializable
data class UpdateScheduledTaskRequest(
    val id: String,
    val prompt: String? = null,
    val appTarget: String? = null,
    val title: String? = null,
    val frequency: String? = null,
    val daysOfWeek: List<Int>? = null,
    val timeOfDay: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class ExecuteTaskRequest(
    val taskId: String
)
