package com.kairos.os.domain.models

sealed class Interaction {
    data class UserCommand(
        val command: String,
        val appTarget: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : Interaction()
    
    data class AssistantResponse(
        val response: KairosResponse,
        val timestamp: Long = System.currentTimeMillis()
    ) : Interaction()
    
    data class Loading(
        val statusLine: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : Interaction()

    data class StreamingResponse(
        val text: String,
        val modelName: String? = null,
        val isComplete: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    ) : Interaction()
}
