package com.homiq.app.data.cloud

import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.backup.HomiqBackupService
import com.homiq.app.data.license.LicenseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HomikaCloudBackupService(
    private val backupService: HomiqBackupService,
    private val licenseRepository: LicenseRepository,
    private val api: CloudBackupApiClient = CloudBackupApiClient(),
) {
    private val uploadMutex = Mutex()

    suspend fun latest(): CloudBackupResult<CloudBackupMetadata?> {
        val credentials = licenseRepository.cloudCredentials()
            ?: return CloudBackupResult.failure(CloudBackupFailureReason.LICENSE_REQUIRED)
        return api.latest(credentials)
    }

    suspend fun backupNow(): CloudBackupResult<Pair<CloudBackupMetadata, BackupPreview>> =
        uploadMutex.withLock {
            val credentials = licenseRepository.cloudCredentials()
                ?: return@withLock CloudBackupResult.failure(
                    CloudBackupFailureReason.LICENSE_REQUIRED,
                )

            val snapshot = runCatching { backupService.captureSnapshot() }
                .getOrElse {
                    return@withLock CloudBackupResult.failure(
                        CloudBackupFailureReason.SERVER_ERROR,
                    )
                }
            val preview = HomiqBackupCodec.preview(snapshot)
            val raw = runCatching { HomiqBackupCodec.encode(snapshot) }
                .getOrElse {
                    return@withLock CloudBackupResult.failure(
                        CloudBackupFailureReason.INVALID_CLOUD_BACKUP,
                    )
                }

            val keyResult = api.fetchCloudKey(credentials)
            val key = keyResult.value
                ?: return@withLock CloudBackupResult.failure(
                    keyResult.failure ?: CloudBackupFailureReason.SERVER_ERROR,
                )

            val encrypted = runCatching {
                CloudBackupCrypto.encrypt(raw, key)
            }.getOrElse {
                return@withLock CloudBackupResult.failure(
                    CloudBackupFailureReason.INVALID_CLOUD_BACKUP,
                )
            }

            val uploaded = api.upload(credentials, encrypted, preview)
            val metadata = uploaded.value
                ?: return@withLock CloudBackupResult.failure(
                    uploaded.failure ?: CloudBackupFailureReason.SERVER_ERROR,
                )

            CloudBackupResult.success(metadata to preview)
        }

    suspend fun prepareLatestRestore(): CloudBackupResult<PreparedCloudRestore> {
        val credentials = licenseRepository.cloudCredentials()
            ?: return CloudBackupResult.failure(CloudBackupFailureReason.LICENSE_REQUIRED)

        val latest = api.latest(credentials)
        if (!latest.isSuccess) {
            return CloudBackupResult.failure(
                latest.failure ?: CloudBackupFailureReason.SERVER_ERROR,
            )
        }
        val metadata = latest.value
            ?: return CloudBackupResult.failure(CloudBackupFailureReason.BACKUP_NOT_FOUND)

        val keyResult = api.fetchCloudKey(credentials)
        val key = keyResult.value
            ?: return CloudBackupResult.failure(
                keyResult.failure ?: CloudBackupFailureReason.SERVER_ERROR,
            )

        val downloaded = api.downloadLatest(credentials)
        val encrypted = downloaded.value
            ?: return CloudBackupResult.failure(
                downloaded.failure ?: CloudBackupFailureReason.SERVER_ERROR,
            )

        val snapshot = runCatching {
            val raw = CloudBackupCrypto.decrypt(encrypted, key)
            HomiqBackupCodec.decode(raw)
        }.getOrElse {
            return CloudBackupResult.failure(CloudBackupFailureReason.INVALID_CLOUD_BACKUP)
        }

        return CloudBackupResult.success(
            PreparedCloudRestore(
                snapshot = snapshot,
                preview = HomiqBackupCodec.preview(snapshot),
                metadata = metadata,
            ),
        )
    }
}
