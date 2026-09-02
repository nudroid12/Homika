package com.homiq.app.ui.viewmodel

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.backup.AutoBackupService
import com.homiq.app.data.backup.BackupDestination
import com.homiq.app.data.backup.BackupFailureReason
import com.homiq.app.data.backup.BackupHistory
import com.homiq.app.data.backup.BackupPreferences
import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.BackupReadResult
import com.homiq.app.data.backup.BackupRestoreResult
import com.homiq.app.data.backup.BackupWriteResult
import com.homiq.app.data.backup.DriveBackupFailureReason
import com.homiq.app.data.backup.DriveBackupReadResult
import com.homiq.app.data.backup.DriveBackupService
import com.homiq.app.data.backup.DriveBackupWriteResult
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.account.AccountPreferences
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(
    val isBusy: Boolean = false,
    val history: BackupHistory =
        BackupHistory(
            lastBackupEpochMillis = null,
            lastRestoreEpochMillis = null,
        ),
    val lastBackupDestination:
        BackupDestination? = null,
    val lastRestoreSource:
        BackupDestination? = null,
    val driveConnected: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoBackupPending: Boolean = false,
    val autoBackupRunning: Boolean = false,
    val pendingDriveResolution:
        PendingIntent? = null,
    val pendingRestorePreview:
        BackupPreview? = null,
    val message: BackupUiMessage? = null,
)

sealed interface BackupUiMessage {
    data class BackupCreated(
        val preview: BackupPreview,
        val destination:
            BackupDestination,
    ) : BackupUiMessage

    data class RestoreCompleted(
        val preview: BackupPreview,
        val source:
            BackupDestination,
    ) : BackupUiMessage

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupUiMessage

    data class DriveFailure(
        val reason:
            DriveBackupFailureReason,
    ) : BackupUiMessage
}

private enum class PendingDriveAction {
    BACKUP,
    RESTORE,
}

