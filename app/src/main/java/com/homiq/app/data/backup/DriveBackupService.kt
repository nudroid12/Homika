package com.homiq.app.data.backup

import android.app.PendingIntent
import android.content.Intent
import androidx.room.withTransaction
import com.homiq.app.data.local.HomiqDatabase
import com.homiq.app.data.sync.DriveAuthorizationResult
import com.homiq.app.data.sync.GoogleDriveAuthorization
import com.homiq.app.data.sync.SyncFailureReason
import com.homiq.app.data.account.AccountPreferences
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class DriveBackupFailureReason {
    NOT_CONNECTED,
    AUTHORIZATION_FAILED,
    AUTHORIZATION_CANCELLED,
    NETWORK_UNAVAILABLE,
    DRIVE_ACCESS_FAILED,
    BACKUP_NOT_FOUND,
    INVALID_BACKUP,
    UNSUPPORTED_FORMAT,
    UNSUPPORTED_DATABASE_VERSION,
    RESTORE_FAILED,
}

sealed interface DriveBackupWriteResult {
    data class Success(
        val preview: BackupPreview,
    ) : DriveBackupWriteResult

    data class NeedsResolution(
        val pendingIntent: PendingIntent,
    ) : DriveBackupWriteResult

    data class Failure(
        val reason: DriveBackupFailureReason,
    ) : DriveBackupWriteResult
}

sealed interface DriveBackupReadResult {
    data class Success(
        val preview: BackupPreview,
        val raw: String,
    ) : DriveBackupReadResult

    data class NeedsResolution(
        val pendingIntent: PendingIntent,
    ) : DriveBackupReadResult

    data class Failure(
        val reason: DriveBackupFailureReason,
    ) : DriveBackupReadResult
}

