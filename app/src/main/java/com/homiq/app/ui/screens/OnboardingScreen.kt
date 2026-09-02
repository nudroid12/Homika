package com.homiq.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.ui.util.messageRes
import com.homiq.app.ui.viewmodel.AppLockViewModel
import com.homiq.app.ui.viewmodel.SyncUiMessage
import com.homiq.app.ui.viewmodel.SyncViewModel

@Composable
fun OnboardingScreen(
    syncViewModel: SyncViewModel,
    appLockViewModel: AppLockViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val syncState by syncViewModel.state.collectAsStateWithLifecycle()

    var setupPin by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf(false) }
    var waitingForGoogle by rememberSaveable { mutableStateOf(false) }
    var syncErrorRes by rememberSaveable { mutableStateOf<Int?>(null) }

    fun updatePinSetup(enabled: Boolean) {
        setupPin = enabled
        pinError = false
        if (!enabled) {
            pin = ""
            confirmPin = ""
        }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        syncViewModel.completeAuthorization(result.data)
    }

    val pendingResolution = syncState.pendingResolution
    if (pendingResolution != null) {
        LaunchedEffect(pendingResolution) {
            syncViewModel.resolutionLaunched()
            authorizationLauncher.launch(
                IntentSenderRequest.Builder(
                    pendingResolution.intentSender,
                ).build(),
            )
        }
    }

    fun pinInputIsValid(): Boolean =
        !setupPin || (pin.length in 4..8 && pin == confirmPin)

    fun persistPinIfRequested(): Boolean {
        if (!pinInputIsValid()) {
            pinError = true
            return false
        }

        return if (setupPin) {
            appLockViewModel.setPin(pin).also { saved ->
                pinError = !saved
            }
        } else {
            true
        }
    }

    val syncMessage = syncState.message
    LaunchedEffect(syncMessage, waitingForGoogle) {
        when (syncMessage) {
            SyncUiMessage.SyncCompleted -> {
                if (waitingForGoogle) {
                    waitingForGoogle = false
                    if (persistPinIfRequested()) {
                        syncViewModel.clearMessage()
                        onFinished()
                    }
                } else {
                    syncViewModel.clearMessage()
                }
            }

            is SyncUiMessage.Failure -> {
                syncErrorRes = syncMessage.reason.messageRes()
                waitingForGoogle = false
                syncViewModel.clearMessage()
            }

            SyncUiMessage.Disconnected -> {
                syncViewModel.clearMessage()
            }

            null -> Unit
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            LanguageToggle(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 82.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.homika_login_logo),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(34.dp)),
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.onboarding_login_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(R.string.onboarding_login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    Spacer(Modifier.height(30.dp))

                    Button(
                        onClick = {
                            if (!pinInputIsValid()) {
                                pinError = true
                                return@Button
                            }

                            pinError = false
                            syncErrorRes = null
                            waitingForGoogle = true
                            syncViewModel.clearMessage()
                            syncViewModel.connect()
                        },
                        enabled = !waitingForGoogle && !syncState.runtime.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        if (waitingForGoogle || syncState.runtime.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.size(9.dp))
                            Text(stringResource(R.string.onboarding_google_progress))
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(9.dp))
                            Text(stringResource(R.string.onboarding_google_title))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            if (persistPinIfRequested()) {
                                onFinished()
                            }
                        },
                        enabled = !waitingForGoogle && !syncState.runtime.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(stringResource(R.string.onboarding_continue_without_account))
                    }

                    val currentSyncErrorRes = syncErrorRes
                    if (currentSyncErrorRes != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = stringResource(currentSyncErrorRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        updatePinSetup(!setupPin)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.onboarding_setup_pin_checkbox),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = stringResource(R.string.onboarding_setup_pin_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }

                                Switch(
                                    checked = setupPin,
                                    onCheckedChange = ::updatePinSetup,
                                )
                            }

                            if (setupPin) {
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = {
                                        pin = it.filter(Char::isDigit).take(8)
                                        pinError = false
                                    },
                                    label = { Text(stringResource(R.string.pin)) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                    ),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = pinError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                )

                                OutlinedTextField(
                                    value = confirmPin,
                                    onValueChange = {
                                        confirmPin = it.filter(Char::isDigit).take(8)
                                        pinError = false
                                    },
                                    label = { Text(stringResource(R.string.confirm_pin)) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                    ),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    isError = pinError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                )

                                if (pinError) {
                                    Text(
                                        text = stringResource(R.string.onboarding_pin_error_compact),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.onboarding_local_first_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            top = 20.dp,
                            start = 10.dp,
                            end = 10.dp,
                            bottom = 6.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageToggle(
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val explicit = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val language = if (explicit.isBlank()) {
        configuration.locales[0].language
    } else {
        explicit.substringBefore(",").substringBefore("-")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            LanguageToken(
                text = "EN",
                selected = language != "ms",
                onClick = { setLanguage("en") },
            )

            Text(
                text = "|",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall,
            )

            LanguageToken(
                text = "MY",
                selected = language == "ms",
                onClick = { setLanguage("ms") },
            )
        }
    }
}

@Composable
private fun LanguageToken(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

private fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
