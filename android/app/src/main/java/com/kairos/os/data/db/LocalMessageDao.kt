package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalMessageDao {
    @Query("SELECT * FROM local_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): List<LocalMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: LocalMessageEntity)
}
