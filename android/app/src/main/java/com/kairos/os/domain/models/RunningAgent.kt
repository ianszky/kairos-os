package com.kairos.os.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents a dispatched agent task shown as a card on the home screen.
 */
data class RunningAgent(
    val id: String,
    val prompt: String,
    val title: String? = null,
    val status: AgentStatus = AgentStatus.PROCESSING,
    val isLocal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val response: KairosResponse? = null
)

enum class AgentStatus {
    PROCESSING,
    COMPLETE,
    ERROR,
    CANCELLED
}
