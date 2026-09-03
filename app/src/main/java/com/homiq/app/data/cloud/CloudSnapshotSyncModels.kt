package com.homiq.app.data.cloud

data class CloudSnapshotMetadata(
    val deviceHash: String,
    val updatedAtEpochMillis: Long,
    val recordCount: Int,
    val byteSize: Long,
    val sha256: String,
    val contentSha256: String,
    val isCurrentDevice: Boolean,
)

data class CloudSnapshotSyncRunSummary(
    val remoteDeviceCount: Int,
    val downloadedSnapshotCount: Int,
    val ignoredSnapshotCount: Int,
    val conflictCount: Int,
    val totalRecordCount: Int,
    val remoteApplied: Boolean,
    val uploaded: Boolean,
)

data class CloudSnapshotSyncResult<out T>(
    val value: T? = null,
    val failure: CloudSnapshotSyncFailureReason? = null,
) {
    val isSuccess: Boolean
        get() = failure == null

    companion object {
        fun <T> success(value: T): CloudSnapshotSyncResult<T> =
            CloudSnapshotSyncResult(value = value)

        fun <T> failure(reason: CloudSnapshotSyncFailureReason): CloudSnapshotSyncResult<T> =
            CloudSnapshotSyncResult(failure = reason)
    }
}

enum class CloudSnapshotSyncFailureReason {
    LICENSE_REQUIRED,
    NETWORK_UNAVAILABLE,
    CLOUD_NOT_CONFIGURED,
    INVALID_REMOTE_SNAPSHOT,
    SNAPSHOT_TOO_LARGE,
    SERVER_REJECTED,
    SERVER_ERROR,
}
