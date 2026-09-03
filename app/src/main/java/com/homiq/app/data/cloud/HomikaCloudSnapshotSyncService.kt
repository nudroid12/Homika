package com.homiq.app.data.cloud

import androidx.room.withTransaction
import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.backup.HomiqBackupSnapshot
import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.local.HomiqDatabase
import com.homiq.app.data.sync.HomiqSyncMerger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomikaCloudSnapshotSyncService(
    private val database: HomiqDatabase,
    private val backupService: HomiqBackupService,
    private val licenseRepository: LicenseRepository,
    private val preferences: CloudSnapshotSyncPreferences,
    private val syncApi: CloudSnapshotSyncApiClient = CloudSnapshotSyncApiClient(),
    private val cloudBackupApi: CloudBackupApiClient = CloudBackupApiClient(),
) {
    private val syncMutex = Mutex()

    suspend fun syncOnce(): CloudSnapshotSyncResult<CloudSnapshotSyncRunSummary> =
        syncMutex.withLock {
            val credentials = licenseRepository.cloudCredentials()
                ?: return@withLock CloudSnapshotSyncResult.failure(
                    CloudSnapshotSyncFailureReason.LICENSE_REQUIRED,
                )

            val keyResult = cloudBackupApi.fetchCloudKey(credentials)
            val cloudKey = keyResult.value
                ?: return@withLock CloudSnapshotSyncResult.failure(
                    mapCloudKeyFailure(keyResult.failure),
                )

            val listResult = syncApi.listSnapshots(credentials)
            val metadata = listResult.value
                ?: return@withLock CloudSnapshotSyncResult.failure(
                    listResult.failure ?: CloudSnapshotSyncFailureReason.SERVER_ERROR,
                )

            val ownMetadata = metadata.firstOrNull { it.isCurrentDevice }
            val localSnapshot = backupService.captureSnapshot()
            val localContentSha256 = CloudSnapshotSyncCrypto.contentSha256(localSnapshot)
            val initialSync = !preferences.hasCompletedInitialSync(credentials.licenseId)

            val snapshotsToDownload = if (initialSync) {
                // Includes our own prior snapshot. This makes a cleared/reinstalled local database
                // able to recover from the device's cloud state before it uploads anything new.
                metadata
            } else {
                metadata.filter { remote ->
                    !remote.isCurrentDevice &&
                        preferences.seenContentSha256(
                            credentials.licenseId,
                            remote.deviceHash,
                        ) != remote.contentSha256
                }
            }

            val localDirty = ownMetadata == null || ownMetadata.contentSha256 != localContentSha256
            if (!localDirty && snapshotsToDownload.isEmpty()) {
                metadata.forEach { item ->
                    preferences.markSeen(credentials.licenseId, item.deviceHash, item.contentSha256)
                }
                preferences.markInitialSyncComplete(credentials.licenseId)
                preferences.localChangePending = false
                preferences.recordSuccess(
                    licenseId = credentials.licenseId,
                    epochMillis = System.currentTimeMillis(),
                    conflictCount = 0,
                    remoteDeviceCount = metadata.count { !it.isCurrentDevice },
                )
                return@withLock CloudSnapshotSyncResult.success(
                    CloudSnapshotSyncRunSummary(
                        remoteDeviceCount = metadata.count { !it.isCurrentDevice },
                        downloadedSnapshotCount = 0,
                        ignoredSnapshotCount = 0,
                        conflictCount = 0,
                        totalRecordCount = recordCount(localSnapshot),
                        remoteApplied = false,
                        uploaded = false,
                    ),
                )
            }

            val mergeInputs = mutableListOf(localSnapshot)
            val successfullyRead = mutableListOf<CloudSnapshotMetadata>()
            var ignored = 0

            for (remote in snapshotsToDownload) {
                val download = syncApi.downloadSnapshot(credentials, remote.deviceHash)
                val encrypted = download.value
                    ?: return@withLock CloudSnapshotSyncResult.failure(
                        download.failure ?: CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE,
                    )

                val decoded = runCatching {
                    require(CloudSnapshotSyncCrypto.encryptedSha256(encrypted) == remote.sha256) {
                        "Encrypted cloud sync snapshot hash mismatch."
                    }
                    val raw = CloudSnapshotSyncCrypto.decrypt(encrypted, cloudKey)
                    val snapshot = HomiqBackupCodec.decode(raw)
                    require(CloudSnapshotSyncCrypto.contentSha256(snapshot) == remote.contentSha256) {
                        "Cloud sync snapshot content hash mismatch."
                    }
                    snapshot
                }.getOrNull()

                if (decoded == null) {
                    ignored += 1
                    continue
                }

                mergeInputs += decoded
                successfullyRead += remote
            }

            val merged = HomiqSyncMerger.merge(mergeInputs)
            val mergedContentSha256 = CloudSnapshotSyncCrypto.contentSha256(merged.snapshot)
            val remoteApplied = mergedContentSha256 != localContentSha256

            if (remoteApplied) {
                applyMerged(merged.snapshot)
            }

            val shouldUpload = ownMetadata == null || ownMetadata.contentSha256 != mergedContentSha256
            var uploadedMetadata: CloudSnapshotMetadata? = null

            if (shouldUpload) {
                val raw = HomiqBackupCodec.encode(merged.snapshot)
                val encrypted = runCatching {
                    CloudSnapshotSyncCrypto.encrypt(raw, cloudKey)
                }.getOrElse {
                    return@withLock CloudSnapshotSyncResult.failure(
                        CloudSnapshotSyncFailureReason.INVALID_REMOTE_SNAPSHOT,
                    )
                }

                val upload = syncApi.uploadCurrent(
                    credentials = credentials,
                    encrypted = encrypted,
                    contentSha256 = mergedContentSha256,
                    createdAtEpochMillis = merged.snapshot.createdAtEpochMillis,
                    recordCount = recordCount(merged.snapshot),
                )
                uploadedMetadata = upload.value
                    ?: return@withLock CloudSnapshotSyncResult.failure(
                        upload.failure ?: CloudSnapshotSyncFailureReason.SERVER_ERROR,
                    )
            }

            // Mark only verified remote snapshots as seen. Corrupt snapshots remain unseen so a
            // future run can retry after the cloud object is replaced.
            successfullyRead.forEach { remote ->
                preferences.markSeen(credentials.licenseId, remote.deviceHash, remote.contentSha256)
            }
            metadata.filterNot { it in snapshotsToDownload }.forEach { item ->
                preferences.markSeen(credentials.licenseId, item.deviceHash, item.contentSha256)
            }
            (uploadedMetadata ?: ownMetadata)?.let { own ->
                preferences.markSeen(credentials.licenseId, own.deviceHash, own.contentSha256)
            }

            preferences.markInitialSyncComplete(credentials.licenseId)
            preferences.localChangePending = false
            preferences.recordSuccess(
                licenseId = credentials.licenseId,
                epochMillis = System.currentTimeMillis(),
                conflictCount = merged.conflictCount,
                remoteDeviceCount = metadata.count { !it.isCurrentDevice },
            )

            CloudSnapshotSyncResult.success(
                CloudSnapshotSyncRunSummary(
                    remoteDeviceCount = metadata.count { !it.isCurrentDevice },
                    downloadedSnapshotCount = successfullyRead.size,
                    ignoredSnapshotCount = ignored,
                    conflictCount = merged.conflictCount,
                    totalRecordCount = recordCount(merged.snapshot),
                    remoteApplied = remoteApplied,
                    uploaded = uploadedMetadata != null,
                ),
            )
        }

    private suspend fun applyMerged(snapshot: HomiqBackupSnapshot) {
        database.withTransaction {
            val dao = database.backupDao()
            // Same semantics as Homika Personal sync: never clear tables during sync. Tombstones
            // and revision winners are upserted so remote changes cannot resurrect deleted rows.
            dao.upsertProperties(snapshot.properties)
            dao.upsertBookings(snapshot.bookings)
            dao.upsertPayments(snapshot.payments)
            dao.upsertDeposits(snapshot.deposits)
            dao.upsertExpenses(snapshot.expenses)
            dao.upsertBlockedDates(snapshot.blockedDates)
        }
    }

    private fun mapCloudKeyFailure(reason: CloudBackupFailureReason?): CloudSnapshotSyncFailureReason =
        when (reason) {
            CloudBackupFailureReason.LICENSE_REQUIRED -> CloudSnapshotSyncFailureReason.LICENSE_REQUIRED
            CloudBackupFailureReason.NETWORK_UNAVAILABLE -> CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE
            CloudBackupFailureReason.CLOUD_NOT_CONFIGURED -> CloudSnapshotSyncFailureReason.CLOUD_NOT_CONFIGURED
            CloudBackupFailureReason.SERVER_ERROR -> CloudSnapshotSyncFailureReason.SERVER_ERROR
            else -> CloudSnapshotSyncFailureReason.SERVER_REJECTED
        }

    private fun recordCount(snapshot: HomiqBackupSnapshot): Int =
        snapshot.properties.size +
            snapshot.bookings.size +
            snapshot.payments.size +
            snapshot.deposits.size +
            snapshot.expenses.size +
            snapshot.blockedDates.size
}
