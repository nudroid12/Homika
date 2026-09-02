package com.homiq.app.data.sync

import androidx.room.withTransaction
import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.backup.HomiqBackupSnapshot
import com.homiq.app.data.local.HomiqDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncEngineResult(
    val conflictCount: Int,
    val remoteDeviceCount: Int,
    val totalRecordCount: Int,
    val ignoredRemoteFileCount: Int,
)

class HomiqSyncEngine(
    private val database: HomiqDatabase,
    private val drive: GoogleDriveRestClient,
    private val preferences: SyncPreferences,
) {
    suspend fun sync(
        accessToken: String,
    ): SyncEngineResult =
        withContext(Dispatchers.IO) {
            val local =
                localSnapshot()
            val ownFileName =
                GoogleDriveRestClient
                    .FILE_PREFIX +
                    preferences.deviceId +
                    ".json"

            val files =
                drive.listSyncFiles(
                    accessToken,
                )

            val snapshots =
                mutableListOf(local)
            var ignored = 0

            files.forEach { file ->
                val raw =
                    runCatching {
                        drive.download(
                            fileId = file.id,
                            accessToken =
                                accessToken,
                        )
                    }.getOrElse {
                        throw it
                    }

                val snapshot =
                    runCatching {
                        HomiqBackupCodec.decode(
                            raw,
                        )
                    }.getOrNull()

                if (snapshot == null) {
                    ignored += 1
                } else {
                    snapshots += snapshot
                }
            }

            val merged =
                HomiqSyncMerger.merge(
                    snapshots,
                )

            applyMerged(
                merged.snapshot,
            )

            val encoded =
                HomiqBackupCodec.encode(
                    merged.snapshot,
                )

            val own =
                files.firstOrNull {
                    it.name == ownFileName
                }

            if (own == null) {
                drive.create(
                    fileName = ownFileName,
                    content = encoded,
                    accessToken =
                        accessToken,
                )
            } else {
                drive.update(
                    fileId = own.id,
                    content = encoded,
                    accessToken =
                        accessToken,
                )
            }

            SyncEngineResult(
                conflictCount =
                    merged.conflictCount,
                remoteDeviceCount =
                    files
                        .count {
                            it.name !=
                                ownFileName
                        },
                totalRecordCount =
                    recordCount(
                        merged.snapshot,
                    ),
                ignoredRemoteFileCount =
                    ignored,
            )
        }

    private suspend fun localSnapshot():
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

    private suspend fun applyMerged(
        snapshot: HomiqBackupSnapshot,
    ) {
        database.withTransaction {
            val dao =
                database.backupDao()

            /*
             * No table is cleared. Local rows are part of
             * the merge input, so direct upsert preserves
             * every known UUID and tombstone while adding
             * remote winners.
             */
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
    }

    private fun recordCount(
        snapshot: HomiqBackupSnapshot,
    ): Int =
        snapshot.properties.size +
            snapshot.bookings.size +
            snapshot.payments.size +
            snapshot.deposits.size +
            snapshot.expenses.size +
            snapshot.blockedDates.size
}
