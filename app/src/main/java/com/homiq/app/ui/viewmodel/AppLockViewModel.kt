package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.homiq.app.data.security.AppLockService
import com.homiq.app.data.security.AppLockState
import kotlinx.coroutines.flow.StateFlow

class AppLockViewModel(
    private val service: AppLockService,
) : ViewModel() {
    val state: StateFlow<AppLockState> = service.state

    fun setPin(pin: String): Boolean = service.setPin(pin)

    fun changePin(currentPin: String, newPin: String): Boolean =
        service.changePin(currentPin, newPin)

    fun disable(currentPin: String): Boolean = service.disable(currentPin)

    fun unlock(pin: String): Boolean = service.verifyAndUnlock(pin)

    fun biometricUnlock() = service.unlockFromTrustedAuthentication()

    fun lockNow() = service.lockNow()

    fun setBiometricEnabled(enabled: Boolean) = service.setBiometricEnabled(enabled)

    fun setTimeoutMinutes(minutes: Int) = service.setTimeoutMinutes(minutes)
}
