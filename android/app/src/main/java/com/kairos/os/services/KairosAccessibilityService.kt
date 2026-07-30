package com.kairos.os.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.kairos.os.data.TrapAppStore
import com.kairos.os.domain.session.AppSessionManager
import com.kairos.os.util.AccessibilityUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KairosAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionManager: AppSessionManager

    @Inject
    lateinit var trapAppStore: TrapAppStore

    private var lastBlockedPackage: String? = null
    private var lastBlockedAtMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val foregroundPackage = event.packageName?.toString() ?: rootInActiveWindow?.packageName?.toString()
        if (foregroundPackage.isNullOrBlank()) return
        if (AccessibilityUtils.shouldNeverBlock(foregroundPackage, packageName)) return
        if (!trapAppStore.isDistractingPackage(foregroundPackage)) return
        if (sessionManager.hasValidGrant(foregroundPackage)) return

        val now = System.currentTimeMillis()
        if (foregroundPackage == lastBlockedPackage && now - lastBlockedAtMs < DEBOUNCE_MS) return
        lastBlockedPackage = foregroundPackage
        lastBlockedAtMs = now

        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val DEBOUNCE_MS = 500L
    }
}
