package com.kairos.os.domain.usecases

import android.app.Notification
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

    // Only essential OS system categories for hardware/life-critical features (calls, alarms, navigation turn-by-turn)
    private val osSystemCriticalCategories = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_SYSTEM
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

        // --- STEP 2: Strict OS Hardware / Telephony Bypass (Only core OS calls/alarms) ---
        val lowerPackage = packageName.lowercase()
        if (lowerPackage.contains("dialer") ||
            lowerPackage.contains("telecom") ||
            lowerPackage.contains("deskclock") ||
            lowerPackage.contains("clock") ||
            lowerPackage.contains("incallui")
        ) {
            Log.i(TAG, "✅ [OS System Bypass] Core phone dialer/alarm package ($packageName). Delivering directly on-device.")
            return ClassificationTier.CRITICAL
        }

        if (category != null && osSystemCriticalCategories.contains(category)) {
            Log.i(TAG, "✅ [OS Category Bypass] System critical category match ($category). Delivering directly on-device.")
            return ClassificationTier.CRITICAL
        }

        // --- STEP 3: Tier 1 Keyword Heuristics (Kai Decides Analysis) ---
        Log.i(TAG, "[Tier 1] Checking urgent keyword heuristics for $packageName...")
        val combinedText = "$title $text".lowercase()
        val matchedKeyword = criticalKeywords.firstOrNull { combinedText.contains(it) }
        if (matchedKeyword != null) {
            Log.i(TAG, "✅ [Tier 1] CRITICAL keyword match ('$matchedKeyword'). Delivering directly on-device.")
            return ClassificationTier.CRITICAL
        }

        // --- STEP 4: Tier 2 On-Device AI Classification (Gemma via LiteRT-LM) ---
        Log.i(TAG, "[Tier 2] Checking On-Device LiteRT-LM (Gemma Model) Status...")
        val engine = localLlmClient.getEngine()
        if (engine != null) {
            Log.i(TAG, "⚡ [Tier 2] Local LiteRT-LM Engine IS ACTIVE! Running Gemma model inference for $packageName...")
            try {
                val prompt = """
                    You are KAIROS OS's notification classifier. Decide if the following notification is CRITICAL (urgent, requires immediate human attention, e.g. direct text messages, work updates, meeting reminders, security alerts, OTPs) or DIGEST (non-urgent, casual chat, social media likes/follows, newsletters, group chats).
                    
                    Notification details:
                    App Package: $packageName
                    Title: $title
                    Text: $text
                    
                    Respond with exactly one word: CRITICAL or DIGEST. Do not write any other explanation or text.
                """.trimIndent()

                Log.i(TAG, "🤖 [Tier 2] Prompt sent to Gemma model: App=$packageName | Title=$title")
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
                Log.i(TAG, "🤖 [Tier 2] Gemma raw response: '$result'")

                if (result.contains("CRITICAL")) {
                    Log.i(TAG, "✅ [Tier 2] Gemma classified notification as CRITICAL")
                    return ClassificationTier.CRITICAL
                } else if (result.contains("DIGEST")) {
                    Log.i(TAG, "🔕 [Tier 2] Gemma classified notification as DIGEST")
                    return ClassificationTier.DIGEST
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [Tier 2] Error executing on-device Gemma LiteRT-LM classification", e)
            }
        } else {
            Log.w(TAG, "⚠️ [Tier 2] Local LiteRT-LM Engine is NOT initialized (Gemma model binary missing or unreadable on device). Skipping AI tier.")
        }

        // --- STEP 5: Safe Fallback ---
        // For general apps without critical markers when in Kai Decides mode, route to DIGEST
        Log.i(TAG, "🔕 [Fallback] No critical markers detected. Routing notification to DIGEST.")
        return ClassificationTier.DIGEST
    }
}
