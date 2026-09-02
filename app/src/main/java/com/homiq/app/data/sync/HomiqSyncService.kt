package com.homiq.app.data.sync

import android.app.PendingIntent
import android.content.Intent
import com.homiq.app.data.account.GoogleAccountService
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncFailureReason {
    AUTHORIZATION_FAILED,
    AUTHORIZATION_CANCELLED,
    NETWORK_UNAVAILABLE,
    DRIVE_ACCESS_FAILED,
    REMOTE_DATA_INVALID,
    UNKNOWN,
}

data class SyncRuntimeState(
    val enabled: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncEpochMillis: Long? = null,
    val lastConflictCount: Int = 0,
    val remoteDeviceCount: Int = 0,
    val ignoredRemoteFileCount: Int = 0,
    val lastRecordCount: Int = 0,
    val authorizationRequired: Boolean = false,
    val lastFailure: SyncFailureReason? = null,
)

sealed interface SyncActionResult {
    data object Completed :
        SyncActionResult

    data class NeedsResolution(
        val pendingIntent: PendingIntent,
    ) : SyncActionResult

    data class Failure(
        val reason: SyncFailureReason,
    ) : SyncActionResult
}

class HomiqSyncService(
    private val authorization:
        GoogleDriveAuthorization,
    private val engine:
        HomiqSyncEngine,
    private val preferences:
        SyncPreferences,
    private val changes:
        SyncChangeSignal,
    private val accountService:
        GoogleAccountService,
) {
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO,
        )
    private val mutex = Mutex()
    private var autoJob: Job? = null

    private val initial =
        preferences.state()

    private val mutableState =
        MutableStateFlow(
            SyncRuntimeState(
                enabled = initial.enabled,
                lastSyncEpochMillis =
                    initial.lastSyncEpochMillis,
                lastConflictCount =
                    initial.lastConflictCount,
                remoteDeviceCount =
                    initial.lastRemoteDeviceCount,
            ),
        )

    val state: StateFlow<SyncRuntimeState> =
        mutableState.asStateFlow()

    fun startAutoSync() {
        if (autoJob != null) return

        autoJob = scope.launch {
            changes.changes
                .collect {
                    delay(1_500)
                    syncSilently()
                }
        }
    }

    fun onAppForeground() {
        if (!preferences.state().enabled) {
            return
        }

        scope.launch {
            syncSilently()
        }
    }

    suspend fun connect():
        SyncActionResult {
        return when (
            val auth =
                authorization.authorize()
        ) {
            is DriveAuthorizationResult
                .Authorized -> {
                accountService.rememberAuthorizedToken(
                    auth.accessToken,
                )
                preferences.setEnabled(true)
                mutableState.value =
                    mutableState.value.copy(
                        enabled = true,
                        authorizationRequired =
                            false,
                        lastFailure = null,
                    )
                runSync(auth.accessToken)
            }

            is DriveAuthorizationResult
                .NeedsResolution ->
                SyncActionResult.NeedsResolution(
                    auth.pendingIntent,
                )

            is DriveAuthorizationResult
                .Failure ->
                fail(auth.reason)
        }
    }

    suspend fun completeAuthorization(
        data: Intent?,
    ): SyncActionResult =
        when (
            val auth =
                authorization
                    .authorizationResultFromIntent(
                        data,
                    )
        ) {
            is DriveAuthorizationResult
                .Authorized -> {
                accountService.rememberAuthorizedToken(
                    auth.accessToken,
                )
                preferences.setEnabled(true)
                mutableState.value =
                    mutableState.value.copy(
                        enabled = true,
                        authorizationRequired =
                            false,
                        lastFailure = null,
                    )
                runSync(auth.accessToken)
            }

            is DriveAuthorizationResult
                .NeedsResolution ->
                SyncActionResult.NeedsResolution(
                    auth.pendingIntent,
                )

            is DriveAuthorizationResult
                .Failure ->
                fail(auth.reason)
        }

    suspend fun syncNow():
        SyncActionResult {
        if (!preferences.state().enabled) {
            return connect()
        }

        return when (
            val auth =
                authorization.authorize()
        ) {
            is DriveAuthorizationResult
                .Authorized ->
                runSync(auth.accessToken)

            is DriveAuthorizationResult
                .NeedsResolution -> {
                mutableState.value =
                    mutableState.value.copy(
                        authorizationRequired =
                            true,
                    )
                SyncActionResult.NeedsResolution(
                    auth.pendingIntent,
                )
            }

            is DriveAuthorizationResult
                .Failure ->
                fail(auth.reason)
        }
    }

    suspend fun disconnect(): Boolean {
        preferences.setEnabled(false)
        mutableState.value =
            mutableState.value.copy(
                enabled = false,
                isSyncing = false,
                authorizationRequired = false,
                lastFailure = null,
            )
        return true
    }

    private suspend fun syncSilently() {
        if (!preferences.state().enabled) {
            return
        }

        when (
            val auth =
                authorization.authorize()
        ) {
            is DriveAuthorizationResult
                .Authorized ->
                runSync(auth.accessToken)

            is DriveAuthorizationResult
                .NeedsResolution ->
                mutableState.value =
                    mutableState.value.copy(
                        authorizationRequired =
                            true,
                    )

            is DriveAuthorizationResult
                .Failure ->
                fail(auth.reason)
        }
    }

    private suspend fun runSync(
        accessToken: String,
    ): SyncActionResult =
        mutex.withLock {
            mutableState.value =
                mutableState.value.copy(
                    isSyncing = true,
                    lastFailure = null,
                )

            val result =
                runCatching {
                    engine.sync(accessToken)
                }

            result.fold(
                onSuccess = { success ->
                    preferences.recordSync(
                        conflictCount =
                            success.conflictCount,
                        remoteDeviceCount =
                            success.remoteDeviceCount,
                    )
                    val stored =
                        preferences.state()

                    mutableState.value =
                        mutableState.value.copy(
                            enabled = true,
                            isSyncing = false,
                            lastSyncEpochMillis =
                                stored
                                    .lastSyncEpochMillis,
                            lastConflictCount =
                                success.conflictCount,
                            remoteDeviceCount =
                                success
                                    .remoteDeviceCount,
                            ignoredRemoteFileCount =
                                success
                                    .ignoredRemoteFileCount,
                            lastRecordCount =
                                success
                                    .totalRecordCount,
                            authorizationRequired =
                                false,
                            lastFailure = null,
                        )

                    SyncActionResult.Completed
                },
                onFailure = { error ->
                    fail(
                        mapFailure(error),
                    )
                },
            )
        }

    private fun mapFailure(
        error: Throwable,
    ): SyncFailureReason =
        when (error) {
            is DriveHttpException -> {
                when (error.statusCode) {
                    401, 403 ->
                        SyncFailureReason
                            .DRIVE_ACCESS_FAILED
                    in 500..599,
                    429 ->
                        SyncFailureReason
                            .NETWORK_UNAVAILABLE
                    else ->
                        SyncFailureReason
                            .DRIVE_ACCESS_FAILED
                }
            }

            is IOException ->
                SyncFailureReason
                    .NETWORK_UNAVAILABLE

            is IllegalArgumentException ->
                SyncFailureReason
                    .REMOTE_DATA_INVALID

            else ->
                SyncFailureReason.UNKNOWN
        }

    private fun fail(
        reason: SyncFailureReason,
    ): SyncActionResult.Failure {
        mutableState.value =
            mutableState.value.copy(
                isSyncing = false,
                lastFailure = reason,
            )
        return SyncActionResult.Failure(
            reason,
        )
    }
}
