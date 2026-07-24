package com.kairos.os.domain.tools

import android.content.ContentUris
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
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE
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
                val accountTypeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val visibleCol = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                var googleCalId: Long? = null
                var primaryCalId: Long? = null
                var firstVisibleId: Long? = null

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountType = if (accountTypeCol != -1) cursor.getString(accountTypeCol) else null
                    val isPrimary = primaryCol != -1 && cursor.getInt(primaryCol) == 1
                    val isVisible = visibleCol != -1 && cursor.getInt(visibleCol) == 1

                    if (accountType == "com.google" && isPrimary) return id
                    if (accountType == "com.google" && googleCalId == null) googleCalId = id
                    if (isPrimary && primaryCalId == null) primaryCalId = id
                    if (isVisible && firstVisibleId == null) firstVisibleId = id
                }

                return googleCalId ?: primaryCalId ?: firstVisibleId ?: 1L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1L
    }

    fun createEvent(title: String, description: String, startMillis: Long, durationMinutes: Int): Long? {
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
            uri?.let { ContentUris.parseId(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.ACCOUNT_NAME
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.VISIBLE} = ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString(), "1")
        
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
                val calNameCol = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
                val accountCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME)
                
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: "No Title",
                            description = cursor.getString(descCol) ?: "",
                            startMillis = cursor.getLong(startCol),
                            endMillis = cursor.getLong(endCol),
                            calendarName = if (calNameCol != -1) cursor.getString(calNameCol) else null,
                            accountName = if (accountCol != -1) cursor.getString(accountCol) else null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    fun deleteEvent(eventId: Long): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startMillis: Long,
    val endMillis: Long,
    val calendarName: String? = null,
    val accountName: String? = null
)
