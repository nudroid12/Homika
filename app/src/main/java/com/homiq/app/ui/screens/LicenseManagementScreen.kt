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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.license.LicenseDeviceInfo
import com.homiq.app.data.license.LicensePlanType
import com.homiq.app.data.license.LicenseUiState
import com.homiq.app.ui.viewmodel.LicenseCheckoutUiState
import com.homiq.app.ui.viewmodel.LicenseDeviceUiState
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

private const val VERIFY_FEEDBACK_SUCCESS = "success"
private const val VERIFY_FEEDBACK_OFFLINE = "offline"
private const val VERIFY_FEEDBACK_FAILED = "failed"

@Composable
fun LicenseManagementScreen(
    state: LicenseUiState,
    deviceState: LicenseDeviceUiState,
    onRefresh: () -> Unit,
    onRefreshDevices: () -> Unit,
    onDeactivateOtherDevice: (String) -> Unit,
    onDeactivate: () -> Unit,
    checkoutState: LicenseCheckoutUiState,
    onOpenRenewal: () -> Unit,
    onCheckoutConsumed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDeactivate by rememberSaveable { mutableStateOf(false) }
    var pendingRemoveDevice by remember { mutableStateOf<LicenseDeviceInfo?>(null) }
    var verificationRequested by rememberSaveable { mutableStateOf(false) }
    var verificationFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val daysRemaining = licenseDaysRemaining(state)

    LaunchedEffect(Unit) {
        onRefreshDevices()
    }

    LaunchedEffect(checkoutState.checkoutUrl) {
        checkoutState.checkoutUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }
            onCheckoutConsumed()
        }
    }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            shape = MaterialTheme.shapes.large,
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        ) {
            Column {
                LicenseInfoRow(
                    icon = Icons.Outlined.Key,
                    label = stringResource(R.string.license_code),
                    value = state.licenseHint.ifBlank { "••••" },
                )
                LicenseDivider()
                LicenseInfoRow(
                    icon = Icons.Outlined.VerifiedUser,
                    label = stringResource(R.string.license_plan),
                    value = licensePlanLabel(state.planType, state.planKey),
                )
                LicenseDivider()
                LicenseInfoRow(
                    icon = Icons.Outlined.Event,
                    label = stringResource(R.string.license_expires),
                    value = if (state.planType == LicensePlanType.LIFETIME) {
                        stringResource(R.string.license_plan_lifetime)
                    } else {
                        state.expiresAt
                            ?.let { formatServerDateTime(context, it) }
                            ?: stringResource(R.string.license_unknown)
                    },
                )
                if (state.planType != LicensePlanType.LIFETIME) {
                    LicenseDivider()
                    LicenseInfoRow(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.license_time_remaining),
                        value = remainingDaysText(daysRemaining),
                    )
                }
                LicenseDivider()
                LicenseInfoRow(
                    icon = Icons.Outlined.Devices,
                    label = stringResource(R.string.license_devices),
                    value = stringResource(
                        R.string.license_devices_value,
                        state.activeDevices,
                        state.maxDevices,
                    ),
                )
                LicenseDivider()
                LicenseInfoRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.license_last_verified),
                    value = lastVerifiedText(state.lastValidatedAtMillis),
                )
            }
        }

        if (state.planType != LicensePlanType.LIFETIME) {
            RenewalCard(
                planType = state.planType,
                daysRemaining = daysRemaining,
                openingCheckout = checkoutState.loading,
                checkoutErrorCode = checkoutState.errorCode,
                onRenew = onOpenRenewal,
            )
        }

        DeviceManagementCard(
            state = deviceState,
            onRefresh = onRefreshDevices,
            onRemove = { pendingRemoveDevice = it },
        )

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
                Text(
                    text = when (feedback) {
                        VERIFY_FEEDBACK_SUCCESS -> stringResource(R.string.license_verify_success)
                        VERIFY_FEEDBACK_OFFLINE -> stringResource(R.string.license_verify_offline)
                        else -> stringResource(R.string.license_verify_failed)
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (success) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                )
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
            title = { Text(stringResource(R.string.license_deactivate_confirm_title)) },
            text = { Text(stringResource(R.string.license_deactivate_confirm_body)) },
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

    pendingRemoveDevice?.let { device ->
        val removeDeviceName = device.deviceName.ifBlank {
            stringResource(R.string.license_unknown_device)
        }
        AlertDialog(
            onDismissRequest = { pendingRemoveDevice = null },
            title = { Text(stringResource(R.string.license_remove_device_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.license_remove_device_body,
                        removeDeviceName,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoveDevice = null
                        onDeactivateOtherDevice(device.deviceHash)
                    },
                ) {
                    Text(stringResource(R.string.license_remove_device_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveDevice = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RenewalCard(
    planType: LicensePlanType,
    daysRemaining: Long?,
    openingCheckout: Boolean,
    checkoutErrorCode: String?,
    onRenew: () -> Unit,
) {
    val urgent = daysRemaining != null && daysRemaining <= 30L
    val trial = planType == LicensePlanType.TRIAL
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (urgent || trial) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when {
                    trial -> stringResource(R.string.license_trial_status_title)
                    urgent -> stringResource(R.string.license_renewal_due_title)
                    else -> stringResource(R.string.license_renewal_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    trial -> stringResource(R.string.license_trial_status_body)
                    urgent -> stringResource(R.string.license_renewal_due_body)
                    else -> stringResource(R.string.license_renewal_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onRenew,
                modifier = Modifier.fillMaxWidth(),
                enabled = !openingCheckout,
            ) {
                if (openingCheckout) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.license_store_opening))
                } else {
                    Text(
                        if (trial) {
                            stringResource(R.string.license_upgrade_online)
                        } else {
                            stringResource(R.string.license_renew_online)
                        },
                    )
                }
            }
            checkoutErrorCode?.let {
                Text(
                    text = checkoutErrorText(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun remainingDaysText(daysRemaining: Long?): String =
    when {
        daysRemaining == null -> stringResource(R.string.license_unknown)
        daysRemaining <= 0L -> stringResource(R.string.license_time_remaining_expired)
        daysRemaining == 1L -> stringResource(R.string.license_time_remaining_one_day)
        else -> stringResource(R.string.license_time_remaining_days, daysRemaining)
    }

private fun licenseDaysRemaining(state: LicenseUiState): Long? {
    if (state.planType == LicensePlanType.LIFETIME || state.expiresAtEpochMillis <= 0L) return null
    val remainingMillis = state.expiresAtEpochMillis - System.currentTimeMillis()
    if (remainingMillis <= 0L) return 0L
    val dayMillis = 24L * 60L * 60L * 1000L
    return (remainingMillis + dayMillis - 1L) / dayMillis
}

@Composable
private fun DeviceManagementCard(
    state: LicenseDeviceUiState,
    onRefresh: () -> Unit,
    onRemove: (LicenseDeviceInfo) -> Unit,
) {
    val availableSlots = (state.maxDevices - state.activeDevices).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.license_device_management_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.license_device_slots_summary,
                            state.activeDevices,
                            state.maxDevices,
                            availableSlots,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !state.loading && state.busyDeviceHash == null,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.license_refresh_devices),
                    )
                }
            }

            if (state.loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.license_loading_devices),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (state.devices.isEmpty() && state.errorCode == null) {
                Text(
                    text = stringResource(R.string.license_no_devices),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.devices.forEachIndexed { index, device ->
                    if (index > 0) HorizontalDivider()
                    DeviceRow(
                        device = device,
                        busy = state.busyDeviceHash == device.deviceHash,
                        onRemove = { onRemove(device) },
                    )
                }
            }

            state.feedbackCode?.let {
                Text(
                    text = stringResource(R.string.license_device_removed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            state.errorCode?.let { code ->
                Text(
                    text = deviceErrorText(code),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = stringResource(R.string.license_device_management_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: LicenseDeviceInfo,
    busy: Boolean,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Devices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.deviceName.ifBlank { stringResource(R.string.license_unknown_device) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (device.isCurrentDevice) {
                    stringResource(R.string.license_current_device)
                } else {
                    stringResource(
                        R.string.license_device_last_seen,
                        formatServerDateTime(context, device.lastSeenAt),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (device.isCurrentDevice) {
            Text(
                text = stringResource(R.string.license_this_device),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            TextButton(onClick = onRemove, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.license_remove_device))
                }
            }
        }
    }
}

@Composable
private fun deviceErrorText(code: String): String =
    when (code) {
        "devices_network" -> stringResource(R.string.license_devices_error_network)
        "license_required", "invalid_activation_token", "token_device_mismatch" ->
            stringResource(R.string.license_devices_error_verify)
        "device_not_activated" -> stringResource(R.string.license_devices_error_missing)
        "current_device_use_self_deactivate" -> stringResource(R.string.license_devices_error_current)
        else -> stringResource(R.string.license_devices_error_generic)
    }

@Composable
private fun checkoutErrorText(code: String): String =
    when (code) {
        "checkout_network" -> stringResource(R.string.license_store_error_network)
        "license_required", "invalid_activation_token", "token_device_mismatch" ->
            stringResource(R.string.license_store_error_verify)
        else -> stringResource(R.string.license_store_error_generic)
    }

@Composable
private fun licensePlanLabel(
    planType: LicensePlanType,
    planKey: String,
): String =
    when (planKey.trim().lowercase()) {
        "trial_7d" -> stringResource(R.string.license_plan_trial)
        "1_month" -> stringResource(R.string.license_plan_1_month)
        "3_month" -> stringResource(R.string.license_plan_3_months)
        "6_month" -> stringResource(R.string.license_plan_6_months)
        "1_year" -> stringResource(R.string.license_plan_1_year)
        "lifetime" -> stringResource(R.string.license_plan_lifetime)
        else -> when (planType) {
            LicensePlanType.TRIAL -> stringResource(R.string.license_plan_trial)
            LicensePlanType.MONTHLY -> stringResource(R.string.license_plan_monthly)
            LicensePlanType.ANNUAL -> stringResource(R.string.license_plan_annual)
            LicensePlanType.LIFETIME -> stringResource(R.string.license_plan_lifetime)
        }
    }

@Composable
private fun lastVerifiedText(lastValidatedAtMillis: Long): String {
    if (lastValidatedAtMillis <= 0L) return stringResource(R.string.license_never_verified)
    return remember(lastValidatedAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(lastValidatedAtMillis))
    }
}

@Composable
private fun LicenseInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
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

@Composable
private fun LicenseDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
}

private fun formatServerDateTime(
    context: android.content.Context,
    raw: String,
): String {
    val epochMillis = runCatching {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    }.getOrNull() ?: runCatching {
        LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }.getOrNull()

    if (epochMillis == null) return raw.ifBlank { "—" }
    val date = Date(epochMillis)
    val dateText = android.text.format.DateFormat.getMediumDateFormat(context).format(date)
    val timeText = android.text.format.DateFormat.getTimeFormat(context).format(date)
    return "$dateText $timeText"
}
