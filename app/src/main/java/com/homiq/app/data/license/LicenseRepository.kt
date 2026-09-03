package com.homiq.app.data.license

import android.content.Context
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class LicenseRepository(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val preferences = LicensePreferences(applicationContext)
    private val api = LicenseApiClient()
    private val deviceId = LicenseDeviceIdentity.fingerprint(applicationContext)
    private val deviceName = LicenseDeviceIdentity.displayName()

    data class CloudCredentials(
        val activationToken: String,
        val deviceId: String,
        val licenseId: String,
    )

    fun cloudCredentials(
        nowMillis: Long = System.currentTimeMillis(),
    ): CloudCredentials? {
        val stored = preferences.read() ?: return null
        val verified = ActivationTokenVerifier.verify(
            token = stored.activationToken,
            expectedDeviceId = deviceId,
            nowMillis = nowMillis,
        ) ?: return null

        if (
            verified.planType != LicensePlanType.LIFETIME &&
            nowMillis >= verified.expiresAtEpochMillis
        ) {
            return null
        }

        return CloudCredentials(
            activationToken = stored.activationToken,
            deviceId = deviceId,
            licenseId = verified.licenseId,
        )
    }

    fun localState(nowMillis: Long = System.currentTimeMillis()): LicenseUiState {
        val stored = preferences.read()
        if (stored == null) {
            return if (!preferences.legacyLicenseKey().isNullOrBlank()) {
                LicenseUiState(access = LicenseAccess.CHECKING)
            } else {
                LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)
            }
        }

        val verified = ActivationTokenVerifier.verify(
            token = stored.activationToken,
            expectedDeviceId = deviceId,
            nowMillis = nowMillis,
        )

        if (stored.planType != LicensePlanType.LIFETIME && nowMillis >= stored.expiresAtEpochMillis) {
            return uiFromStored(
                stored = stored,
                access = LicenseAccess.EXPIRED,
                errorCode = "license_expired",
            )
        }

        if (verified == null) {
            return uiFromStored(
                stored = stored,
                access = LicenseAccess.INVALID,
                errorCode = "invalid_activation_token",
            )
        }

        val trusted = stored.copy(
            licenseHint = verified.licenseHint,
            planType = verified.planType,
            expiresAtEpochMillis = verified.expiresAtEpochMillis,
            maxDevices = verified.maxDevices,
        )

        val clockRolledBack =
            trusted.lastObservedAtMillis > 0L &&
                nowMillis + CLOCK_ROLLBACK_TOLERANCE_MS < trusted.lastObservedAtMillis

        if (clockRolledBack) {
            return uiFromStored(
                stored = trusted,
                access = LicenseAccess.CHECKING,
                errorCode = "clock_verification_required",
            )
        }

        preferences.markObserved(nowMillis)

        if (trusted.planType != LicensePlanType.LIFETIME && nowMillis >= trusted.expiresAtEpochMillis) {
            return uiFromStored(
                stored = trusted,
                access = LicenseAccess.EXPIRED,
                errorCode = "license_expired",
            )
        }

        val withinOfflineGrace =
            trusted.lastValidatedAtMillis > 0L &&
                nowMillis - trusted.lastValidatedAtMillis <= OFFLINE_GRACE_MS

        return if (withinOfflineGrace) {
            uiFromStored(
                stored = trusted,
                access = LicenseAccess.ACTIVE,
                usingOfflineGrace =
                    nowMillis - trusted.lastValidatedAtMillis > VALIDATION_INTERVAL_MS,
            )
        } else {
            uiFromStored(
                stored = trusted,
                access = LicenseAccess.CHECKING,
            )
        }
    }

    fun shouldRefresh(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!preferences.legacyLicenseKey().isNullOrBlank()) return true

        val stored = preferences.read() ?: return false
        if (stored.planType != LicensePlanType.LIFETIME && nowMillis >= stored.expiresAtEpochMillis) return true
        if (stored.lastValidatedAtMillis <= 0L) return true
        if (nowMillis + CLOCK_ROLLBACK_TOLERANCE_MS < stored.lastObservedAtMillis) return true
        return nowMillis - stored.lastValidatedAtMillis >= VALIDATION_INTERVAL_MS
    }

    suspend fun activate(rawKey: String): LicenseUiState {
        val key = normalizeKey(rawKey)
        if (key.isBlank()) {
            return LicenseUiState(
                access = LicenseAccess.INVALID,
                errorCode = "license_key_required",
            )
        }

        return when (val result = api.activate(key, deviceId, deviceName)) {
            is LicenseApiResult.Success ->
                saveAndBuildActive(result.activation, enteredKey = key)
            is LicenseApiResult.Rejected ->
                rejectedState(key, result)
            LicenseApiResult.NetworkError ->
                networkState(key)
        }
    }

    suspend fun validate(): LicenseUiState {
        val stored = preferences.read()
        if (stored == null) {
            val legacyKey = preferences.legacyLicenseKey()
                ?: return LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)

            return when (val result = api.activate(legacyKey, deviceId, deviceName)) {
                is LicenseApiResult.Success ->
                    saveAndBuildActive(result.activation, enteredKey = legacyKey)
                is LicenseApiResult.Rejected ->
                    rejectedState(legacyKey, result)
                LicenseApiResult.NetworkError ->
                    LicenseUiState(
                        access = LicenseAccess.NEEDS_INTERNET,
                        errorCode = "migration_network_required",
                    )
            }
        }

        return when (val result = api.validate(stored.activationToken, deviceId)) {
            is LicenseApiResult.Success ->
                saveAndBuildActive(result.activation)
            is LicenseApiResult.Rejected ->
                rejectedState("", result)
            LicenseApiResult.NetworkError ->
                networkState("")
        }
    }

    suspend fun deactivate(): LicenseUiState {
        val stored = preferences.read()
            ?: return LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)

        return when (val result = api.deactivate(stored.activationToken, deviceId)) {
            is LicenseDeactivateResult.Success -> {
                preferences.clear()
                LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)
            }
            is LicenseDeactivateResult.Rejected -> {
                if (
                    result.code == "device_not_activated" ||
                    result.code == "invalid_activation_token" ||
                    result.code == "token_device_mismatch"
                ) {
                    preferences.clear()
                    LicenseUiState(
                        access = LicenseAccess.ACTIVATION_REQUIRED,
                        errorCode = result.code,
                    )
                } else {
                    localState().copy(errorCode = result.code)
                }
            }
            LicenseDeactivateResult.NetworkError ->
                localState().copy(errorCode = "deactivate_network")
        }
    }

    private fun networkState(key: String): LicenseUiState {
        val now = System.currentTimeMillis()
        val stored = preferences.read()

        val canUseGrace =
            stored != null &&
                (stored.planType == LicensePlanType.LIFETIME || now < stored.expiresAtEpochMillis) &&
                stored.lastValidatedAtMillis > 0L &&
                now >= stored.lastObservedAtMillis - CLOCK_ROLLBACK_TOLERANCE_MS &&
                now - stored.lastValidatedAtMillis <= OFFLINE_GRACE_MS &&
                ActivationTokenVerifier.verify(
                    token = stored.activationToken,
                    expectedDeviceId = deviceId,
                    nowMillis = now,
                ) != null

        return if (canUseGrace) {
            preferences.markObserved(now)
            uiFromStored(
                stored = stored!!,
                access = LicenseAccess.ACTIVE,
                errorCode = "offline_grace",
                usingOfflineGrace = true,
            )
        } else {
            LicenseUiState(
                access = LicenseAccess.NEEDS_INTERNET,
                licenseKey = key,
                licenseHint = stored?.licenseHint.orEmpty(),
                planType = stored?.planType ?: LicensePlanType.ANNUAL,
                expiresAt = stored?.expiresAt,
                maxDevices = stored?.maxDevices ?: 3,
                activeDevices = stored?.activeDevices ?: 0,
                lastValidatedAtMillis = stored?.lastValidatedAtMillis ?: 0L,
                errorCode = "network_required",
            )
        }
    }

    private fun rejectedState(
        key: String,
        result: LicenseApiResult.Rejected,
    ): LicenseUiState {
        val stored = preferences.read()
        val access = when (result.code) {
            "license_expired" -> LicenseAccess.EXPIRED
            "license_inactive" -> LicenseAccess.INACTIVE
            "device_limit_reached" -> LicenseAccess.DEVICE_LIMIT
            "device_not_activated" -> LicenseAccess.ACTIVATION_REQUIRED
            "invalid_activation_token",
            "token_device_mismatch",
            -> LicenseAccess.ACTIVATION_REQUIRED
            "license_not_found",
            "license_key_required",
            "activation_token_required",
            "invalid_json",
            "invalid_server_response",
            -> LicenseAccess.INVALID
            else -> LicenseAccess.INVALID
        }

        if (
            result.code == "device_not_activated" ||
            result.code == "invalid_activation_token" ||
            result.code == "token_device_mismatch"
        ) {
            preferences.clear()
        }

        return LicenseUiState(
            access = access,
            licenseKey = key,
            licenseHint = stored?.licenseHint.orEmpty(),
            planType = result.planType ?: stored?.planType ?: LicensePlanType.ANNUAL,
            expiresAt = result.expiresAt ?: stored?.expiresAt,
            maxDevices = result.maxDevices ?: stored?.maxDevices ?: 3,
            activeDevices = result.activeDevices ?: stored?.activeDevices ?: 0,
            lastValidatedAtMillis = stored?.lastValidatedAtMillis ?: 0L,
            errorCode = result.code,
        )
    }

    private fun saveAndBuildActive(
        activation: LicenseActivation,
        enteredKey: String? = null,
    ): LicenseUiState {
        val expiryMillis = parseExpiryMillis(activation.expiresAt)
            ?: return LicenseUiState(
                access = LicenseAccess.INVALID,
                licenseKey = enteredKey.orEmpty(),
                errorCode = "invalid_expiry",
            )

        val verified = ActivationTokenVerifier.verify(
            token = activation.activationToken,
            expectedDeviceId = deviceId,
        ) ?: return LicenseUiState(
            access = LicenseAccess.INVALID,
            licenseKey = enteredKey.orEmpty(),
            errorCode = "invalid_activation_token",
        )

        if (abs(verified.expiresAtEpochMillis - expiryMillis) > EXPIRY_MATCH_TOLERANCE_MS) {
            return LicenseUiState(
                access = LicenseAccess.INVALID,
                licenseKey = enteredKey.orEmpty(),
                errorCode = "token_expiry_mismatch",
            )
        }

        val trustedActivation = activation.copy(
            licenseHint = verified.licenseHint,
            planType = verified.planType,
            maxDevices = verified.maxDevices,
        )

        val now = System.currentTimeMillis()
        val saved = preferences.saveValidated(
            activation = trustedActivation,
            expiresAtEpochMillis = verified.expiresAtEpochMillis,
            nowMillis = now,
        )
        if (!saved) {
            return LicenseUiState(
                access = LicenseAccess.INVALID,
                errorCode = "secure_store_failed",
            )
        }

        return LicenseUiState(
            access = LicenseAccess.ACTIVE,
            licenseHint = trustedActivation.licenseHint,
            planType = trustedActivation.planType,
            expiresAt = trustedActivation.expiresAt,
            maxDevices = trustedActivation.maxDevices,
            activeDevices = trustedActivation.activeDevices,
            lastValidatedAtMillis = now,
        )
    }

    private fun uiFromStored(
        stored: StoredLicense,
        access: LicenseAccess,
        errorCode: String? = null,
        usingOfflineGrace: Boolean = false,
    ): LicenseUiState =
        LicenseUiState(
            access = access,
            licenseHint = stored.licenseHint,
            planType = stored.planType,
            expiresAt = stored.expiresAt,
            maxDevices = stored.maxDevices,
            activeDevices = stored.activeDevices,
            lastValidatedAtMillis = stored.lastValidatedAtMillis,
            errorCode = errorCode,
            usingOfflineGrace = usingOfflineGrace,
        )

    private fun normalizeKey(raw: String): String =
        raw.trim()
            .uppercase()
            .replace(" ", "")
            .filter { it.isLetterOrDigit() || it == '-' }
            .take(80)

    private fun parseExpiryMillis(value: String): Long? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        runCatching {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        }.getOrNull()?.let { return it }

        runCatching {
            LocalDateTime.parse(
                raw,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            ).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()?.let { return it }

        return null
    }

    companion object {
        private const val VALIDATION_INTERVAL_MS = 24L * 60L * 60L * 1000L
        private const val OFFLINE_GRACE_MS = 7L * 24L * 60L * 60L * 1000L
        private const val CLOCK_ROLLBACK_TOLERANCE_MS = 5L * 60L * 1000L
        private const val EXPIRY_MATCH_TOLERANCE_MS = 2_000L
    }
}
