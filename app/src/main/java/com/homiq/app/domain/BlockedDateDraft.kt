package com.homiq.app.domain

data class BlockedDateDraft(
    val id: String? = null,
    val propertyId: String,
    val startEpochDay: Long,
    val endEpochDayExclusive: Long,
    val reason: String,
)

enum class BlockedDateSaveIssue {
    PROPERTY_REQUIRED,
    PROPERTY_NOT_FOUND,
    INVALID_DATES,
    BOOKING_OVERLAP,
    BLOCKED_DATE_OVERLAP,
}

sealed interface BlockedDateSaveResult {
    data class Success(val blockedDateId: String) : BlockedDateSaveResult
    data class Failure(
        val issue: BlockedDateSaveIssue,
    ) : BlockedDateSaveResult
}
