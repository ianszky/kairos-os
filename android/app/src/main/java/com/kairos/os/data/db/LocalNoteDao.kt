package com.kairos.os.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalNoteDao {
    @Query("SELECT * FROM local_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): List<LocalNote>

    @Query("SELECT * FROM local_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    fun searchNotes(query: String): List<LocalNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: LocalNote): Long

    @Delete
    fun delete(note: LocalNote)
}
