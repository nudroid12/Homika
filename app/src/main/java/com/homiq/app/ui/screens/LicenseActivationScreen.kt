package com.homiq.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.homiq.app.R
import com.homiq.app.data.license.LicenseAccess
import com.homiq.app.data.license.LicenseCommercialLinks
import com.homiq.app.data.license.LicenseUiState
import com.homiq.app.ui.components.HomikaBrandMark
import com.homiq.app.ui.viewmodel.LicenseCheckoutUiState

private const val ENTRY_HOME = "home"
private const val ENTRY_TRIAL = "trial"
private const val ENTRY_PURCHASE = "purchase"
private const val ENTRY_LICENCE_KEY = "licence_key"

@Composable
fun LicenseActivationScreen(
    state: LicenseUiState,
    onActivate: (String) -> Unit,
    onActivatePurchase: (String, String) -> Unit,
    onClaimTrial: (String) -> Unit,
    onRetry: () -> Unit,
    checkoutState: LicenseCheckoutUiState,
    onOpenRenewal: () -> Unit,
    onCheckoutConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var licenseKey by rememberSaveable { mutableStateOf(state.licenseKey) }
    var purchaseEmail by rememberSaveable { mutableStateOf("") }
    var purchasePin by rememberSaveable { mutableStateOf("") }
    var trialEmail by rememberSaveable { mutableStateOf("") }
    var entryPage by rememberSaveable { mutableStateOf(ENTRY_HOME) }
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
                HomikaActivationHeader(
                    compact = state.access == LicenseAccess.ACTIVATION_REQUIRED ||
                        state.access == LicenseAccess.INVALID,
                )

                if (state.access == LicenseAccess.CHECKING) {
                    CheckingCard()
                    return@Column
                }

                when {
                    state.access == LicenseAccess.EXPIRED -> {
                        ExpiredAccessContent(
                            state = state,
                            checkoutState = checkoutState,
                            onOpenRenewal = onOpenRenewal,
                            onRetry = onRetry,
                        )
                        LicenceKeyForm(
                            licenseKey = licenseKey,
                            busy = state.busy,
                            onLicenseKeyChange = { licenseKey = sanitizeLicenceKey(it) },
                            onActivate = { onActivate(licenseKey) },
                        )
                    }

                    state.access == LicenseAccess.ACTIVATION_REQUIRED ||
                        state.access == LicenseAccess.INVALID -> {
                        when (entryPage) {
                            ENTRY_TRIAL -> TrialActivationPage(
                                state = state,
                                email = trialEmail,
                                onEmailChange = { trialEmail = it.take(254) },
                                onClaim = { onClaimTrial(trialEmail) },
                                onBack = { entryPage = ENTRY_HOME },
                            )

                            ENTRY_PURCHASE -> PurchaseActivationPage(
                                state = state,
                                email = purchaseEmail,
                                pin = purchasePin,
                                onEmailChange = { purchaseEmail = it.take(254) },
                                onPinChange = { value ->
                                    purchasePin = value.filter(Char::isDigit).take(6)
                                },
                                onActivate = { onActivatePurchase(purchaseEmail, purchasePin) },
                                onUseLicenceKey = { entryPage = ENTRY_LICENCE_KEY },
                                onBack = { entryPage = ENTRY_HOME },
                            )

                            ENTRY_LICENCE_KEY -> LicenceKeyActivationPage(
                                state = state,
                                licenseKey = licenseKey,
                                onLicenseKeyChange = { licenseKey = sanitizeLicenceKey(it) },
                                onActivate = { onActivate(licenseKey) },
                                onBack = { entryPage = ENTRY_PURCHASE },
                            )

                            else -> ActivationEntryLanding(
                                onStartTrial = { entryPage = ENTRY_TRIAL },
                                onAlreadyPurchased = { entryPage = ENTRY_PURCHASE },
                                onBuy = {
                                    runCatching {
                                        uriHandler.openUri(LicenseCommercialLinks.BUY_URL)
                                    }
                                },
                            )
                        }
                    }

                    else -> {
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

                        LicenceKeyForm(
                            licenseKey = licenseKey,
                            busy = state.busy,
                            onLicenseKeyChange = { licenseKey = sanitizeLicenceKey(it) },
                            onActivate = { onActivate(licenseKey) },
                        )

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
    }
}

@Composable
private fun HomikaActivationHeader(compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HomikaBrandMark()
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = stringResource(R.string.license_homika_pro),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.license_commercial_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckingCard() {
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
}

@Composable
private fun ActivationEntryLanding(
    onStartTrial: () -> Unit,
    onAlreadyPurchased: () -> Unit,
    onBuy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.license_activate_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.license_entry_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(4.dp))

        Button(
            onClick = onStartTrial,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.license_trial_start))
        }

        OutlinedButton(
            onClick = onAlreadyPurchased,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.license_entry_already_purchased))
        }

        TextButton(
            onClick = onBuy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.license_entry_buy))
        }
    }
}

