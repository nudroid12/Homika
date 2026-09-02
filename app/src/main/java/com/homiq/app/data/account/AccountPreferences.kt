package com.homiq.app.data.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountState(
    val localProfileName: String = "",
    val googleConnected: Boolean = false,
    val googleEmail: String? = null,
    val googleDisplayName: String? = null,
)

class AccountPreferences(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    private val mutableState =
        MutableStateFlow(readState())

    val state: StateFlow<AccountState> =
        mutableState.asStateFlow()

    fun saveLocalProfile(name: String) {
        prefs.edit()
            .putString(
                KEY_LOCAL_PROFILE_NAME,
                name.trim(),
            )
            .apply()
        publish()
    }

    fun setGoogleProfile(
        email: String?,
        displayName: String?,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_GOOGLE_CONNECTED,
                true,
            )
            .putString(
                KEY_GOOGLE_EMAIL,
                email?.takeIf { it.isNotBlank() },
            )
            .putString(
                KEY_GOOGLE_DISPLAY_NAME,
                displayName?.takeIf { it.isNotBlank() },
            )
            .apply()
        publish()
    }

    fun markGoogleConnected() {
        prefs.edit()
            .putBoolean(
                KEY_GOOGLE_CONNECTED,
                true,
            )
            .apply()
        publish()
    }

    fun clearGoogleAccount() {
        prefs.edit()
            .putBoolean(
                KEY_GOOGLE_CONNECTED,
                false,
            )
            .remove(KEY_GOOGLE_EMAIL)
            .remove(KEY_GOOGLE_DISPLAY_NAME)
            .apply()
        publish()
    }

    private fun publish() {
        mutableState.value = readState()
    }

    private fun readState(): AccountState =
        AccountState(
            localProfileName =
                prefs.getString(
                    KEY_LOCAL_PROFILE_NAME,
                    "",
                ).orEmpty(),
            googleConnected =
                prefs.getBoolean(
                    KEY_GOOGLE_CONNECTED,
                    false,
                ),
            googleEmail =
                prefs.getString(
                    KEY_GOOGLE_EMAIL,
                    null,
                ),
            googleDisplayName =
                prefs.getString(
                    KEY_GOOGLE_DISPLAY_NAME,
                    null,
                ),
        )

    private companion object {
        const val PREFS_NAME =
            "homika_account"
        const val KEY_LOCAL_PROFILE_NAME =
            "local_profile_name"
        const val KEY_GOOGLE_CONNECTED =
            "google_connected"
        const val KEY_GOOGLE_EMAIL =
            "google_email"
        const val KEY_GOOGLE_DISPLAY_NAME =
            "google_display_name"
    }
}
