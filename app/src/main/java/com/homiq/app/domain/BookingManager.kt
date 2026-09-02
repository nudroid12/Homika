package com.homiq.app.domain

import com.homiq.app.data.local.entity.BookingEntity
import com.homiq.app.data.model.BookingStatus
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.PropertyRepository

class BookingManager(
    private val properties: PropertyRepository,
    private val bookings: BookingRepository,
    private val blockedDates: BlockedDateRepository,
) {
    suspend fun save(draft: BookingDraft): BookingSaveResult {
        if (draft.propertyId.isBlank()) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.PROPERTY_REQUIRED,
            )
        }

        val property = properties.getById(draft.propertyId)
            ?: return BookingSaveResult.Failure(
                BookingSaveIssue.PROPERTY_NOT_FOUND,
            )

        if (property.isDeleted) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.PROPERTY_NOT_FOUND,
            )
        }

        if (draft.guestName.isBlank()) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.GUEST_REQUIRED,
            )
        }

        if (!BookingRules.isDateRangeValid(
                draft.checkInEpochDay,
                draft.checkOutEpochDay,
            )
        ) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.INVALID_DATES,
            )
        }

        if (draft.totalAmountSen < 0L) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.INVALID_AMOUNT,
            )
        }

        val existing = if (draft.id != null) {
            bookings.getById(draft.id)
        } else {
            null
        }

        val bookingOverlaps = bookings.findOverlaps(
            propertyId = draft.propertyId,
            checkIn = draft.checkInEpochDay,
            checkOutExclusive = draft.checkOutEpochDay,
            excludeBookingId = draft.id.orEmpty(),
        )
        if (bookingOverlaps.isNotEmpty()) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.BOOKING_OVERLAP,
            )
        }

        val blockedOverlaps = blockedDates.findOverlaps(
            propertyId = draft.propertyId,
            start = draft.checkInEpochDay,
            endExclusive = draft.checkOutEpochDay,
        )
        if (blockedOverlaps.isNotEmpty()) {
            return BookingSaveResult.Failure(
                BookingSaveIssue.BLOCKED_DATE_OVERLAP,
            )
        }

        val booking = BookingEntity(
            id = existing?.id ?: draft.id ?: java.util.UUID.randomUUID().toString(),
            propertyId = draft.propertyId,
            bookingReference = existing?.bookingReference
                ?.takeIf { it.isNotBlank() }
                ?: BookingReferenceRules.create(
                    propertyCode = property.bookingCode,
                    propertyName = property.name,
                    checkInEpochDay = draft.checkInEpochDay,
                ),
            guestName = draft.guestName.trim(),
            guestPhone = draft.guestPhone.trim().ifBlank { null },
            checkInEpochDay = draft.checkInEpochDay,
            checkOutEpochDay = draft.checkOutEpochDay,
            source = draft.source,
            totalAmountSen = draft.totalAmountSen,
            status = if (existing?.status == BookingStatus.CANCELLED) {
                BookingStatus.CANCELLED
            } else {
                draft.status
            },
            notes = draft.notes.trim().ifBlank { null },
            createdAtEpochMillis = existing?.createdAtEpochMillis
                ?: System.currentTimeMillis(),
            updatedAtEpochMillis = existing?.updatedAtEpochMillis
                ?: System.currentTimeMillis(),
            revision = existing?.revision ?: 0L,
            isDeleted = existing?.isDeleted ?: false,
        )

        bookings.save(booking)
        return BookingSaveResult.Success(booking.id)
    }

    suspend fun cancel(bookingId: String): Boolean {
        val booking = bookings.getById(bookingId) ?: return false
        if (booking.isDeleted) return false
        if (booking.status == BookingStatus.CANCELLED) return true

        bookings.save(
            booking.copy(
                status = BookingStatus.CANCELLED,
            ),
        )
        return true
    }
}
