package com.kairos.os.data.db

data class LocalChatSearchRow(
    val conversationId: String,
    val title: String?,
    val matchedText: String,
    val messageId: String?,
    val sortTimestamp: String
)
