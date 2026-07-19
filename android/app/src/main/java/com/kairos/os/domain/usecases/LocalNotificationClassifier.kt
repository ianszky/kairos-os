package com.kairos.os.domain.usecases

import android.app.Notification
import android.util.Log
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.common.FeatureStatus
import javax.inject.Inject
import javax.inject.Singleton

enum class ClassificationTier {
    CRITICAL,
    DIGEST
}

@Singleton
class LocalNotificationClassifier @Inject constructor() {
    private val TAG = "LocalNotificationClassifier"
    
    // Lazy initialization of GenerativeModel client from ML Kit
    private val generativeModel by lazy {
        try {
            Log.d(TAG, "Initializing ML Kit GenAI client...")
            Generation.getClient()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ML Kit GenAI client", e)
            null
        }
    }

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
        Log.d(TAG, "==== Intercepted Notification Details ====")
        Log.d(TAG, "App Package: $packageName")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")
        Log.d(TAG, "Category: $category")
        Log.d(TAG, "==========================================")

        // --- Tier 0: Direct Rule Checks (System Bypass) ---
        Log.d(TAG, "[Tier 0] Checking direct rule bypasses...")
        
        // 1. Check notification category
        if (category == Notification.CATEGORY_CALL ||
            category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_NAVIGATION ||
            category == Notification.CATEGORY_SYSTEM
        ) {
            Log.d(TAG, "[Tier 0] CRITICAL match: notification category is system-critical ($category)")
            return ClassificationTier.CRITICAL
        }

        // 2. Dialer/Phone/Clock app packages are critical
        if (packageName.contains("dialer") ||
            packageName.contains("telecom") ||
            packageName.contains("deskclock") ||
            packageName.contains("calendar")
        ) {
            Log.d(TAG, "[Tier 0] CRITICAL match: app package is phone/calendar/clock related ($packageName)")
            return ClassificationTier.CRITICAL
        }

        // 3. Known social media apps are categorized as digest by default
        if (digestAppPackages.contains(packageName)) {
            Log.d(TAG, "[Tier 0] DIGEST match: package $packageName matches a known social app list.")
            
            // But we will still run regex keyword checking in case it's an urgent DM
            val combinedText = "$title $text".lowercase()
            if (criticalKeywords.any { combinedText.contains(it) }) {
                Log.d(TAG, "[Tier 0] OVERRIDE: Social app notification matches a critical keyword. Promoting to CRITICAL.")
                return ClassificationTier.CRITICAL
            }
            Log.d(TAG, "[Tier 0] Suppressing and sorting to DIGEST.")
            return ClassificationTier.DIGEST
        }

        // --- Tier 1: Regex Keyword Heuristics ---
        Log.d(TAG, "[Tier 1] Checking keyword heuristics...")
        val combinedText = "$title $text".lowercase()
        val matchedKeyword = criticalKeywords.firstOrNull { combinedText.contains(it) }
        if (matchedKeyword != null) {
            Log.d(TAG, "[Tier 1] CRITICAL match: text contains urgent keyword '$matchedKeyword'")
            return ClassificationTier.CRITICAL
        }

        // --- Tier 2: On-Device AI Classification (Gemma-4-e2b via ML Kit GenAI) ---
        Log.d(TAG, "[Tier 2] Preparing local Gemma-4-e2b model...")
        val model = generativeModel
        if (model != null) {
            try {
                // Check if the model is available
                val status = model.checkStatus()
                Log.d(TAG, "[Tier 2] Local Gemma model status: $status (AVAILABLE = 3, DOWNLOADABLE = 1, DOWNLOADING = 2, UNAVAILABLE = 0)")
                
                if (status == FeatureStatus.AVAILABLE) {
                    val prompt = """
                        You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, promotional, social media likes/follows, newsletters, group chats).
                        
                        Notification details:
                        App Package: $packageName
                        Title: $title
                        Text: $text
                        
                        Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                    """.trimIndent()

                    Log.d(TAG, "[Tier 2] Sending prompt to Gemma-4-e2b...")
                    Log.d(TAG, "[Tier 2] PROMPT:\n$prompt")

                    val response = model.generateContent(prompt)
                    val result = response.candidates.firstOrNull()?.text?.trim()?.uppercase()
                    Log.d(TAG, "[Tier 2] Gemma-4-e2b response text: $result")
                    
                    if (result == "CRITICAL") {
                        Log.d(TAG, "[Tier 2] Gemma classified as CRITICAL")
                        return ClassificationTier.CRITICAL
                    } else if (result == "DIGEST") {
                        Log.d(TAG, "[Tier 2] Gemma classified as DIGEST")
                        return ClassificationTier.DIGEST
                    } else {
                        Log.w(TAG, "[Tier 2] Gemma returned unexpected response shape. Defaulting to DIGEST.")
                    }
                } else {
                    Log.d(TAG, "[Tier 2] Gemma model not ready or downloadable (status: $status). Falling back to rules.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Tier 2] Error executing on-device Gemma classification", e)
            }
        } else {
            Log.d(TAG, "[Tier 2] ML Kit GenAI client not available. Falling back to rules.")
        }

        // Default to DIGEST if rules/AI don't mark as CRITICAL
        Log.d(TAG, "No critical markers found. Defaulting to DIGEST.")
        return ClassificationTier.DIGEST
    }
}
