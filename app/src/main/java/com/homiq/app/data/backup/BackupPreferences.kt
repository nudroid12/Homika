package com.homiq.app.data.backup

import android.content.Context

enum class BackupDestination {
    DEVICE_FILE,
    HOMIKA_CLOUD,
}

class BackupPreferences(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    val lastBackupDestination: BackupDestination?
        get() =
            preferences.getString(
                KEY_LAST_BACKUP_DESTINATION,
                null,
            )?.let {
                runCatching {
                    enumValueOf<BackupDestination>(it)
                }.getOrNull()
            }

    val lastRestoreSource: BackupDestination?
        get() =
            preferences.getString(
                KEY_LAST_RESTORE_SOURCE,
                null,
            )?.let {
                runCatching {
                    enumValueOf<BackupDestination>(it)
                }.getOrNull()
            }

    val automaticCloudBackupEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_CLOUD_BACKUP_ENABLED, true)

    val cloudBackupPending: Boolean
        get() = preferences.getBoolean(KEY_CLOUD_BACKUP_PENDING, false)

    val lastCloudBackupEpochMillis: Long?
        get() = preferences.getLong(KEY_LAST_CLOUD_BACKUP, 0L).takeIf { it > 0L }

    val lastAutomaticCloudBackupEpochMillis: Long?
        get() = preferences
            .getLong(KEY_LAST_AUTO_CLOUD_BACKUP, 0L)
            .takeIf { it > 0L }

    fun recordBackup(
        destination: BackupDestination,
    ) {
        preferences.edit()
            .putString(
                KEY_LAST_BACKUP_DESTINATION,
                destination.name,
            )
            .apply()
    }

    fun recordRestore(
        source: BackupDestination,
    ) {
        preferences.edit()
            .putString(
                KEY_LAST_RESTORE_SOURCE,
                source.name,
            )
            .apply()
    }

    fun setAutomaticCloudBackupEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_AUTO_CLOUD_BACKUP_ENABLED, enabled)
            .apply()
    }

    fun setCloudBackupPending(pending: Boolean) {
        preferences.edit()
            .putBoolean(KEY_CLOUD_BACKUP_PENDING, pending)
            .apply()
    }

    fun recordCloudBackupSuccess(
        epochMillis: Long,
        automatic: Boolean,
    ) {
        preferences.edit()
            .putLong(KEY_LAST_CLOUD_BACKUP, epochMillis)
            .apply {
                if (automatic) {
                    putLong(KEY_LAST_AUTO_CLOUD_BACKUP, epochMillis)
                }
            }
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "homiq_backup"
        private const val KEY_LAST_BACKUP_DESTINATION = "last_backup_destination"
        private const val KEY_LAST_RESTORE_SOURCE = "last_restore_source"
        private const val KEY_AUTO_CLOUD_BACKUP_ENABLED = "auto_cloud_backup_enabled"
        private const val KEY_CLOUD_BACKUP_PENDING = "cloud_backup_pending"
        private const val KEY_LAST_CLOUD_BACKUP = "last_cloud_backup"
        private const val KEY_LAST_AUTO_CLOUD_BACKUP = "last_auto_cloud_backup"
    }
}
