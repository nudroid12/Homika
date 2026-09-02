package com.homiq.app.data.preferences

import android.content.Context

class CalendarPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    val selectedPropertyId: String?
        get() = prefs.getString(KEY_SELECTED_PROPERTY_ID, null)

    fun setSelectedProperty(propertyId: String?) {
        val editor = prefs.edit()
        if (propertyId == null) {
            editor.remove(KEY_SELECTED_PROPERTY_ID)
        } else {
            editor.putString(KEY_SELECTED_PROPERTY_ID, propertyId)
        }
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "homika_calendar"
        const val KEY_SELECTED_PROPERTY_ID = "selected_property_id"
    }
}
