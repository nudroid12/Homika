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
 * Foreground/process sync foundation.
 *
 * Phase 3A syncs shortly after local writes, once after startup, and periodically while
 * the Homika process is alive. Phase 3B can layer explicit UI, WorkManager scheduling,
 * and conflict resolution on top of this engine without changing the wire protocol.
 */
class CloudSyncCoordinator(
    private val changes: SyncChangeSignal,
    private val service: HomikaCloudSyncService,
    private val preferences: CloudSyncPreferences,
    private val licenseRepository: LicenseRepository,
    private val onRemoteApplied: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runMutex = Mutex()
    private var scheduledJob: Job? = null
    private var started = false

    private val mutableState = MutableStateFlow(CloudSyncCoordinatorState())
    val state: StateFlow<CloudSyncCoordinatorState> = mutableState.asStateFlow()

    @Synchronized
    fun start() {
        if (started) return
        started = true

        scope.launch {
            changes.changes.collect {
                schedule(DEBOUNCE_AFTER_LOCAL_CHANGE_MILLIS)
            }
        }

        scope.launch {
            delay(STARTUP_DELAY_MILLIS)
            runSync()
            while (isActive) {
                delay(FOREGROUND_POLL_INTERVAL_MILLIS)
                runSync()
            }
        }
    }

    fun syncSoon() {
        schedule(250L)
    }

    @Synchronized
    private fun schedule(delayMillis: Long) {
        scheduledJob?.cancel()
        scheduledJob = scope.launch {
            delay(delayMillis)
            synchronized(this@CloudSyncCoordinator) {
                scheduledJob = null
            }
            runSync()
        }
    }

    private suspend fun runSync() {
        runMutex.withLock {
            mutableState.value = mutableState.value.copy(isRunning = true)
            val result = service.syncOnce()
            val summary = result.value

            if (summary != null) {
                if (summary.remoteApplied > 0) {
                    onRemoteApplied()
                }
                val licenseId = licenseRepository.cloudCredentials()?.licenseId
                mutableState.value = CloudSyncCoordinatorState(
                    isRunning = false,
                    lastSuccessEpochMillis = licenseId?.let(preferences::lastSuccessEpochMillis),
                    lastFailure = null,
                    lastSummary = summary,
                    unresolvedConflicts = licenseId?.let { preferences.conflicts(it).size } ?: 0,
                )
            } else {
                mutableState.value = mutableState.value.copy(
                    isRunning = false,
                    lastFailure = result.failure,
                )
            }
        }
    }

    companion object {
        const val DEBOUNCE_AFTER_LOCAL_CHANGE_MILLIS = 5_000L
        const val FOREGROUND_POLL_INTERVAL_MILLIS = 60_000L
        private const val STARTUP_DELAY_MILLIS = 5_000L
    }
}

data class CloudSyncCoordinatorState(
    val isRunning: Boolean = false,
    val lastSuccessEpochMillis: Long? = null,
    val lastFailure: CloudSyncFailureReason? = null,
    val lastSummary: CloudSyncRunSummary? = null,
    val unresolvedConflicts: Int = 0,
)
