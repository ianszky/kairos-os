package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_conversations")
data class LocalConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String?,
    val createdAt: String,
    val updatedAt: String,
    val isActive: Boolean = true
)
