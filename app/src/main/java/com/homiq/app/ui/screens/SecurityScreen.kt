package com.homiq.app.ui.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.data.security.AppLockService
import com.homiq.app.ui.components.ScreenHeader
import com.homiq.app.ui.security.canUseHomiqBiometrics
import com.homiq.app.ui.viewmodel.AppLockViewModel

private enum class PinDialogMode { SET, CHANGE, DISABLE }

@Composable
fun SecurityScreen(
    viewModel: AppLockViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? AppCompatActivity
    val biometricAvailable = remember(activity) {
        activity?.let(::canUseHomiqBiometrics) == true
    }
    var dialogMode by rememberSaveable { mutableStateOf<PinDialogMode?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 18.dp,
            end = 16.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.security),
                subtitle = stringResource(R.string.security_subtitle),
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
            ) {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.app_lock),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (state.hasPin) R.string.app_lock_on_body
                                    else R.string.homika_app_lock_off_body_v2,
                                ),
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Outlined.Password, contentDescription = null)
                        },
                        trailingContent = {
                            Text(
                                text = stringResource(
                                    if (state.hasPin) R.string.on else R.string.off,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )

                    HorizontalDivider()

                    if (state.hasPin) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.biometric_unlock),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (biometricAvailable) {
                                            R.string.biometric_unlock_body
                                        } else {
                                            R.string.biometric_unavailable
                                        },
                                    ),
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.biometricEnabled && biometricAvailable,
                                    onCheckedChange = viewModel::setBiometricEnabled,
                                    enabled = biometricAvailable,
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            if (!state.hasPin) {
                Button(
                    onClick = { dialogMode = PinDialogMode.SET },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.set_pin))
                }
            } else {
                OutlinedButton(
                    onClick = { dialogMode = PinDialogMode.CHANGE },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.change_pin))
                }
            }
        }

        if (state.hasPin) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.auto_lock),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.auto_lock_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0, 1, 5, 15).forEach { minutes ->
                            FilterChip(
                                selected = state.timeoutMinutes == minutes,
                                onClick = { viewModel.setTimeoutMinutes(minutes) },
                                label = { Text(timeoutLabel(minutes)) },
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::lockNow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.LockClock, contentDescription = null)
                    Text(
                        text = stringResource(R.string.lock_now),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            item {
                TextButton(
                    onClick = { dialogMode = PinDialogMode.DISABLE },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.disable_app_lock))
                }
            }
        }
    }

    val activeDialogMode = dialogMode
    if (activeDialogMode != null) {
        PinActionDialog(
            mode = activeDialogMode,
            onDismiss = { dialogMode = null },
            onSet = { newPin ->
                if (viewModel.setPin(newPin)) {
                    dialogMode = null
                    true
                } else {
                    false
                }
            },
            onChange = { currentPin, newPin ->
                if (viewModel.changePin(currentPin, newPin)) {
                    dialogMode = null
                    true
                } else {
                    false
                }
            },
            onDisable = { currentPin ->
                if (viewModel.disable(currentPin)) {
                    dialogMode = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun timeoutLabel(minutes: Int): String = when (minutes) {
    0 -> stringResource(R.string.immediately)
    1 -> stringResource(R.string.one_minute)
    5 -> stringResource(R.string.five_minutes)
    else -> stringResource(R.string.fifteen_minutes)
}

@Composable
private fun PinActionDialog(
    mode: PinDialogMode,
    onDismiss: () -> Unit,
    onSet: (String) -> Boolean,
    onChange: (String, String) -> Boolean,
    onDisable: (String) -> Boolean,
) {
    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val errorPinRules = stringResource(R.string.error_pin_rules)
    val errorPinMismatch = stringResource(R.string.error_pin_mismatch)
    val errorCurrentPin = stringResource(R.string.error_current_pin)
    val errorPinSave = stringResource(R.string.error_pin_save)

    fun validNewPin(): Boolean =
        AppLockService.isValidPin(newPin) && newPin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (mode) {
                        PinDialogMode.SET -> R.string.set_pin
                        PinDialogMode.CHANGE -> R.string.change_pin
                        PinDialogMode.DISABLE -> R.string.disable_app_lock
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (mode != PinDialogMode.SET) {
                    PinField(
                        value = currentPin,
                        onValueChange = {
                            currentPin = it
                            error = null
                        },
                        label = stringResource(R.string.current_pin),
                    )
                }

                if (mode != PinDialogMode.DISABLE) {
                    PinField(
                        value = newPin,
                        onValueChange = {
                            newPin = it
                            error = null
                        },
                        label = stringResource(R.string.new_pin),
                    )
                    PinField(
                        value = confirmPin,
                        onValueChange = {
                            confirmPin = it
                            error = null
                        },
                        label = stringResource(R.string.confirm_pin),
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = null
                    when (mode) {
                        PinDialogMode.SET -> {
                            if (!validNewPin()) {
                                error = if (!AppLockService.isValidPin(newPin)) {
                                    errorPinRules
                                } else {
                                    errorPinMismatch
                                }
                            } else if (!onSet(newPin)) {
                                error = errorPinSave
                            }
                        }

                        PinDialogMode.CHANGE -> {
                            if (!validNewPin()) {
                                error = if (!AppLockService.isValidPin(newPin)) {
                                    errorPinRules
                                } else {
                                    errorPinMismatch
                                }
                            } else if (!onChange(currentPin, newPin)) {
                                error = errorCurrentPin
                            }
                        }

                        PinDialogMode.DISABLE -> {
                            if (!onDisable(currentPin)) {
                                error = errorCurrentPin
                            }
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.confirm))
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
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it.filter(Char::isDigit).take(8))
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
        ),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
