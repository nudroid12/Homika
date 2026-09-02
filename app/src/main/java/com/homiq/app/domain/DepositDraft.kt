package com.homiq.app.domain

enum class DepositActionIssue {
    BOOKING_NOT_FOUND,
    INVALID_AMOUNT,
    DEPOSIT_NOT_FOUND,
    INVALID_STATE,
    RETURN_EXCEEDS_REMAINING,
}

sealed interface DepositActionResult {
    data object Success : DepositActionResult
    data class Failure(
        val issue: DepositActionIssue,
    ) : DepositActionResult
}
