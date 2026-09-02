package com.homiq.app.data.sync

import com.homiq.app.data.backup.HomiqBackupSnapshot
import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.local.entity.PropertyEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomiqSyncMergerTest {
    @Test
    fun higherRevisionWins() {
        val property =
            PropertyEntity(
                id = "p1",
                name = "A",
                revision = 1L,
                updatedAtEpochMillis = 10L,
            )
        val newer =
            property.copy(
                name = "B",
                revision = 2L,
                updatedAtEpochMillis = 11L,
            )

        val result =
            HomiqSyncMerger.merge(
                listOf(
                    snapshot(
                        properties =
                            listOf(property),
                    ),
                    snapshot(
                        properties =
                            listOf(newer),
                    ),
                ),
                nowEpochMillis = 20L,
            )

        assertEquals(
            "B",
            result.snapshot
                .properties
                .single()
                .name,
        )
    }

    @Test
    fun sameRevisionConflictIsCountedAndDeterministic() {
        val first =
            PropertyEntity(
                id = "p1",
                name = "Alpha",
                revision = 3L,
                updatedAtEpochMillis = 100L,
            )
        val second =
            first.copy(
                name = "Beta",
                updatedAtEpochMillis = 101L,
            )

        val result =
            HomiqSyncMerger.merge(
                listOf(
                    snapshot(
                        properties =
                            listOf(first),
                    ),
                    snapshot(
                        properties =
                            listOf(second),
                    ),
                ),
                nowEpochMillis = 200L,
            )

        assertEquals(
            1,
            result.conflictCount,
        )
        assertEquals(
            "Beta",
            result.snapshot
                .properties
                .single()
                .name,
        )
    }

    @Test
    fun depositMergesByBookingIdentity() {
        val first =
            DepositEntity(
                id = "d1",
                bookingId = "b1",
                amountSen = 10_000L,
                revision = 1L,
                updatedAtEpochMillis = 100L,
            )
        val second =
            first.copy(
                id = "d2",
                amountSen = 12_000L,
                revision = 2L,
                updatedAtEpochMillis = 101L,
            )

        val result =
            HomiqSyncMerger.merge(
                listOf(
                    snapshot(
                        deposits =
                            listOf(first),
                    ),
                    snapshot(
                        deposits =
                            listOf(second),
                    ),
                ),
                nowEpochMillis = 200L,
            )

        assertEquals(
            1,
            result.snapshot.deposits.size,
        )
        assertEquals(
            12_000L,
            result.snapshot
                .deposits
                .single()
                .amountSen,
        )
    }

    private fun snapshot(
        properties:
            List<PropertyEntity> =
            emptyList(),
        bookings:
            List<BookingEntity> =
            emptyList(),
        deposits:
            List<DepositEntity> =
            emptyList(),
    ) =
        HomiqBackupSnapshot(
            createdAtEpochMillis = 1L,
            properties = properties,
            bookings = bookings,
            payments = emptyList(),
            deposits = deposits,
            expenses = emptyList(),
            blockedDates = emptyList(),
        )
}
