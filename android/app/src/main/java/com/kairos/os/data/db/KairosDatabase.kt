package com.kairos.os.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalNotification::class, LocalNote::class, LocalAlarm::class], version = 2, exportSchema = false)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun localNotificationDao(): LocalNotificationDao
    abstract fun localNoteDao(): LocalNoteDao
    abstract fun localAlarmDao(): LocalAlarmDao
}
