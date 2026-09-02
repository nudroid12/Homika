package com.homiq.app.domain

import com.homiq.app.data.local.entity.PaymentEntity
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.PaymentRepository
import kotlinx.coroutines.flow.first

class PaymentManager(
    private val bookings: BookingRepository,
    private val payments: PaymentRepository,
) {
    suspend fun record(
        draft: PaymentDraft,
    ): PaymentSaveResult {
        val booking = bookings.getById(draft.bookingId)
            ?: return PaymentSaveResult.Failure(
                PaymentSaveIssue.BOOKING_NOT_FOUND,
            )

        if (booking.isDeleted) {
            return PaymentSaveResult.Failure(
                PaymentSaveIssue.BOOKING_NOT_FOUND,
            )
        }

        if (booking.status == BookingStatus.CANCELLED) {
            return PaymentSaveResult.Failure(
                PaymentSaveIssue.BOOKING_CANCELLED,
            )
        }

        if (draft.amountSen <= 0L) {
            return PaymentSaveResult.Failure(
                PaymentSaveIssue.INVALID_AMOUNT,
            )
        }

        val paidSen = payments
            .observeTotalPaidSen(booking.id)
            .first()

        val outstanding = PaymentRules.outstandingSen(
            bookingTotalSen = booking.totalAmountSen,
            totalPaidSen = paidSen,
        )

        if (outstanding <= 0L) {
            return PaymentSaveResult.Failure(
                PaymentSaveIssue.NO_OUTSTANDING_BALANCE,
            )
        }

        if (draft.amountSen > outstanding) {
            return PaymentSaveResult.Failure(
                PaymentSaveIssue.EXCEEDS_OUTSTANDING_BALANCE,
            )
        }

        val payment = PaymentEntity(
            bookingId = booking.id,
            amountSen = draft.amountSen,
            paymentDateEpochDay = draft.paymentDateEpochDay,
            method = draft.method,
            notes = draft.notes.trim().ifBlank { null },
        )

        payments.save(payment)
        return PaymentSaveResult.Success(payment.id)
    }
}
