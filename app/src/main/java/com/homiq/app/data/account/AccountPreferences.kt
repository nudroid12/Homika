package com.homiq.app.data.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountState(
    val localProfileName: String = "",
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
        )

    private companion object {
        const val PREFS_NAME = "homika_account"
        const val KEY_LOCAL_PROFILE_NAME = "local_profile_name"
    }
}
