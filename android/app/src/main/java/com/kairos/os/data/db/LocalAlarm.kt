package com.kairos.os.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_alarms")
data class LocalAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isActive: Boolean = true
)
