package com.kairos.os.domain.models

data class KairosResponse(
    val type: String,
    val widget: WidgetPayload? = null,
    val text: String? = null,
    val intent: AndroidIntentPayload? = null,
    val deepLink: String? = null,
    val meta: ResponseMeta? = null
)

data class AndroidIntentPayload(
    val action: String,
    val uri: String? = null
)

data class ResponseMeta(
    val conversationId: String,
    val timestamp: String,
    val model: String
)

data class WidgetPayload(
    val widgetType: String,
    val title: String? = null,
    val items: List<WidgetItem>,
    val actions: List<WidgetAction>? = null
)

data class WidgetItem(
    val id: String,
    val primary: String,
    val secondary: String? = null,
    val icon: String? = null,
    val metadata: Map<String, String>? = null
)

data class WidgetAction(
    val label: String,
    val actionType: String,
    val target: String
)
