package com.homiq.app.data.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class AppearancePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val mode: AppearanceMode
        get() = runCatching {
            AppearanceMode.valueOf(prefs.getString(KEY_MODE, AppearanceMode.SYSTEM.name) ?: AppearanceMode.SYSTEM.name)
        }.getOrDefault(AppearanceMode.SYSTEM)

    fun set(mode: AppearanceMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
    }

    fun applySavedMode() {
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
    }

    private fun AppearanceMode.toNightMode(): Int = when (this) {
        AppearanceMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppearanceMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AppearanceMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    private companion object {
        const val PREFS_NAME = "homika_appearance"
        const val KEY_MODE = "mode"
    }
}
