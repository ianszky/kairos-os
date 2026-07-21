package com.kairos.os.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_active") val isActive: Boolean = true,
    val isLocal: Boolean = false
)
