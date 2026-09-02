package com.homiq.app.ui.util

import androidx.annotation.StringRes
import com.homiq.app.R
import com.homiq.app.data.backup.BackupFailureReason
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatBackupTime(
    epochMillis: Long?,
    locale: Locale,
): String? {
    if (epochMillis == null) return null

    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        locale,
    ).format(
        Date(epochMillis),
    )
}

@StringRes
fun BackupFailureReason.messageRes(): Int =
    when (this) {
        BackupFailureReason.FILE_UNAVAILABLE ->
            R.string.backup_error_file
        BackupFailureReason.INVALID_BACKUP ->
            R.string.backup_error_invalid
        BackupFailureReason.UNSUPPORTED_FORMAT ->
            R.string.backup_error_format
        BackupFailureReason.UNSUPPORTED_DATABASE_VERSION ->
            R.string.backup_error_database_version
        BackupFailureReason.WRITE_FAILED ->
            R.string.backup_error_write
        BackupFailureReason.RESTORE_FAILED ->
            R.string.backup_error_restore
    }
