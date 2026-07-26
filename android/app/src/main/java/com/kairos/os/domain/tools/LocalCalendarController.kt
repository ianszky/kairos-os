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
    private fun getCalendarId(preferGoogle: Boolean = true): Long {
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
                var localCalId: Long? = null
                var primaryCalId: Long? = null
                var firstVisibleId: Long? = null

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountType = if (accountTypeCol != -1) cursor.getString(accountTypeCol) else null
                    val isPrimary = primaryCol != -1 && cursor.getInt(primaryCol) == 1
                    val isVisible = visibleCol != -1 && cursor.getInt(visibleCol) == 1

                    if (accountType == "com.google") {
                        if (isPrimary && preferGoogle) return id
                        if (googleCalId == null) googleCalId = id
                    } else {
                        if (localCalId == null) localCalId = id
                    }
                    if (isPrimary && primaryCalId == null) primaryCalId = id
                    if (isVisible && firstVisibleId == null) firstVisibleId = id
                }

                return if (preferGoogle) {
                    googleCalId ?: primaryCalId ?: firstVisibleId ?: 1L
                } else {
                    localCalId ?: primaryCalId ?: firstVisibleId ?: 1L
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 1L
    }

    fun createEvent(title: String, description: String, startMillis: Long, durationMinutes: Int): Long? {
        val endMillis = startMillis + (durationMinutes * 60 * 1000L)
        return createEvent(title, description, startMillis, endMillis, isAllDay = false, syncGoogle = true)
    }

    fun createEvent(
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean = false,
        syncGoogle: Boolean = true
    ): Long? {
        return try {
            val calendarId = getCalendarId(preferGoogle = syncGoogle)
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, if (endMillis > startMillis) endMillis else startMillis + 3600000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                if (isAllDay) {
                    put(CalendarContract.Events.ALL_DAY, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                } else {
                    put(CalendarContract.Events.ALL_DAY, 0)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
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
            CalendarContract.Events.ALL_DAY,
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
                val allDayCol = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val calNameCol = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)
                val accountCol = cursor.getColumnIndex(CalendarContract.Events.ACCOUNT_NAME)
                
                while (cursor.moveToNext()) {
                    val isAllDay = allDayCol != -1 && cursor.getInt(allDayCol) == 1
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: "No Title",
                            description = cursor.getString(descCol) ?: "",
                            startMillis = cursor.getLong(startCol),
                            endMillis = cursor.getLong(endCol),
                            isAllDay = isAllDay,
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
    val isAllDay: Boolean = false,
    val calendarName: String? = null,
    val accountName: String? = null
)
