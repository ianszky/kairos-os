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
        Log.d(TAG, "Classifying notification from $packageName (title: $title, category: $category)")

        // --- Tier 0: Direct Rule Checks (System Bypass) ---
        
        // 1. Check notification category
        if (category == Notification.CATEGORY_CALL ||
            category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_NAVIGATION ||
            category == Notification.CATEGORY_SYSTEM
        ) {
            Log.d(TAG, "Direct critical match: category = $category")
            return ClassificationTier.CRITICAL
        }

        // 2. Dialer/Phone/Clock app packages are critical
        if (packageName.contains("dialer") ||
            packageName.contains("telecom") ||
            packageName.contains("deskclock") ||
            packageName.contains("calendar")
        ) {
            Log.d(TAG, "Direct critical match: package = $packageName")
            return ClassificationTier.CRITICAL
        }

        // 3. Known social media apps are categorized as digest by default
        if (digestAppPackages.contains(packageName)) {
            // But we will still run regex keyword checking in case it's an urgent DM
            val combinedText = "$title $text".lowercase()
            if (criticalKeywords.any { combinedText.contains(it) }) {
                Log.d(TAG, "Digest package overridden by critical keyword: $combinedText")
                return ClassificationTier.CRITICAL
            }
            Log.d(TAG, "Direct digest match: social package = $packageName")
            return ClassificationTier.DIGEST
        }

        // --- Tier 1: Regex Keyword Heuristics ---
        val combinedText = "$title $text".lowercase()
        if (criticalKeywords.any { combinedText.contains(it) }) {
            Log.d(TAG, "Critical keyword match in text: $combinedText")
            return ClassificationTier.CRITICAL
        }

        // --- Tier 2: On-Device AI Classification (Gemma-4-e2b via ML Kit GenAI) ---
        val model = generativeModel
        if (model != null) {
            try {
                // Check if the model is available
                val status = model.checkStatus()
                if (status == FeatureStatus.AVAILABLE) {
                    val prompt = """
                        You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, promotional, social media likes/follows, newsletters, group chats).
                        
                        Notification details:
                        App Package: $packageName
                        Title: $title
                        Text: $text
                        
                        Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                    """.trimIndent()

                    val response = model.generateContent(prompt)
                    val result = response.candidates.firstOrNull()?.text?.trim()?.uppercase()
                    Log.d(TAG, "Gemma-4-e2b classification result: $result")
                    
                    if (result == "CRITICAL") {
                        return ClassificationTier.CRITICAL
                    } else if (result == "DIGEST") {
                        return ClassificationTier.DIGEST
                    }
                } else {
                    Log.d(TAG, "Gemma model not ready or downloadable (status: $status). Falling back to rules.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing on-device Gemma classification", e)
            }
        } else {
            Log.d(TAG, "ML Kit GenAI client not available. Falling back to rules.")
        }

        // Default to DIGEST if rules/AI don't mark as CRITICAL
        Log.d(TAG, "Defaulting to DIGEST")
        return ClassificationTier.DIGEST
    }
}
