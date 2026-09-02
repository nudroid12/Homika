package com.homiq.app.data.preferences

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        "homika_onboarding",
        Context.MODE_PRIVATE,
    )

    val isComplete: Boolean
        get() = preferences.getBoolean(KEY_COMPLETE, false)

    fun complete() {
        preferences.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    private companion object {
        const val KEY_COMPLETE = "complete_v2"
    }
}
