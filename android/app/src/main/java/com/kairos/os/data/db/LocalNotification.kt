package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_notifications")
data class LocalNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
