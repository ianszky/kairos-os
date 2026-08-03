package com.kairos.os.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class KairosResponse(
    val type: String,
    val widget: WidgetPayload? = null,
    val text: String? = null,
    val intent: AndroidIntentPayload? = null,
    val deepLink: String? = null,
    val meta: ResponseMeta? = null
)

@Serializable
data class AndroidIntentPayload(
    val action: String,
    val uri: String? = null
)

@Serializable
data class ResponseMeta(
    val conversationId: String,
    val timestamp: String,
    val model: String,
    val runId: String? = null,
    val status: String? = null
)

@Serializable
data class PromptRunStatusResponse(
    val status: String,
    val runId: String,
    val conversationId: String,
    val response: KairosResponse? = null,
    val error: String? = null
)

@Serializable
data class WidgetPayload(
    val widgetType: String,
    val title: String? = null,
    val items: List<WidgetItem>,
    val actions: List<WidgetAction>? = null
)

@Serializable
data class WidgetItem(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val icon: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class WidgetAction(
    val label: String,
    val actionType: String,
    val target: String
)
