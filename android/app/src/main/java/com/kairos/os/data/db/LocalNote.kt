package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_notes")
data class LocalNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
