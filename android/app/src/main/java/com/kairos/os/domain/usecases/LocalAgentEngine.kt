package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.kairos.os.domain.models.KairosResponse
import com.kairos.os.domain.models.WidgetPayload
import com.kairos.os.domain.models.WidgetItem
import com.kairos.os.domain.models.ResponseMeta
import com.kairos.os.domain.tools.LocalAlarmController
import com.kairos.os.domain.tools.LocalCalendarController
import com.kairos.os.domain.tools.LocalNotesController
import com.kairos.os.data.db.LocalConversationDao
import com.kairos.os.data.db.LocalConversationEntity
import com.kairos.os.data.db.LocalMessageDao
import com.kairos.os.data.db.LocalMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAgentEngine @Inject constructor(
    private val localLlmClient: LocalLlmClient,
    private val notesController: LocalNotesController,
    private val alarmController: LocalAlarmController,
    private val calendarController: LocalCalendarController,
    private val localConversationDao: LocalConversationDao,
    private val localMessageDao: LocalMessageDao
) {
    private val TAG = "LocalAgentEngine"

    enum class Classification {
        SIMPLE,
        LOCAL_AGENT,
        CLOUD_AGENT
    }

    suspend fun execute(
        prompt: String,
        appTarget: String?,
        conversationId: String,
        userId: String
    ): KairosResponse {
        Log.i(TAG, "Executing local agent pipeline for prompt: '$prompt'")

        // 1. Classification
        val classification = classifyPrompt(prompt, appTarget)
        Log.i(TAG, "Prompt classification result: $classification")

        if (classification == Classification.CLOUD_AGENT) {
            return KairosResponse(type = "CLOUD_FALLBACK")
        }

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        // 2. Ensure Local Conversation Entity exists in Room DB
        withContext(Dispatchers.IO) {
            val existing = localConversationDao.getConversationById(conversationId)
            if (existing == null) {
                localConversationDao.insertOrUpdate(
                    LocalConversationEntity(
                        id = conversationId,
                        userId = userId,
                        title = "New Conversation",
                        createdAt = nowIso,
                        updatedAt = nowIso,
                        isActive = true
                    )
                )
            } else {
                localConversationDao.insertOrUpdate(existing.copy(updatedAt = nowIso))
            }
        }

        // 3. Save User Message locally to Room DB
        insertLocalMessage(conversationId, "user", prompt, appTarget)

        val response = when (classification) {
            Classification.SIMPLE -> handleSimplePrompt(prompt)
            Classification.LOCAL_AGENT -> handleLocalAgentPrompt(prompt)
            else -> KairosResponse(type = "ERROR", text = "Unknown classification")
        }

        // 4. Save Assistant Message locally to Room DB
        insertLocalMessage(
            conversationId = conversationId,
            role = "assistant",
            content = response.text ?: "",
            appTarget = appTarget,
            widgetPayload = response.widget
        )

        return response.copy(
            meta = ResponseMeta(
                conversationId = conversationId,
                timestamp = nowIso,
                model = "Gemma 4 (Local)"
            )
        )
    }

    private suspend fun classifyPrompt(prompt: String, appTarget: String?): Classification {
        val lowerPrompt = prompt.lowercase()
        val localTargets = listOf("kai", "kairos", "kainotes", "notes", "kaicalendar", "calendar", "kaiclock", "clock", "alarm")

        if (appTarget != null && localTargets.contains(appTarget.lowercase())) {
            return Classification.LOCAL_AGENT
        }
        if (appTarget != null && !localTargets.contains(appTarget.lowercase())) {
            return Classification.CLOUD_AGENT
        }
        
        if (prompt.contains("@gmail") || prompt.contains("@googlecalendar") || 
            prompt.contains("@spotify") || prompt.contains("@github") || 
            prompt.contains("@notion") || prompt.contains("@googlesheets") || 
            prompt.contains("@googledrive")
        ) {
            return Classification.CLOUD_AGENT
        }

        if (localTargets.any { lowerPrompt.contains("@$it") }) {
            return Classification.LOCAL_AGENT
        }

        val engine = localLlmClient.getEngine() ?: return Classification.CLOUD_AGENT

        val classificationPrompt = """
            You are the KAIROS OS local intent classifier. Analyze the user request.
            Categorize the request into exactly one of these tiers:
            1. SIMPLE - general conversational inputs, trivia, direct questions, or greetings (e.g. "hi", "how are you", "what is photosynthesis", "tell me a joke"). There are no app mentions, and no actions like setting alarms, calendar events, or notes are required.
            2. LOCAL_AGENT - commands specifically relating to notes, alarms, clock, or calendars (e.g. "set alarm for 6am", "create a shopping list note", "what are my plans today", "add doctor meeting tomorrow at 3pm to calendar").
            3. CLOUD_AGENT - tasks requiring cloud apps, emails, search, spotify, documents, sheets, slack, or explicit cloud app tags (e.g. "send an email to my boss", "play lo-fi music on spotify", "@notion summarize project").

            User Request: "$prompt"

            Return ONLY one word: SIMPLE, LOCAL_AGENT, or CLOUD_AGENT. Do not write any other explanation or text.
        """.trimIndent()

        return try {
            val responseText = withContext(Dispatchers.IO) {
                val conversation = engine.createConversation()
                try {
                    val response = conversation.sendMessage(classificationPrompt)
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("\n") { it.text }
                        .trim().uppercase()
                } finally {
                    conversation.close()
                }
            }
            Log.i(TAG, "Classifier raw output: '$responseText'")
            
            if (responseText.contains("LOCAL_AGENT") || localTargets.any { lowerPrompt.contains("@$it") }) {
                Classification.LOCAL_AGENT
            } else if (responseText.contains("SIMPLE")) {
                Classification.SIMPLE
            } else {
                Classification.CLOUD_AGENT
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in classification LLM execution", e)
            Classification.CLOUD_AGENT
        }
    }

    private suspend fun handleSimplePrompt(prompt: String): KairosResponse {
        val engine = localLlmClient.getEngine() 
            ?: return KairosResponse(type = "TEXT", text = "LiteRT-LM local engine not available.")

        return try {
            val responseText = withContext(Dispatchers.IO) {
                val conversation = engine.createConversation()
                try {
                    val response = conversation.sendMessage(prompt)
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("\n") { it.text }
                } finally {
                    conversation.close()
                }
            }
            KairosResponse(type = "TEXT", text = responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error running local simple prompt", e)
            KairosResponse(type = "TEXT", text = "Error running local model: ${e.message}")
        }
    }

    private suspend fun handleLocalAgentPrompt(prompt: String): KairosResponse {
        val engine = localLlmClient.getEngine()
            ?: return KairosResponse(type = "TEXT", text = "LiteRT-LM local engine not available.")

        val currentDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.getDefault()).format(Date())

        val existingNotes = notesController.getAllNotes()
        val notesSummary = if (existingNotes.isEmpty()) {
            "No notes currently stored."
        } else {
            existingNotes.joinToString("\n") { note ->
                "[ID: ${note.id}] Title: \"${note.title}\" | Content: \"${note.content.take(150)}\""
            }
        }

        val systemPrompt = """
            You are the local KAIROS OS agent. You execute actions on the phone using Notes, Alarms, Timers, and Calendar tools.
            Current date and time: $currentDateStr

            Existing Notes in Database:
            $notesSummary

            Available Tools:
            1. create_note(title: String, content: String)
            2. list_notes()
            3. search_notes(query: String)
               - Performs a title/content query or retrieves notes matching the user's intent.
            4. get_note_by_id(id: Int)
               - Use this tool when one of the Existing Notes above matches what the user is looking for (even if exact words differ).
            5. update_note(id: Int, title: String, content: String)
            6. delete_note(id: Int)
            7. set_alarm(hour: Int, minute: Int, label: String)
            8. list_alarms()
            9. cancel_alarm(id: Int)
            10. start_timer(hours: Int, minutes: Int, seconds: Int, label: String)
               - Starts a countdown timer for specified hours, minutes, and seconds
            11. cancel_timer()
            12. create_calendar_event(title: String, description: String, start_date: String, start_time: String, end_date: String, end_time: String, is_all_day: Boolean, sync_google: Boolean)
                - start_date & end_date format: YYYY-MM-DD
                - start_time & end_time format: HH:MM (ignored if is_all_day is true)
                - is_all_day: boolean
                - sync_google: boolean (default true)
            13. list_calendar_events()
            14. list_calendar_events_range(start_date: String, end_date: String)
            15. delete_calendar_event(id: Long)

            Analyze the user command: "$prompt"
            Respond strictly with a JSON object. No markdown styling, no ```json formatting.
            
            JSON schema:
            {
              "tool": "tool_name",
              "args": { ... arguments key-value map ... }
            }
        """.trimIndent()

        try {
            val responseText = withContext(Dispatchers.IO) {
                val conversation = engine.createConversation()
                try {
                    val response = conversation.sendMessage(systemPrompt)
                    response.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("\n") { it.text }
                } finally {
                    conversation.close()
                }
            }

            Log.i(TAG, "Local Agent tool JSON response: '$responseText'")

            val cleanJson = if (responseText.startsWith("```json")) {
                responseText.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (responseText.startsWith("```")) {
                responseText.substringAfter("```").substringBeforeLast("```").trim()
            } else {
                responseText
            }

            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(cleanJson).jsonObject
            val toolName = root["tool"]?.jsonPrimitive?.content ?: ""
            val args = root["args"]?.jsonObject

            return when (toolName) {
                "create_note" -> {
                    val title = args?.get("title")?.jsonPrimitive?.content ?: "Untitled"
                    val content = args?.get("content")?.jsonPrimitive?.content ?: ""
                    val note = notesController.createNote(title, content)
                    KairosResponse(
                        type = "WIDGET",
                        text = "Successfully created note: '$title'",
                        widget = WidgetPayload(
                            widgetType = "NOTE_CARD",
                            title = "Note Created",
                            items = listOf(WidgetItem(id = note.id.toString(), primary = note.title, secondary = note.content, icon = "note"))
                        )
                    )
                }
                "list_notes" -> {
                    val notes = notesController.getAllNotes()
                    KairosResponse(
                        type = "WIDGET",
                        text = "Here are your local notes.",
                        widget = WidgetPayload(
                            widgetType = "NOTE_CARD",
                            title = "Local Notes (${notes.size})",
                            items = notes.map { WidgetItem(id = it.id.toString(), primary = it.title, secondary = it.content, icon = "note") }
                        )
                    )
                }
                "get_note_by_id" -> {
                    val id = args?.get("id")?.jsonPrimitive?.int ?: 0
                    val note = notesController.getNoteById(id)
                    if (note != null) {
                        KairosResponse(
                            type = "WIDGET",
                            text = "Found note: '${note.title}'",
                            widget = WidgetPayload(
                                widgetType = "NOTE_CARD",
                                title = "Note Found",
                                items = listOf(WidgetItem(id = note.id.toString(), primary = note.title, secondary = note.content, icon = "note"))
                            )
                        )
                    } else {
                        KairosResponse(type = "TEXT", text = "Note #$id not found.")
                    }
                }
                "search_notes" -> {
                    val query = args?.get("query")?.jsonPrimitive?.content ?: ""
                    val idArg = args?.get("id")?.jsonPrimitive?.intOrNull
                    var notes = notesController.searchNotes(query)

                    // Semantic fallback: If SQL query returned empty, check if ID was passed or match candidate notes by title/content
                    if (notes.isEmpty() && idArg != null) {
                        val matchedById = notesController.getNoteById(idArg)
                        if (matchedById != null) notes = listOf(matchedById)
                    }

                    if (notes.isEmpty() && query.isNotBlank()) {
                        // Soft matching against existing notes loaded in memory
                        notes = existingNotes.filter { n ->
                            n.title.contains(query, ignoreCase = true) ||
                            n.content.contains(query, ignoreCase = true) ||
                            query.split(" ").any { word -> word.length > 3 && (n.title.contains(word, ignoreCase = true) || n.content.contains(word, ignoreCase = true)) }
                        }
                    }

                    KairosResponse(
                        type = "WIDGET",
                        text = if (notes.isNotEmpty()) "Found ${notes.size} note(s) matching '$query'." else "No notes found matching '$query'.",
                        widget = WidgetPayload(
                            widgetType = "NOTE_CARD",
                            title = "Search Notes: '$query'",
                            items = notes.map { WidgetItem(id = it.id.toString(), primary = it.title, secondary = it.content, icon = "note") }
                        )
                    )
                }
                "update_note" -> {
                    val id = args?.get("id")?.jsonPrimitive?.int ?: 0
                    val title = args?.get("title")?.jsonPrimitive?.content ?: "Untitled"
                    val content = args?.get("content")?.jsonPrimitive?.content ?: ""
                    val note = notesController.updateNote(id, title, content)
                    KairosResponse(
                        type = "WIDGET",
                        text = "Updated note: '$title'",
                        widget = WidgetPayload(
                            widgetType = "NOTE_CARD",
                            title = "Note Updated",
                            items = listOf(WidgetItem(id = note.id.toString(), primary = note.title, secondary = note.content, icon = "note"))
                        )
                    )
                }
                "delete_note" -> {
                    val id = args?.get("id")?.jsonPrimitive?.int ?: 0
                    val deleted = notesController.deleteNote(id)
                    KairosResponse(
                        type = "TEXT",
                        text = if (deleted) "Deleted note #$id." else "Note #$id not found."
                    )
                }
                "set_alarm" -> {
                    val hour = args?.get("hour")?.jsonPrimitive?.int ?: 8
                    val minute = args?.get("minute")?.jsonPrimitive?.int ?: 0
                    val label = args?.get("label")?.jsonPrimitive?.content ?: "Alarm"
                    val alarm = alarmController.setAlarm(hour, minute, label)
                    KairosResponse(
                        type = "WIDGET",
                        text = "Set alarm for ${String.format("%02d:%02d", hour, minute)} with label '$label'",
                        widget = WidgetPayload(
                            widgetType = "ALARM_CONFIRM",
                            title = "Alarm Scheduled",
                            items = listOf(WidgetItem(id = alarm.id.toString(), primary = String.format("%02d:%02d", hour, minute), secondary = label, icon = "alarm"))
                        )
                    )
                }
                "list_alarms" -> {
                    val alarms = alarmController.getAllAlarms()
                    KairosResponse(
                        type = "WIDGET",
                        text = "Here are your local alarms.",
                        widget = WidgetPayload(
                            widgetType = "ALARM_CONFIRM",
                            title = "Local Alarms",
                            items = alarms.map { WidgetItem(id = it.id.toString(), primary = String.format("%02d:%02d", it.hour, it.minute), secondary = it.label, icon = "alarm") }
                        )
                    )
                }
                "cancel_alarm" -> {
                    val id = args?.get("id")?.jsonPrimitive?.int ?: 0
                    val success = alarmController.cancelAlarm(id)
                    KairosResponse(
                        type = "TEXT",
                        text = if (success) "Cancelled alarm #$id." else "Alarm #$id not found."
                    )
                }
                "start_timer" -> {
                    val hours = args?.get("hours")?.jsonPrimitive?.int ?: 0
                    val minutes = args?.get("minutes")?.jsonPrimitive?.int ?: 0
                    val seconds = args?.get("seconds")?.jsonPrimitive?.int ?: 0
                    val label = args?.get("label")?.jsonPrimitive?.content ?: "Timer"
                    val totalDurationMs = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

                    if (totalDurationMs > 0) {
                        alarmController.startTimer(totalDurationMs, label)
                        val timeDesc = buildString {
                            if (hours > 0) append("${hours}h ")
                            if (minutes > 0) append("${minutes}m ")
                            if (seconds > 0 || isEmpty()) append("${seconds}s")
                        }.trim()
                        KairosResponse(
                            type = "WIDGET",
                            text = "Started timer for $timeDesc ($label).",
                            widget = WidgetPayload(
                                widgetType = "TIMER_CONFIRM",
                                title = "Timer Started",
                                items = listOf(WidgetItem(id = "timer", primary = timeDesc, secondary = label, icon = "timer")),
                                actions = listOf(
                                    com.kairos.os.domain.models.WidgetAction(
                                        label = "Open Clock",
                                        actionType = "INTENT",
                                        target = "android.intent.action.SHOW_TIMERS"
                                    )
                                )
                            )
                        )
                    } else {
                        KairosResponse(type = "TEXT", text = "Invalid timer duration specified.")
                    }
                }
                "cancel_timer" -> {
                    alarmController.cancelTimer()
                    KairosResponse(type = "TEXT", text = "Timer cancelled.")
                }
                "create_calendar_event" -> {
                    val title = args?.get("title")?.jsonPrimitive?.content ?: "Meeting"
                    val description = args?.get("description")?.jsonPrimitive?.content ?: ""
                    val startDateStr = args?.get("start_date")?.jsonPrimitive?.content ?: ""
                    val startTimeStr = args?.get("start_time")?.jsonPrimitive?.content ?: "09:00"
                    val endDateStr = args?.get("end_date")?.jsonPrimitive?.content ?: startDateStr
                    val endTimeStr = args?.get("end_time")?.jsonPrimitive?.content ?: "10:00"
                    val isAllDay = args?.get("is_all_day")?.jsonPrimitive?.booleanOrNull ?: false
                    val syncGoogle = args?.get("sync_google")?.jsonPrimitive?.booleanOrNull ?: true

                    val startCal = Calendar.getInstance()
                    val endCal = Calendar.getInstance()
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val sDate = format.parse("$startDateStr $startTimeStr")
                        if (sDate != null) startCal.time = sDate

                        val eDate = format.parse("$endDateStr $endTimeStr")
                        if (eDate != null) endCal.time = eDate
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing calendar date/time", e)
                    }

                    val eventId = calendarController.createEvent(
                        title = title,
                        description = description,
                        startMillis = startCal.timeInMillis,
                        endMillis = if (endCal.timeInMillis > startCal.timeInMillis) endCal.timeInMillis else startCal.timeInMillis + 3600000L,
                        isAllDay = isAllDay,
                        syncGoogle = syncGoogle
                    )
                    if (eventId != null) {
                        KairosResponse(
                            type = "WIDGET",
                            text = "Scheduled calendar event: '$title'",
                            widget = WidgetPayload(
                                widgetType = "CALENDAR_EVENT",
                                title = "Event Scheduled",
                                items = listOf(WidgetItem(id = eventId.toString(), primary = title, secondary = if (isAllDay) "$startDateStr (All Day)" else "$startDateStr $startTimeStr - $endTimeStr", icon = "calendar"))
                            )
                        )
                    } else {
                        KairosResponse(type = "TEXT", text = "Failed to schedule calendar event due to permission or system error.")
                    }
                }
                "list_calendar_events" -> {
                    val startOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                    val endOfDay = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.timeInMillis
                    val events = calendarController.listEvents(startOfDay, endOfDay)
                    KairosResponse(
                        type = "WIDGET",
                        text = "Here is your schedule for today.",
                        widget = WidgetPayload(
                            widgetType = "CALENDAR_EVENT",
                            title = "Today's Agenda (${events.size})",
                            items = events.map { 
                                WidgetItem(
                                    id = it.id.toString(),
                                    primary = it.title,
                                    secondary = buildString {
                                        append(it.description)
                                        if (!it.accountName.isNullOrBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append(it.accountName)
                                        }
                                    },
                                    icon = "calendar"
                                )
                            }
                        )
                    )
                }
                "list_calendar_events_range" -> {
                    val startDateStr = args?.get("start_date")?.jsonPrimitive?.content ?: ""
                    val endDateStr = args?.get("end_date")?.jsonPrimitive?.content ?: ""
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    val startMillis = try { dateFormat.parse(startDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    val endMillis = try { 
                        val parsed = dateFormat.parse(endDateStr)
                        if (parsed != null) parsed.time + 86399000L else System.currentTimeMillis() + (7 * 86400000L)
                    } catch (e: Exception) { System.currentTimeMillis() + (7 * 86400000L) }

                    val events = calendarController.listEvents(startMillis, endMillis)
                    KairosResponse(
                        type = "WIDGET",
                        text = "Events from $startDateStr to $endDateStr (${events.size} total).",
                        widget = WidgetPayload(
                            widgetType = "CALENDAR_EVENT",
                            title = "Agenda Range",
                            items = events.map { 
                                WidgetItem(
                                    id = it.id.toString(),
                                    primary = it.title,
                                    secondary = buildString {
                                        append(it.description)
                                        if (!it.accountName.isNullOrBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append(it.accountName)
                                        }
                                    },
                                    icon = "calendar"
                                )
                            }
                        )
                    )
                }
                "delete_calendar_event" -> {
                    val id = args?.get("id")?.jsonPrimitive?.long ?: 0L
                    val deleted = calendarController.deleteEvent(id)
                    KairosResponse(
                        type = "TEXT",
                        text = if (deleted) "Deleted calendar event #$id." else "Calendar event #$id not found."
                    )
                }
                else -> {
                    handleLocalRuleFallback(prompt)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error executing local agent prompt. Falling back to rule parser.", e)
            return handleLocalRuleFallback(prompt)
        }
    }

    private suspend fun handleLocalRuleFallback(prompt: String): KairosResponse {
        val lower = prompt.lowercase()
        return when {
            lower.contains("timer") || lower.contains("countdown") -> {
                val regex = Regex("(\\d+)\\s*(hour|hr|minute|min|second|sec)")
                val matches = regex.findAll(lower).toList()
                var totalSec = 0L
                for (match in matches) {
                    val valNum = match.groupValues[1].toLongOrNull() ?: 0L
                    val unit = match.groupValues[2]
                    if (unit.startsWith("h")) totalSec += valNum * 3600
                    else if (unit.startsWith("m")) totalSec += valNum * 60
                    else if (unit.startsWith("s")) totalSec += valNum
                }
                if (totalSec == 0L) totalSec = 300L // Default 5 mins

                alarmController.startTimer(totalSec * 1000L, "Timer")
                KairosResponse(
                    type = "WIDGET",
                    text = "[Rule Fallback] Started timer for ${totalSec / 60}m ${totalSec % 60}s.",
                    widget = WidgetPayload(
                        widgetType = "TIMER_CONFIRM",
                        title = "Timer Started",
                        items = listOf(WidgetItem(id = "timer", primary = "${totalSec / 60}m ${totalSec % 60}s", secondary = "Timer", icon = "timer")),
                        actions = listOf(
                            com.kairos.os.domain.models.WidgetAction(
                                label = "Open Clock",
                                actionType = "INTENT",
                                target = "android.intent.action.SHOW_TIMERS"
                            )
                        )
                    )
                )
            }
            lower.contains("alarm") || lower.contains("clock") || lower.contains("kaiclock") -> {
                val regex = Regex("(\\d{1,2})\\s*(am|pm|:(\\d{2}))")
                val match = regex.find(lower)
                var hour = 8
                var minute = 0
                if (match != null) {
                    val rawHour = match.groupValues[1].toInt()
                    val ampm = match.groupValues[2]
                    val colonMin = match.groupValues[3]
                    hour = rawHour
                    if (ampm.contains("pm") && hour < 12) hour += 12
                    if (ampm.contains("am") && hour == 12) hour = 0
                    if (colonMin.isNotBlank()) minute = colonMin.toInt()
                }
                val alarm = alarmController.setAlarm(hour, minute, "Kairos Alarm")
                KairosResponse(
                    type = "WIDGET",
                    text = "[Rule Fallback] Set alarm for ${String.format("%02d:%02d", hour, minute)}",
                    widget = WidgetPayload(
                        widgetType = "ALARM_CONFIRM",
                        title = "Alarm Scheduled",
                        items = listOf(WidgetItem(id = alarm.id.toString(), primary = String.format("%02d:%02d", hour, minute), secondary = "Kairos Alarm", icon = "alarm"))
                    )
                )
            }
            lower.contains("note") || lower.contains("kainotes") -> {
                val content = prompt.substringAfter("kainotes").substringAfter("note").trim()
                val note = notesController.createNote("Quick Note", if (content.isBlank()) prompt else content)
                KairosResponse(
                    type = "WIDGET",
                    text = "[Rule Fallback] Saved local note.",
                    widget = WidgetPayload(
                        widgetType = "NOTE_CARD",
                        title = "Note Created",
                        items = listOf(WidgetItem(id = note.id.toString(), primary = note.title, secondary = note.content, icon = "note"))
                    )
                )
            }
            lower.contains("calendar") || lower.contains("kaicalendar") || lower.contains("event") || lower.contains("meeting") -> {
                val title = prompt.substringAfter("kaicalendar").substringAfter("calendar").substringAfter("event").substringAfter("meeting").trim().ifEmpty { "New Event" }
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR, 1)
                val eventId = calendarController.createEvent(title, "Local Event", calendar.timeInMillis, 60)
                if (eventId != null) {
                    KairosResponse(
                        type = "WIDGET",
                        text = "[Rule Fallback] Scheduled calendar event: '$title'",
                        widget = WidgetPayload(
                            widgetType = "CALENDAR_EVENT",
                            title = "Event Scheduled",
                            items = listOf(WidgetItem(id = eventId.toString(), primary = title, secondary = "Scheduled via rule fallback", icon = "calendar"))
                        )
                    )
                } else {
                    KairosResponse(type = "TEXT", text = "Failed to schedule calendar event due to permission or system error.")
                }
            }
            else -> {
                KairosResponse(type = "CLOUD_FALLBACK")
            }
        }
    }

    private suspend fun insertLocalMessage(
        conversationId: String,
        role: String,
        content: String,
        appTarget: String?,
        widgetPayload: WidgetPayload? = null
    ) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        val widgetJson = widgetPayload?.let {
            Json.encodeToString(WidgetPayload.serializer(), it)
        }
        val entity = LocalMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            appTarget = appTarget,
            modelTier = "gemma-local",
            widgetPayloadJson = widgetJson,
            createdAt = nowIso
        )
        withContext(Dispatchers.IO) {
            try {
                localMessageDao.insertMessage(entity)
                Log.d(TAG, "Saved $role message locally into Room database.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert $role message into Room database", e)
            }
        }
    }
}
