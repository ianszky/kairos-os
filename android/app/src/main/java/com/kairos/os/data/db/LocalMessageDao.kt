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

    @Query(
        """
        SELECT m.conversationId AS conversationId, c.title AS title, m.content AS matchedText,
               m.id AS messageId, m.createdAt AS sortTimestamp
        FROM local_messages m
        INNER JOIN local_conversations c ON c.id = m.conversationId
        WHERE m.content LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY m.createdAt DESC
        LIMIT :limit
        """
    )
    fun searchMessagesByContent(query: String, limit: Int = 50): List<LocalChatSearchRow>
}
