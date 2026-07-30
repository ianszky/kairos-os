package com.kairos.os.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.ai.IntentValidationResult
import com.kairos.os.ai.OnDeviceIntentValidator
import com.kairos.os.data.TrapAppStore
import com.kairos.os.data.api.AppConfigItemResponse
import com.kairos.os.data.api.IntentLogResult
import com.kairos.os.data.api.KairosApiClient
import com.kairos.os.data.api.UserSettingsResponse
import com.kairos.os.data.db.AppNotificationRuleDao
import com.kairos.os.data.db.AppNotificationRuleEntity
import com.kairos.os.domain.usecases.NotificationAppRule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntentViewModel @Inject constructor(
    private val apiClient: KairosApiClient,
    private val validator: OnDeviceIntentValidator,
    private val appNotificationRuleDao: AppNotificationRuleDao,
    private val trapAppStore: TrapAppStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("kairos_app_settings", Context.MODE_PRIVATE)

    // Default built-in distracting app slugs as initial fallback
    private val defaultDistractingApps = setOf(
        "youtube", "facebook", "instagram", "twitter", "tiktok", "reddit", "discord"
    )

    // Core essential system apps default to ALLOWED on initial launch
    private val defaultAllowedPackages = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.google.android.calendar",
        "com.android.calendar"
    )

    private val _distractingAppIds = MutableStateFlow<Set<String>>(emptySet())
    val distractingAppIds: StateFlow<Set<String>> = _distractingAppIds.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettingsResponse())
    val userSettings: StateFlow<UserSettingsResponse> = _userSettings.asStateFlow()

    private val _appConfigsMap = MutableStateFlow<Map<String, AppConfigItemResponse>>(emptyMap())
    val appConfigsMap: StateFlow<Map<String, AppConfigItemResponse>> = _appConfigsMap.asStateFlow()

    private val _appNotificationRules = MutableStateFlow<Map<String, NotificationAppRule>>(emptyMap())
    val appNotificationRules: StateFlow<Map<String, NotificationAppRule>> = _appNotificationRules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLocalDistractingApps()
        seedDefaultNotificationRules()
        loadData()
        observeNotificationRules()
    }

    private fun loadLocalDistractingApps() {
        val savedSet = prefs.getStringSet("distracting_app_ids", null)
        if (savedSet == null) {
            _distractingAppIds.value = defaultDistractingApps
            prefs.edit().putStringSet("distracting_app_ids", defaultDistractingApps).apply()
        } else {
            _distractingAppIds.value = savedSet.toSet()
        }
    }

    private fun seedDefaultNotificationRules() {
        viewModelScope.launch(Dispatchers.IO) {
            val isSeeded = prefs.getBoolean("notification_rules_seeded_v1", false)
            if (!isSeeded) {
                defaultAllowedPackages.forEach { pkg ->
                    val existing = appNotificationRuleDao.getRuleForPackage(pkg)
                    if (existing == null) {
                        appNotificationRuleDao.insertOrUpdate(
                            AppNotificationRuleEntity(packageName = pkg, rule = NotificationAppRule.ALLOWED.name)
                        )
                    }
                }
                prefs.edit().putBoolean("notification_rules_seeded_v1", true).apply()
            }
        }
    }

    private fun observeNotificationRules() {
        viewModelScope.launch {
            appNotificationRuleDao.getAllRulesFlow().collect { rulesList ->
                val map = rulesList.associate { 
                    it.packageName to (runCatching { NotificationAppRule.valueOf(it.rule) }.getOrDefault(NotificationAppRule.KAI_DECIDES))
                }
                _appNotificationRules.value = map
            }
        }
    }

    fun setAppNotificationRule(packageName: String, rule: NotificationAppRule) {
        // Immediate local state update via Room on IO thread for instant 0ms response
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (rule == NotificationAppRule.KAI_DECIDES) {
                    appNotificationRuleDao.deleteRule(packageName)
                } else {
                    appNotificationRuleDao.insertOrUpdate(
                        AppNotificationRuleEntity(packageName = packageName, rule = rule.name)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiClient.getUserSettings()?.let { settings ->
                    _userSettings.value = settings
                }

                val configs = apiClient.getAppConfigs()
                if (configs.isNotEmpty()) {
                    val map = configs.associateBy { it.appIdentifier.lowercase() }
                    _appConfigsMap.value = map

                    val distractingSet = _distractingAppIds.value.toMutableSet()
                    configs.forEach { item ->
                        val id = item.appIdentifier.lowercase()
                        if (item.category == "TRAP") {
                            distractingSet.add(id)
                        } else if (item.category == "UTILITY") {
                            distractingSet.remove(id)
                        }
                    }
                    _distractingAppIds.value = distractingSet.toSet()
                    prefs.edit().putStringSet("distracting_app_ids", distractingSet).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun refreshSettings() {
        apiClient.getUserSettings()?.let { settings ->
            _userSettings.value = settings
        }
    }

    fun isDistractingApp(appId: String?): Boolean {
        if (appId == null) return false
        val cleanId = appId.removePrefix("app:").lowercase()
        return _distractingAppIds.value.contains(cleanId)
    }

    fun isDistractingPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return trapAppStore.isDistractingPackage(packageName)
    }

    fun syncDistractingPackages(installedApps: List<com.kairos.os.ui.AppConnection>) {
        val packages = installedApps
            .filter { app ->
                val cleanId = app.id.removePrefix("app:").lowercase()
                _distractingAppIds.value.contains(cleanId) && app.packageName != null
            }
            .mapNotNull { it.packageName }
            .toSet()
        trapAppStore.setDistractingPackages(packages)
    }

    suspend fun validateReason(reason: String, appName: String): IntentValidationResult {
        return validator.validateReason(reason, appName)
    }

    suspend fun logIntent(
        appId: String,
        displayName: String,
        reason: String,
        minutes: Int,
        aiApproved: Boolean
    ): IntentLogResult {
        val result = apiClient.logIntent(appId, displayName, reason, minutes, aiApproved)
        if (result.logged) {
            val daily = _userSettings.value.dailyLeisureMinutes
            _userSettings.value = _userSettings.value.copy(
                remainingLeisureMinutes = result.remainingMinutes,
                todayUsedMinutes = (daily - result.remainingMinutes).coerceAtLeast(0)
            )
        }
        return result
    }

    fun updateDailyLeisureTime(newMinutes: Int, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            val result = apiClient.updateDailyLeisureTime(newMinutes)
            if (result.status == "ERROR") {
                onResult(false, result.message)
                return@launch
            }
            val snap = result.settings
            _userSettings.value = _userSettings.value.copy(
                pendingLeisureMinutes = snap?.pendingLeisureMinutes ?: newMinutes,
                pendingChangeEffectiveAt = snap?.pendingChangeEffectiveAt ?: result.effectiveAt
            )
            onResult(true, result.message)
        }
    }

    fun toggleAppDistracting(
        appId: String,
        packageName: String?,
        isDistracting: Boolean,
        onResult: (String) -> Unit
    ) {
        val cleanId = appId.removePrefix("app:").lowercase()
        val currentSet = _distractingAppIds.value.toMutableSet()
        if (isDistracting) {
            currentSet.add(cleanId)
            packageName?.let { trapAppStore.addPackage(it) }
        } else {
            currentSet.remove(cleanId)
            packageName?.let { trapAppStore.removePackage(it) }
        }
        _distractingAppIds.value = currentSet.toSet()
        prefs.edit().putStringSet("distracting_app_ids", currentSet).apply()

        // Background sync attempt if backend available, without blocking UI or reverting local state on error
        viewModelScope.launch {
            try {
                apiClient.toggleAppDistracting(cleanId, isDistracting)
            } catch (_: Exception) {
                // Ignore background sync errors as local storage is authoritative
            }
        }

        onResult(if (isDistracting) "App set as distracting" else "App set as utility")
    }
}
