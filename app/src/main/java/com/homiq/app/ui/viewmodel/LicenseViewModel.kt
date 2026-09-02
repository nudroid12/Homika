package com.homiq.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.license.LicenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LicenseViewModel(
    private val repository: LicenseRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(repository.localState())
    val state: StateFlow<LicenseUiState> = mutableState.asStateFlow()

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

    fun retry() {
        if (mutableState.value.busy) return

        val hasStoredKey = mutableState.value.licenseKey.isNotBlank()
        if (!hasStoredKey) {
            mutableState.value = LicenseUiState(
                access = LicenseAccess.ACTIVATION_REQUIRED,
            )
            return
        }

        refresh(forceBusy = true)
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
