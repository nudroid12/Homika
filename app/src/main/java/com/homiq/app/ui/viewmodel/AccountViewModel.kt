package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.account.AccountPreferences
import com.homiq.app.data.account.AccountState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val account: AccountState = AccountState(),
    val message: AccountUiMessage? = null,
)

sealed interface AccountUiMessage {
    data object ProfileSaved : AccountUiMessage
}

class AccountViewModel(
    private val accountPreferences: AccountPreferences,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            AccountUiState(
                account = accountPreferences.state.value,
            ),
        )

    val state: StateFlow<AccountUiState> =
        mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            accountPreferences.state.collect {
                mutableState.value =
                    mutableState.value.copy(
                        account = it,
                    )
            }
        }
    }

    fun saveLocalProfile(name: String) {
        accountPreferences.saveLocalProfile(name)
        mutableState.value =
            mutableState.value.copy(
                message = AccountUiMessage.ProfileSaved,
            )
    }

    fun clearMessage() {
        mutableState.value =
            mutableState.value.copy(
                message = null,
            )
    }
}
