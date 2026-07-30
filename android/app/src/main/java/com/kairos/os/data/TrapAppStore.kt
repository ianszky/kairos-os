package com.kairos.os.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrapAppStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDistractingPackages(): Set<String> {
        return prefs.getStringSet(KEY_DISTRACTING_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun setDistractingPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_DISTRACTING_PACKAGES, packages.toSet()).apply()
    }

    fun addPackage(packageName: String) {
        val updated = getDistractingPackages().toMutableSet()
        updated.add(packageName)
        setDistractingPackages(updated)
    }

    fun removePackage(packageName: String) {
        val updated = getDistractingPackages().toMutableSet()
        updated.remove(packageName)
        setDistractingPackages(updated)
    }

    fun isDistractingPackage(packageName: String): Boolean {
        return getDistractingPackages().contains(packageName)
    }

    companion object {
        const val PREFS_NAME = "kairos_app_settings"
        private const val KEY_DISTRACTING_PACKAGES = "distracting_package_names"
    }
}
