package com.homiq.app.domain

data class PropertyDraft(
    val id: String? = null,
    val name: String,
    val bookingCode: String,
    val address: String,
    val notes: String,
    val defaultNightlyRateSen: Long,
    val isActive: Boolean,
)

enum class PropertySaveIssue {
    NAME_REQUIRED,
    INVALID_RATE,
}

sealed interface PropertySaveResult {
    data class Success(val propertyId: String) : PropertySaveResult
    data class Failure(val issue: PropertySaveIssue) : PropertySaveResult
}
