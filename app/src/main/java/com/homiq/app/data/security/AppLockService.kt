package com.homiq.app.data.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppLockState(
    val hasPin: Boolean = false,
    val biometricEnabled: Boolean = false,
    val timeoutMinutes: Int = 1,
    val locked: Boolean = false,
)

class AppLockService(
    private val preferences: AppLockPreferences,
) {
    private var backgroundAtElapsedRealtime: Long? = null

    private val initial = preferences.state()
    private val mutableState = MutableStateFlow(
        AppLockState(
            hasPin = initial.hasPin,
            biometricEnabled = initial.biometricEnabled && initial.hasPin,
            timeoutMinutes = initial.timeoutMinutes,
            // A fresh process must never expose business data before unlock.
            locked = initial.hasPin,
        ),
    )

    val state: StateFlow<AppLockState> = mutableState.asStateFlow()

    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        preferences.savePin(pin)
        refresh(locked = false)
        return true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!preferences.verifyPin(currentPin) || !isValidPin(newPin)) return false
        preferences.savePin(newPin)
        refresh(locked = false)
        return true
    }

    fun disable(currentPin: String): Boolean {
        if (!preferences.verifyPin(currentPin)) return false
        preferences.clearPin()
        backgroundAtElapsedRealtime = null
        refresh(locked = false)
        return true
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val valid = preferences.verifyPin(pin)
        if (valid) unlockFromTrustedAuthentication()
        return valid
    }

    fun unlockFromTrustedAuthentication() {
        backgroundAtElapsedRealtime = null
        refresh(locked = false)
    }

    fun lockNow() {
        if (preferences.state().hasPin) refresh(locked = true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val canEnable = enabled && preferences.state().hasPin
        preferences.setBiometricEnabled(canEnable)
        refresh()
    }

    fun setTimeoutMinutes(minutes: Int) {
        preferences.setTimeoutMinutes(minutes)
        refresh()
    }

    fun onAppBackground() {
        if (!preferences.state().hasPin) return
        backgroundAtElapsedRealtime = SystemClock.elapsedRealtime()
    }

    fun onAppForeground() {
        val stored = preferences.state()
        if (!stored.hasPin) {
            refresh(locked = false)
            return
        }

        val backgroundAt = backgroundAtElapsedRealtime ?: return
        val elapsed = (SystemClock.elapsedRealtime() - backgroundAt).coerceAtLeast(0L)
        val timeoutMillis = stored.timeoutMinutes * 60_000L
        if (stored.timeoutMinutes == 0 || elapsed >= timeoutMillis) {
            refresh(locked = true)
        }
        backgroundAtElapsedRealtime = null
    }

    private fun refresh(locked: Boolean = mutableState.value.locked) {
        val stored = preferences.state()
        mutableState.value = AppLockState(
            hasPin = stored.hasPin,
            biometricEnabled = stored.biometricEnabled && stored.hasPin,
            timeoutMinutes = stored.timeoutMinutes,
            locked = locked && stored.hasPin,
        )
    }

    companion object {
        fun isValidPin(pin: String): Boolean =
            pin.length in 4..8 && pin.all(Char::isDigit)
    }
}
