package com.homiq.app.data.backup

import com.homiq.app.data.sync.SyncChangeSignal
import com.homiq.app.data.account.AccountPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AutoBackupRuntimeState(
    val pending: Boolean = false,
    val isBackingUp: Boolean = false,
    val lastFailure:
        DriveBackupFailureReason? = null,
)

class AutoBackupService(
    private val driveBackup:
        DriveBackupService,
    private val preferences:
        BackupPreferences,
    private val accountPreferences:
        AccountPreferences,
    private val changes:
        SyncChangeSignal,
) {
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO,
        )

    private var collectorJob: Job? = null
    private var scheduledJob: Job? = null

    private val mutableState =
        MutableStateFlow(
            AutoBackupRuntimeState(
                pending =
                    preferences
                        .autoBackupPending,
            ),
        )

    val state:
        StateFlow<AutoBackupRuntimeState> =
        mutableState.asStateFlow()

    fun start() {
        if (collectorJob != null) return

        collectorJob = scope.launch {
            if (
                preferences.autoBackupEnabled &&
                preferences.autoBackupPending
            ) {
                scheduleBackup(
                    delayMillis = 1_000L,
                )
            }

            changes.changes.collect {
                if (
                    preferences
                        .autoBackupEnabled
                ) {
                    markPending()
                    scheduleBackup(
                        delayMillis =
                            CHANGE_DEBOUNCE_MILLIS,
                    )
                }
            }
        }
    }

    fun setEnabled(
        enabled: Boolean,
    ) {
        preferences
            .setAutoBackupEnabled(
                enabled,
            )

        if (enabled) {
            requestBackup()
        } else {
            scheduledJob?.cancel()
            scheduledJob = null
            preferences
                .setAutoBackupPending(
                    false,
                )
            mutableState.value =
                AutoBackupRuntimeState()
        }
    }

    fun requestBackup() {
        if (
            !preferences
                .autoBackupEnabled
        ) {
            return
        }

        markPending()
        scheduleBackup(
            delayMillis = 250L,
        )
    }

    private fun markPending() {
        preferences
            .setAutoBackupPending(true)
        mutableState.value =
            mutableState.value.copy(
                pending = true,
                lastFailure = null,
            )
    }

    private fun scheduleBackup(
        delayMillis: Long,
    ) {
        scheduledJob?.cancel()
        scheduledJob =
            scope.launch {
                delay(delayMillis)
                performBackup()
            }
    }

    private suspend fun performBackup() {
        if (
            !preferences
                .autoBackupEnabled
        ) {
            return
        }

        if (
            !accountPreferences
                .state
                .value
                .googleConnected
        ) {
            mutableState.value =
                mutableState.value.copy(
                    pending = true,
                    isBackingUp = false,
                    lastFailure =
                        DriveBackupFailureReason
                            .NOT_CONNECTED,
                )
            return
        }

        mutableState.value =
            mutableState.value.copy(
                pending = true,
                isBackingUp = true,
                lastFailure = null,
            )

        when (
            val result =
                driveBackup.createBackup()
        ) {
            is DriveBackupWriteResult.Success -> {
                preferences
                    .setAutoBackupPending(
                        false,
                    )
                mutableState.value =
                    AutoBackupRuntimeState(
                        pending = false,
                        isBackingUp = false,
                        lastFailure = null,
                    )
            }

            is DriveBackupWriteResult.NeedsResolution -> {
                mutableState.value =
                    mutableState.value.copy(
                        pending = true,
                        isBackingUp = false,
                        lastFailure =
                            DriveBackupFailureReason
                                .AUTHORIZATION_FAILED,
                    )
            }

            is DriveBackupWriteResult.Failure -> {
                mutableState.value =
                    mutableState.value.copy(
                        pending = true,
                        isBackingUp = false,
                        lastFailure =
                            result.reason,
                    )

                if (
                    result.reason ==
                        DriveBackupFailureReason
                            .NETWORK_UNAVAILABLE ||
                    result.reason ==
                        DriveBackupFailureReason
                            .DRIVE_ACCESS_FAILED
                ) {
                    scheduleBackup(
                        delayMillis =
                            RETRY_MILLIS,
                    )
                }
            }
        }
    }

    companion object {
        private const val CHANGE_DEBOUNCE_MILLIS =
            2_500L
        private const val RETRY_MILLIS =
            60_000L
    }
}
