package com.kairos.os.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.ai.IntentValidationResult
import com.kairos.os.ai.OnDeviceIntentValidator
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("kairos_app_settings", Context.MODE_PRIVATE)

    // Default built-in distracting app slugs as initial fallback
    private val defaultDistractingApps = setOf(
        "youtube", "facebook", "instagram", "twitter", "tiktok", "reddit", "discord"
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
                // Fetch settings
                val settings = apiClient.getUserSettings()
                _userSettings.value = settings

                // Fetch app configs if remote endpoint is reachable
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

    fun isDistractingApp(appId: String?): Boolean {
        if (appId == null) return false
        val cleanId = appId.lowercase()
        return _distractingAppIds.value.contains(cleanId)
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
            // Update local remaining leisure minutes
            _userSettings.value = _userSettings.value.copy(
                remainingLeisureMinutes = result.remainingMinutes
            )
        }
        return result
    }

    fun updateDailyLeisureTime(newMinutes: Int, onResult: (String) -> Unit) {
        // Update local settings state immediately
        _userSettings.value = _userSettings.value.copy(dailyLeisureMinutes = newMinutes)
        onResult("Leisure limit set to ${newMinutes}m")

        viewModelScope.launch {
            try {
                apiClient.updateDailyLeisureTime(newMinutes)
            } catch (e: Exception) {
                // Ignore background sync failure as local is authoritative
            }
        }
    }

    fun toggleAppDistracting(appId: String, isDistracting: Boolean, onResult: (String) -> Unit) {
        val cleanId = appId.lowercase()
        val currentSet = _distractingAppIds.value.toMutableSet()
        if (isDistracting) {
            currentSet.add(cleanId)
        } else {
            currentSet.remove(cleanId)
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
