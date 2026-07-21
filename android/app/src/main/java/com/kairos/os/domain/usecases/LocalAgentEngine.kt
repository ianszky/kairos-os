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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MessageInsert(
    @SerialName("conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @SerialName("app_target") val appTarget: String?,
    @SerialName("model_tier") val modelTier: String?,
    @SerialName("widget_payload") val widgetPayload: WidgetPayload?
)

@Singleton
class LocalAgentEngine @Inject constructor(
    private val localLlmClient: LocalLlmClient,
    private val notesController: LocalNotesController,
    private val alarmController: LocalAlarmController,
    private val calendarController: LocalCalendarController,
    private val supabaseClient: SupabaseClient
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

        // 2. Insert User Message to Supabase
        insertMessage(conversationId, "user", prompt, appTarget)

        val response = when (classification) {
            Classification.SIMPLE -> handleSimplePrompt(prompt)
            Classification.LOCAL_AGENT -> handleLocalAgentPrompt(prompt)
            else -> KairosResponse(type = "ERROR", text = "Unknown classification")
        }

        // 3. Insert Assistant Response to Supabase
        insertMessage(
            conversationId = conversationId,
            role = "assistant",
            content = response.text ?: "",
            appTarget = appTarget,
            widgetPayload = response.widget
        )

        return response.copy(
            meta = ResponseMeta(
                conversationId = conversationId,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
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

        val systemPrompt = """
            You are the local KAIROS OS agent. You execute actions on the phone using Notes, Alarms, and Calendar tools.
            Available Tools:
            1. create_note(title: String, content: String)
            2. list_notes()
            3. set_alarm(hour: Int, minute: Int, label: String)
            4. list_alarms()
            5. create_calendar_event(title: String, description: String, start_date: String, start_time: String, duration_minutes: Int)
               - Format start_date as YYYY-MM-DD
               - Format start_time as HH:MM
            6. list_calendar_events()

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
                "create_calendar_event" -> {
                    val title = args?.get("title")?.jsonPrimitive?.content ?: "Meeting"
                    val description = args?.get("description")?.jsonPrimitive?.content ?: ""
                    val startDateStr = args?.get("start_date")?.jsonPrimitive?.content ?: ""
                    val startTimeStr = args?.get("start_time")?.jsonPrimitive?.content ?: ""
                    val duration = args?.get("duration_minutes")?.jsonPrimitive?.int ?: 60

                    val calendar = Calendar.getInstance()
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val date = format.parse("$startDateStr $startTimeStr")
                        if (date != null) calendar.time = date
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing calendar date/time", e)
                    }

                    val success = calendarController.createEvent(title, description, calendar.timeInMillis, duration)
                    if (success) {
                        KairosResponse(
                            type = "WIDGET",
                            text = "Scheduled calendar event: '$title'",
                            widget = WidgetPayload(
                                widgetType = "CALENDAR_EVENT",
                                title = "Event Scheduled",
                                items = listOf(WidgetItem(id = "cal_event", primary = title, secondary = "$startDateStr at $startTimeStr ($duration min)", icon = "calendar"))
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
                            title = "Today's Agenda",
                            items = events.map { WidgetItem(id = it.id.toString(), primary = it.title, secondary = it.description, icon = "calendar") }
                        )
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
                val success = calendarController.createEvent(title, "Local Event", calendar.timeInMillis, 60)
                if (success) {
                    KairosResponse(
                        type = "WIDGET",
                        text = "[Rule Fallback] Scheduled calendar event: '$title'",
                        widget = WidgetPayload(
                            widgetType = "CALENDAR_EVENT",
                            title = "Event Scheduled",
                            items = listOf(WidgetItem(id = "cal_event", primary = title, secondary = "Scheduled via rule fallback", icon = "calendar"))
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

    private fun isValidUuid(uuidStr: String): Boolean {
        return try {
            java.util.UUID.fromString(uuidStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun insertMessage(
        conversationId: String,
        role: String,
        content: String,
        appTarget: String?,
        widgetPayload: WidgetPayload? = null
    ) {
        if (!isValidUuid(conversationId)) {
            Log.w(TAG, "Skipping Supabase message insert: conversationId '$conversationId' is not a valid UUID.")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val insertObj = MessageInsert(
                    conversationId = conversationId,
                    role = role,
                    content = content,
                    appTarget = appTarget,
                    modelTier = "gemma-local",
                    widgetPayload = widgetPayload
                )
                supabaseClient.postgrest["messages"].insert(insertObj)
                Log.d(TAG, "Saved $role message locally to Supabase database.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert $role message into Supabase database", e)
            }
        }
    }
}
