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

enum class LicensePlanType(val apiValue: String) {
    TRIAL("trial"),
    MONTHLY("monthly"),
    ANNUAL("annual"),
    LIFETIME("lifetime");

    companion object {
        fun fromApi(value: String?): LicensePlanType =
            entries.firstOrNull { it.apiValue == value?.trim()?.lowercase() } ?: ANNUAL
    }
}

data class LicenseUiState(
    val access: LicenseAccess = LicenseAccess.CHECKING,
    val licenseKey: String = "",
    val licenseHint: String = "",
    val planType: LicensePlanType = LicensePlanType.ANNUAL,
    val expiresAt: String? = null,
    val expiresAtEpochMillis: Long = 0L,
    val maxDevices: Int = 3,
    val activeDevices: Int = 0,
    val lastValidatedAtMillis: Long = 0L,
    val busy: Boolean = false,
    val errorCode: String? = null,
    val usingOfflineGrace: Boolean = false,
)

data class StoredLicense(
    val activationToken: String,
    val licenseHint: String,
    val planType: LicensePlanType,
    val expiresAt: String,
    val expiresAtEpochMillis: Long,
    val maxDevices: Int,
    val activeDevices: Int,
    val lastValidatedAtMillis: Long,
    val lastObservedAtMillis: Long,
)

data class LicenseActivation(
    val activationToken: String,
    val licenseHint: String,
    val planType: LicensePlanType,
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
        val planType: LicensePlanType? = null,
        val expiresAt: String? = null,
        val maxDevices: Int? = null,
        val activeDevices: Int? = null,
    ) : LicenseApiResult

    data object NetworkError : LicenseApiResult
}

sealed interface LicenseDeactivateResult {
    data class Success(
        val maxDevices: Int,
        val activeDevices: Int,
    ) : LicenseDeactivateResult

    data class Rejected(
        val code: String,
    ) : LicenseDeactivateResult

    data object NetworkError : LicenseDeactivateResult
}

data class LicenseDeviceInfo(
    val deviceHash: String,
    val deviceName: String,
    val activatedAt: String,
    val lastSeenAt: String,
    val isCurrentDevice: Boolean,
)

sealed interface LicenseDevicesResult {
    data class Success(
        val maxDevices: Int,
        val activeDevices: Int,
        val devices: List<LicenseDeviceInfo>,
    ) : LicenseDevicesResult

    data class Rejected(
        val code: String,
    ) : LicenseDevicesResult

    data object NetworkError : LicenseDevicesResult
}

sealed interface LicenseRemoteDeviceDeactivateResult {
    data class Success(
        val deviceHash: String,
        val maxDevices: Int,
        val activeDevices: Int,
    ) : LicenseRemoteDeviceDeactivateResult

    data class Rejected(
        val code: String,
    ) : LicenseRemoteDeviceDeactivateResult

    data object NetworkError : LicenseRemoteDeviceDeactivateResult
}

data class VerifiedActivationToken(
    val licenseId: String,
    val planType: LicensePlanType,
    val expiresAtEpochMillis: Long,
    val licenseHint: String,
    val maxDevices: Int,
)
