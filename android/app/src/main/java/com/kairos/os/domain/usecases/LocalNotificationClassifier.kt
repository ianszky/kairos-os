package com.kairos.os.domain.usecases

import android.app.Notification
import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ClassificationTier {
    CRITICAL,
    DIGEST
}

@Singleton
class LocalNotificationClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "LocalNotificationClassifier"
    private val MODEL_PATH = "/data/local/tmp/llm/gemma.bin"
    
    private var llmInference: LlmInference? = null

    init {
        initializeLlm()
    }

    private fun initializeLlm() {
        try {
            val modelFile = File(MODEL_PATH)
            if (modelFile.exists() && modelFile.canRead()) {
                Log.d(TAG, "Initializing MediaPipe LlmInference with model at $MODEL_PATH...")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(MODEL_PATH)
                    .setMaxTokens(128)
                    .setTemperature(0.2f)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                Log.i(TAG, "MediaPipe LlmInference successfully initialized.")
            } else {
                Log.w(TAG, "Gemma model file not found or unreadable at $MODEL_PATH. Gemma classification will be bypassed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LlmInference client", e)
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
        
        if (category == Notification.CATEGORY_CALL ||
            category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_NAVIGATION ||
            category == Notification.CATEGORY_SYSTEM
        ) {
            Log.d(TAG, "[Tier 0] CRITICAL match: notification category is system-critical ($category)")
            return ClassificationTier.CRITICAL
        }

        if (packageName.contains("dialer") ||
            packageName.contains("telecom") ||
            packageName.contains("deskclock") ||
            packageName.contains("calendar")
        ) {
            Log.d(TAG, "[Tier 0] CRITICAL match: app package is phone/calendar/clock related ($packageName)")
            return ClassificationTier.CRITICAL
        }

        if (digestAppPackages.contains(packageName)) {
            Log.d(TAG, "[Tier 0] DIGEST match: package $packageName matches a known social app list.")
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

        // --- Tier 2: On-Device AI Classification (Gemma via MediaPipe LlmInference) ---
        Log.d(TAG, "[Tier 2] Checking MediaPipe LlmInference...")
        
        if (llmInference == null) {
            initializeLlm()
        }

        val inference = llmInference
        if (inference != null) {
            try {
                val prompt = """
                    You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, promotional, social media likes/follows, newsletters, group chats).
                    
                    Notification details:
                    App Package: $packageName
                    Title: $title
                    Text: $text
                    
                    Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                """.trimIndent()

                Log.d(TAG, "[Tier 2] Sending prompt to Gemma via MediaPipe...")
                val result = withContext(Dispatchers.IO) {
                    inference.generateResponse(prompt).trim().uppercase()
                }
                Log.d(TAG, "[Tier 2] MediaPipe response text: $result")
                
                if (result.contains("CRITICAL")) {
                    Log.d(TAG, "[Tier 2] Classified as CRITICAL")
                    return ClassificationTier.CRITICAL
                } else if (result.contains("DIGEST")) {
                    Log.d(TAG, "[Tier 2] Classified as DIGEST")
                    return ClassificationTier.DIGEST
                }
            } catch (e: Exception) {
                Log.e(TAG, "[Tier 2] Error executing on-device MediaPipe classification", e)
            }
        } else {
            Log.d(TAG, "[Tier 2] MediaPipe LlmInference client not available. Falling back to rules.")
        }

        Log.d(TAG, "No critical markers found. Defaulting to DIGEST.")
        return ClassificationTier.DIGEST
    }
}
