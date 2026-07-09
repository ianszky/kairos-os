package com.kairos.os.data.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KairosApiClient @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        defaultRequest {
            url("http://192.168.100.132:3001/") // replace with real URL later
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun postPrompt(intent: String, appTarget: String?): com.kairos.os.domain.models.KairosResponse {
        val session = supabaseClient.auth.currentSessionOrNull()
        val token = session?.accessToken

        val response = client.post("api/prompt") {
            if (token != null) {
                bearerAuth(token)
            }
            setBody(
                mapOf(
                    "intent" to intent,
                    "appTarget" to appTarget
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
}
