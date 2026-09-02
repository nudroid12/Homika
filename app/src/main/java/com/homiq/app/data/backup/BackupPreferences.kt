package com.homiq.app.data.backup

import android.content.Context

enum class BackupDestination {
    DEVICE_FILE,
    GOOGLE_DRIVE,
}

class BackupPreferences(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    val autoBackupEnabled: Boolean
        get() =
            preferences.getBoolean(
                KEY_AUTO_BACKUP,
                false,
            )

    val autoBackupPending: Boolean
        get() =
            preferences.getBoolean(
                KEY_AUTO_BACKUP_PENDING,
                false,
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

    fun setAutoBackupEnabled(
        enabled: Boolean,
    ) {
        preferences.edit()
            .putBoolean(
                KEY_AUTO_BACKUP,
                enabled,
            )
            .apply()
    }

    fun setAutoBackupPending(
        pending: Boolean,
    ) {
        preferences.edit()
            .putBoolean(
                KEY_AUTO_BACKUP_PENDING,
                pending,
            )
            .apply()
    }

    fun recordBackup(
        destination: BackupDestination,
    ) {
        preferences.edit()
            .putLong(
                KEY_LAST_BACKUP,
                System.currentTimeMillis(),
            )
            .putString(
                KEY_LAST_BACKUP_DESTINATION,
                destination.name,
            )
            .putBoolean(
                KEY_AUTO_BACKUP_PENDING,
                false,
            )
            .apply()
    }

    fun recordRestore(
        source: BackupDestination,
    ) {
        preferences.edit()
            .putLong(
                KEY_LAST_RESTORE,
                System.currentTimeMillis(),
            )
            .putString(
                KEY_LAST_RESTORE_SOURCE,
                source.name,
            )
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "homiq_backup"
        private const val KEY_LAST_BACKUP =
            "last_backup"
        private const val KEY_LAST_RESTORE =
            "last_restore"
        private const val KEY_LAST_BACKUP_DESTINATION =
            "last_backup_destination"
        private const val KEY_LAST_RESTORE_SOURCE =
            "last_restore_source"
        private const val KEY_AUTO_BACKUP =
            "auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_PENDING =
            "auto_backup_pending"
    }
}
