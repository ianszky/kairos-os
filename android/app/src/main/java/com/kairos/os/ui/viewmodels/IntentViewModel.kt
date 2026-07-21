package com.kairos.os.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.ai.IntentValidationResult
import com.kairos.os.ai.OnDeviceIntentValidator
import com.kairos.os.data.api.AppConfigItemResponse
import com.kairos.os.data.api.IntentLogResult
import com.kairos.os.data.api.KairosApiClient
import com.kairos.os.data.api.UserSettingsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntentViewModel @Inject constructor(
    private val apiClient: KairosApiClient,
    private val validator: OnDeviceIntentValidator
) : ViewModel() {

    // Default built-in distracting app slugs as initial fallback
    private val defaultDistractingApps = setOf(
        "youtube", "facebook", "instagram", "twitter", "tiktok", "reddit", "discord"
    )

    private val _distractingAppIds = MutableStateFlow<Set<String>>(defaultDistractingApps)
    val distractingAppIds: StateFlow<Set<String>> = _distractingAppIds.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettingsResponse())
    val userSettings: StateFlow<UserSettingsResponse> = _userSettings.asStateFlow()

    private val _appConfigsMap = MutableStateFlow<Map<String, AppConfigItemResponse>>(emptyMap())
    val appConfigsMap: StateFlow<Map<String, AppConfigItemResponse>> = _appConfigsMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch settings
                val settings = apiClient.getUserSettings()
                _userSettings.value = settings

                // Fetch app configs
                val configs = apiClient.getAppConfigs()
                val map = configs.associateBy { it.appIdentifier.lowercase() }
                _appConfigsMap.value = map

                // Compute active distracting set
                val distractingSet = defaultDistractingApps.toMutableSet()
                configs.forEach { item ->
                    val id = item.appIdentifier.lowercase()
                    if (item.category == "TRAP") {
                        distractingSet.add(id)
                    } else if (item.category == "UTILITY") {
                        distractingSet.remove(id)
                    }
                }
                _distractingAppIds.value = distractingSet
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
        viewModelScope.launch {
            try {
                val res = apiClient.updateDailyLeisureTime(newMinutes)
                loadData()
                onResult(res.message)
            } catch (e: Exception) {
                onResult("Failed to update daily leisure time: ${e.message}")
            }
        }
    }

    fun toggleAppDistracting(appId: String, isDistracting: Boolean, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = apiClient.toggleAppDistracting(appId, isDistracting)
                loadData()
                onResult(res.message)
            } catch (e: Exception) {
                onResult("Failed to update app setting: ${e.message}")
            }
        }
    }
}
