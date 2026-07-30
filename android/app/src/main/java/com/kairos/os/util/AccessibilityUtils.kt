package com.kairos.os.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.kairos.os.services.KairosAccessibilityService

object AccessibilityUtils {

    private val NEVER_BLOCK_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller"
    )

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val component = ComponentName(context, KairosAccessibilityService::class.java)
        val flattened = component.flattenToString()
        val shortFlattened = component.flattenToShortString()
        return enabledServices.split(':').any { entry ->
            entry.equals(flattened, ignoreCase = true) ||
                entry.equals(shortFlattened, ignoreCase = true)
        }
    }

    fun shouldNeverBlock(packageName: String, selfPackageName: String): Boolean {
        if (packageName == selfPackageName) return true
        if (NEVER_BLOCK_PACKAGES.contains(packageName)) return true
        if (packageName.startsWith("com.android.launcher")) return true
        return false
    }
}
