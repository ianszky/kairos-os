package com.kairos.os.domain.usecases

import android.app.Notification
import android.util.Log
import com.google.ai.edge.litertlm.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ClassificationTier {
    CRITICAL,
    DIGEST
}

@Singleton
class LocalNotificationClassifier @Inject constructor(
    private val localLlmClient: LocalLlmClient
) {
    private val TAG = "LocalNotificationClassifier"

    private val criticalKeywords = listOf(
        "urgent", "emergency", "asap", "hospital", "doctor",
        "accident", "police", "help", "otp", "verification",
        "code", "alert", "security", "warning", "meeting"
    )

    private val digestAppPackages = listOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.pinterest",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.tiktok.android",
        "com.zhiliaoapp.musically"
    )

    suspend fun classify(
        packageName: String,
        title: String,
        text: String,
        category: String?
    ): ClassificationTier {
        Log.i(TAG, "==== Intercepted Notification Details ====")
        Log.i(TAG, "App Package: $packageName")
        Log.i(TAG, "Title: $title")
        Log.i(TAG, "Text: $text")
        Log.i(TAG, "Category: $category")
        Log.i(TAG, "==========================================")

        // --- Tier 0: Direct Rule Checks (System Bypass) ---
        Log.i(TAG, "[Tier 0] Checking direct rule bypasses...")
        
        if (category == Notification.CATEGORY_CALL ||
            category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_NAVIGATION ||
            category == Notification.CATEGORY_SYSTEM
        ) {
            Log.i(TAG, "[Tier 0] CRITICAL match: notification category is system-critical ($category)")
            return ClassificationTier.CRITICAL
        }

        if (packageName.contains("dialer") ||
            packageName.contains("telecom") ||
            packageName.contains("deskclock") ||
            packageName.contains("calendar")
        ) {
            Log.i(TAG, "[Tier 0] CRITICAL match: app package is phone/calendar/clock related ($packageName)")
            return ClassificationTier.CRITICAL
        }

        if (digestAppPackages.contains(packageName)) {
            Log.i(TAG, "[Tier 0] DIGEST match: package $packageName matches a known social app list.")
            val combinedText = "$title $text".lowercase()
            if (criticalKeywords.any { combinedText.contains(it) }) {
                Log.i(TAG, "[Tier 0] OVERRIDE: Social app notification matches a critical keyword. Promoting to CRITICAL.")
                return ClassificationTier.CRITICAL
            }
            Log.i(TAG, "[Tier 0] Suppressing and sorting to DIGEST.")
            return ClassificationTier.DIGEST
        }

        // --- Tier 1: Regex Keyword Heuristics ---
        Log.i(TAG, "[Tier 1] Checking keyword heuristics...")
        val combinedText = "$title $text".lowercase()
        val matchedKeyword = criticalKeywords.firstOrNull { combinedText.contains(it) }
        if (matchedKeyword != null) {
            Log.i(TAG, "[Tier 1] CRITICAL match: text contains urgent keyword '$matchedKeyword'")
            return ClassificationTier.CRITICAL
        }

        // --- Tier 2: On-Device AI Classification (Gemma via LiteRT-LM) ---
        Log.i(TAG, "[Tier 2] Requesting Gemma classification via LiteRT-LM...")
        val engine = localLlmClient.getEngine()
        if (engine != null) {
            try {
                val prompt = """
                    You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, promotional, social media likes/follows, newsletters, group chats).
                    
                    Notification details:
                    App Package: $packageName
                    Title: $title
                    Text: $text
                    
                    Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                """.trimIndent()

                Log.i(TAG, "[Tier 2] Sending prompt to Gemma model...")
                val result = withContext(Dispatchers.IO) {
                    val conversation = engine.createConversation()
                    try {
                        val response = conversation.sendMessage(prompt)
                        val responseText = response.contents.contents
                            .filterIsInstance<Content.Text>()
                            .joinToString("\n") { it.text }
                        responseText.trim().uppercase()
                    } finally {
                        conversation.close()
                    }
                }
                Log.i(TAG, "[Tier 2] Gemma raw response: '$result'")
                
                if (result.contains("CRITICAL")) {
                    Log.i(TAG, "[Tier 2] Classified notification as CRITICAL")
                    return ClassificationTier.CRITICAL
                } else if (result.contains("DIGEST")) {
                    Log.i(TAG, "[Tier 2] Classified notification as DIGEST")
                    return ClassificationTier.DIGEST
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Tier 2] Error executing on-device LiteRT-LM classification", e)
            }
        } else {
            Log.i(TAG, "[Tier 2] LiteRT-LM Engine not initialized (Gemma model missing). Skipping on-device AI.")
        }

        Log.d(TAG, "No critical markers found. Defaulting to DIGEST.")
        return ClassificationTier.DIGEST
    }
}
