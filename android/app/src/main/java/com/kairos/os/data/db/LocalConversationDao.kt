package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalConversationDao {
    @Query("SELECT * FROM local_conversations ORDER BY updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<LocalConversationEntity>>

    @Query("SELECT * FROM local_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): List<LocalConversationEntity>

    @Query("SELECT * FROM local_conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): LocalConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(conversation: LocalConversationEntity)

    @Query("UPDATE local_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    fun updateTitle(id: String, title: String, updatedAt: String)

    @Query("DELETE FROM local_conversations WHERE id = :id")
    fun deleteById(id: String)

    @Query(
        """
        SELECT id AS conversationId, title AS title, COALESCE(title, '') AS matchedText,
               NULL AS messageId, updatedAt AS sortTimestamp
        FROM local_conversations
        WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    fun searchConversationsByTitle(query: String, limit: Int = 50): List<LocalChatSearchRow>
}
