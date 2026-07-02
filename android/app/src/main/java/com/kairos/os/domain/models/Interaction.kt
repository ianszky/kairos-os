package com.kairos.os.domain.models

sealed class Interaction {
    data class UserCommand(val command: String) : Interaction()
    data class WidgetResponse(val widget: WidgetPayload) : Interaction()
    data class TextResponse(val text: String) : Interaction()
    object Loading : Interaction()
}
