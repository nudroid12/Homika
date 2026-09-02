package com.homiq.app.data.sync

import com.homiq.app.data.backup.HomiqBackupSnapshot
import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.ExpenseEntity
import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.local.entity.PropertyEntity

data class SyncMergeResult(
    val snapshot: HomiqBackupSnapshot,
    val conflictCount: Int,
)

object HomiqSyncMerger {
    fun merge(
        snapshots: List<HomiqBackupSnapshot>,
        nowEpochMillis: Long =
            System.currentTimeMillis(),
    ): SyncMergeResult {
        val conflictKeys =
            mutableSetOf<String>()

        val properties =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.properties
                    },
                type = "property",
                key = { it.id },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        val bookings =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.bookings
                    },
                type = "booking",
                key = { it.id },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        val payments =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.payments
                    },
                type = "payment",
                key = { it.id },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        /*
         * Deposit has a UNIQUE bookingId constraint.
         * Its semantic identity is therefore bookingId,
         * not only the randomly generated row UUID.
         */
        val deposits =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.deposits
                    },
                type = "deposit",
                key = { it.bookingId },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        val expenses =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.expenses
                    },
                type = "expense",
                key = { it.id },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        val blockedDates =
            mergeRecords(
                lists =
                    snapshots.map {
                        it.blockedDates
                    },
                type = "blockedDate",
                key = { it.id },
                revision = { it.revision },
                updatedAt = {
                    it.updatedAtEpochMillis
                },
                fingerprint = {
                    it.toString()
                },
                conflictKeys =
                    conflictKeys,
            )

        return SyncMergeResult(
            snapshot =
                HomiqBackupSnapshot(
                    createdAtEpochMillis =
                        nowEpochMillis,
                    properties = properties,
                    bookings = bookings,
                    payments = payments,
                    deposits = deposits,
                    expenses = expenses,
                    blockedDates =
                        blockedDates,
                ),
            conflictCount =
                conflictKeys.size,
        )
    }

    private fun <T> mergeRecords(
        lists: List<List<T>>,
        type: String,
        key: (T) -> String,
        revision: (T) -> Long,
        updatedAt: (T) -> Long,
        fingerprint: (T) -> String,
        conflictKeys: MutableSet<String>,
    ): List<T> {
        val winners =
            linkedMapOf<String, T>()

        lists.forEach { items ->
            items.forEach { candidate ->
                val recordKey =
                    key(candidate)
                val current =
                    winners[recordKey]

                if (current == null) {
                    winners[recordKey] =
                        candidate
                } else {
                    val currentFingerprint =
                        fingerprint(current)
                    val candidateFingerprint =
                        fingerprint(candidate)

                    if (
                        revision(current) ==
                        revision(candidate) &&
                        currentFingerprint !=
                        candidateFingerprint
                    ) {
                        conflictKeys +=
                            "$type:$recordKey"
                    }

                    winners[recordKey] =
                        chooseWinner(
                            first = current,
                            second = candidate,
                            revision = revision,
                            updatedAt = updatedAt,
                            fingerprint =
                                fingerprint,
                        )
                }
            }
        }

        return winners.entries
            .sortedBy { it.key }
            .map { it.value }
    }

    private fun <T> chooseWinner(
        first: T,
        second: T,
        revision: (T) -> Long,
        updatedAt: (T) -> Long,
        fingerprint: (T) -> String,
    ): T {
        val revisionCompare =
            revision(second)
                .compareTo(revision(first))
        if (revisionCompare > 0) {
            return second
        }
        if (revisionCompare < 0) {
            return first
        }

        val timeCompare =
            updatedAt(second)
                .compareTo(updatedAt(first))
        if (timeCompare > 0) {
            return second
        }
        if (timeCompare < 0) {
            return first
        }

        return if (
            fingerprint(second) >
            fingerprint(first)
        ) {
            second
        } else {
            first
        }
    }
}
