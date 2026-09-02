package com.homiq.app.data.license

import android.content.Context

class LicensePreferences(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun read(): StoredLicense? {
        val key = prefs.getString(KEY_LICENSE_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val expiresAt = prefs.getString(KEY_EXPIRES_AT, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val expiryMillis = prefs.getLong(KEY_EXPIRES_AT_MILLIS, 0L)
        if (expiryMillis <= 0L) return null

        return StoredLicense(
            licenseKey = key,
            expiresAt = expiresAt,
            expiresAtEpochMillis = expiryMillis,
            maxDevices = prefs.getInt(KEY_MAX_DEVICES, 3),
            activeDevices = prefs.getInt(KEY_ACTIVE_DEVICES, 0),
            lastValidatedAtMillis = prefs.getLong(KEY_LAST_VALIDATED_AT, 0L),
            lastObservedAtMillis = prefs.getLong(KEY_LAST_OBSERVED_AT, 0L),
        )
    }

    fun saveValidated(
        licenseKey: String,
        activation: LicenseActivation,
        expiresAtEpochMillis: Long,
        nowMillis: Long,
    ) {
        prefs.edit()
            .putString(KEY_LICENSE_KEY, licenseKey)
            .putString(KEY_EXPIRES_AT, activation.expiresAt)
            .putLong(KEY_EXPIRES_AT_MILLIS, expiresAtEpochMillis)
            .putInt(KEY_MAX_DEVICES, activation.maxDevices)
            .putInt(KEY_ACTIVE_DEVICES, activation.activeDevices)
            .putLong(KEY_LAST_VALIDATED_AT, nowMillis)
            .putLong(KEY_LAST_OBSERVED_AT, nowMillis)
            .apply()
    }

    fun markObserved(nowMillis: Long) {
        prefs.edit()
            .putLong(KEY_LAST_OBSERVED_AT, nowMillis)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "homika_pro_license"
        const val KEY_LICENSE_KEY = "license_key"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_EXPIRES_AT_MILLIS = "expires_at_millis"
        const val KEY_MAX_DEVICES = "max_devices"
        const val KEY_ACTIVE_DEVICES = "active_devices"
        const val KEY_LAST_VALIDATED_AT = "last_validated_at"
        const val KEY_LAST_OBSERVED_AT = "last_observed_at"
    }
}
