package com.homiq.app.ui.viewmodel

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homiq.app.data.sync.HomiqSyncService
import com.homiq.app.data.sync.SyncActionResult
import com.homiq.app.data.sync.SyncFailureReason
import com.homiq.app.data.sync.SyncPreferences
import com.homiq.app.data.sync.SyncRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val runtime: SyncRuntimeState =
        SyncRuntimeState(),
    val deviceId: String = "",
    val pendingResolution:
        PendingIntent? = null,
    val message: SyncUiMessage? = null,
)

sealed interface SyncUiMessage {
    data object SyncCompleted :
        SyncUiMessage

    data object Disconnected :
        SyncUiMessage

    data class Failure(
        val reason: SyncFailureReason,
    ) : SyncUiMessage
}

class SyncViewModel(
    private val service:
        HomiqSyncService,
    preferences: SyncPreferences,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            SyncUiState(
                runtime =
                    service.state.value,
                deviceId =
                    preferences.deviceId,
            ),
        )

    val state: StateFlow<SyncUiState> =
        mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            service.state.collect { runtime ->
                mutableState.value =
                    mutableState.value.copy(
                        runtime = runtime,
                    )
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            handle(
                service.connect(),
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            handle(
                service.syncNow(),
            )
        }
    }

    fun completeAuthorization(
        data: Intent?,
    ) {
        mutableState.value =
            mutableState.value.copy(
                pendingResolution = null,
            )

        viewModelScope.launch {
            handle(
                service.completeAuthorization(
                    data,
                ),
            )
        }
    }

    fun resolutionLaunched() {
        mutableState.value =
            mutableState.value.copy(
                pendingResolution = null,
            )
    }

    fun disconnect() {
        viewModelScope.launch {
            val success =
                service.disconnect()
            mutableState.value =
                mutableState.value.copy(
                    message = if (success) {
                        SyncUiMessage.Disconnected
                    } else {
                        SyncUiMessage.Failure(
                            SyncFailureReason
                                .AUTHORIZATION_FAILED,
                        )
                    },
                )
        }
    }

    fun clearMessage() {
        mutableState.value =
            mutableState.value.copy(
                message = null,
            )
    }

    private fun handle(
        result: SyncActionResult,
    ) {
        when (result) {
            SyncActionResult.Completed ->
                mutableState.value =
                    mutableState.value.copy(
                        message =
                            SyncUiMessage
                                .SyncCompleted,
                    )

            is SyncActionResult
                .NeedsResolution ->
                mutableState.value =
                    mutableState.value.copy(
                        pendingResolution =
                            result.pendingIntent,
                    )

            is SyncActionResult.Failure ->
                mutableState.value =
                    mutableState.value.copy(
                        message =
                            SyncUiMessage.Failure(
                                result.reason,
                            ),
                    )
        }
    }
}
