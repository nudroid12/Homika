package com.homiq.app.data.backup

import android.content.Context

enum class BackupDestination {
    DEVICE_FILE,
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

    companion object {
        private const val PREFERENCES_NAME = "homiq_backup"
        private const val KEY_LAST_BACKUP_DESTINATION = "last_backup_destination"
        private const val KEY_LAST_RESTORE_SOURCE = "last_restore_source"
    }
}
