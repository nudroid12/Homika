package com.homiq.app.data.cloud

import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.HomiqBackupSnapshot

data class CloudBackupMetadata(
    val id: String,
    val createdAtEpochMillis: Long,
    val recordCount: Int,
    val byteSize: Long,
    val sha256: String,
)

data class PreparedCloudRestore(
    val snapshot: HomiqBackupSnapshot,
    val preview: BackupPreview,
    val metadata: CloudBackupMetadata,
)

data class CloudBackupResult<out T>(
    val value: T? = null,
    val failure: CloudBackupFailureReason? = null,
) {
    val isSuccess: Boolean
        get() = failure == null

    companion object {
        fun <T> success(value: T): CloudBackupResult<T> =
            CloudBackupResult(value = value)

        fun <T> failure(reason: CloudBackupFailureReason): CloudBackupResult<T> =
            CloudBackupResult(failure = reason)
    }
}

enum class CloudBackupFailureReason {
    LICENSE_REQUIRED,
    NETWORK_UNAVAILABLE,
    CLOUD_NOT_CONFIGURED,
    BACKUP_NOT_FOUND,
    BACKUP_TOO_LARGE,
    INVALID_CLOUD_BACKUP,
    SERVER_REJECTED,
    SERVER_ERROR,
}