@Composable
private fun TrialActivationPage(
    state: LicenseUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    onClaim: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailPageTitle(
            title = stringResource(R.string.license_trial_offer_title),
            body = stringResource(R.string.license_trial_offer_body),
        )
        LicenseStatusCard(state)

        NeutralFormSurface {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.license_trial_email)) },
                placeholder = { Text("nama@email.com") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
            )
            Button(
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && !state.busy,
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.license_trial_start))
            }
            Text(
                text = stringResource(R.string.license_trial_once_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BackToStartButton(onBack)
    }
}

@Composable
private fun PurchaseActivationPage(
    state: LicenseUiState,
    email: String,
    pin: String,
    onEmailChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onActivate: () -> Unit,
    onUseLicenceKey: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailPageTitle(
            title = stringResource(R.string.license_purchase_page_title),
            body = stringResource(R.string.license_purchase_page_body),
        )
        LicenseStatusCard(state)

        NeutralFormSurface {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.license_purchase_email)) },
                placeholder = { Text("nama@email.com") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.license_purchase_pin)) },
                placeholder = { Text("••••••") },
                singleLine = true,
                enabled = !state.busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
            )
            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && pin.length == 6 && !state.busy,
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.license_purchase_activate))
            }
            Text(
                text = stringResource(R.string.license_purchase_pin_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(
            onClick = onUseLicenceKey,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !state.busy,
        ) {
            Text(stringResource(R.string.license_use_licence_key))
        }

        BackToStartButton(onBack)
    }
}

@Composable
private fun LicenceKeyActivationPage(
    state: LicenseUiState,
    licenseKey: String,
    onLicenseKeyChange: (String) -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailPageTitle(
            title = stringResource(R.string.license_key_backup_title),
            body = stringResource(R.string.license_key_backup_body),
        )
        LicenseStatusCard(state)
        LicenceKeyForm(
            licenseKey = licenseKey,
            busy = state.busy,
            onLicenseKeyChange = onLicenseKeyChange,
            onActivate = onActivate,
            showIntro = false,
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !state.busy,
        ) {
            Text(stringResource(R.string.license_back_to_purchase))
        }
    }
}

@Composable
private fun DetailPageTitle(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NeutralFormSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun BackToStartButton(onBack: () -> Unit) {
    TextButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.license_back_to_start))
    }
}

@Composable
private fun LicenceKeyForm(
    licenseKey: String,
    busy: Boolean,
    onLicenseKeyChange: (String) -> Unit,
    onActivate: () -> Unit,
    showIntro: Boolean = true,
) {
    NeutralFormSurface {
        if (showIntro) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.license_key_backup_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.license_key_backup_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = licenseKey,
            onValueChange = onLicenseKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.license_code)) },
            placeholder = { Text("HMK-XXXX-XXXX-XXXX") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Button(
            onClick = onActivate,
            modifier = Modifier.fillMaxWidth(),
            enabled = licenseKey.isNotBlank() && !busy,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.license_activate_button))
            }
        }
    }
}

@Composable
private fun ExpiredAccessContent(
    state: LicenseUiState,
    checkoutState: LicenseCheckoutUiState,
    onOpenRenewal: () -> Unit,
    onRetry: () -> Unit,
) {
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
}

private fun sanitizeLicenceKey(value: String): String =
    value
        .uppercase()
        .filter { it.isLetterOrDigit() || it == '-' || it == ' ' }
        .take(80)

@Composable
private fun checkoutErrorText(code: String): String =
    when (code) {
        "checkout_network" -> stringResource(R.string.license_store_error_network)
        "license_required", "invalid_activation_token", "token_device_mismatch" ->
            stringResource(R.string.license_store_error_verify)
        else -> stringResource(R.string.license_store_error_generic)
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
        "purchase_invalid_email" -> R.string.license_trial_error_email_title to R.string.license_purchase_invalid_email_body
        "purchase_pin_required" -> R.string.license_purchase_invalid_title to R.string.license_purchase_pin_required_body
        "purchase_pending" -> R.string.license_purchase_pending_title to R.string.license_purchase_pending_body
        "purchase_rejected" -> R.string.license_purchase_rejected_title to R.string.license_purchase_rejected_body
        "purchase_not_submitted" -> R.string.license_purchase_not_submitted_title to R.string.license_purchase_not_submitted_body
        "purchase_credentials_invalid", "purchase_pin_invalid" -> R.string.license_purchase_invalid_title to R.string.license_purchase_invalid_body
        "purchase_pin_locked" -> R.string.license_purchase_locked_title to R.string.license_purchase_locked_body
        "purchase_pin_not_configured", "purchase_account_not_ready", "purchase_not_approved" -> R.string.license_purchase_unavailable_title to R.string.license_purchase_unavailable_body
        "purchase_network" -> R.string.license_purchase_network_title to R.string.license_purchase_network_body
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
                text = when {
                    state.errorCode == "purchase_rejected" && !state.errorDetail.isNullOrBlank() ->
                        stringResource(R.string.license_purchase_rejected_body_with_reason, state.errorDetail)
                    state.access == LicenseAccess.DEVICE_LIMIT ->
                        stringResource(content.second, state.activeDevices, state.maxDevices)
                    else -> stringResource(content.second)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
