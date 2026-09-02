package com.homiq.app.data.license

import android.content.Context
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LicenseRepository(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val preferences = LicensePreferences(applicationContext)
    private val api = LicenseApiClient()
    private val deviceId = LicenseDeviceIdentity.fingerprint(applicationContext)
    private val deviceName = LicenseDeviceIdentity.displayName()

    fun localState(nowMillis: Long = System.currentTimeMillis()): LicenseUiState {
        val stored = preferences.read()
            ?: return LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)

        val clockRolledBack =
            stored.lastObservedAtMillis > 0L &&
                nowMillis + CLOCK_ROLLBACK_TOLERANCE_MS < stored.lastObservedAtMillis

        if (clockRolledBack) {
            return uiFromStored(
                stored = stored,
                access = LicenseAccess.CHECKING,
                errorCode = "clock_verification_required",
            )
        }

        preferences.markObserved(nowMillis)

        if (nowMillis >= stored.expiresAtEpochMillis) {
            return uiFromStored(
                stored = stored,
                access = LicenseAccess.EXPIRED,
                errorCode = "license_expired",
            )
        }

        val withinOfflineGrace =
            stored.lastValidatedAtMillis > 0L &&
                nowMillis - stored.lastValidatedAtMillis <= OFFLINE_GRACE_MS

        return if (withinOfflineGrace) {
            uiFromStored(
                stored = stored,
                access = LicenseAccess.ACTIVE,
                usingOfflineGrace = nowMillis - stored.lastValidatedAtMillis > VALIDATION_INTERVAL_MS,
            )
        } else {
            uiFromStored(
                stored = stored,
                access = LicenseAccess.CHECKING,
            )
        }
    }

    fun shouldRefresh(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val stored = preferences.read() ?: return false
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
            is LicenseApiResult.Success -> saveAndBuildActive(key, result.activation)
            is LicenseApiResult.Rejected -> rejectedState(key, result)
            LicenseApiResult.NetworkError -> networkState(key)
        }
    }

    suspend fun validate(): LicenseUiState {
        val stored = preferences.read()
            ?: return LicenseUiState(access = LicenseAccess.ACTIVATION_REQUIRED)

        return when (val result = api.validate(stored.licenseKey, deviceId)) {
            is LicenseApiResult.Success -> saveAndBuildActive(stored.licenseKey, result.activation)
            is LicenseApiResult.Rejected -> rejectedState(stored.licenseKey, result)
            LicenseApiResult.NetworkError -> networkState(stored.licenseKey)
        }
    }

    private fun networkState(key: String): LicenseUiState {
        val now = System.currentTimeMillis()
        val stored = preferences.read()

        val canUseGrace = stored != null &&
            stored.licenseKey == key &&
            now < stored.expiresAtEpochMillis &&
            stored.lastValidatedAtMillis > 0L &&
            now >= stored.lastObservedAtMillis - CLOCK_ROLLBACK_TOLERANCE_MS &&
            now - stored.lastValidatedAtMillis <= OFFLINE_GRACE_MS

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
                expiresAt = stored?.expiresAt,
                maxDevices = stored?.maxDevices ?: 3,
                activeDevices = stored?.activeDevices ?: 0,
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
            "license_not_found",
            "license_key_required",
            "invalid_json",
            "invalid_server_response",
            -> LicenseAccess.INVALID
            else -> LicenseAccess.INVALID
        }

        return LicenseUiState(
            access = access,
            licenseKey = key,
            expiresAt = result.expiresAt ?: stored?.expiresAt,
            maxDevices = result.maxDevices ?: stored?.maxDevices ?: 3,
            activeDevices = result.activeDevices ?: stored?.activeDevices ?: 0,
            errorCode = result.code,
        )
    }

    private fun saveAndBuildActive(
        key: String,
        activation: LicenseActivation,
    ): LicenseUiState {
        val expiryMillis = parseExpiryMillis(activation.expiresAt)
            ?: return LicenseUiState(
                access = LicenseAccess.INVALID,
                licenseKey = key,
                errorCode = "invalid_expiry",
            )

        val now = System.currentTimeMillis()
        preferences.saveValidated(
            licenseKey = key,
            activation = activation,
            expiresAtEpochMillis = expiryMillis,
            nowMillis = now,
        )

        return LicenseUiState(
            access = LicenseAccess.ACTIVE,
            licenseKey = key,
            expiresAt = activation.expiresAt,
            maxDevices = activation.maxDevices,
            activeDevices = activation.activeDevices,
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
            licenseKey = stored.licenseKey,
            expiresAt = stored.expiresAt,
            maxDevices = stored.maxDevices,
            activeDevices = stored.activeDevices,
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
    }
}
