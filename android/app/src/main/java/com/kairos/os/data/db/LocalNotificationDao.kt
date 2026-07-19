package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(notification: LocalNotification)

    @Query("SELECT * FROM local_notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): List<LocalNotification>

    @Query("UPDATE local_notifications SET isRead = 1 WHERE id IN (:ids)")
    fun markAsRead(ids: List<Int>)
}
