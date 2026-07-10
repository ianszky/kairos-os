package com.kairos.os.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @SerialName("app_target") val appTarget: String? = null,
    @SerialName("model_tier") val modelTier: String? = null,
    @SerialName("widget_payload") val widgetPayload: WidgetPayload? = null,
    @SerialName("created_at") val createdAt: String
)
