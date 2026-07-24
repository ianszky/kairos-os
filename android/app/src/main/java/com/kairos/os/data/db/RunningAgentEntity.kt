package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kairos.os.domain.models.AgentStatus
import com.kairos.os.domain.models.KairosResponse
import com.kairos.os.domain.models.RunningAgent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "running_agents")
data class RunningAgentEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val title: String? = null,
    val status: String = "PROCESSING",
    val isLocal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val responseJson: String? = null
)

fun RunningAgentEntity.toDomain(): RunningAgent {
    val parsedResponse = responseJson?.let { jsonStr ->
        runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<KairosResponse>(jsonStr)
        }.getOrNull()
    }
    val agentStatus = runCatching { AgentStatus.valueOf(status) }.getOrDefault(AgentStatus.PROCESSING)
    return RunningAgent(
        id = id,
        prompt = prompt,
        title = title,
        status = agentStatus,
        isLocal = isLocal,
        createdAt = createdAt,
        response = parsedResponse
    )
}

fun RunningAgent.toEntity(): RunningAgentEntity {
    val json = response?.let {
        runCatching { Json.encodeToString(KairosResponse.serializer(), it) }.getOrNull()
    }
    return RunningAgentEntity(
        id = id,
        prompt = prompt,
        title = title,
        status = status.name,
        isLocal = isLocal,
        createdAt = createdAt,
        responseJson = json
    )
}
