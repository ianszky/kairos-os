package com.kairos.os.domain.tools

import com.kairos.os.data.db.LocalNote
import com.kairos.os.data.db.LocalNoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotesController @Inject constructor(
    private val localNoteDao: LocalNoteDao
) {
    suspend fun createNote(title: String, content: String): LocalNote = withContext(Dispatchers.IO) {
        val note = LocalNote(title = title, content = content)
        val id = localNoteDao.insert(note)
        note.copy(id = id.toInt())
    }

    suspend fun updateNote(id: Int, title: String, content: String): LocalNote = withContext(Dispatchers.IO) {
        val note = LocalNote(id = id, title = title, content = content)
        localNoteDao.insert(note)
        note
    }

    suspend fun getAllNotes(): List<LocalNote> = withContext(Dispatchers.IO) {
        localNoteDao.getAllNotes()
    }

    suspend fun searchNotes(query: String): List<LocalNote> = withContext(Dispatchers.IO) {
        localNoteDao.searchNotes(query)
    }

    suspend fun getNoteById(id: Int): LocalNote? = withContext(Dispatchers.IO) {
        localNoteDao.getAllNotes().find { it.id == id }
    }

    suspend fun deleteNote(id: Int): Boolean = withContext(Dispatchers.IO) {
        val notes = localNoteDao.getAllNotes()
        val note = notes.find { it.id == id }
        if (note != null) {
            localNoteDao.delete(note)
            true
        } else {
            false
        }
    }
}
