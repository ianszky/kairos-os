package com.kairos.os.domain.models

enum class ChatSearchMatchKind {
    TITLE,
    MESSAGE
}

data class ChatSearchResult(
    val conversationId: String,
    val title: String,
    val matchedText: String,
    val matchKind: ChatSearchMatchKind,
    val messageId: String? = null,
    val sortTimestamp: String
)
