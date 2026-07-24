package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningAgentDao {
    @Query("SELECT * FROM running_agents ORDER BY createdAt DESC")
    fun getAllAgentsFlow(): Flow<List<RunningAgentEntity>>

    @Query("SELECT * FROM running_agents WHERE status = 'PROCESSING' ORDER BY createdAt DESC")
    fun getActiveAgentsFlow(): Flow<List<RunningAgentEntity>>

    @Query("SELECT * FROM running_agents WHERE id = :id LIMIT 1")
    fun getAgentById(id: String): RunningAgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(agent: RunningAgentEntity)

    @Update
    fun update(agent: RunningAgentEntity)

    @Query("UPDATE running_agents SET title = :title WHERE id = :id")
    fun updateTitle(id: String, title: String)

    @Query("UPDATE running_agents SET status = :status, responseJson = :responseJson WHERE id = :id")
    fun updateStatusAndResponse(id: String, status: String, responseJson: String?)

    @Query("DELETE FROM running_agents WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM running_agents WHERE status IN ('COMPLETE', 'CANCELLED', 'ERROR') AND createdAt < :cutoff")
    fun deleteStale(cutoff: Long)
}
