package com.homiq.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.backup.BackupDestination
import com.homiq.app.data.backup.BackupFailureReason
import com.homiq.app.data.backup.BackupHistory
import com.homiq.app.data.backup.BackupPreferences
import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.BackupReadResult
import com.homiq.app.data.backup.BackupRestoreResult
import com.homiq.app.data.backup.BackupWriteResult
import com.homiq.app.data.backup.HomiqBackupService
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
    val lastBackupDestination: BackupDestination? = null,
    val lastRestoreSource: BackupDestination? = null,
    val pendingRestorePreview: BackupPreview? = null,
    val message: BackupUiMessage? = null,
)

sealed interface BackupUiMessage {
    data class BackupCreated(
        val preview: BackupPreview,
    ) : BackupUiMessage

    data class RestoreCompleted(
        val preview: BackupPreview,
    ) : BackupUiMessage

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupUiMessage
}

class BackupViewModel(
    private val service: HomiqBackupService,
    private val backupPreferences: BackupPreferences,
) : ViewModel() {
    private var pendingFileRestoreUri: Uri? = null

    private val mutableState =
        MutableStateFlow(
            BackupUiState(
                history = service.history(),
                lastBackupDestination =
                    backupPreferences.lastBackupDestination,
                lastRestoreSource =
                    backupPreferences.lastRestoreSource,
            ),
        )

    val state: StateFlow<BackupUiState> =
        mutableState.asStateFlow()

    fun backupFileName(): String {
        val stamp =
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern(
                    "yyyyMMdd-HHmm",
                ),
            )
        return "Homika-backup-$stamp.homika"
    }

    fun createBackup(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            updateBusy(true)
            when (val result = service.writeBackup(uri)) {
                is BackupWriteResult.Success -> {
                    backupPreferences.recordBackup(
                        BackupDestination.DEVICE_FILE,
                    )
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = false,
                            history = service.history(),
                            lastBackupDestination =
                                BackupDestination.DEVICE_FILE,
                            message =
                                BackupUiMessage.BackupCreated(
                                    result.preview,
                                ),
                        )
                }

                is BackupWriteResult.Failure -> {
                    fail(result.reason)
                }
            }
        }
    }

    fun inspectRestore(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            updateBusy(true)
            when (val result = service.inspectBackup(uri)) {
                is BackupReadResult.Success -> {
                    pendingFileRestoreUri = uri
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = false,
                            pendingRestorePreview =
                                result.preview,
                        )
                }

                is BackupReadResult.Failure -> {
                    fail(result.reason)
                }
            }
        }
    }

    fun confirmRestore() {
        val uri = pendingFileRestoreUri ?: return

        viewModelScope.launch {
            updateBusy(true)
            when (val result = service.restoreBackup(uri)) {
                is BackupRestoreResult.Success -> {
                    backupPreferences.recordRestore(
                        BackupDestination.DEVICE_FILE,
                    )
                    pendingFileRestoreUri = null
                    mutableState.value =
                        mutableState.value.copy(
                            isBusy = false,
                            history = service.history(),
                            lastRestoreSource =
                                BackupDestination.DEVICE_FILE,
                            pendingRestorePreview = null,
                            message =
                                BackupUiMessage.RestoreCompleted(
                                    result.preview,
                                ),
                        )
                }

                is BackupRestoreResult.Failure -> {
                    fail(result.reason)
                }
            }
        }
    }

    fun cancelRestore() {
        pendingFileRestoreUri = null
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

    private fun updateBusy(busy: Boolean) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = busy,
                message = null,
            )
    }

    private fun fail(reason: BackupFailureReason) {
        mutableState.value =
            mutableState.value.copy(
                isBusy = false,
                pendingRestorePreview = null,
                message = BackupUiMessage.Failure(reason),
            )
    }
}
