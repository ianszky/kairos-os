package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocalAlarmDao {
    @Query("SELECT * FROM local_alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): List<LocalAlarm>

    @Query("SELECT * FROM local_alarms WHERE id = :id LIMIT 1")
    fun getAlarmById(id: Int): LocalAlarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alarm: LocalAlarm): Long

    @Update
    fun update(alarm: LocalAlarm)

    @Delete
    fun delete(alarm: LocalAlarm)
}
