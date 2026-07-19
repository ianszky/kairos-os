package com.kairos.os.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalNotification::class], version = 1, exportSchema = false)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun localNotificationDao(): LocalNotificationDao
}
