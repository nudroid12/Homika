package com.homiq.app.data.cloud

import com.homiq.app.data.license.LicenseRepository
import com.homiq.app.data.sync.SyncChangeSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Foreground-only cloud sync coordinator.
 *
 * It observes local writes for the whole process, but it performs no network activity while
 * Homika is in the background. Foreground entry triggers an immediate check. Local edits are
 * debounced by 1.5 seconds, matching the proven Homika Personal behavior, and an active app does
 * a lightweight metadata check every 30 seconds for changes from other licensed devices.
 */
class CloudSnapshotSyncCoordinator(
    private val changes: SyncChangeSignal,
    private val service: HomikaCloudSnapshotSyncService,
    private val preferences: CloudSnapshotSyncPreferences,
    private val licenseRepository: LicenseRepository,
    private val onRemoteApplied: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runMutex = Mutex()
    private var observerStarted = false
    private var foreground = false
    private var localDebounceJob: Job? = null
    private var pollJob: Job? = null

    private val mutableState = MutableStateFlow(CloudSnapshotSyncCoordinatorState())
    val state: StateFlow<CloudSnapshotSyncCoordinatorState> = mutableState.asStateFlow()

    @Synchronized
    fun startObserver() {
        if (observerStarted) return
        observerStarted = true

        scope.launch {
            changes.changes.collect {
                preferences.localChangePending = true
                if (isForeground()) {
                    scheduleLocalChangeSync()
                }
            }
        }
    }

    @Synchronized
    fun onAppForeground() {
        foreground = true
        localDebounceJob?.cancel()
        localDebounceJob = null
        pollJob?.cancel()
        pollJob = scope.launch {
            runSync()
            while (isActive && isForeground()) {
                delay(FOREGROUND_POLL_INTERVAL_MILLIS)
                if (isForeground()) runSync()
            }
        }
    }

    @Synchronized
    fun onAppBackground() {
        foreground = false
        localDebounceJob?.cancel()
        localDebounceJob = null
        pollJob?.cancel()
        pollJob = null
    }

    fun syncNow() {
        if (!isForeground()) return
        scope.launch { runSync() }
    }

    @Synchronized
    private fun scheduleLocalChangeSync() {
        localDebounceJob?.cancel()
        localDebounceJob = scope.launch {
            delay(LOCAL_CHANGE_DEBOUNCE_MILLIS)
            synchronized(this@CloudSnapshotSyncCoordinator) {
                localDebounceJob = null
            }
            if (isForeground()) runSync()
        }
    }

    private suspend fun runSync() {
        if (!isForeground()) return
        runMutex.withLock {
            if (!isForeground()) return@withLock
            mutableState.value = mutableState.value.copy(isRunning = true)
            val result = service.syncOnce()
            val summary = result.value

            if (summary != null) {
                if (summary.remoteApplied) onRemoteApplied()
                val licenseId = licenseRepository.cloudCredentials()?.licenseId
                mutableState.value = CloudSnapshotSyncCoordinatorState(
                    isRunning = false,
                    lastSuccessEpochMillis = licenseId?.let(preferences::lastSuccessEpochMillis),
                    lastFailure = null,
                    lastSummary = summary,
                )
            } else {
                mutableState.value = mutableState.value.copy(
                    isRunning = false,
                    lastFailure = result.failure,
                )
            }
        }
    }

    @Synchronized
    private fun isForeground(): Boolean = foreground

    companion object {
        const val LOCAL_CHANGE_DEBOUNCE_MILLIS = 1_500L
        const val FOREGROUND_POLL_INTERVAL_MILLIS = 30_000L
    }
}

data class CloudSnapshotSyncCoordinatorState(
    val isRunning: Boolean = false,
    val lastSuccessEpochMillis: Long? = null,
    val lastFailure: CloudSnapshotSyncFailureReason? = null,
    val lastSummary: CloudSnapshotSyncRunSummary? = null,
)
