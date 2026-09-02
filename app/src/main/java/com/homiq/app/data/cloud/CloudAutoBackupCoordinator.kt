package com.homiq.app.data.cloud

import com.homiq.app.data.backup.BackupPreferences
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-level automatic cloud backup coordinator.
 *
 * Local repository writes raise [SyncChangeSignal]. Changes are persisted as pending,
 * grouped with a short debounce window and uploaded at a conservative minimum interval.
 * If Android stops the process before an upload completes, the pending flag survives and
 * is retried the next time Homika starts.
 */
class CloudAutoBackupCoordinator(
    private val changes: SyncChangeSignal,
    private val cloudService: HomikaCloudBackupService,
    private val preferences: BackupPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runMutex = Mutex()
    private var scheduledJob: Job? = null
    private var started = false

    private val mutableState = MutableStateFlow(readState())
    val state: StateFlow<CloudAutoBackupState> = mutableState.asStateFlow()

    @Synchronized
    fun start() {
        if (started) return
        started = true

        scope.launch {
            changes.changes.collect {
                preferences.setCloudBackupPending(true)
                publishState()
                scheduleAfterDebounce()
            }
        }

        if (
            preferences.automaticCloudBackupEnabled &&
            preferences.cloudBackupPending
        ) {
            schedule(delayMillis = STARTUP_RETRY_DELAY_MILLIS)
        }
    }

    fun markLocalDataChanged() {
        preferences.setCloudBackupPending(true)
        publishState()
        scheduleAfterDebounce()
    }

    fun setEnabled(enabled: Boolean) {
        preferences.setAutomaticCloudBackupEnabled(enabled)
        publishState()

        if (enabled && preferences.cloudBackupPending) {
            scheduleAfterDebounce()
        } else if (!enabled) {
            scheduledJob?.cancel()
            scheduledJob = null
        }
    }

    /**
     * A successful manual cloud backup also makes the current local database cloud-current,
     * so a queued automatic upload for the same edits can be discarded.
     */
    fun markCloudCurrent(epochMillis: Long = System.currentTimeMillis()) {
        preferences.recordCloudBackupSuccess(
            epochMillis = epochMillis,
            automatic = false,
        )
        preferences.setCloudBackupPending(false)
        publishState()
    }

    private fun scheduleAfterDebounce() {
        if (!preferences.automaticCloudBackupEnabled) return
        schedule(DEBOUNCE_MILLIS)
    }

    @Synchronized
    private fun schedule(delayMillis: Long) {
        scheduledJob?.cancel()
        scheduledJob = scope.launch {
            delay(delayMillis)
            synchronized(this@CloudAutoBackupCoordinator) {
                scheduledJob = null
            }
            runPendingBackup()
        }
    }

    private suspend fun runPendingBackup() {
        if (
            !preferences.automaticCloudBackupEnabled ||
            !preferences.cloudBackupPending
        ) {
            publishState()
            return
        }

        val lastCloudBackup = preferences.lastCloudBackupEpochMillis
        if (lastCloudBackup != null) {
            val elapsed = System.currentTimeMillis() - lastCloudBackup
            if (elapsed < MINIMUM_UPLOAD_INTERVAL_MILLIS) {
                schedule(MINIMUM_UPLOAD_INTERVAL_MILLIS - elapsed)
                return
            }
        }

        runMutex.withLock {
            if (
                !preferences.automaticCloudBackupEnabled ||
                !preferences.cloudBackupPending
            ) {
                publishState()
                return@withLock
            }

            mutableState.value = readState().copy(isRunning = true)
            val result = cloudService.backupNow()
            val success = result.value

            if (success != null) {
                val (metadata, _) = success
                preferences.recordCloudBackupSuccess(
                    epochMillis = metadata.createdAtEpochMillis,
                    automatic = true,
                )
                preferences.setCloudBackupPending(false)
                mutableState.value = readState().copy(
                    isRunning = false,
                    lastFailure = null,
                )
            } else {
                // Keep the pending flag. A later edit or the next app launch retries it.
                mutableState.value = readState().copy(
                    isRunning = false,
                    lastFailure = result.failure ?: CloudBackupFailureReason.SERVER_ERROR,
                )
            }
        }
    }

    private fun publishState() {
        val previous = mutableState.value
        mutableState.value = readState().copy(
            isRunning = previous.isRunning,
            lastFailure = previous.lastFailure,
        )
    }

    private fun readState(): CloudAutoBackupState =
        CloudAutoBackupState(
            enabled = preferences.automaticCloudBackupEnabled,
            pending = preferences.cloudBackupPending,
            isRunning = false,
            lastSuccessEpochMillis = preferences.lastAutomaticCloudBackupEpochMillis,
            lastFailure = null,
        )

    companion object {
        const val DEBOUNCE_MILLIS = 30_000L
        const val MINIMUM_UPLOAD_INTERVAL_MILLIS = 2 * 60_000L
        private const val STARTUP_RETRY_DELAY_MILLIS = 10_000L
    }
}

data class CloudAutoBackupState(
    val enabled: Boolean,
    val pending: Boolean,
    val isRunning: Boolean,
    val lastSuccessEpochMillis: Long?,
    val lastFailure: CloudBackupFailureReason?,
)
