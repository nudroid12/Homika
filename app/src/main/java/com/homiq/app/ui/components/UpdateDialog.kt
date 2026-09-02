package com.homiq.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.update.UpdateFailureReason
import com.homiq.app.data.update.UpdateState

@Composable
fun HomikaUpdateDialog(
    state: UpdateState,
    currentVersion: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
) {
    when (state) {
        UpdateState.Idle -> Unit
        is UpdateState.Checking -> {
            if (state.manual) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(stringResource(R.string.updater_checking_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.updater_checking_body))
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }

        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.updater_available_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(
                                R.string.updater_available_body,
                                currentVersion,
                                state.release.versionName,
                            ),
                        )
                        if (state.release.notes.isNotBlank()) {
                            Text(
                                text = state.release.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDownload) {
                        Text(stringResource(R.string.updater_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.updater_later))
                    }
                },
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.updater_downloading_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(
                                    R.string.updater_progress,
                                    (state.progress * 100).toInt(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.updater_download_preparing),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {},
            )
        }

        is UpdateState.Ready -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.updater_ready_title)) },
                text = {
                    Text(stringResource(R.string.updater_ready_body, state.release.versionName))
                },
                confirmButton = {
                    Button(onClick = onInstall) {
                        Text(stringResource(R.string.updater_install))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.updater_later))
                    }
                },
            )
        }

        is UpdateState.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.updater_permission_title)) },
                text = { Text(stringResource(R.string.updater_permission_body)) },
                confirmButton = {
                    Button(onClick = onOpenInstallSettings) {
                        Text(stringResource(R.string.updater_open_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.updater_later))
                    }
                },
            )
        }

        is UpdateState.Installing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.updater_installing_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.updater_installing_body))
                    }
                },
                confirmButton = {},
            )
        }

        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.updater_current_title)) },
                text = {
                    Text(stringResource(R.string.updater_current_body, currentVersion))
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.updater_error_title)) },
                text = {
                    Text(stringResource(errorMessage(state.reason)))
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.ok))
                    }
                },
            )
        }
    }
}

private fun errorMessage(reason: UpdateFailureReason): Int {
    return when (reason) {
        UpdateFailureReason.NETWORK -> R.string.updater_error_network
        UpdateFailureReason.RELEASE_INVALID -> R.string.updater_error_release
        UpdateFailureReason.DOWNLOAD_FAILED -> R.string.updater_error_download
        UpdateFailureReason.APK_INVALID -> R.string.updater_error_apk
        UpdateFailureReason.SIGNATURE_MISMATCH -> R.string.updater_error_signature
        UpdateFailureReason.INSTALL_FAILED -> R.string.updater_error_install
    }
}
