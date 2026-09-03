package com.homiq.app.data.cloud

enum class CloudSyncEntityType(
    val wireName: String,
) {
    PROPERTY("property"),
    BOOKING("booking"),
    PAYMENT("payment"),
    DEPOSIT("deposit"),
    EXPENSE("expense"),
    BLOCKED_DATE("blocked_date"),
    ;

    companion object {
        fun fromWireName(value: String): CloudSyncEntityType? =
            entries.firstOrNull { it.wireName == value }
    }
}

data class CloudSyncLocalRecord(
    val type: CloudSyncEntityType,
    val entityId: String,
    val revision: Long,
    val updatedAtEpochMillis: Long,
    val isDeleted: Boolean,
    val rawJson: String,
)

data class CloudSyncPushChange(
    val type: CloudSyncEntityType,
    val entityId: String,
    val revision: Long,
    val baseRevision: Long,
    val updatedAtEpochMillis: Long,
    val isDeleted: Boolean,
    val payloadBase64: String,
    val contentSha256: String,
)

data class CloudSyncAcceptedChange(
    val type: CloudSyncEntityType,
    val entityId: String,
    val revision: Long,
    val serverSequence: Long,
)

data class CloudSyncRemoteChange(
    val serverSequence: Long,
    val type: CloudSyncEntityType,
    val entityId: String,
    val revision: Long,
    val updatedAtEpochMillis: Long,
    val isDeleted: Boolean,
    val payloadBase64: String,
    val contentSha256: String,
    val sourceDeviceHash: String,
)

data class CloudSyncServerConflict(
    val type: CloudSyncEntityType,
    val entityId: String,
    val localRevision: Long,
    val baseRevision: Long,
    val reason: String,
    val current: CloudSyncRemoteChange?,
)

data class CloudSyncPushResponse(
    val accepted: List<CloudSyncAcceptedChange>,
    val conflicts: List<CloudSyncServerConflict>,
)

data class CloudSyncPullResponse(
    val nextCursor: Long,
    val hasMore: Boolean,
    val changes: List<CloudSyncRemoteChange>,
)

data class CloudSyncConflictRecord(
    val type: CloudSyncEntityType,
    val entityId: String,
    val localRevision: Long,
    val remoteRevision: Long,
    val serverSequence: Long,
    val remotePayloadBase64: String,
    val remoteContentSha256: String,
    val reason: String,
    val detectedAtEpochMillis: Long = System.currentTimeMillis(),
)

data class CloudSyncRunSummary(
    val pulled: Int,
    val pushed: Int,
    val conflicts: Int,
    val remoteApplied: Int,
    val cursor: Long,
)

data class CloudSyncResult<out T>(
    val value: T? = null,
    val failure: CloudSyncFailureReason? = null,
) {
    val isSuccess: Boolean
        get() = failure == null

    companion object {
        fun <T> success(value: T): CloudSyncResult<T> =
            CloudSyncResult(value = value)

        fun <T> failure(reason: CloudSyncFailureReason): CloudSyncResult<T> =
            CloudSyncResult(failure = reason)
    }
}

enum class CloudSyncFailureReason {
    LICENSE_REQUIRED,
    NETWORK_UNAVAILABLE,
    CLOUD_NOT_CONFIGURED,
    INVALID_REMOTE_CHANGE,
    SERVER_REJECTED,
    SERVER_ERROR,
}
