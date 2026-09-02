package com.homiq.app.domain

import com.homiq.app.data.model.BookingSource
import com.homiq.app.data.model.BookingStatus

data class BookingDraft(
    val id: String? = null,
    val propertyId: String,
    val guestName: String,
    val guestPhone: String,
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val source: BookingSource,
    // Legacy field name kept for compatibility. Semantically: Amount Received.
    val totalAmountSen: Long,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val notes: String,
)

enum class BookingSaveIssue {
    PROPERTY_REQUIRED,
    PROPERTY_NOT_FOUND,
    GUEST_REQUIRED,
    INVALID_DATES,
    INVALID_AMOUNT,
    BOOKING_OVERLAP,
    BLOCKED_DATE_OVERLAP,
}

sealed interface BookingSaveResult {
    data class Success(val bookingId: String) : BookingSaveResult
    data class Failure(val issue: BookingSaveIssue) : BookingSaveResult
}
