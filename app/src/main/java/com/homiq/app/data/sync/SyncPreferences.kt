package com.homiq.app.data.sync

import android.content.Context
import java.util.UUID

data class SyncStoredState(
    val enabled: Boolean,
    val lastSyncEpochMillis: Long?,
    val lastConflictCount: Int,
    val lastRemoteDeviceCount: Int,
)

class SyncPreferences(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    val deviceId: String
        get() {
            val current =
                preferences.getString(
                    KEY_DEVICE_ID,
                    null,
                )
            if (!current.isNullOrBlank()) {
                return current
            }

            val generated =
                UUID.randomUUID().toString()
            preferences.edit()
                .putString(
                    KEY_DEVICE_ID,
                    generated,
                )
                .apply()
            return generated
        }

    fun state(): SyncStoredState =
        SyncStoredState(
            enabled =
                preferences.getBoolean(
                    KEY_ENABLED,
                    false,
                ),
            lastSyncEpochMillis =
                preferences
                    .getLong(
                        KEY_LAST_SYNC,
                        0L,
                    )
                    .takeIf { it > 0L },
            lastConflictCount =
                preferences.getInt(
                    KEY_LAST_CONFLICTS,
                    0,
                ),
            lastRemoteDeviceCount =
                preferences.getInt(
                    KEY_LAST_REMOTE_DEVICES,
                    0,
                ),
        )

    fun setEnabled(
        enabled: Boolean,
    ) {
        preferences.edit()
            .putBoolean(
                KEY_ENABLED,
                enabled,
            )
            .apply()
    }

    fun recordSync(
        conflictCount: Int,
        remoteDeviceCount: Int,
    ) {
        preferences.edit()
            .putLong(
                KEY_LAST_SYNC,
                System.currentTimeMillis(),
            )
            .putInt(
                KEY_LAST_CONFLICTS,
                conflictCount,
            )
            .putInt(
                KEY_LAST_REMOTE_DEVICES,
                remoteDeviceCount,
            )
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "homiq_sync"
        private const val KEY_DEVICE_ID =
            "device_id"
        private const val KEY_ENABLED =
            "enabled"
        private const val KEY_LAST_SYNC =
            "last_sync"
        private const val KEY_LAST_CONFLICTS =
            "last_conflicts"
        private const val KEY_LAST_REMOTE_DEVICES =
            "last_remote_devices"
    }
}
