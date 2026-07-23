package com.kairos.os.domain.tools

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCalendarController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getPrimaryCalendarId(): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryCol = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                while (cursor.moveToNext()) {
                    if (primaryCol != -1 && cursor.getInt(primaryCol) == 1) {
                        return cursor.getLong(idCol)
                    }
                }
                if (cursor.moveToFirst()) {
                    return cursor.getLong(idCol)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1L
    }

    fun createEvent(title: String, description: String, startMillis: Long, durationMinutes: Int): Boolean {
        return try {
            val calendarId = getPrimaryCalendarId()
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + (durationMinutes * 60 * 1000))
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
        
        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleCol = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val descCol = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startCol = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endCol = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: "No Title",
                            description = cursor.getString(descCol) ?: "",
                            startMillis = cursor.getLong(startCol),
                            endMillis = cursor.getLong(endCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long
)
