package com.homiq.app.data.security

import android.content.Context

data class AppLockStoredState(
    val hasPin: Boolean,
    val biometricEnabled: Boolean,
    val timeoutMinutes: Int,
)

class AppLockPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun state(): AppLockStoredState = AppLockStoredState(
        hasPin = !preferences.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !preferences.getString(KEY_PIN_SALT, null).isNullOrBlank(),
        biometricEnabled = preferences.getBoolean(KEY_BIOMETRIC, false),
        timeoutMinutes = preferences.getInt(KEY_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES),
    )

    fun savePin(pin: String) {
        val value = AppLockHasher.hashNew(pin)
        preferences.edit()
            .putString(KEY_PIN_SALT, value.saltBase64)
            .putString(KEY_PIN_HASH, value.hashBase64)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = preferences.getString(KEY_PIN_SALT, null) ?: return false
        val hash = preferences.getString(KEY_PIN_HASH, null) ?: return false
        return AppLockHasher.verify(pin, salt, hash)
    }

    fun clearPin() {
        preferences.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setTimeoutMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_TIMEOUT_MINUTES, minutes.coerceAtLeast(0)).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "homiq_app_lock"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_TIMEOUT_MINUTES = "timeout_minutes"
        private const val DEFAULT_TIMEOUT_MINUTES = 1
    }
}
