package com.kairos.os.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notification_rules")
data class AppNotificationRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    
    @ColumnInfo(name = "rule")
    val rule: String // "ALLOWED", "BLOCKED", "KAI_DECIDES"
)
