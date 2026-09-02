package com.homiq.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.backup.BackupDestination
import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.DriveBackupFailureReason
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.util.formatBackupTime
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.viewmodel.BackupUiMessage
import com.homiq.app.ui.viewmodel.BackupViewModel

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
) {
    val state by
        viewModel.state
            .collectAsStateWithLifecycle()
    val locale =
        LocalConfiguration
            .current
            .locales[0]

    val createLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/json",
                    ),
        ) { uri ->
            if (uri != null) {
                viewModel.createBackup(uri)
            }
        }

    val openLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .OpenDocument(),
        ) { uri ->
            if (uri != null) {
                viewModel.inspectRestore(uri)
            }
        }

    val authorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartIntentSenderForResult(),
        ) { result ->
            viewModel
                .completeDriveAuthorization(
                    result.data,
                )
        }

    state.pendingDriveResolution
        ?.let { pending ->
            LaunchedEffect(pending) {
                viewModel
                    .driveResolutionLaunched()
                authorizationLauncher.launch(
                    IntentSenderRequest.Builder(
                        pending.intentSender,
                    ).build(),
                )
            }
        }

    val lastBackup =
        formatBackupTime(
            state.history
                .lastBackupEpochMillis,
            locale,
        )
            ?: stringResource(
                R.string.never,
            )

    val lastRestore =
        formatBackupTime(
            state.history
                .lastRestoreEpochMillis,
            locale,
        )
            ?: stringResource(
                R.string.never,
            )

    LazyColumn(
        modifier =
            modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = 28.dp,
            ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title =
                    stringResource(
                        R.string
                            .backup_restore_title,
                    ),
                subtitle =
                    stringResource(
                        R.string
                            .backup_final_subtitle,
                    ),
            )
        }

        item {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    MaterialTheme
                        .shapes
                        .extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(
                                    R.string
                                        .backup_last_backup,
                                ),
                            )
                        },
                        supportingContent = {
                            Text(
                                backupHistoryText(
                                    time =
                                        lastBackup,
                                    destination =
                                        state
                                            .lastBackupDestination,
                                ),
                            )
                        },
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(
                                    R.string
                                        .backup_last_restore,
                                ),
                            )
                        },
                        supportingContent = {
                            Text(
                                backupHistoryText(
                                    time =
                                        lastRestore,
                                    destination =
                                        state
                                            .lastRestoreSource,
                                ),
                            )
                        },
                    )
                }
            }
        }

        item {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    MaterialTheme
                        .shapes
                        .extraLarge,
                tonalElevation = 1.dp,
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string
                                    .backup_auto_title,
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                        )
                    },
                    supportingContent = {
                        Column(
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        3.dp,
                                    ),
                        ) {
                            Text(
                                stringResource(
                                    if (
                                        state
                                            .driveConnected
                                    ) {
                                        R.string
                                            .backup_auto_body
                                    } else {
                                        R.string
                                            .backup_auto_requires_drive
                                    },
                                ),
                            )

                            if (
                                state.autoBackupPending
                            ) {
                                Text(
                                    text =
                                        stringResource(
                                            if (
                                                state.autoBackupRunning
                                            ) {
                                                R.string
                                                    .backup_auto_running
                                            } else {
                                                R.string
                                                    .backup_auto_pending
                                            },
                                        ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked =
                                state
                                    .autoBackupEnabled,
                            onCheckedChange =
                                viewModel::
                                    setAutoBackupEnabled,
                            enabled =
                                state
                                    .driveConnected &&
                                    !state.isBusy,
                        )
                    },
                )
            }
        }

        item {
            Text(
                text =
                    stringResource(
                        R.string
                            .backup_now_title,
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
            )
        }

        item {
            Button(
                onClick =
                    viewModel::
                        createDriveBackup,
                enabled =
                    !state.isBusy &&
                        state.driveConnected,
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector =
                        Icons.Outlined
                            .CloudUpload,
                    contentDescription = null,
                )
                Text(
                    text =
                        stringResource(
                            R.string
                                .backup_to_google_drive,
                        ),
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                        ),
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    createLauncher.launch(
                        viewModel
                            .backupFileName(),
                    )
                },
                enabled = !state.isBusy,
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector =
                        Icons.Outlined
                            .SaveAlt,
                    contentDescription = null,
                )
                Text(
                    text =
                        stringResource(
                            R.string
                                .backup_save_to_device,
                        ),
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                        ),
                )
            }
        }

        if (!state.driveConnected) {
            item {
                Text(
                    text =
                        stringResource(
                            R.string
                                .backup_drive_connect_hint,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                text =
                    stringResource(
                        R.string
                            .restore_title_short,
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                modifier =
                    Modifier.padding(
                        top = 4.dp,
                    ),
            )
        }

        item {
            OutlinedButton(
                onClick =
                    viewModel::
                        inspectDriveRestore,
                enabled =
                    !state.isBusy &&
                        state.driveConnected,
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector =
                        Icons.Outlined
                            .CloudDownload,
                    contentDescription = null,
                )
                Text(
                    text =
                        stringResource(
                            R.string
                                .restore_from_google_drive,
                        ),
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                        ),
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    openLauncher.launch(
                        arrayOf(
                            "application/json",
                            "application/octet-stream",
                            "text/plain",
                        ),
                    )
                },
                enabled = !state.isBusy,
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector =
                        Icons.Outlined
                            .FolderOpen,
                    contentDescription = null,
                )
                Text(
                    text =
                        stringResource(
                            R.string
                                .restore_from_file,
                        ),
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                        ),
                )
            }
        }

        if (state.isBusy) {
            item {
                androidx.compose
                    .foundation
                    .layout
                    .Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    state.pendingRestorePreview
        ?.let {
            RestoreConfirmationDialog(
                preview = it,
                locale = locale,
                onConfirm =
                    viewModel::
                        confirmRestore,
                onDismiss =
                    viewModel::
                        cancelRestore,
            )
        }

    state.message?.let { message ->
        BackupResultDialog(
            message = message,
            locale = locale,
            onDismiss =
                viewModel::clearMessage,
        )
    }
}

@Composable
private fun backupHistoryText(
    time: String,
    destination: BackupDestination?,
): String {
    if (destination == null) {
        return time
    }

    val source =
        stringResource(
            when (destination) {
                BackupDestination
                    .GOOGLE_DRIVE ->
                    R.string
                        .backup_destination_drive
                BackupDestination
                    .DEVICE_FILE ->
                    R.string
                        .backup_destination_file
            },
        )

    return stringResource(
        R.string.backup_history_value,
        time,
        source,
    )
}

@Composable
private fun RestoreConfirmationDialog(
    preview: BackupPreview,
    locale: java.util.Locale,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string
                        .restore_confirm_title,
                ),
            )
        },
        text = {
            Text(
                stringResource(
                    R.string
                        .homika_restore_confirm_body_v2,
                    formatBackupTime(
                        preview
                            .createdAtEpochMillis,
                        locale,
                    ).orEmpty(),
                    preview.propertyCount,
                    preview.bookingCount,
                    preview.expenseCount,
                    preview.depositCount,
                    preview.blockedDateCount,
                    preview.propertyCount +
                        preview.bookingCount +
                        preview.expenseCount +
                        preview.depositCount +
                        preview.blockedDateCount,
                ),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    stringResource(
                        R.string.restore_now,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        R.string.cancel,
                    ),
                )
            }
        },
    )
}

