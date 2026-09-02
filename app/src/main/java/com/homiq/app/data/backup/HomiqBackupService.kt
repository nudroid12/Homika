package com.homiq.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.homiq.app.data.local.HomiqDatabase
import java.io.BufferedReader
import java.io.InputStreamReader

class HomiqBackupService(
    private val context: Context,
    private val database: HomiqDatabase,
) {
    private val dao
        get() = database.backupDao()

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    suspend fun writeBackup(
        uri: Uri,
    ): BackupWriteResult =
        runCatching {
            val snapshot =
                database.withTransaction {
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

            val encoded =
                HomiqBackupCodec.encode(snapshot)

            val output =
                context.contentResolver
                    .openOutputStream(
                        uri,
                        "wt",
                    )
                    ?: return BackupWriteResult.Failure(
                        BackupFailureReason.FILE_UNAVAILABLE,
                    )

            output.bufferedWriter(
                Charsets.UTF_8,
            ).use {
                it.write(encoded)
            }

            preferences.edit()
                .putLong(
                    KEY_LAST_BACKUP,
                    System.currentTimeMillis(),
                )
                .apply()

            BackupWriteResult.Success(
                HomiqBackupCodec.preview(
                    snapshot,
                ),
            )
        }.getOrElse {
            BackupWriteResult.Failure(
                BackupFailureReason.WRITE_FAILED,
            )
        }

    suspend fun inspectBackup(
        uri: Uri,
    ): BackupReadResult {
        val snapshot = when (
            val read = readSnapshot(uri)
        ) {
            is SnapshotRead.Success ->
                read.snapshot

            is SnapshotRead.Failure ->
                return BackupReadResult.Failure(
                    read.reason,
                )
        }

        return BackupReadResult.Success(
            HomiqBackupCodec.preview(snapshot),
        )
    }

    suspend fun restoreBackup(
        uri: Uri,
    ): BackupRestoreResult {
        val snapshot = when (
            val read = readSnapshot(uri)
        ) {
            is SnapshotRead.Success ->
                read.snapshot

            is SnapshotRead.Failure ->
                return BackupRestoreResult.Failure(
                    read.reason,
                )
        }

        return runCatching {
            database.withTransaction {
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

            preferences.edit()
                .putLong(
                    KEY_LAST_RESTORE,
                    System.currentTimeMillis(),
                )
                .apply()

            BackupRestoreResult.Success(
                HomiqBackupCodec.preview(snapshot),
            )
        }.getOrElse {
            BackupRestoreResult.Failure(
                BackupFailureReason.RESTORE_FAILED,
            )
        }
    }

    fun history(): BackupHistory =
        BackupHistory(
            lastBackupEpochMillis =
                preferences
                    .getLong(
                        KEY_LAST_BACKUP,
                        0L,
                    )
                    .takeIf { it > 0L },
            lastRestoreEpochMillis =
                preferences
                    .getLong(
                        KEY_LAST_RESTORE,
                        0L,
                    )
                    .takeIf { it > 0L },
        )

    private fun readSnapshot(
        uri: Uri,
    ): SnapshotRead {
        val raw = runCatching {
            val input =
                context.contentResolver
                    .openInputStream(uri)
                    ?: return SnapshotRead.Failure(
                        BackupFailureReason.FILE_UNAVAILABLE,
                    )

            BufferedReader(
                InputStreamReader(
                    input,
                    Charsets.UTF_8,
                ),
            ).use {
                it.readText()
            }
        }.getOrElse {
            return SnapshotRead.Failure(
                BackupFailureReason.FILE_UNAVAILABLE,
            )
        }

        if (raw.isBlank()) {
            return SnapshotRead.Failure(
                BackupFailureReason.INVALID_BACKUP,
            )
        }

        return try {
            val snapshot =
                HomiqBackupCodec.decode(raw)

            validate(snapshot)
            SnapshotRead.Success(snapshot)
        } catch (
            error: IllegalArgumentException
        ) {
            val message =
                error.message.orEmpty()
            when {
                "format" in message.lowercase() ->
                    SnapshotRead.Failure(
                        BackupFailureReason.UNSUPPORTED_FORMAT,
                    )

                "database" in message.lowercase() ->
                    SnapshotRead.Failure(
                        BackupFailureReason
                            .UNSUPPORTED_DATABASE_VERSION,
                    )

                else ->
                    SnapshotRead.Failure(
                        BackupFailureReason.INVALID_BACKUP,
                    )
            }
        } catch (
            error: Exception
        ) {
            SnapshotRead.Failure(
                BackupFailureReason.INVALID_BACKUP,
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
        ) {
            "Booking references missing property."
        }

        require(
            snapshot.payments.all {
                it.bookingId in bookingIds
            },
        ) {
            "Payment references missing booking."
        }

        require(
            snapshot.deposits.all {
                it.bookingId in bookingIds
            },
        ) {
            "Deposit references missing booking."
        }

        require(
            snapshot.expenses.all {
                it.propertyId == null ||
                    it.propertyId in propertyIds
            },
        ) {
            "Expense references missing property."
        }

        require(
            snapshot.blockedDates.all {
                it.propertyId in propertyIds
            },
        ) {
            "Block references missing property."
        }

        require(
            snapshot.deposits
                .groupBy { it.bookingId }
                .values
                .all { it.size <= 1 },
        ) {
            "Duplicate deposits for booking."
        }
    }

    private sealed interface SnapshotRead {
        data class Success(
            val snapshot: HomiqBackupSnapshot,
        ) : SnapshotRead

        data class Failure(
            val reason: BackupFailureReason,
        ) : SnapshotRead
    }

    companion object {
        private const val PREFERENCES_NAME =
            "homiq_backup"
        private const val KEY_LAST_BACKUP =
            "last_backup"
        private const val KEY_LAST_RESTORE =
            "last_restore"
    }
}
