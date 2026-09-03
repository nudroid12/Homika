package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.license.LicenseDeviceInfo
import com.homiq.app.data.license.LicenseDevicesResult
import com.homiq.app.data.license.LicenseRemoteDeviceDeactivateResult
import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.license.LicenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LicenseDeviceUiState(
    val loading: Boolean = false,
    val busyDeviceHash: String? = null,
    val devices: List<LicenseDeviceInfo> = emptyList(),
    val maxDevices: Int = 3,
    val activeDevices: Int = 0,
    val errorCode: String? = null,
    val feedbackCode: String? = null,
)

class LicenseViewModel(
    private val repository: LicenseRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(repository.localState())
    val state: StateFlow<LicenseUiState> = mutableState.asStateFlow()

    private val mutableDeviceState = MutableStateFlow(
        LicenseDeviceUiState(
            maxDevices = mutableState.value.maxDevices,
            activeDevices = mutableState.value.activeDevices,
        ),
    )
    val deviceState: StateFlow<LicenseDeviceUiState> = mutableDeviceState.asStateFlow()

    init {
        if (repository.shouldRefresh()) {
            refresh()
        }
    }

    fun activate(licenseKey: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(
            busy = true,
            errorCode = null,
        )

        viewModelScope.launch {
            mutableState.value = repository.activate(licenseKey).copy(busy = false)
        }
    }

    fun claimTrial(email: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(
            busy = true,
            errorCode = null,
        )

        viewModelScope.launch {
            mutableState.value = repository.claimTrial(email).copy(busy = false)
        }
    }

    fun retry() {
        if (mutableState.value.busy) return
        refresh(forceBusy = true)
    }

    fun refreshNow() {
        if (mutableState.value.busy) return
        refresh(forceBusy = true)
    }

    fun deactivate() {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(
            busy = true,
            errorCode = null,
        )

        viewModelScope.launch {
            mutableState.value = repository.deactivate().copy(busy = false)
        }
    }

    fun refreshDevices() {
        if (mutableDeviceState.value.loading || mutableDeviceState.value.busyDeviceHash != null) return
        mutableDeviceState.value = mutableDeviceState.value.copy(
            loading = true,
            errorCode = null,
            feedbackCode = null,
        )
        viewModelScope.launch {
            when (val result = repository.listDevices()) {
                is LicenseDevicesResult.Success -> {
                    mutableDeviceState.value = LicenseDeviceUiState(
                        loading = false,
                        devices = result.devices,
                        maxDevices = result.maxDevices,
                        activeDevices = result.activeDevices,
                    )
                    mutableState.value = mutableState.value.copy(
                        maxDevices = result.maxDevices,
                        activeDevices = result.activeDevices,
                    )
                }
                is LicenseDevicesResult.Rejected -> {
                    mutableDeviceState.value = mutableDeviceState.value.copy(
                        loading = false,
                        errorCode = result.code,
                    )
                }
                LicenseDevicesResult.NetworkError -> {
                    mutableDeviceState.value = mutableDeviceState.value.copy(
                        loading = false,
                        errorCode = "devices_network",
                    )
                }
            }
        }
    }

    fun deactivateOtherDevice(deviceHash: String) {
        if (mutableDeviceState.value.loading || mutableDeviceState.value.busyDeviceHash != null) return
        val target = mutableDeviceState.value.devices.firstOrNull { it.deviceHash == deviceHash }
            ?: return
        if (target.isCurrentDevice) return

        mutableDeviceState.value = mutableDeviceState.value.copy(
            busyDeviceHash = deviceHash,
            errorCode = null,
            feedbackCode = null,
        )
        viewModelScope.launch {
            when (val result = repository.deactivateOtherDevice(deviceHash)) {
                is LicenseRemoteDeviceDeactivateResult.Success -> {
                    mutableDeviceState.value = mutableDeviceState.value.copy(
                        busyDeviceHash = null,
                        devices = mutableDeviceState.value.devices.filterNot {
                            it.deviceHash == result.deviceHash
                        },
                        maxDevices = result.maxDevices,
                        activeDevices = result.activeDevices,
                        feedbackCode = "device_removed",
                    )
                    mutableState.value = mutableState.value.copy(
                        maxDevices = result.maxDevices,
                        activeDevices = result.activeDevices,
                    )
                }
                is LicenseRemoteDeviceDeactivateResult.Rejected -> {
                    mutableDeviceState.value = mutableDeviceState.value.copy(
                        busyDeviceHash = null,
                        errorCode = result.code,
                    )
                }
                LicenseRemoteDeviceDeactivateResult.NetworkError -> {
                    mutableDeviceState.value = mutableDeviceState.value.copy(
                        busyDeviceHash = null,
                        errorCode = "devices_network",
                    )
                }
            }
        }
    }

    private fun refresh(forceBusy: Boolean = false) {
        if (mutableState.value.busy) return

        if (forceBusy || mutableState.value.access != LicenseAccess.ACTIVE) {
            mutableState.value = mutableState.value.copy(
                busy = true,
                errorCode = null,
            )
        }

        viewModelScope.launch {
            mutableState.value = repository.validate().copy(busy = false)
        }
    }
}
