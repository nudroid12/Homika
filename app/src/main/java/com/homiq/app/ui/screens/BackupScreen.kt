package com.homiq.app.ui.screens

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.backup.BackupFailureReason
import com.homiq.app.data.backup.BackupPreview
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

    val createBackup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/octet-stream",
            ),
        ) { uri ->
            viewModel.createBackup(uri)
        }

    val inspectRestore =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            viewModel.inspectRestore(uri)
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_restore_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = stringResource(R.string.backup_restore_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            ?.let {
                                DateFormat.getMediumDateFormat(context)
                                    .format(Date(it))
                            }
                            ?: stringResource(R.string.never),
                )
                HistoryRow(
                    label = stringResource(R.string.backup_last_restore),
                    value =
                        state.history.lastRestoreEpochMillis
                            ?.let {
                                DateFormat.getMediumDateFormat(context)
                                    .format(Date(it))
                            }
                            ?: stringResource(R.string.never),
                )
            }
        }

        Button(
            onClick = {
                createBackup.launch(
                    viewModel.backupFileName(),
                )
            },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Outlined.Backup,
                contentDescription = null,
            )
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
            Icon(
                Icons.Outlined.Restore,
                contentDescription = null,
            )
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
private fun HistoryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
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
    val created =
        DateFormat.getMediumDateFormat(context)
            .format(Date(preview.createdAtEpochMillis))

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
            body =
                stringResource(
                    R.string.backup_success_body,
                    message.preview.totalRecordCount,
                )
        }

        is BackupUiMessage.RestoreCompleted -> {
            title = stringResource(R.string.restore_success_title)
            val created =
                DateFormat.getMediumDateFormat(context)
                    .format(
                        Date(
                            message.preview.createdAtEpochMillis,
                        ),
                    )
            body =
                stringResource(
                    R.string.restore_success_body,
                    message.preview.totalRecordCount,
                    created,
                )
        }

        is BackupUiMessage.Failure -> {
            title = stringResource(R.string.backup_error_title)
            body =
                stringResource(
                    when (message.reason) {
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
