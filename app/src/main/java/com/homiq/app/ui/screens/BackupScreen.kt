package com.homiq.app.ui.screens

import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.backup.BackupDestination
import com.homiq.app.data.backup.BackupFailureReason
import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.cloud.CloudBackupFailureReason
import com.homiq.app.data.cloud.CloudSnapshotSyncFailureReason
import com.homiq.app.ui.viewmodel.BackupUiMessage
import com.homiq.app.ui.viewmodel.BackupViewModel
import java.util.Date

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCloudRecovery by remember { mutableStateOf(false) }
    var showCloudInfo by remember { mutableStateOf(false) }

    BackHandler(enabled = showCloudRecovery) {
        showCloudRecovery = false
    }

    val createBackup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            viewModel.createBackup(uri)
        }

    val inspectRestore =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            viewModel.inspectRestore(uri)
        }

    val syncStatus = when {
        state.cloudSyncRunning -> stringResource(R.string.cloud_sync_status_syncing)
        state.cloudSyncFailure != null -> stringResource(R.string.cloud_sync_status_issue)
        state.cloudSyncLastSuccessEpochMillis != null -> stringResource(R.string.cloud_sync_status_synced)
        else -> stringResource(R.string.cloud_sync_status_ready)
    }
    val syncTime = state.cloudSyncLastSuccessEpochMillis
        ?.let { compactRelativeTime(it) }
        ?: stringResource(R.string.cloud_sync_time_never)
    val syncDeviceCount = stringResource(
        R.string.cloud_sync_devices_value,
        state.cloudSyncRemoteDeviceCount + 1,
    )
    val compactSyncSummary = stringResource(
        R.string.cloud_sync_compact_summary,
        syncStatus,
        syncTime,
        syncDeviceCount,
    )

    val latestCloudText = when {
        state.isCloudRefreshing -> stringResource(R.string.cloud_checking)
        state.cloudLatest == null -> stringResource(R.string.cloud_no_backup)
        else -> {
            val latest = state.cloudLatest!!
            stringResource(
                R.string.cloud_latest_summary,
                formatDateTime(context, latest.createdAtEpochMillis),
                latest.recordCount,
                formatBytes(latest.byteSize),
            )
        }
    }

    val automaticStatus: String? = when {
        !state.automaticCloudBackupEnabled -> stringResource(R.string.cloud_auto_backup_off)
        state.automaticCloudBackupRunning -> stringResource(R.string.cloud_auto_backup_running)
        state.automaticCloudBackupPending -> stringResource(R.string.cloud_auto_backup_pending)
        else -> null
    }

    if (showCloudRecovery) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = { showCloudRecovery = false }) {
                Text(stringResource(R.string.cloud_back))
            }

            Text(
                text = stringResource(R.string.cloud_recovery_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.cloud_recovery_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HistoryRow(
                        label = stringResource(R.string.cloud_latest_backup),
                        value = latestCloudText,
                    )

                    Text(
                        text = stringResource(R.string.cloud_backup_restore_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = viewModel::inspectCloudRestore,
                        enabled = !state.isBusy && state.cloudLatest != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null)
                        Text(
                            text = stringResource(R.string.cloud_restore_latest),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    if (state.cloudLatest == null && !state.isCloudRefreshing) {
                        Text(
                            text = stringResource(R.string.cloud_restore_no_backup_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = stringResource(R.string.cloud_encryption_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_restore_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(R.string.cloud_backup_page_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_backup_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = compactSyncSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = viewModel::syncNow,
                            enabled = !state.cloudSyncRunning,
                        ) {
                            if (state.cloudSyncRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.cloud_sync_now),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }

                    state.cloudSyncFailure?.let { reason ->
                        Text(
                            text = cloudSyncFailureText(reason),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (state.cloudSyncConflictCount > 0 || state.cloudSyncIgnoredSnapshotCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.cloud_sync_attention,
                                state.cloudSyncConflictCount,
                                state.cloudSyncIgnoredSnapshotCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_auto_backup_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.cloud_auto_backup_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.automaticCloudBackupEnabled,
                            onCheckedChange = viewModel::setAutomaticCloudBackup,
                            enabled = !state.isBusy,
                        )
                    }

                    automaticStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HistoryRow(
                        label = stringResource(R.string.cloud_latest_backup),
                        value = latestCloudText,
                    )

                    Button(
                        onClick = viewModel::createCloudBackup,
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Text(
                            text = stringResource(R.string.cloud_backup_now),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.isBusy) {
                                    showCloudRecovery = true
                                }
                                .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_recovery_row_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.cloud_recovery_row_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    TextButton(onClick = { showCloudInfo = true }) {
                        Text(stringResource(R.string.cloud_learn_more))
                    }
                }
            }

            Text(
                text = stringResource(R.string.local_backup_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.backup_zero_cost_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.backup_zero_cost_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HistoryRow(
                        label = stringResource(R.string.backup_last_backup),
                        value =
                            state.history.lastBackupEpochMillis
                                ?.let { formatDateTime(context, it) }
                                ?: stringResource(R.string.never),
                    )
                    HistoryRow(
                        label = stringResource(R.string.backup_last_restore),
                        value =
                            state.history.lastRestoreEpochMillis
                                ?.let { formatDateTime(context, it) }
                                ?: stringResource(R.string.never),
                    )
                }
            }

            Button(
                onClick = {
                    createBackup.launch(viewModel.backupFileName())
                },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = null)
                Text(
                    text = stringResource(R.string.create_backup),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    inspectRestore.launch(
                        arrayOf(
                            "application/octet-stream",
                            "application/json",
                            "text/plain",
                            "*/*",
                        ),
                    )
                },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null)
                Text(
                    text = stringResource(R.string.restore_backup),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Text(
                text = stringResource(R.string.backup_picker_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isBusy) {
                CircularProgressIndicator()
            }
        }
    }

    if (showCloudInfo) {
        AlertDialog(
            onDismissRequest = { showCloudInfo = false },
            title = {
                Text(stringResource(R.string.cloud_info_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.cloud_sync_foreground_note))
                    Text(stringResource(R.string.cloud_encryption_note))
                    Text(stringResource(R.string.cloud_sync_backup_distinction))
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudInfo = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    state.pendingRestorePreview?.let { preview ->
        RestoreConfirmation(
            preview = preview,
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }

    state.message?.let { message ->
        BackupMessageDialog(
            message = message,
            onDismiss = viewModel::clearMessage,
        )
    }
}

@Composable
private fun compactRelativeTime(epochMillis: Long): String {
    val elapsed = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60L * minute
    val day = 24L * hour

    return when {
        elapsed < minute -> stringResource(R.string.cloud_sync_time_now)
        elapsed < hour -> stringResource(R.string.cloud_sync_time_minutes, elapsed / minute)
        elapsed < day -> stringResource(R.string.cloud_sync_time_hours, elapsed / hour)
        else -> formatDateTime(LocalContext.current, epochMillis)
    }
}

@Composable
private fun HistoryRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RestoreConfirmation(
    preview: BackupPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val created = formatDateTime(context, preview.createdAtEpochMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.restore_confirm_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.restore_confirm_body,
                    created,
                    preview.propertyCount,
                    preview.bookingCount,
                    preview.paymentCount,
                    preview.expenseCount,
                    preview.totalRecordCount,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.restore_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun BackupMessageDialog(
    message: BackupUiMessage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val title: String
    val body: String

    when (message) {
        is BackupUiMessage.BackupCreated -> {
            title = stringResource(R.string.backup_success_title)
            body = stringResource(
                R.string.backup_success_body,
                message.preview.totalRecordCount,
            )
        }

        is BackupUiMessage.CloudBackupCreated -> {
            title = stringResource(R.string.cloud_backup_success_title)
            body = stringResource(
                R.string.cloud_backup_success_body,
                message.preview.totalRecordCount,
            )
        }

        is BackupUiMessage.RestoreCompleted -> {
            title = if (message.source == BackupDestination.HOMIKA_CLOUD) {
                stringResource(R.string.cloud_restore_success_title)
            } else {
                stringResource(R.string.restore_success_title)
            }
            val created = formatDateTime(context, message.preview.createdAtEpochMillis)
            body = stringResource(
                R.string.restore_success_body,
                message.preview.totalRecordCount,
                created,
            )
        }

        is BackupUiMessage.Failure -> {
            title = stringResource(R.string.backup_error_title)
            body = stringResource(
                when (message.reason) {
                    BackupFailureReason.FILE_UNAVAILABLE -> R.string.backup_error_file
                    BackupFailureReason.INVALID_BACKUP -> R.string.backup_error_invalid
                    BackupFailureReason.UNSUPPORTED_FORMAT -> R.string.backup_error_format
                    BackupFailureReason.UNSUPPORTED_DATABASE_VERSION ->
                        R.string.backup_error_database_version
                    BackupFailureReason.WRITE_FAILED -> R.string.backup_error_write
                    BackupFailureReason.RESTORE_FAILED -> R.string.backup_error_restore
                },
            )
        }

        is BackupUiMessage.CloudFailure -> {
            title = stringResource(R.string.cloud_error_title)
            body = stringResource(
                when (message.reason) {
                    CloudBackupFailureReason.LICENSE_REQUIRED -> R.string.cloud_error_license
                    CloudBackupFailureReason.NETWORK_UNAVAILABLE -> R.string.cloud_error_network
                    CloudBackupFailureReason.CLOUD_NOT_CONFIGURED -> R.string.cloud_error_not_configured
                    CloudBackupFailureReason.BACKUP_NOT_FOUND -> R.string.cloud_error_not_found
                    CloudBackupFailureReason.BACKUP_TOO_LARGE -> R.string.cloud_error_too_large
                    CloudBackupFailureReason.INVALID_CLOUD_BACKUP -> R.string.cloud_error_invalid
                    CloudBackupFailureReason.SERVER_REJECTED -> R.string.cloud_error_rejected
                    CloudBackupFailureReason.SERVER_ERROR -> R.string.cloud_error_server
                },
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

private fun formatDateTime(
    context: Context,
    epochMillis: Long,
): String {
    val date = Date(epochMillis)
    val dateText = DateFormat.getMediumDateFormat(context).format(date)
    val timeText = DateFormat.getTimeFormat(context).format(date)
    return "$dateText $timeText"
}

@Composable
private fun cloudSyncFailureText(reason: CloudSnapshotSyncFailureReason): String =
    stringResource(
        when (reason) {
            CloudSnapshotSyncFailureReason.LICENSE_REQUIRED -> R.string.cloud_sync_error_license
            CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE -> R.string.cloud_sync_error_network
            CloudSnapshotSyncFailureReason.CLOUD_NOT_CONFIGURED -> R.string.cloud_sync_error_not_configured
            CloudSnapshotSyncFailureReason.INVALID_REMOTE_SNAPSHOT -> R.string.cloud_sync_error_invalid
            CloudSnapshotSyncFailureReason.SNAPSHOT_TOO_LARGE -> R.string.cloud_sync_error_too_large
            CloudSnapshotSyncFailureReason.SERVER_REJECTED -> R.string.cloud_sync_error_rejected
            CloudSnapshotSyncFailureReason.SERVER_ERROR -> R.string.cloud_sync_error_server
        },
    )

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
