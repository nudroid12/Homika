package com.homiq.app.data.cloud

import androidx.room.withTransaction
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.local.HomiqDatabase
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomikaCloudSyncService(
    private val database: HomiqDatabase,
    private val backupService: HomiqBackupService,
    private val licenseRepository: LicenseRepository,
    private val preferences: CloudSyncPreferences,
    private val syncApi: CloudSyncApiClient = CloudSyncApiClient(),
    private val cloudBackupApi: CloudBackupApiClient = CloudBackupApiClient(),
) {
    private val syncMutex = Mutex()

    suspend fun syncOnce(): CloudSyncResult<CloudSyncRunSummary> =
        syncMutex.withLock {
            val credentials = licenseRepository.cloudCredentials()
                ?: return@withLock CloudSyncResult.failure(CloudSyncFailureReason.LICENSE_REQUIRED)

            val keyResult = cloudBackupApi.fetchCloudKey(credentials)
            val cloudKey = keyResult.value
                ?: return@withLock CloudSyncResult.failure(
                    when (keyResult.failure) {
                        CloudBackupFailureReason.LICENSE_REQUIRED -> CloudSyncFailureReason.LICENSE_REQUIRED
                        CloudBackupFailureReason.NETWORK_UNAVAILABLE -> CloudSyncFailureReason.NETWORK_UNAVAILABLE
                        CloudBackupFailureReason.CLOUD_NOT_CONFIGURED -> CloudSyncFailureReason.CLOUD_NOT_CONFIGURED
                        CloudBackupFailureReason.SERVER_ERROR -> CloudSyncFailureReason.SERVER_ERROR
                        else -> CloudSyncFailureReason.SERVER_REJECTED
                    },
                )

            var pulled = 0
            var pushed = 0
            var conflictCount = 0
            var remoteApplied = 0
            var cursor = preferences.cursor(credentials.licenseId)
            var pages = 0
            var hasMore: Boolean

            do {
                val pull = syncApi.pull(
                    credentials = credentials,
                    cursor = cursor,
                    limit = PULL_PAGE_SIZE,
                )
                val page = pull.value
                    ?: return@withLock CloudSyncResult.failure(
                        pull.failure ?: CloudSyncFailureReason.SERVER_ERROR,
                    )

                val applied = applyRemotePage(
                    licenseId = credentials.licenseId,
                    cloudKey = cloudKey,
                    changes = page.changes,
                )
                if (applied.failure != null) {
                    return@withLock CloudSyncResult.failure(applied.failure)
                }

                pulled += page.changes.size
                remoteApplied += applied.applied
                conflictCount += applied.conflicts
                cursor = page.nextCursor
                preferences.setCursor(credentials.licenseId, cursor)
                hasMore = page.hasMore
                pages += 1
            } while (hasMore && pages < MAX_PULL_PAGES_PER_RUN)

            val snapshot = backupService.captureSnapshot()
            val pending = CloudSyncCodec.recordsFromSnapshot(snapshot)
                .filter { record ->
                    !preferences.hasConflict(
                        credentials.licenseId,
                        record.type,
                        record.entityId,
                    ) && run {
                        val acknowledged = preferences.acknowledgedRevision(
                            credentials.licenseId,
                            record.type,
                            record.entityId,
                        )
                        acknowledged == null || record.revision > acknowledged
                    }
                }

            for (chunk in pending.chunked(PUSH_BATCH_SIZE)) {
                val prepared = runCatching {
                    chunk.map { record ->
                        val acknowledged = preferences.acknowledgedRevision(
                            credentials.licenseId,
                            record.type,
                            record.entityId,
                        ) ?: 0L
                        CloudSyncPushChange(
                            type = record.type,
                            entityId = record.entityId,
                            revision = record.revision,
                            baseRevision = acknowledged,
                            updatedAtEpochMillis = record.updatedAtEpochMillis,
                            isDeleted = record.isDeleted,
                            payloadBase64 = CloudSyncCrypto.encrypt(
                                rawJson = record.rawJson,
                                keyBase64 = cloudKey,
                                type = record.type,
                                entityId = record.entityId,
                                revision = record.revision,
                            ),
                            contentSha256 = CloudSyncCrypto.contentSha256(record.rawJson),
                        )
                    }
                }.getOrElse {
                    return@withLock CloudSyncResult.failure(CloudSyncFailureReason.SERVER_ERROR)
                }

                val push = syncApi.push(credentials, prepared)
                val response = push.value
                    ?: return@withLock CloudSyncResult.failure(
                        push.failure ?: CloudSyncFailureReason.SERVER_ERROR,
                    )

                response.accepted.forEach { accepted ->
                    preferences.setAcknowledgedRevision(
                        credentials.licenseId,
                        accepted.type,
                        accepted.entityId,
                        accepted.revision,
                    )
                }
                pushed += response.accepted.size

                response.conflicts.forEach { conflict ->
                    val current = conflict.current ?: return@forEach
                    preferences.recordConflict(
                        credentials.licenseId,
                        CloudSyncConflictRecord(
                            type = conflict.type,
                            entityId = conflict.entityId,
                            localRevision = conflict.localRevision,
                            remoteRevision = current.revision,
                            serverSequence = current.serverSequence,
                            remotePayloadBase64 = current.payloadBase64,
                            remoteContentSha256 = current.contentSha256,
                            reason = conflict.reason,
                        ),
                    )
                    conflictCount += 1
                }
            }

            preferences.recordSuccess(credentials.licenseId)
            CloudSyncResult.success(
                CloudSyncRunSummary(
                    pulled = pulled,
                    pushed = pushed,
                    conflicts = conflictCount,
                    remoteApplied = remoteApplied,
                    cursor = cursor,
                ),
            )
        }

    private suspend fun applyRemotePage(
        licenseId: String,
        cloudKey: String,
        changes: List<CloudSyncRemoteChange>,
    ): ApplyRemotePageResult {
        if (changes.isEmpty()) return ApplyRemotePageResult()

        val localSnapshot = backupService.captureSnapshot()
        val localRecords = CloudSyncCodec.recordsFromSnapshot(localSnapshot)
            .associateBy { recordKey(it.type, it.entityId) }

        val batch = RemoteApplyBatch()
        val acknowledgements = mutableListOf<CloudSyncRemoteChange>()
        var conflicts = 0

        for (remote in changes) {
            val acknowledged = preferences.acknowledgedRevision(
                licenseId,
                remote.type,
                remote.entityId,
            )
            if (acknowledged != null && remote.revision <= acknowledged) {
                continue
            }

            val local = localRecords[recordKey(remote.type, remote.entityId)]
            val localHasUnpushedChange = when {
                local == null -> false
                acknowledged == null -> true
                else -> local.revision > acknowledged
            }

            if (localHasUnpushedChange) {
                preferences.recordConflict(
                    licenseId,
                    CloudSyncConflictRecord(
                        type = remote.type,
                        entityId = remote.entityId,
                        localRevision = local?.revision ?: 0L,
                        remoteRevision = remote.revision,
                        serverSequence = remote.serverSequence,
                        remotePayloadBase64 = remote.payloadBase64,
                        remoteContentSha256 = remote.contentSha256,
                        reason = "local_and_remote_changed",
                    ),
                )
                conflicts += 1
                continue
            }

            val decoded = runCatching {
                val raw = CloudSyncCrypto.decrypt(
                    payloadBase64 = remote.payloadBase64,
                    keyBase64 = cloudKey,
                    type = remote.type,
                    entityId = remote.entityId,
                    revision = remote.revision,
                )
                require(CloudSyncCrypto.contentSha256(raw) == remote.contentSha256) {
                    "Cloud sync content hash mismatch."
                }
                CloudSyncCodec.decode(remote.type, raw)
            }.getOrElse {
                return ApplyRemotePageResult(
                    failure = CloudSyncFailureReason.INVALID_REMOTE_CHANGE,
                )
            }

            if (
                decoded.entityId != remote.entityId ||
                decoded.revision != remote.revision ||
                decoded.updatedAtEpochMillis != remote.updatedAtEpochMillis ||
                decoded.isDeleted != remote.isDeleted
            ) {
                return ApplyRemotePageResult(
                    failure = CloudSyncFailureReason.INVALID_REMOTE_CHANGE,
                )
            }

            batch.add(decoded)
            acknowledgements += remote
        }

        if (batch.size > 0) {
            val dao = database.backupDao()
            database.withTransaction {
                if (batch.properties.isNotEmpty()) dao.upsertProperties(batch.properties)
                if (batch.bookings.isNotEmpty()) dao.upsertBookings(batch.bookings)
                if (batch.payments.isNotEmpty()) dao.upsertPayments(batch.payments)
                if (batch.deposits.isNotEmpty()) dao.upsertDeposits(batch.deposits)
                if (batch.expenses.isNotEmpty()) dao.upsertExpenses(batch.expenses)
                if (batch.blockedDates.isNotEmpty()) dao.upsertBlockedDates(batch.blockedDates)
            }

            acknowledgements.forEach { remote ->
                preferences.setAcknowledgedRevision(
                    licenseId,
                    remote.type,
                    remote.entityId,
                    remote.revision,
                )
            }
        }

        return ApplyRemotePageResult(
            applied = batch.size,
            conflicts = conflicts,
        )
    }

    private fun recordKey(
        type: CloudSyncEntityType,
        entityId: String,
    ): String = "${type.wireName}|$entityId"

    private data class ApplyRemotePageResult(
        val applied: Int = 0,
        val conflicts: Int = 0,
        val failure: CloudSyncFailureReason? = null,
    )

    private class RemoteApplyBatch {
        val properties = mutableListOf<PropertyEntity>()
        val bookings = mutableListOf<BookingEntity>()
        val payments = mutableListOf<PaymentEntity>()
        val deposits = mutableListOf<DepositEntity>()
        val expenses = mutableListOf<ExpenseEntity>()
        val blockedDates = mutableListOf<BlockedDateEntity>()

        val size: Int
            get() =
                properties.size +
                    bookings.size +
                    payments.size +
                    deposits.size +
                    expenses.size +
                    blockedDates.size

        fun add(decoded: DecodedCloudSyncEntity) {
            when (decoded) {
                is DecodedCloudSyncEntity.Property -> properties += decoded.entity
                is DecodedCloudSyncEntity.Booking -> bookings += decoded.entity
                is DecodedCloudSyncEntity.Payment -> payments += decoded.entity
                is DecodedCloudSyncEntity.Deposit -> deposits += decoded.entity
                is DecodedCloudSyncEntity.Expense -> expenses += decoded.entity
                is DecodedCloudSyncEntity.BlockedDate -> blockedDates += decoded.entity
            }
        }
    }

    companion object {
        private const val PULL_PAGE_SIZE = 100
        private const val PUSH_BATCH_SIZE = 50
        private const val MAX_PULL_PAGES_PER_RUN = 10
    }
}
