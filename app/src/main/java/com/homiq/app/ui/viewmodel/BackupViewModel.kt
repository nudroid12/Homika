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
import com.homiq.app.data.cloud.CloudAutoBackupCoordinator
import com.homiq.app.data.cloud.CloudBackupFailureReason
import com.homiq.app.data.cloud.CloudBackupMetadata
import com.homiq.app.data.cloud.CloudSnapshotSyncCoordinator
import com.homiq.app.data.cloud.CloudSnapshotSyncFailureReason
import com.homiq.app.data.cloud.HomikaCloudBackupService
import com.homiq.app.data.cloud.PreparedCloudRestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class BackupUiState(
    val isBusy: Boolean = false,
    val isCloudRefreshing: Boolean = false,
    val history: BackupHistory =
        BackupHistory(
            lastBackupEpochMillis = null,
            lastRestoreEpochMillis = null,
        ),
    val lastBackupDestination: BackupDestination? = null,
    val lastRestoreSource: BackupDestination? = null,
    val cloudLatest: CloudBackupMetadata? = null,
    val automaticCloudBackupEnabled: Boolean = true,
    val automaticCloudBackupPending: Boolean = false,
    val automaticCloudBackupRunning: Boolean = false,
    val lastAutomaticCloudBackupEpochMillis: Long? = null,
    val cloudSyncRunning: Boolean = false,
    val cloudSyncLastSuccessEpochMillis: Long? = null,
    val cloudSyncFailure: CloudSnapshotSyncFailureReason? = null,
    val cloudSyncRemoteDeviceCount: Int = 0,
    val cloudSyncConflictCount: Int = 0,
    val cloudSyncIgnoredSnapshotCount: Int = 0,
    val pendingRestorePreview: BackupPreview? = null,
    val message: BackupUiMessage? = null,
)

sealed interface BackupUiMessage {
    data class BackupCreated(
        val preview: BackupPreview,
    ) : BackupUiMessage

    data class CloudBackupCreated(
        val preview: BackupPreview,
    ) : BackupUiMessage

    data class RestoreCompleted(
        val preview: BackupPreview,
        val source: BackupDestination,
    ) : BackupUiMessage

    data class Failure(
        val reason: BackupFailureReason,
    ) : BackupUiMessage

    data class CloudFailure(
        val reason: CloudBackupFailureReason,
    ) : BackupUiMessage
}

