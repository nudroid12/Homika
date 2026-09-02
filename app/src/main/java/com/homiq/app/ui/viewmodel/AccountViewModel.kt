package com.homiq.app.ui.viewmodel

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.account.AccountPreferences
import com.homiq.app.data.account.AccountState
import com.homiq.app.data.account.GoogleAccountActionResult
import com.homiq.app.data.account.GoogleAccountService
import com.homiq.app.data.backup.AutoBackupService
import com.homiq.app.data.sync.HomiqSyncService
import com.homiq.app.data.sync.SyncFailureReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val account: AccountState = AccountState(),
    val syncEnabled: Boolean = false,
    val isBusy: Boolean = false,
    val pendingResolution: PendingIntent? = null,
    val message: AccountUiMessage? = null,
)

sealed interface AccountUiMessage {
    data object ProfileSaved : AccountUiMessage
    data object GoogleConnected : AccountUiMessage
    data object GoogleSignedOut : AccountUiMessage
    data class Failure(
        val reason: SyncFailureReason,
    ) : AccountUiMessage
}

class AccountViewModel(
    private val accountPreferences:
        AccountPreferences,
    private val accountService:
        GoogleAccountService,
    private val syncService:
        HomiqSyncService,
    private val autoBackupService:
        AutoBackupService,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            AccountUiState(
                account =
                    accountPreferences
                        .state
                        .value,
                syncEnabled =
                    syncService
                        .state
                        .value
                        .enabled,
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
        viewModelScope.launch {
            syncService.state.collect {
                mutableState.value =
                    mutableState.value.copy(
                        syncEnabled =
                            it.enabled,
                    )
            }
        }
    }

    fun saveLocalProfile(name: String) {
        accountPreferences.saveLocalProfile(name)
        mutableState.value =
            mutableState.value.copy(
                message =
                    AccountUiMessage.ProfileSaved,
            )
    }

    fun signInGoogle() {
        viewModelScope.launch {
            setBusy(true)
            handle(accountService.signIn())
        }
    }

    fun completeGoogleSignIn(
        data: Intent?,
    ) {
        mutableState.value =
            mutableState.value.copy(
                pendingResolution = null,
            )
        viewModelScope.launch {
            setBusy(true)
            handle(
                accountService.completeSignIn(data),
            )
        }
    }

    fun resolutionLaunched() {
        mutableState.value =
            mutableState.value.copy(
                pendingResolution = null,
            )
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            setBusy(true)
            syncService.disconnect()
            autoBackupService.setEnabled(false)
            accountService.signOut()
            mutableState.value =
                mutableState.value.copy(
                    isBusy = false,
                    message =
                        AccountUiMessage
                            .GoogleSignedOut,
                )
        }
    }

    fun clearMessage() {
        mutableState.value =
            mutableState.value.copy(
                message = null,
            )
    }

    private fun handle(
        result: GoogleAccountActionResult,
    ) {
        when (result) {
            GoogleAccountActionResult.Connected ->
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        message =
                            AccountUiMessage
                                .GoogleConnected,
                    )

            is GoogleAccountActionResult.NeedsResolution ->
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        pendingResolution =
                            result.pendingIntent,
                        message = null,
                    )

            is GoogleAccountActionResult.Failure ->
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        message =
                            AccountUiMessage.Failure(
                                result.reason,
                            ),
                    )
        }
    }

    private fun setBusy(busy: Boolean) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = busy,
                message = null,
            )
    }
}
