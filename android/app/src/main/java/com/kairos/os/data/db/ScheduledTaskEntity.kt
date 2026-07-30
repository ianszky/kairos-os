package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kairos.os.domain.models.ScheduledTask

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val prompt: String,
    val appTarget: String? = null,
    val title: String? = null,
    val frequency: String,
    val daysOfWeekJson: String = "[]",
    val timeOfDay: String,
    val timezone: String = "Asia/Manila",
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

fun ScheduledTaskEntity.toDomain(): ScheduledTask {
    val days = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<Int>>(daysOfWeekJson)
    }.getOrDefault(emptyList())

    return ScheduledTask(
        id = id,
        userId = userId,
        prompt = prompt,
        appTarget = appTarget,
        title = title,
        frequency = frequency,
        daysOfWeek = days,
        timeOfDay = timeOfDay,
        timezone = timezone,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ScheduledTask.toEntity(): ScheduledTaskEntity {
    val json = runCatching {
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<Int>()),
            daysOfWeek
        )
    }.getOrDefault("[]")

    return ScheduledTaskEntity(
        id = id,
        userId = userId,
        prompt = prompt,
        appTarget = appTarget,
        title = title,
        frequency = frequency,
        daysOfWeekJson = json,
        timeOfDay = timeOfDay,
        timezone = timezone,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