class BackupViewModel(
    private val service: HomiqBackupService,
    private val backupPreferences: BackupPreferences,
    private val cloudService: HomikaCloudBackupService,
    private val autoBackupCoordinator: CloudAutoBackupCoordinator,
    private val cloudSyncCoordinator: CloudSnapshotSyncCoordinator,
) : ViewModel() {
    private var pendingFileRestoreUri: Uri? = null
    private var pendingCloudRestore: PreparedCloudRestore? = null

    private val mutableState =
        MutableStateFlow(
            BackupUiState(
                history = service.history(),
                lastBackupDestination = backupPreferences.lastBackupDestination,
                lastRestoreSource = backupPreferences.lastRestoreSource,
                automaticCloudBackupEnabled = autoBackupCoordinator.state.value.enabled,
                automaticCloudBackupPending = autoBackupCoordinator.state.value.pending,
                automaticCloudBackupRunning = autoBackupCoordinator.state.value.isRunning,
                lastAutomaticCloudBackupEpochMillis =
                    autoBackupCoordinator.state.value.lastSuccessEpochMillis,
            ),
        )

    val state: StateFlow<BackupUiState> = mutableState.asStateFlow()

    init {
        refreshCloud()
        viewModelScope.launch {
            cloudSyncCoordinator.state.collect { syncState ->
                mutableState.value = mutableState.value.copy(
                    cloudSyncRunning = syncState.isRunning,
                    cloudSyncLastSuccessEpochMillis = syncState.lastSuccessEpochMillis,
                    cloudSyncFailure = syncState.lastFailure,
                    cloudSyncRemoteDeviceCount = syncState.lastSummary?.remoteDeviceCount
                        ?: mutableState.value.cloudSyncRemoteDeviceCount,
                    cloudSyncConflictCount = syncState.lastSummary?.conflictCount
                        ?: mutableState.value.cloudSyncConflictCount,
                    cloudSyncIgnoredSnapshotCount = syncState.lastSummary?.ignoredSnapshotCount
                        ?: mutableState.value.cloudSyncIgnoredSnapshotCount,
                )
            }
        }
        viewModelScope.launch {
            autoBackupCoordinator.state.collect { autoState ->
                val previousAutoSuccess = mutableState.value.lastAutomaticCloudBackupEpochMillis
                mutableState.value = mutableState.value.copy(
                    automaticCloudBackupEnabled = autoState.enabled,
                    automaticCloudBackupPending = autoState.pending,
                    automaticCloudBackupRunning = autoState.isRunning,
                    lastAutomaticCloudBackupEpochMillis = autoState.lastSuccessEpochMillis,
                )
                if (
                    autoState.lastSuccessEpochMillis != null &&
                    autoState.lastSuccessEpochMillis != previousAutoSuccess
                ) {
                    refreshCloud()
                }
            }
        }
    }

    fun backupFileName(): String {
        val stamp =
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"),
            )
        return "Homika-backup-$stamp.homika"
    }

    fun refreshCloud() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isCloudRefreshing = true)
            val result = cloudService.latest()
            mutableState.value = mutableState.value.copy(
                isCloudRefreshing = false,
                cloudLatest = if (result.isSuccess) result.value else mutableState.value.cloudLatest,
            )
        }
    }

    fun syncNow() {
        cloudSyncCoordinator.syncNow()
    }

    fun createCloudBackup() {
        viewModelScope.launch {
            updateBusy(true)
            val result = cloudService.backupNow()
            val success = result.value
            if (success != null) {
                val (metadata, preview) = success
                backupPreferences.recordBackup(BackupDestination.HOMIKA_CLOUD)
                autoBackupCoordinator.markCloudCurrent(metadata.createdAtEpochMillis)
                mutableState.value = mutableState.value.copy(
                    isBusy = false,
                    cloudLatest = metadata,
                    lastBackupDestination = BackupDestination.HOMIKA_CLOUD,
                    message = BackupUiMessage.CloudBackupCreated(preview),
                )
            } else {
                cloudFail(result.failure ?: CloudBackupFailureReason.SERVER_ERROR)
            }
        }
    }


    fun setAutomaticCloudBackup(enabled: Boolean) {
        autoBackupCoordinator.setEnabled(enabled)
    }

    fun inspectCloudRestore() {
        viewModelScope.launch {
            updateBusy(true)
            val result = cloudService.prepareLatestRestore()
            val prepared = result.value
            if (prepared != null) {
                pendingFileRestoreUri = null
                pendingCloudRestore = prepared
                mutableState.value = mutableState.value.copy(
                    isBusy = false,
                    pendingRestorePreview = prepared.preview,
                )
            } else {
                cloudFail(result.failure ?: CloudBackupFailureReason.SERVER_ERROR)
            }
        }
    }

    fun createBackup(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            updateBusy(true)
            when (val result = service.writeBackup(uri)) {
                is BackupWriteResult.Success -> {
                    backupPreferences.recordBackup(BackupDestination.DEVICE_FILE)
                    mutableState.value = mutableState.value.copy(
                        isBusy = false,
                        history = service.history(),
                        lastBackupDestination = BackupDestination.DEVICE_FILE,
                        message = BackupUiMessage.BackupCreated(result.preview),
                    )
                }
                is BackupWriteResult.Failure -> fail(result.reason)
            }
        }
    }

    fun inspectRestore(uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            updateBusy(true)
            when (val result = service.inspectBackup(uri)) {
                is BackupReadResult.Success -> {
                    pendingCloudRestore = null
                    pendingFileRestoreUri = uri
                    mutableState.value = mutableState.value.copy(
                        isBusy = false,
                        pendingRestorePreview = result.preview,
                    )
                }
                is BackupReadResult.Failure -> fail(result.reason)
            }
        }
    }

    fun confirmRestore() {
        val cloud = pendingCloudRestore
        val file = pendingFileRestoreUri
        if (cloud == null && file == null) return

        viewModelScope.launch {
            updateBusy(true)
            val source: BackupDestination
            val result: BackupRestoreResult

            if (cloud != null) {
                source = BackupDestination.HOMIKA_CLOUD
                result = service.restoreSnapshot(cloud.snapshot)
            } else {
                source = BackupDestination.DEVICE_FILE
                result = service.restoreBackup(file!!)
            }

            when (result) {
                is BackupRestoreResult.Success -> {
                    backupPreferences.recordRestore(source)
                    if (source == BackupDestination.HOMIKA_CLOUD) {
                        autoBackupCoordinator.markCloudCurrent(
                            cloud!!.metadata.createdAtEpochMillis,
                        )
                    } else {
                        autoBackupCoordinator.markLocalDataChanged()
                    }
                    pendingFileRestoreUri = null
                    pendingCloudRestore = null
                    mutableState.value = mutableState.value.copy(
                        isBusy = false,
                        history = service.history(),
                        lastRestoreSource = source,
                        pendingRestorePreview = null,
                        message = BackupUiMessage.RestoreCompleted(
                            preview = result.preview,
                            source = source,
                        ),
                    )
                }
                is BackupRestoreResult.Failure -> fail(result.reason)
            }
        }
    }

    fun cancelRestore() {
        pendingFileRestoreUri = null
        pendingCloudRestore = null
        mutableState.value = mutableState.value.copy(pendingRestorePreview = null)
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun updateBusy(busy: Boolean) {
        mutableState.value = mutableState.value.copy(
            isBusy = busy,
            message = null,
        )
    }

    private fun fail(reason: BackupFailureReason) {
        mutableState.value = mutableState.value.copy(
            isBusy = false,
            pendingRestorePreview = null,
            message = BackupUiMessage.Failure(reason),
        )
    }

    private fun cloudFail(reason: CloudBackupFailureReason) {
        pendingCloudRestore = null
        mutableState.value = mutableState.value.copy(
            isBusy = false,
            pendingRestorePreview = null,
            message = BackupUiMessage.CloudFailure(reason),
        )
    }
}
