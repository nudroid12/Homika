package com.homiq.app.data.license

enum class LicenseAccess {
    CHECKING,
    ACTIVE,
    ACTIVATION_REQUIRED,
    NEEDS_INTERNET,
    EXPIRED,
    INACTIVE,
    DEVICE_LIMIT,
    INVALID,
}

data class LicenseUiState(
    val access: LicenseAccess = LicenseAccess.CHECKING,
    val licenseKey: String = "",
    val expiresAt: String? = null,
    val maxDevices: Int = 3,
    val activeDevices: Int = 0,
    val busy: Boolean = false,
    val errorCode: String? = null,
    val usingOfflineGrace: Boolean = false,
)

data class StoredLicense(
    val licenseKey: String,
    val expiresAt: String,
    val expiresAtEpochMillis: Long,
    val maxDevices: Int,
    val activeDevices: Int,
    val lastValidatedAtMillis: Long,
    val lastObservedAtMillis: Long,
)

data class LicenseActivation(
    val expiresAt: String,
    val maxDevices: Int,
    val activeDevices: Int,
)

sealed interface LicenseApiResult {
    data class Success(
        val activation: LicenseActivation,
    ) : LicenseApiResult

    data class Rejected(
        val code: String,
        val expiresAt: String? = null,
        val maxDevices: Int? = null,
        val activeDevices: Int? = null,
    ) : LicenseApiResult

    data object NetworkError : LicenseApiResult
}