@Composable
private fun BackupResultDialog(
    message: BackupUiMessage,
    locale: java.util.Locale,
    onDismiss: () -> Unit,
) {
    val title: String
    val body: String

    when (message) {
        is BackupUiMessage.BackupCreated -> {
            title =
                stringResource(
                    R.string
                        .backup_success_title,
                )
            body =
                stringResource(
                    if (
                        message.destination ==
                            BackupDestination
                                .GOOGLE_DRIVE
                    ) {
                        R.string
                            .backup_drive_success_body
                    } else {
                        R.string
                            .backup_file_success_body
                    },
                    message.preview
                        .totalRecordCount,
                )
        }

        is BackupUiMessage.RestoreCompleted -> {
            title =
                stringResource(
                    R.string
                        .restore_success_title,
                )
            body =
                stringResource(
                    R.string
                        .restore_success_body,
                    message.preview
                        .totalRecordCount,
                    formatBackupTime(
                        message.preview
                            .createdAtEpochMillis,
                        locale,
                    ).orEmpty(),
                )
        }

        is BackupUiMessage.Failure -> {
            title =
                stringResource(
                    R.string
                        .backup_error_title,
                )
            body =
                stringResource(
                    message.reason
                        .messageRes(),
                )
        }

        is BackupUiMessage.DriveFailure -> {
            title =
                stringResource(
                    R.string
                        .backup_drive_error_title,
                )
            body =
                stringResource(
                    driveFailureMessage(
                        message.reason,
                    ),
                )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(
                        R.string.ok,
                    ),
                )
            }
        },
    )
}

private fun driveFailureMessage(
    reason: DriveBackupFailureReason,
): Int =
    when (reason) {
        DriveBackupFailureReason
            .NOT_CONNECTED ->
            R.string
                .backup_drive_error_not_connected
        DriveBackupFailureReason
            .AUTHORIZATION_FAILED ->
            R.string
                .backup_drive_error_auth
        DriveBackupFailureReason
            .AUTHORIZATION_CANCELLED ->
            R.string
                .backup_drive_error_cancelled
        DriveBackupFailureReason
            .NETWORK_UNAVAILABLE ->
            R.string
                .backup_drive_error_network
        DriveBackupFailureReason
            .DRIVE_ACCESS_FAILED ->
            R.string
                .backup_drive_error_access
        DriveBackupFailureReason
            .BACKUP_NOT_FOUND ->
            R.string
                .backup_drive_error_not_found
        DriveBackupFailureReason
            .INVALID_BACKUP ->
            R.string
                .backup_error_invalid
        DriveBackupFailureReason
            .UNSUPPORTED_FORMAT ->
            R.string
                .backup_error_format
        DriveBackupFailureReason
            .UNSUPPORTED_DATABASE_VERSION ->
            R.string
                .backup_error_database_version
        DriveBackupFailureReason
            .RESTORE_FAILED ->
            R.string
                .backup_error_restore
    }
