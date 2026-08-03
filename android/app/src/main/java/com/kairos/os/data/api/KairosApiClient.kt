package com.kairos.os.data.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AttachmentInfo(
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long
)

@Serializable
data class PromptRequest(
    val intent: String,
    val appTarget: String? = null,
    val sessionId: String? = null,
    val attachments: List<AttachmentInfo>? = null,
    val currentDate: String? = null
)

@Serializable
data class LogIntentRequest(
    val appIdentifier: String,
    val appDisplayName: String,
    val reason: String,
    val timeLimitMinutes: Int,
    val aiApproved: Boolean
)

@Serializable
data class IntentLogResult(
    val logged: Boolean = false,
    val remainingMinutes: Int = 0,
    val budgetExceeded: Boolean = false,
    val message: String? = null
)

@Serializable
data class UserSettingsResponse(
    val dailyLeisureMinutes: Int = 60,
    val pendingLeisureMinutes: Int? = null,
    val pendingChangeEffectiveAt: String? = null,
    val todayUsedMinutes: Int? = null,
    val remainingLeisureMinutes: Int? = null
)

@Serializable
data class UpdateLeisureRequest(
    val dailyLeisureMinutes: Int
)

@Serializable
data class SettingsUpdateResult(
    val status: String = "PENDING",
    val message: String = "",
    val effectiveAt: String? = null,
    val settings: SettingsSnapshot? = null,
    val error: String? = null
)

@Serializable
data class SettingsSnapshot(
    val dailyLeisureMinutes: Int = 60,
    val pendingLeisureMinutes: Int? = null,
    val pendingChangeEffectiveAt: String? = null
)

@Serializable
data class AppConfigItemResponse(
    val appIdentifier: String,
    val category: String,
    val pendingCategory: String? = null,
    val pendingChangeEffectiveAt: String? = null,
    val intentGateEnabled: Boolean? = null
)

@Serializable
data class AppConfigsResponse(
    val configs: List<AppConfigItemResponse> = emptyList()
)

@Serializable
data class ToggleAppDistractingRequest(
    val appIdentifier: String,
    val isDistracting: Boolean
)

