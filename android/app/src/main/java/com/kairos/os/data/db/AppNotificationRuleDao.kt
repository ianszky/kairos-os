package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationRuleDao {

    @Query("SELECT * FROM app_notification_rules")
    fun getAllRulesFlow(): Flow<List<AppNotificationRuleEntity>>

    @Query("SELECT * FROM app_notification_rules")
    fun getAllRules(): List<AppNotificationRuleEntity>

    @Query("SELECT * FROM app_notification_rules WHERE package_name = :packageName LIMIT 1")
    fun getRuleForPackage(packageName: String): AppNotificationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(ruleEntity: AppNotificationRuleEntity)

    @Query("DELETE FROM app_notification_rules WHERE package_name = :packageName")
    fun deleteRule(packageName: String)
}
