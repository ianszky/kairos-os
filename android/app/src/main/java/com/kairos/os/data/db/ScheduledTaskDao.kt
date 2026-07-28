package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<ScheduledTaskEntity>>

    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): List<ScheduledTaskEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): ScheduledTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tasks: List<ScheduledTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: ScheduledTaskEntity)

    @Update
    fun update(task: ScheduledTaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM scheduled_tasks")
    fun deleteAll()
}
