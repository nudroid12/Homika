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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.license.LicenseCommercialLinks
import com.homiq.app.data.license.LicenseUiState
import com.homiq.app.ui.components.HomikaBrandMark
import com.homiq.app.ui.viewmodel.LicenseCheckoutUiState
import com.homiq.app.ui.theme.HomikaInk
import com.homiq.app.ui.theme.HomikaMintSoft
import com.homiq.app.ui.theme.HomikaOutline
import com.homiq.app.ui.theme.HomikaSurface
import com.homiq.app.ui.theme.HomikaTeal
import com.homiq.app.ui.theme.HomikaTealDeep

@Composable
fun LicenseActivationScreen(
    state: LicenseUiState,
    onActivate: (String) -> Unit,
    onClaimTrial: (String) -> Unit,
    onRetry: () -> Unit,
    checkoutState: LicenseCheckoutUiState,
    onOpenRenewal: () -> Unit,
    onCheckoutConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var licenseKey by rememberSaveable { mutableStateOf(state.licenseKey) }
    var trialEmail by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.licenseKey) {
        if (state.licenseKey.isNotBlank() && licenseKey.isBlank()) {
            licenseKey = state.licenseKey
        }
    }

    LaunchedEffect(checkoutState.checkoutUrl) {
        checkoutState.checkoutUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }
            onCheckoutConsumed()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomikaBrandMark()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.license_homika_pro),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.license_commercial_subtitle),
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
                return@Column
            }

            Text(
                text = activationHeadline(state.access),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = activationBody(state.access),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LicenseStatusCard(state)

            if (state.access == LicenseAccess.EXPIRED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.license_renew_same_code_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.license_renew_same_code_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        if (state.licenseHint.isNotBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.license_current_code_hint,
                                    state.licenseHint,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenRenewal,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy && !checkoutState.loading,
                ) {
                    if (checkoutState.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.license_store_opening))
                    } else {
                        Text(stringResource(R.string.license_renew_online))
                    }
                }

                checkoutState.errorCode?.let {
                    Text(
                        text = checkoutErrorText(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.license_renew_verify_button))
                }
            } else if (
                state.access == LicenseAccess.ACTIVATION_REQUIRED ||
                state.access == LicenseAccess.INVALID
            ) {
                TrialOfferCard(
                    email = trialEmail,
                    busy = state.busy,
                    onEmailChange = { trialEmail = it.take(254) },
                    onClaim = { onClaimTrial(trialEmail) },
                )

                AnnualOfferCard(state.maxDevices)
                OutlinedButton(
                    onClick = {
                        runCatching { uriHandler.openUri(LicenseCommercialLinks.BUY_URL) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.license_buy_online))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.license_existing_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.license_existing_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.access == LicenseAccess.DEVICE_LIMIT) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(R.string.license_device_limit_help),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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

            if (
                state.access != LicenseAccess.ACTIVATION_REQUIRED &&
                state.access != LicenseAccess.INVALID
            ) {
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
private fun TrialOfferCard(
    email: String,
    busy: Boolean,
    onEmailChange: (String) -> Unit,
    onClaim: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = HomikaTeal,
        contentColor = HomikaSurface,
        border = BorderStroke(1.dp, HomikaTealDeep),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.license_trial_offer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomikaSurface,
            )
            Text(
                text = stringResource(R.string.license_trial_offer_body),
                style = MaterialTheme.typography.bodySmall,
                color = HomikaSurface,
            )
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.license_trial_email)) },
                placeholder = { Text("nama@email.com") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = HomikaSurface,
                    unfocusedTextColor = HomikaSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Button(
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && !busy,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = HomikaMintSoft,
                    contentColor = HomikaTealDeep,
                    disabledContainerColor = HomikaMintSoft.copy(alpha = 0.45f),
                    disabledContentColor = HomikaTealDeep.copy(alpha = 0.60f),
                ),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = HomikaTealDeep,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.license_trial_start))
            }
            Text(
                text = stringResource(R.string.license_trial_once_note),
                style = MaterialTheme.typography.labelSmall,
                color = HomikaSurface,
            )
        }
    }
}

@Composable
private fun AnnualOfferCard(maxDevices: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = HomikaMintSoft,
        contentColor = HomikaInk,
        border = BorderStroke(1.dp, HomikaOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.license_annual_primary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomikaInk,
            )
            Text(
                text = stringResource(R.string.license_annual_primary_body, maxDevices),
                style = MaterialTheme.typography.bodySmall,
                color = HomikaInk,
            )
        }
    }
}

@Composable
private fun activationHeadline(access: LicenseAccess): String =
    when (access) {
        LicenseAccess.EXPIRED -> stringResource(R.string.license_renew_title)
        LicenseAccess.DEVICE_LIMIT -> stringResource(R.string.license_device_limit_title)
        LicenseAccess.NEEDS_INTERNET -> stringResource(R.string.license_verify_title)
        else -> stringResource(R.string.license_activate_title)
    }

@Composable
private fun activationBody(access: LicenseAccess): String =
    when (access) {
        LicenseAccess.EXPIRED -> stringResource(R.string.license_renew_body)
        LicenseAccess.DEVICE_LIMIT -> stringResource(R.string.license_device_limit_activation_body)
        LicenseAccess.NEEDS_INTERNET -> stringResource(R.string.license_verify_activation_body)
        else -> stringResource(R.string.license_activate_body)
    }

@Composable
private fun LicenseStatusCard(state: LicenseUiState) {
    val trialContent = when (state.errorCode) {
        "invalid_email" -> R.string.license_trial_error_email_title to R.string.license_trial_error_email_body
        "trial_already_used_device" -> R.string.license_trial_error_device_title to R.string.license_trial_error_device_body
        "trial_already_used_customer" -> R.string.license_trial_error_customer_title to R.string.license_trial_error_customer_body
        "trial_already_used" -> R.string.license_trial_error_used_title to R.string.license_trial_error_used_body
        "trial_unavailable" -> R.string.license_trial_error_unavailable_title to R.string.license_trial_error_unavailable_body
        "trial_setup_required" -> R.string.license_trial_error_setup_title to R.string.license_trial_error_setup_body
        "trial_endpoint_unavailable" ->
            R.string.license_trial_error_endpoint_title to R.string.license_trial_error_endpoint_body
        "trial_server_error", "internal_error", "server_unavailable" ->
            R.string.license_trial_error_server_title to R.string.license_trial_error_server_body
        "trial_network" -> R.string.license_trial_error_network_title to R.string.license_trial_error_network_body
        else -> null
    }
    val content = trialContent ?: when (state.access) {
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
                fontWeight = FontWeight.SemiBold,
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
