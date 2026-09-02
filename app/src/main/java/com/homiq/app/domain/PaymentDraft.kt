package com.homiq.app.domain

import com.homiq.app.data.model.PaymentMethod

data class PaymentDraft(
    val bookingId: String,
    val amountSen: Long,
    val paymentDateEpochDay: Long,
    val method: PaymentMethod,
    val notes: String,
)

enum class PaymentSaveIssue {
    BOOKING_NOT_FOUND,
    BOOKING_CANCELLED,
    INVALID_AMOUNT,
    NO_OUTSTANDING_BALANCE,
    EXCEEDS_OUTSTANDING_BALANCE,
}

sealed interface PaymentSaveResult {
    data class Success(val paymentId: String) : PaymentSaveResult
    data class Failure(val issue: PaymentSaveIssue) : PaymentSaveResult
}