@Singleton
class KairosApiClient @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L
            connectTimeoutMillis = 30000L
            socketTimeoutMillis = 60000L
        }
        defaultRequest {
            url("http://192.168.100.132:3001/") // replace with real URL later
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun getPromptStatus(runId: String): com.kairos.os.domain.models.PromptRunStatusResponse {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        val response = client.get("api/prompt/status") {
            if (token != null) {
                bearerAuth(token)
            }
            parameter("runId", runId)
        }

        return response.body<com.kairos.os.domain.models.PromptRunStatusResponse>()
    }

    suspend fun postPromptAndAwait(
        intent: String,
        appTarget: String?,
        sessionId: String? = null,
        attachments: List<AttachmentInfo>? = null,
        currentDate: String? = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.getDefault()).format(Date()),
        pollIntervalMs: Long = 2000L,
        pollTimeoutMs: Long = 300000L
    ): com.kairos.os.domain.models.KairosResponse {
        val accepted = postPrompt(intent, appTarget, sessionId, attachments, currentDate)

        if (accepted.type == "ERROR") {
            return accepted
        }

        if (accepted.type != "ACCEPTED") {
            return accepted
        }

        val runId = accepted.meta?.runId
            ?: return com.kairos.os.domain.models.KairosResponse(
                type = "ERROR",
                text = "Server accepted prompt but did not return a runId."
            )

        val deadline = System.currentTimeMillis() + pollTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            val status = getPromptStatus(runId)
            when (status.status) {
                "completed" -> {
                    return status.response ?: com.kairos.os.domain.models.KairosResponse(
                        type = "ERROR",
                        text = "Prompt completed but no response payload was returned."
                    )
                }
                "failed" -> {
                    return status.response ?: com.kairos.os.domain.models.KairosResponse(
                        type = "ERROR",
                        text = status.error ?: "Prompt execution failed."
                    )
                }
            }
            delay(pollIntervalMs)
        }

        return com.kairos.os.domain.models.KairosResponse(
            type = "ERROR",
            text = "Prompt is still processing in the cloud. Reopen this conversation shortly to see the result."
        )
    }

    suspend fun postPrompt(
        intent: String, 
        appTarget: String?, 
        sessionId: String? = null,
        attachments: List<AttachmentInfo>? = null,
        currentDate: String? = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.getDefault()).format(Date())
    ): com.kairos.os.domain.models.KairosResponse {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        val response = client.post("api/prompt") {
            if (token != null) {
                bearerAuth(token)
            }
            setBody(
                PromptRequest(
                    intent = intent,
                    appTarget = appTarget,
                    sessionId = sessionId,
                    attachments = attachments,
                    currentDate = currentDate
                )
            )
        }

        val rawText = response.body<String>()

        // Try to deserialize as KairosResponse
        return try {
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<com.kairos.os.domain.models.KairosResponse>(rawText)
        } catch (e: Exception) {
            // If deserialization fails (e.g. backend returned a non-KairosResponse shape),
            // wrap it in a proper KairosResponse
            com.kairos.os.domain.models.KairosResponse(
                type = "ERROR",
                text = if (!response.status.isSuccess()) {
                    "Server error (${response.status.value}): $rawText"
                } else {
                    "Failed to parse server response: ${e.message}"
                }
            )
        }
    }

    suspend fun checkConnectionStatus(): Boolean {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken ?: return false

        try {
            val response = client.get("api/composio/connect/status") {
                bearerAuth(token)
            }
            if (!response.status.isSuccess()) return false
            val body = response.body<Map<String, Boolean>>()
            return body["connected"] ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun initiateConnection(): String? {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken ?: return null

        try {
            val response = client.get("api/composio/connect") {
                bearerAuth(token)
            }
            if (!response.status.isSuccess()) return null
            val body = response.body<Map<String, String>>()
            return body["connectUrl"]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun logIntent(
        appIdentifier: String,
        appDisplayName: String,
        reason: String,
        timeLimitMinutes: Int,
        aiApproved: Boolean
    ): IntentLogResult {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.post("api/intent/log") {
                if (token != null) bearerAuth(token)
                setBody(
                    LogIntentRequest(
                        appIdentifier = appIdentifier,
                        appDisplayName = appDisplayName,
                        reason = reason,
                        timeLimitMinutes = timeLimitMinutes,
                        aiApproved = aiApproved
                    )
                )
            }
            if (!response.status.isSuccess()) {
                IntentLogResult(
                    logged = false,
                    budgetExceeded = true,
                    message = "Could not verify leisure budget (server ${response.status.value})."
                )
            } else {
                response.body<IntentLogResult>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            IntentLogResult(
                logged = false,
                budgetExceeded = true,
                message = "Could not verify leisure budget. Check your connection."
            )
        }
    }

    suspend fun getUserSettings(): UserSettingsResponse? {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.get("api/settings") {
                if (token != null) bearerAuth(token)
            }
            if (!response.status.isSuccess()) {
                null
            } else {
                response.body<UserSettingsResponse>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateDailyLeisureTime(dailyLeisureMinutes: Int): SettingsUpdateResult {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.put("api/settings") {
                if (token != null) bearerAuth(token)
                setBody(UpdateLeisureRequest(dailyLeisureMinutes))
            }
            if (!response.status.isSuccess()) {
                SettingsUpdateResult(
                    status = "ERROR",
                    message = "Failed to save leisure limit (${response.status.value}).",
                    error = response.status.description
                )
            } else {
                response.body<SettingsUpdateResult>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SettingsUpdateResult(
                status = "ERROR",
                message = "Could not reach server. Leisure limit was not saved.",
                error = e.message
            )
        }
    }

    suspend fun getAppConfigs(): List<AppConfigItemResponse> {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.get("api/settings/apps") {
                if (token != null) bearerAuth(token)
            }
            response.body<AppConfigsResponse>().configs
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun toggleAppDistracting(appIdentifier: String, isDistracting: Boolean): SettingsUpdateResult {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.put("api/settings/apps") {
                if (token != null) bearerAuth(token)
                setBody(ToggleAppDistractingRequest(appIdentifier = appIdentifier, isDistracting = isDistracting))
            }
            response.body<SettingsUpdateResult>()
        } catch (e: Exception) {
            e.printStackTrace()
            SettingsUpdateResult(status = "APPLIED", message = "Updated locally")
        }
    }

    suspend fun getScheduledTasks(): List<com.kairos.os.domain.models.ScheduledTask> {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.get("api/scheduled") {
                if (token != null) bearerAuth(token)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createScheduledTask(
        request: com.kairos.os.domain.models.CreateScheduledTaskRequest
    ): com.kairos.os.domain.models.ScheduledTask? {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.post("api/scheduled") {
                if (token != null) bearerAuth(token)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateScheduledTask(
        request: com.kairos.os.domain.models.UpdateScheduledTaskRequest
    ): com.kairos.os.domain.models.ScheduledTask? {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.put("api/scheduled") {
                if (token != null) bearerAuth(token)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteScheduledTask(taskId: String): Boolean {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.delete("api/scheduled") {
                if (token != null) bearerAuth(token)
                parameter("id", taskId)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeScheduledTask(taskId: String): com.kairos.os.domain.models.KairosResponse {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.post("api/scheduled/execute") {
                if (token != null) bearerAuth(token)
                setBody(com.kairos.os.domain.models.ExecuteTaskRequest(taskId))
            }
            val rawText = response.body<String>()
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<com.kairos.os.domain.models.KairosResponse>(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            com.kairos.os.domain.models.KairosResponse(
                type = "ERROR",
                text = "Failed to execute scheduled task: ${e.message}"
            )
        }
    }

    suspend fun getScheduledTaskRuns(taskId: String? = null): List<com.kairos.os.domain.models.ScheduledTaskRun> {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        return try {
            val response = client.get("api/scheduled/runs") {
                if (token != null) bearerAuth(token)
                if (taskId != null) parameter("taskId", taskId)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

