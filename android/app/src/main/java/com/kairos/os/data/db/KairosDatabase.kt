package com.kairos.os.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalNotification::class,
        LocalNote::class,
        LocalAlarm::class,
        LocalConversationEntity::class,
        LocalMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KairosDatabase : RoomDatabase() {
    abstract fun localNotificationDao(): LocalNotificationDao
    abstract fun localNoteDao(): LocalNoteDao
    abstract fun localAlarmDao(): LocalAlarmDao
    abstract fun localConversationDao(): LocalConversationDao
    abstract fun localMessageDao(): LocalMessageDao
}
