package com.homiq.app.domain

import com.homiq.app.data.local.entity.DepositEntity
import com.homiq.app.data.model.DepositStatus
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.DepositRepository
import kotlinx.coroutines.flow.first

class DepositManager(
    private val bookings: BookingRepository,
    private val deposits: DepositRepository,
) {
    suspend fun setRequired(
        bookingId: String,
        amountSen: Long,
        notes: String,
    ): DepositActionResult {
        val booking = bookings.getById(bookingId)
            ?: return DepositActionResult.Failure(
                DepositActionIssue.BOOKING_NOT_FOUND,
            )

        if (booking.isDeleted) {
            return DepositActionResult.Failure(
                DepositActionIssue.BOOKING_NOT_FOUND,
            )
        }

        if (amountSen <= 0L) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_AMOUNT,
            )
        }

        val existing = deposits
            .observeForBooking(bookingId)
            .first()

        if (
            existing != null &&
            existing.status !in setOf(
                DepositStatus.NOT_REQUIRED,
                DepositStatus.PENDING,
            )
        ) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        val entity = DepositEntity(
            id = existing?.id
                ?: java.util.UUID.randomUUID().toString(),
            bookingId = bookingId,
            amountSen = amountSen,
            status = DepositStatus.PENDING,
            receivedAtEpochDay = null,
            returnedAmountSen = 0L,
            returnedAtEpochDay = null,
            notes = notes.trim().ifBlank { null },
            createdAtEpochMillis = existing?.createdAtEpochMillis
                ?: System.currentTimeMillis(),
            updatedAtEpochMillis = existing?.updatedAtEpochMillis
                ?: System.currentTimeMillis(),
            revision = existing?.revision ?: 0L,
            isDeleted = existing?.isDeleted ?: false,
        )

        deposits.save(entity)
        return DepositActionResult.Success
    }

    suspend fun markNotRequired(
        bookingId: String,
    ): DepositActionResult {
        val booking = bookings.getById(bookingId)
            ?: return DepositActionResult.Failure(
                DepositActionIssue.BOOKING_NOT_FOUND,
            )

        if (booking.isDeleted) {
            return DepositActionResult.Failure(
                DepositActionIssue.BOOKING_NOT_FOUND,
            )
        }

        val existing = deposits
            .observeForBooking(bookingId)
            .first()

        if (
            existing != null &&
            existing.status !in setOf(
                DepositStatus.NOT_REQUIRED,
                DepositStatus.PENDING,
            )
        ) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        val entity = DepositEntity(
            id = existing?.id
                ?: java.util.UUID.randomUUID().toString(),
            bookingId = bookingId,
            amountSen = 0L,
            status = DepositStatus.NOT_REQUIRED,
            notes = existing?.notes,
            createdAtEpochMillis = existing?.createdAtEpochMillis
                ?: System.currentTimeMillis(),
            updatedAtEpochMillis = existing?.updatedAtEpochMillis
                ?: System.currentTimeMillis(),
            revision = existing?.revision ?: 0L,
            isDeleted = existing?.isDeleted ?: false,
        )

        deposits.save(entity)
        return DepositActionResult.Success
    }

    suspend fun markReceived(
        bookingId: String,
        receivedEpochDay: Long,
    ): DepositActionResult {
        val deposit = deposits
            .observeForBooking(bookingId)
            .first()
            ?: return DepositActionResult.Failure(
                DepositActionIssue.DEPOSIT_NOT_FOUND,
            )

        if (
            deposit.status != DepositStatus.PENDING ||
            deposit.amountSen <= 0L
        ) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        deposits.save(
            deposit.copy(
                status = DepositStatus.RECEIVED,
                receivedAtEpochDay = receivedEpochDay,
            ),
        )
        return DepositActionResult.Success
    }

    suspend fun recordReturn(
        bookingId: String,
        returnAmountSen: Long,
        returnedEpochDay: Long,
    ): DepositActionResult {
        if (returnAmountSen <= 0L) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_AMOUNT,
            )
        }

        val deposit = deposits
            .observeForBooking(bookingId)
            .first()
            ?: return DepositActionResult.Failure(
                DepositActionIssue.DEPOSIT_NOT_FOUND,
            )

        if (
            deposit.status !in setOf(
                DepositStatus.RECEIVED,
                DepositStatus.PARTIALLY_RETURNED,
            )
        ) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        val remaining = DepositRules.remainingSen(
            depositAmountSen = deposit.amountSen,
            returnedAmountSen = deposit.returnedAmountSen,
        )

        if (returnAmountSen > remaining) {
            return DepositActionResult.Failure(
                DepositActionIssue.RETURN_EXCEEDS_REMAINING,
            )
        }

        val newReturned = deposit.returnedAmountSen +
            returnAmountSen

        deposits.save(
            deposit.copy(
                returnedAmountSen = newReturned,
                returnedAtEpochDay = returnedEpochDay,
                status = DepositRules.statusAfterReturn(
                    depositAmountSen = deposit.amountSen,
                    returnedAmountSen = newReturned,
                ),
            ),
        )

        return DepositActionResult.Success
    }

    suspend fun retainRemaining(
        bookingId: String,
    ): DepositActionResult {
        val deposit = deposits
            .observeForBooking(bookingId)
            .first()
            ?: return DepositActionResult.Failure(
                DepositActionIssue.DEPOSIT_NOT_FOUND,
            )

        if (
            deposit.status !in setOf(
                DepositStatus.RECEIVED,
                DepositStatus.PARTIALLY_RETURNED,
            )
        ) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        val remaining = DepositRules.remainingSen(
            depositAmountSen = deposit.amountSen,
            returnedAmountSen = deposit.returnedAmountSen,
        )

        if (remaining <= 0L) {
            return DepositActionResult.Failure(
                DepositActionIssue.INVALID_STATE,
            )
        }

        deposits.save(
            deposit.copy(
                status = DepositStatus.RETAINED,
            ),
        )
        return DepositActionResult.Success
    }
}