class DriveBackupService(
    private val database: HomiqDatabase,
    private val authorization: GoogleDriveAuthorization,
    private val drive: GoogleDriveBackupClient,
    private val accountPreferences: AccountPreferences,
    private val backupPreferences: BackupPreferences,
) {
    private val mutex = Mutex()

    suspend fun createBackup():
        DriveBackupWriteResult {
        if (!accountPreferences.state.value.googleConnected) {
            return DriveBackupWriteResult.Failure(
                DriveBackupFailureReason.NOT_CONNECTED,
            )
        }

        return when (
            val auth = authorization.authorize()
        ) {
            is DriveAuthorizationResult.Authorized ->
                createWithToken(auth.accessToken)
            is DriveAuthorizationResult.NeedsResolution ->
                DriveBackupWriteResult.NeedsResolution(
                    auth.pendingIntent,
                )
            is DriveAuthorizationResult.Failure ->
                DriveBackupWriteResult.Failure(
                    mapAuthorizationFailure(
                        auth.reason,
                    ),
                )
        }
    }

    suspend fun createBackupAfterAuthorization(
        data: Intent?,
    ): DriveBackupWriteResult =
        when (
            val auth =
                authorization
                    .authorizationResultFromIntent(
                        data,
                    )
        ) {
            is DriveAuthorizationResult.Authorized ->
                createWithToken(auth.accessToken)
            is DriveAuthorizationResult.NeedsResolution ->
                DriveBackupWriteResult.NeedsResolution(
                    auth.pendingIntent,
                )
            is DriveAuthorizationResult.Failure ->
                DriveBackupWriteResult.Failure(
                    mapAuthorizationFailure(
                        auth.reason,
                    ),
                )
        }

    suspend fun readLatestBackup():
        DriveBackupReadResult {
        if (!accountPreferences.state.value.googleConnected) {
            return DriveBackupReadResult.Failure(
                DriveBackupFailureReason.NOT_CONNECTED,
            )
        }

        return when (
            val auth = authorization.authorize()
        ) {
            is DriveAuthorizationResult.Authorized ->
                readWithToken(auth.accessToken)
            is DriveAuthorizationResult.NeedsResolution ->
                DriveBackupReadResult.NeedsResolution(
                    auth.pendingIntent,
                )
            is DriveAuthorizationResult.Failure ->
                DriveBackupReadResult.Failure(
                    mapAuthorizationFailure(
                        auth.reason,
                    ),
                )
        }
    }

    suspend fun readLatestBackupAfterAuthorization(
        data: Intent?,
    ): DriveBackupReadResult =
        when (
            val auth =
                authorization
                    .authorizationResultFromIntent(
                        data,
                    )
        ) {
            is DriveAuthorizationResult.Authorized ->
                readWithToken(auth.accessToken)
            is DriveAuthorizationResult.NeedsResolution ->
                DriveBackupReadResult.NeedsResolution(
                    auth.pendingIntent,
                )
            is DriveAuthorizationResult.Failure ->
                DriveBackupReadResult.Failure(
                    mapAuthorizationFailure(
                        auth.reason,
                    ),
                )
        }

    suspend fun restoreRaw(
        raw: String,
    ): DriveBackupWriteResult =
        withContext(Dispatchers.IO) {
            val decoded =
                decodeAndValidate(raw)

            if (decoded is SnapshotDecode.Failure) {
                return@withContext DriveBackupWriteResult.Failure(
                    decoded.reason,
                )
            }

            val snapshot =
                (decoded as SnapshotDecode.Success).snapshot

            runCatching {
                database.withTransaction {
                    val dao =
                        database.backupDao()
                    dao.clearPayments()
                    dao.clearDeposits()
                    dao.clearBlockedDates()
                    dao.clearBookings()
                    dao.clearExpenses()
                    dao.clearProperties()

                    dao.upsertProperties(
                        snapshot.properties,
                    )
                    dao.upsertBookings(
                        snapshot.bookings,
                    )
                    dao.upsertPayments(
                        snapshot.payments,
                    )
                    dao.upsertDeposits(
                        snapshot.deposits,
                    )
                    dao.upsertExpenses(
                        snapshot.expenses,
                    )
                    dao.upsertBlockedDates(
                        snapshot.blockedDates,
                    )
                }

                backupPreferences.recordRestore(
                    BackupDestination.GOOGLE_DRIVE,
                )

                DriveBackupWriteResult.Success(
                    HomiqBackupCodec.preview(
                        snapshot,
                    ),
                )
            }.getOrElse {
                DriveBackupWriteResult.Failure(
                    DriveBackupFailureReason
                        .RESTORE_FAILED,
                )
            }
        }

    private suspend fun createWithToken(
        accessToken: String,
    ): DriveBackupWriteResult =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val snapshot =
                        currentSnapshot()
                    val encoded =
                        HomiqBackupCodec.encode(
                            snapshot,
                        )

                    val existing =
                        drive.findLatestBackup(
                            accessToken,
                        )

                    if (existing == null) {
                        drive.create(
                            content = encoded,
                            accessToken =
                                accessToken,
                        )
                    } else {
                        drive.update(
                            fileId = existing.id,
                            content = encoded,
                            accessToken =
                                accessToken,
                        )
                    }

                    backupPreferences.recordBackup(
                        BackupDestination
                            .GOOGLE_DRIVE,
                    )

                    DriveBackupWriteResult.Success(
                        HomiqBackupCodec.preview(
                            snapshot,
                        ),
                    )
                }.getOrElse {
                    DriveBackupWriteResult.Failure(
                        mapDriveFailure(it),
                    )
                }
            }
        }

    private suspend fun readWithToken(
        accessToken: String,
    ): DriveBackupReadResult =
        withContext(Dispatchers.IO) {
            val file =
                try {
                    drive.findLatestBackup(
                        accessToken,
                    )
                } catch (error: Throwable) {
                    return@withContext DriveBackupReadResult.Failure(
                        mapDriveFailure(error),
                    )
                }

            if (file == null) {
                return@withContext DriveBackupReadResult.Failure(
                    DriveBackupFailureReason.BACKUP_NOT_FOUND,
                )
            }

            runCatching {
                val raw =
                    drive.download(
                        fileId = file.id,
                        accessToken =
                            accessToken,
                    )

                when (
                    val decoded =
                        decodeAndValidate(raw)
                ) {
                    is SnapshotDecode.Success ->
                        DriveBackupReadResult.Success(
                            preview =
                                HomiqBackupCodec
                                    .preview(
                                        decoded.snapshot,
                                    ),
                            raw = raw,
                        )
                    is SnapshotDecode.Failure ->
                        DriveBackupReadResult.Failure(
                            decoded.reason,
                        )
                }
            }.getOrElse {
                DriveBackupReadResult.Failure(
                    mapDriveFailure(it),
                )
            }
        }

    private suspend fun currentSnapshot():
        HomiqBackupSnapshot =
        database.withTransaction {
            val dao =
                database.backupDao()
            HomiqBackupSnapshot(
                createdAtEpochMillis =
                    System.currentTimeMillis(),
                properties =
                    dao.allProperties(),
                bookings =
                    dao.allBookings(),
                payments =
                    dao.allPayments(),
                deposits =
                    dao.allDeposits(),
                expenses =
                    dao.allExpenses(),
                blockedDates =
                    dao.allBlockedDates(),
            )
        }

    private fun decodeAndValidate(
        raw: String,
    ): SnapshotDecode {
        if (raw.isBlank()) {
            return SnapshotDecode.Failure(
                DriveBackupFailureReason
                    .INVALID_BACKUP,
            )
        }

        val snapshot =
            try {
                HomiqBackupCodec.decode(raw)
            } catch (
                error: IllegalArgumentException
            ) {
                val message =
                    error.message.orEmpty()
                        .lowercase()
                return when {
                    "format" in message ->
                        SnapshotDecode.Failure(
                            DriveBackupFailureReason
                                .UNSUPPORTED_FORMAT,
                        )
                    "database" in message ->
                        SnapshotDecode.Failure(
                            DriveBackupFailureReason
                                .UNSUPPORTED_DATABASE_VERSION,
                        )
                    else ->
                        SnapshotDecode.Failure(
                            DriveBackupFailureReason
                                .INVALID_BACKUP,
                        )
                }
            } catch (
                error: Exception
            ) {
                return SnapshotDecode.Failure(
                    DriveBackupFailureReason
                        .INVALID_BACKUP,
                )
            }

        return runCatching {
            validate(snapshot)
            SnapshotDecode.Success(snapshot)
        }.getOrElse {
            SnapshotDecode.Failure(
                DriveBackupFailureReason
                    .INVALID_BACKUP,
            )
        }
    }

    private fun validate(
        snapshot: HomiqBackupSnapshot,
    ) {
        val propertyIds =
            snapshot.properties
                .map { it.id }
                .toSet()
        val bookingIds =
            snapshot.bookings
                .map { it.id }
                .toSet()

        require(
            snapshot.bookings.all {
                it.propertyId in propertyIds
            },
        )
        require(
            snapshot.payments.all {
                it.bookingId in bookingIds
            },
        )
        require(
            snapshot.deposits.all {
                it.bookingId in bookingIds
            },
        )
        require(
            snapshot.expenses.all {
                it.propertyId == null ||
                    it.propertyId in propertyIds
            },
        )
        require(
            snapshot.blockedDates.all {
                it.propertyId in propertyIds
            },
        )
        require(
            snapshot.deposits
                .groupBy { it.bookingId }
                .values
                .all { it.size <= 1 },
        )
    }

    private fun mapAuthorizationFailure(
        reason: SyncFailureReason,
    ): DriveBackupFailureReason =
        when (reason) {
            SyncFailureReason
                .AUTHORIZATION_CANCELLED ->
                DriveBackupFailureReason
                    .AUTHORIZATION_CANCELLED

            SyncFailureReason
                .NETWORK_UNAVAILABLE ->
                DriveBackupFailureReason
                    .NETWORK_UNAVAILABLE

            else ->
                DriveBackupFailureReason
                    .AUTHORIZATION_FAILED
        }

    private fun mapDriveFailure(
        error: Throwable,
    ): DriveBackupFailureReason =
        when (error) {
            is DriveBackupHttpException ->
                when (error.statusCode) {
                    401, 403 ->
                        DriveBackupFailureReason
                            .AUTHORIZATION_FAILED
                    429,
                    in 500..599 ->
                        DriveBackupFailureReason
                            .NETWORK_UNAVAILABLE
                    else ->
                        DriveBackupFailureReason
                            .DRIVE_ACCESS_FAILED
                }

            is IOException ->
                DriveBackupFailureReason
                    .NETWORK_UNAVAILABLE

            else ->
                DriveBackupFailureReason
                    .DRIVE_ACCESS_FAILED
        }

    private sealed interface SnapshotDecode {
        data class Success(
            val snapshot: HomiqBackupSnapshot,
        ) : SnapshotDecode

        data class Failure(
            val reason:
                DriveBackupFailureReason,
        ) : SnapshotDecode
    }
}
