package com.homiq.app.data.preferences

import android.content.Context

enum class TextSizeMode(
    val scale: Float,
) {
    SMALL(0.90f),
    STANDARD(1.00f),
    LARGE(1.12f),
}

class TextSizePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val mode: TextSizeMode
        get() = runCatching {
            TextSizeMode.valueOf(
                prefs.getString(KEY_MODE, TextSizeMode.STANDARD.name)
                    ?: TextSizeMode.STANDARD.name,
            )
        }.getOrDefault(TextSizeMode.STANDARD)

    fun set(mode: TextSizeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "homika_text_size"
        const val KEY_MODE = "mode"
    }
}
