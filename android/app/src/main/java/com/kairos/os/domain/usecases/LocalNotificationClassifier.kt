package com.kairos.os.domain.usecases

import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.kairos.os.data.db.AppNotificationRuleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ClassificationTier {
    CRITICAL,   // Deliver on-device instantly
    DIGEST,     // Intercept & save to local Room database for periodic digest summary
    BLOCKED     // Intercept & silently dismiss without saving to database
}

@Singleton
class LocalNotificationClassifier @Inject constructor(
    private val localLlmClient: LocalLlmClient,
    private val appNotificationRuleDao: AppNotificationRuleDao
) {
    private val TAG = "KairosNotificationClassifier"

    private val criticalKeywords = listOf(
        "urgent", "emergency", "asap", "hospital", "doctor",
        "accident", "police", "help", "otp", "verification",
        "code", "alert", "security", "warning", "meeting",
        "incoming call", "call from"
    )

    suspend fun classify(
        packageName: String,
        title: String,
        text: String,
        category: String?
    ): ClassificationTier {
        Log.i(TAG, "🔍 ========================================================")
        Log.i(TAG, "🔍 INTERCEPTOR TRIGGERED: Evaluating notification...")
        Log.i(TAG, "🔍 App Package: $packageName")
        Log.i(TAG, "🔍 Title      : $title")
        Log.i(TAG, "🔍 Text       : $text")
        Log.i(TAG, "🔍 Category   : $category")
        Log.i(TAG, "🔍 ========================================================")

        // --- STEP 1: Check Explicit User Per-App Notification Rules (Room DB / App Settings) ---
        try {
            val userRuleEntity = appNotificationRuleDao.getRuleForPackage(packageName)
            if (userRuleEntity != null) {
                when (userRuleEntity.rule) {
                    NotificationAppRule.ALLOWED.name -> {
                        Log.i(TAG, "✅ [User Settings Whitelist] App $packageName is explicitly set to ALLOWED. Delivering directly on-device.")
                        return ClassificationTier.CRITICAL
                    }
                    NotificationAppRule.BLOCKED.name -> {
                        Log.i(TAG, "🚫 [User Settings Blacklist] App $packageName is explicitly set to BLOCKED. Suppressing without storing.")
                        return ClassificationTier.BLOCKED
                    }
                    NotificationAppRule.KAI_DECIDES.name -> {
                        Log.i(TAG, "🤖 [User Settings] App $packageName is set to KAI_DECIDES. Proceeding to smart classifier.")
                    }
                }
            } else {
                Log.i(TAG, "ℹ️ [User Settings] No explicit rule set for $packageName (Defaulting to KAI_DECIDES).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to query user app notification rule, falling back to smart classification", e)
        }

        // --- STEP 2: Keyword Heuristics ---
        Log.i(TAG, "Checking urgent keyword heuristics...")
        val combinedText = "$title $text".lowercase()
        val matchedKeyword = criticalKeywords.firstOrNull { combinedText.contains(it) }
        if (matchedKeyword != null) {
            Log.i(TAG, "✅ [Keyword Match] CRITICAL keyword match ('$matchedKeyword'). Delivering directly on-device.")
            return ClassificationTier.CRITICAL
        }

        // --- STEP 3: On-Device AI Classification (Gemma via LiteRT-LM) ---
        Log.i(TAG, "[Kai Decides] Requesting Gemma classification via LiteRT-LM...")
        val engine = localLlmClient.getEngine()
        if (engine != null) {
            Log.i(TAG, "⚡ [Kai Decides] Local LiteRT-LM Engine IS ACTIVE! Running Gemma model inference for $packageName...")
            try {
                val prompt = """
                    You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, casual chat, social media likes/follows, newsletters, group chats).
                    
                    Notification details:
                    App Package: $packageName
                    Title: $title
                    Text: $text
                    
                    Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                """.trimIndent()

                Log.i(TAG, "🤖 Prompt sent to Gemma model: App=$packageName | Title=$title")
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
                Log.i(TAG, "🤖 Gemma raw response: '$result'")

                if (result.contains("CRITICAL")) {
                    Log.i(TAG, "✅ Gemma classified notification as CRITICAL")
                    return ClassificationTier.CRITICAL
                } else if (result.contains("DIGEST")) {
                    Log.i(TAG, "🔕 Gemma classified notification as DIGEST")
                    return ClassificationTier.DIGEST
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error executing on-device Gemma LiteRT-LM classification", e)
            }
        } else {
            Log.w(TAG, "⚠️ Local LiteRT-LM Engine is NOT initialized (Gemma model binary missing or unreadable on device). Skipping AI tier.")
        }

        // --- STEP 4: Safe Fallback ---
        // For general apps set to Kai Decides without critical keywords or LLM triggers, default to DIGEST
        Log.i(TAG, "🔕 [Fallback] No critical markers detected. Routing notification to DIGEST.")
        return ClassificationTier.DIGEST
    }
}
