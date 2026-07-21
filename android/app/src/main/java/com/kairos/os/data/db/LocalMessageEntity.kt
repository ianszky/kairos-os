package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_messages")
data class LocalMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val appTarget: String? = null,
    val modelTier: String? = null,
    val widgetPayloadJson: String? = null,
    val createdAt: String
)
