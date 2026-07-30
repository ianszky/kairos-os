package com.kairos.os.domain.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeSession = MutableStateFlow<AppSession?>(null)
    val activeSession: StateFlow<AppSession?> = _activeSession.asStateFlow()

    init {
        _activeSession.value = loadSession()
    }

    fun getSession(): AppSession? {
        val cached = _activeSession.value
        if (cached != null) {
            if (cached.isExpired) {
                clearSession()
                return null
            }
            return cached
        }
        val loaded = loadSession() ?: return null
        if (loaded.isExpired) {
            clearSession()
            return null
        }
        _activeSession.value = loaded
        return loaded
    }

    fun saveSession(session: AppSession) {
        prefs.edit()
            .putString(KEY_PACKAGE, session.packageName)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_APP_SLUG, session.appSlug)
            .putLong(KEY_GRANTED_AT, session.grantedAtMs)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMs)
            .putInt(KEY_MINUTES, session.minutes)
            .apply()
        _activeSession.value = session
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_PACKAGE)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_APP_SLUG)
            .remove(KEY_GRANTED_AT)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_MINUTES)
            .apply()
        _activeSession.value = null
    }

    private fun loadSession(): AppSession? {
        if (!prefs.contains(KEY_PACKAGE)) return null
        val packageName = prefs.getString(KEY_PACKAGE, null) ?: return null
        return AppSession(
            packageName = packageName,
            displayName = prefs.getString(KEY_DISPLAY_NAME, packageName) ?: packageName,
            appSlug = prefs.getString(KEY_APP_SLUG, "") ?: "",
            grantedAtMs = prefs.getLong(KEY_GRANTED_AT, 0L),
            expiresAtMs = prefs.getLong(KEY_EXPIRES_AT, 0L),
            minutes = prefs.getInt(KEY_MINUTES, 0)
        )
    }

    companion object {
        private const val PREFS_NAME = "kairos_app_session"
        private const val KEY_PACKAGE = "package_name"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_APP_SLUG = "app_slug"
        private const val KEY_GRANTED_AT = "granted_at_ms"
        private const val KEY_EXPIRES_AT = "expires_at_ms"
        private const val KEY_MINUTES = "minutes"
    }
}
