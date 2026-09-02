package com.homiq.app.domain

import com.homiq.app.data.local.entity.BlockedDateEntity
import com.homiq.app.data.repository.BlockedDateRepository
import com.homiq.app.data.repository.BookingRepository
import com.homiq.app.data.repository.PropertyRepository

class BlockedDateManager(
    private val properties: PropertyRepository,
    private val bookings: BookingRepository,
    private val blockedDates: BlockedDateRepository,
) {
    suspend fun save(
        draft: BlockedDateDraft,
    ): BlockedDateSaveResult {
        if (draft.propertyId.isBlank()) {
            return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.PROPERTY_REQUIRED,
            )
        }

        val property = properties.getById(draft.propertyId)
            ?: return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.PROPERTY_NOT_FOUND,
            )

        if (property.isDeleted || !property.isActive) {
            return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.PROPERTY_NOT_FOUND,
            )
        }

        if (!BookingRules.isDateRangeValid(
                draft.startEpochDay,
                draft.endEpochDayExclusive,
            )
        ) {
            return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.INVALID_DATES,
            )
        }

        val bookingOverlaps = bookings.findOverlaps(
            propertyId = draft.propertyId,
            checkIn = draft.startEpochDay,
            checkOutExclusive = draft.endEpochDayExclusive,
        )
        if (bookingOverlaps.isNotEmpty()) {
            return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.BOOKING_OVERLAP,
            )
        }

        val blockedOverlaps = blockedDates.findOverlaps(
            propertyId = draft.propertyId,
            start = draft.startEpochDay,
            endExclusive = draft.endEpochDayExclusive,
            excludeBlockId = draft.id.orEmpty(),
        )
        if (blockedOverlaps.isNotEmpty()) {
            return BlockedDateSaveResult.Failure(
                BlockedDateSaveIssue.BLOCKED_DATE_OVERLAP,
            )
        }

        val existing = if (draft.id != null) {
            blockedDates.getById(draft.id)
        } else {
            null
        }

        val entity = BlockedDateEntity(
            id = existing?.id
                ?: draft.id
                ?: java.util.UUID.randomUUID().toString(),
            propertyId = draft.propertyId,
            startEpochDay = draft.startEpochDay,
            endEpochDay = draft.endEpochDayExclusive,
            reason = draft.reason.trim().ifBlank { null },
            createdAtEpochMillis = existing?.createdAtEpochMillis
                ?: System.currentTimeMillis(),
            updatedAtEpochMillis = existing?.updatedAtEpochMillis
                ?: System.currentTimeMillis(),
            revision = existing?.revision ?: 0L,
            isDeleted = existing?.isDeleted ?: false,
        )

        blockedDates.save(entity)
        return BlockedDateSaveResult.Success(entity.id)
    }

    suspend fun delete(id: String) {
        blockedDates.delete(id)
    }
}