class BackupViewModel(
    private val service: HomiqBackupService,
    private val driveService:
        DriveBackupService,
    private val backupPreferences:
        BackupPreferences,
    private val autoBackupService:
        AutoBackupService,
    private val accountPreferences:
        AccountPreferences,
) : ViewModel() {
    private var pendingFileRestoreUri: Uri? =
        null
    private var pendingDriveRestoreRaw: String? =
        null
    private var pendingDriveAction:
        PendingDriveAction? = null

    private val mutableState =
        MutableStateFlow(
            BackupUiState(
                history = service.history(),
                lastBackupDestination =
                    backupPreferences
                        .lastBackupDestination,
                lastRestoreSource =
                    backupPreferences
                        .lastRestoreSource,
                driveConnected =
                    accountPreferences
                        .state
                        .value
                        .googleConnected,
                autoBackupEnabled =
                    backupPreferences
                        .autoBackupEnabled,
                autoBackupPending =
                    autoBackupService
                        .state
                        .value
                        .pending,
                autoBackupRunning =
                    autoBackupService
                        .state
                        .value
                        .isBackingUp,
            ),
        )

    val state: StateFlow<BackupUiState> =
        mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            accountPreferences.state.collect {
                mutableState.value =
                    mutableState.value.copy(
                        driveConnected =
                            it.googleConnected,
                    )
            }
        }

        viewModelScope.launch {
            autoBackupService.state.collect {
                mutableState.value =
                    mutableState.value.copy(
                        autoBackupPending =
                            it.pending,
                        autoBackupRunning =
                            it.isBackingUp,
                        autoBackupEnabled =
                            backupPreferences
                                .autoBackupEnabled,
                        history =
                            service.history(),
                        lastBackupDestination =
                            backupPreferences
                                .lastBackupDestination,
                    )
            }
        }
    }

    fun backupFileName(): String {
        val stamp =
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd-HHmm",
                ),
            )
        return "Homika-backup-$stamp.homika.json"
    }

    fun createBackup(
        uri: Uri,
    ) {
        viewModelScope.launch {
            updateBusy(true)
            when (
                val result =
                    service.writeBackup(uri)
            ) {
                is BackupWriteResult.Success -> {
                    backupPreferences.recordBackup(
                        BackupDestination
                            .DEVICE_FILE,
                    )
                    successBackup(
                        preview = result.preview,
                        destination =
                            BackupDestination
                                .DEVICE_FILE,
                    )
                }

                is BackupWriteResult.Failure -> {
                    fail(result.reason)
                }
            }
        }
    }

    fun createDriveBackup() {
        if (
            !mutableState
                .value
                .driveConnected
        ) {
            driveFail(
                DriveBackupFailureReason
                    .NOT_CONNECTED,
            )
            return
        }

        pendingDriveAction =
            PendingDriveAction.BACKUP

        viewModelScope.launch {
            updateBusy(true)
            handleDriveBackupResult(
                driveService.createBackup(),
            )
        }
    }

    fun inspectRestore(
        uri: Uri,
    ) {
        pendingDriveRestoreRaw = null

        viewModelScope.launch {
            updateBusy(true)
            when (
                val result =
                    service.inspectBackup(uri)
            ) {
                is BackupReadResult.Success -> {
                    pendingFileRestoreUri = uri
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = false,
                            pendingRestorePreview =
                                result.preview,
                            message = null,
                        )
                }

                is BackupReadResult.Failure -> {
                    fail(result.reason)
                }
            }
        }
    }

    fun inspectDriveRestore() {
        if (
            !mutableState
                .value
                .driveConnected
        ) {
            driveFail(
                DriveBackupFailureReason
                    .NOT_CONNECTED,
            )
            return
        }

        pendingDriveAction =
            PendingDriveAction.RESTORE

        viewModelScope.launch {
            updateBusy(true)
            handleDriveReadResult(
                driveService
                    .readLatestBackup(),
            )
        }
    }

    fun driveResolutionLaunched() {
        mutableState.value =
            mutableState.value.copy(
                pendingDriveResolution = null,
            )
    }

    fun completeDriveAuthorization(
        data: Intent?,
    ) {
        val action =
            pendingDriveAction
                ?: return

        viewModelScope.launch {
            updateBusy(true)

            when (action) {
                PendingDriveAction.BACKUP ->
                    handleDriveBackupResult(
                        driveService
                            .createBackupAfterAuthorization(
                                data,
                            ),
                    )

                PendingDriveAction.RESTORE ->
                    handleDriveReadResult(
                        driveService
                            .readLatestBackupAfterAuthorization(
                                data,
                            ),
                    )
            }
        }
    }

    fun setAutoBackupEnabled(
        enabled: Boolean,
    ) {
        if (
            enabled &&
            !mutableState
                .value
                .driveConnected
        ) {
            driveFail(
                DriveBackupFailureReason
                    .NOT_CONNECTED,
            )
            return
        }

        autoBackupService
            .setEnabled(enabled)
        mutableState.value =
            mutableState.value.copy(
                autoBackupEnabled = enabled,
            )
    }

    fun confirmRestore() {
        val driveRaw =
            pendingDriveRestoreRaw
        val fileUri =
            pendingFileRestoreUri

        when {
            driveRaw != null -> {
                viewModelScope.launch {
                    updateBusy(true)
                    when (
                        val result =
                            driveService
                                .restoreRaw(
                                    driveRaw,
                                )
                    ) {
                        is DriveBackupWriteResult.Success -> {
                            pendingDriveRestoreRaw =
                                null
                            pendingFileRestoreUri =
                                null
                            successRestore(
                                preview =
                                    result.preview,
                                source =
                                    BackupDestination
                                        .GOOGLE_DRIVE,
                            )
                        }

                        is DriveBackupWriteResult.NeedsResolution -> {
                            driveFail(
                                DriveBackupFailureReason
                                    .AUTHORIZATION_FAILED,
                            )
                        }

                        is DriveBackupWriteResult.Failure -> {
                            driveFail(
                                result.reason,
                            )
                        }
                    }
                }
            }

            fileUri != null -> {
                viewModelScope.launch {
                    updateBusy(true)
                    when (
                        val result =
                            service.restoreBackup(
                                fileUri,
                            )
                    ) {
                        is BackupRestoreResult.Success -> {
                            backupPreferences
                                .recordRestore(
                                    BackupDestination
                                        .DEVICE_FILE,
                                )
                            pendingFileRestoreUri =
                                null
                            pendingDriveRestoreRaw =
                                null
                            successRestore(
                                preview =
                                    result.preview,
                                source =
                                    BackupDestination
                                        .DEVICE_FILE,
                            )
                        }

                        is BackupRestoreResult.Failure -> {
                            fail(result.reason)
                        }
                    }
                }
            }
        }
    }

    fun cancelRestore() {
        pendingFileRestoreUri = null
        pendingDriveRestoreRaw = null
        mutableState.value =
            mutableState.value.copy(
                pendingRestorePreview = null,
            )
    }

    fun clearMessage() {
        mutableState.value =
            mutableState.value.copy(
                message = null,
            )
    }

    private fun handleDriveBackupResult(
        result: DriveBackupWriteResult,
    ) {
        when (result) {
            is DriveBackupWriteResult.Success -> {
                pendingDriveAction = null
                successBackup(
                    preview = result.preview,
                    destination =
                        BackupDestination
                            .GOOGLE_DRIVE,
                )
            }

            is DriveBackupWriteResult.NeedsResolution -> {
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        pendingDriveResolution =
                            result.pendingIntent,
                        message = null,
                    )
            }

            is DriveBackupWriteResult.Failure -> {
                pendingDriveAction = null
                driveFail(result.reason)
            }
        }
    }

    private fun handleDriveReadResult(
        result: DriveBackupReadResult,
    ) {
        when (result) {
            is DriveBackupReadResult.Success -> {
                pendingDriveAction = null
                pendingFileRestoreUri = null
                pendingDriveRestoreRaw =
                    result.raw
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        pendingRestorePreview =
                            result.preview,
                        message = null,
                    )
            }

            is DriveBackupReadResult.NeedsResolution -> {
                mutableState.value =
                    mutableState.value.copy(
                        isBusy = false,
                        pendingDriveResolution =
                            result.pendingIntent,
                        message = null,
                    )
            }

            is DriveBackupReadResult.Failure -> {
                pendingDriveAction = null
                driveFail(result.reason)
            }
        }
    }

    private fun successBackup(
        preview: BackupPreview,
        destination: BackupDestination,
    ) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = false,
                history = service.history(),
                lastBackupDestination =
                    backupPreferences
                        .lastBackupDestination,
                message =
                    BackupUiMessage
                        .BackupCreated(
                            preview = preview,
                            destination =
                                destination,
                        ),
            )
    }

    private fun successRestore(
        preview: BackupPreview,
        source: BackupDestination,
    ) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = false,
                history = service.history(),
                lastRestoreSource =
                    backupPreferences
                        .lastRestoreSource,
                pendingRestorePreview = null,
                message =
                    BackupUiMessage
                        .RestoreCompleted(
                            preview = preview,
                            source = source,
                        ),
            )
    }

    private fun updateBusy(
        busy: Boolean,
    ) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = busy,
                message = null,
            )
    }

    private fun fail(
        reason: BackupFailureReason,
    ) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = false,
                pendingRestorePreview = null,
                message =
                    BackupUiMessage.Failure(
                        reason,
                    ),
            )
    }

    private fun driveFail(
        reason:
            DriveBackupFailureReason,
    ) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = false,
                pendingDriveResolution = null,
                pendingRestorePreview = null,
                message =
                    BackupUiMessage
                        .DriveFailure(
                            reason,
                        ),
            )
    }
}
