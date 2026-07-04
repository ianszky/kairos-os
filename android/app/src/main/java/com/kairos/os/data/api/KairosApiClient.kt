package com.kairos.os.data.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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

    suspend fun postPrompt(intent: String, appTarget: String?): String {
        // Fetch current valid session token from Supabase Auth
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
        
        // This MVP just returns string body, adjust as needed
        // val responseBody = response.body<KairosResponse>() 
        return response.status.toString()
    }
}
