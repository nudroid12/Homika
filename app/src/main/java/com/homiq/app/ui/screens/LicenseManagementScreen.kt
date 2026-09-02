package com.homiq.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.license.LicenseUiState
import java.text.DateFormat
import java.util.Date

private const val VERIFY_FEEDBACK_SUCCESS = "success"
private const val VERIFY_FEEDBACK_OFFLINE = "offline"
private const val VERIFY_FEEDBACK_FAILED = "failed"

@Composable
fun LicenseManagementScreen(
    state: LicenseUiState,
    onRefresh: () -> Unit,
    onDeactivate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDeactivate by rememberSaveable { mutableStateOf(false) }
    var verificationRequested by rememberSaveable { mutableStateOf(false) }
    var verificationFeedback by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(
        state.busy,
        state.lastValidatedAtMillis,
        state.usingOfflineGrace,
        state.errorCode,
    ) {
        if (verificationRequested && !state.busy) {
            verificationFeedback = when {
                state.usingOfflineGrace || state.errorCode == "offline_grace" ->
                    VERIFY_FEEDBACK_OFFLINE
                state.errorCode == null ->
                    VERIFY_FEEDBACK_SUCCESS
                else ->
                    VERIFY_FEEDBACK_FAILED
            }
            verificationRequested = false
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.license_back),
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = stringResource(R.string.license_manage_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.license_manage_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.license_active_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = if (state.usingOfflineGrace) {
                            stringResource(R.string.license_active_offline)
                        } else {
                            stringResource(R.string.license_active_verified)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
            ),
        ) {
            Column {
                LicenseInfoRow(
                    icon = Icons.Outlined.Key,
                    label = stringResource(R.string.license_code),
                    value = state.licenseHint.ifBlank { "••••" },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
                LicenseInfoRow(
                    icon = Icons.Outlined.Event,
                    label = stringResource(R.string.license_expires),
                    value = state.expiresAt ?: stringResource(R.string.license_unknown),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
                LicenseInfoRow(
                    icon = Icons.Outlined.Devices,
                    label = stringResource(R.string.license_devices),
                    value = stringResource(
                        R.string.license_devices_value,
                        state.activeDevices,
                        state.maxDevices,
                    ),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                )
                LicenseInfoRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.license_last_verified),
                    value = lastVerifiedText(state.lastValidatedAtMillis),
                )
            }
        }

        if (state.errorCode == "deactivate_network") {
            Text(
                text = stringResource(R.string.license_deactivate_network),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        verificationFeedback?.let { feedback ->
            val success = feedback == VERIFY_FEEDBACK_SUCCESS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (success) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (success) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = when (feedback) {
                            VERIFY_FEEDBACK_SUCCESS -> stringResource(R.string.license_verify_success)
                            VERIFY_FEEDBACK_OFFLINE -> stringResource(R.string.license_verify_offline)
                            else -> stringResource(R.string.license_verify_failed)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (success) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }
        }

        Button(
            onClick = {
                verificationFeedback = null
                verificationRequested = true
                onRefresh()
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.license_verifying_now))
            } else {
                Text(stringResource(R.string.license_verify_now))
            }
        }

        OutlinedButton(
            onClick = { confirmDeactivate = true },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.license_deactivate_device))
        }

        Text(
            text = stringResource(R.string.license_deactivate_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { confirmDeactivate = false },
            title = {
                Text(stringResource(R.string.license_deactivate_confirm_title))
            },
            text = {
                Text(stringResource(R.string.license_deactivate_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeactivate = false
                        onDeactivate()
                    },
                ) {
                    Text(stringResource(R.string.license_deactivate_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun lastVerifiedText(lastValidatedAtMillis: Long): String {
    if (lastValidatedAtMillis <= 0L) {
        return stringResource(R.string.license_never_verified)
    }

    return remember(lastValidatedAtMillis) {
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
        ).format(Date(lastValidatedAtMillis))
    }
}

@Composable
private fun LicenseInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
