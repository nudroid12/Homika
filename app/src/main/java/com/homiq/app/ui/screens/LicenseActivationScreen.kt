package com.homiq.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.license.LicenseUiState
import com.homiq.app.ui.components.HomikaBrandMark

@Composable
fun LicenseActivationScreen(
    state: LicenseUiState,
    onActivate: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var licenseKey by rememberSaveable { mutableStateOf(state.licenseKey) }

    LaunchedEffect(state.licenseKey) {
        if (state.licenseKey.isNotBlank() && licenseKey.isBlank()) {
            licenseKey = state.licenseKey
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomikaBrandMark()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.license_homika_pro),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.license_annual_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.access == LicenseAccess.CHECKING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.license_checking),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.license_checking_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {

            Text(
                text = stringResource(R.string.license_activate_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.license_activate_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LicenseStatusCard(state)

            OutlinedTextField(
                value = licenseKey,
                onValueChange = { value ->
                    licenseKey = value
                        .uppercase()
                        .filter { it.isLetterOrDigit() || it == '-' || it == ' ' }
                        .take(80)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.license_code)) },
                placeholder = { Text("HMK-XXXX-XXXX-XXXX") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Button(
                onClick = { onActivate(licenseKey) },
                modifier = Modifier.fillMaxWidth(),
                enabled = licenseKey.isNotBlank() && !state.busy,
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.license_activate_button))
                }
            }

            if (state.access == LicenseAccess.NEEDS_INTERNET) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !state.busy,
                ) {
                    Text(stringResource(R.string.license_retry))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.license_device_allowance,
                            state.maxDevices,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.license_offline_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun LicenseStatusCard(state: LicenseUiState) {
    val content = when (state.access) {
        LicenseAccess.ACTIVATION_REQUIRED -> null
        LicenseAccess.NEEDS_INTERNET ->
            R.string.license_error_network to R.string.license_error_network_body
        LicenseAccess.EXPIRED ->
            R.string.license_error_expired to R.string.license_error_expired_body
        LicenseAccess.INACTIVE ->
            R.string.license_error_inactive to R.string.license_error_inactive_body
        LicenseAccess.DEVICE_LIMIT ->
            R.string.license_error_device_limit to R.string.license_error_device_limit_body
        LicenseAccess.INVALID ->
            R.string.license_error_invalid to R.string.license_error_invalid_body
        LicenseAccess.CHECKING,
        LicenseAccess.ACTIVE,
        -> null
    } ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(content.first),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = if (state.access == LicenseAccess.DEVICE_LIMIT) {
                    stringResource(
                        content.second,
                        state.activeDevices,
                        state.maxDevices,
                    )
                } else {
                    stringResource(content.second)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
