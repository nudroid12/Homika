package com.homiq.app.ui.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homiq.app.R
import com.homiq.app.ui.components.HomikaBrandMark
import com.homiq.app.ui.security.canUseHomiqBiometrics
import com.homiq.app.ui.security.showHomiqBiometricPrompt
import com.homiq.app.ui.viewmodel.AppLockViewModel

@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? AppCompatActivity
    val biometricAvailable = remember(activity) {
        activity?.let(::canUseHomiqBiometrics) == true
    }
    var pin by rememberSaveable { mutableStateOf("") }
    var wrongPin by rememberSaveable { mutableStateOf(false) }
    var biometricAttempted by rememberSaveable { mutableStateOf(false) }

    fun launchBiometric() {
        val host = activity ?: return
        showHomiqBiometricPrompt(
            activity = host,
            title = host.getString(R.string.unlock_homiq),
            subtitle = host.getString(R.string.biometric_unlock_subtitle),
            negativeButton = host.getString(R.string.use_pin),
            onSuccess = viewModel::biometricUnlock,
        )
    }

    LaunchedEffect(state.locked, state.biometricEnabled, biometricAvailable) {
        if (state.locked && state.biometricEnabled && biometricAvailable && !biometricAttempted) {
            biometricAttempted = true
            launchBiometric()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HomikaBrandMark(modifier = Modifier.size(64.dp))

                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.homiq_locked),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.homiq_locked_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(8)
                        wrongPin = false
                    },
                    label = { Text(stringResource(R.string.pin)) },
                    isError = wrongPin,
                    supportingText = if (wrongPin) {
                        { Text(stringResource(R.string.wrong_pin)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        if (viewModel.unlock(pin)) {
                            pin = ""
                        } else {
                            wrongPin = true
                        }
                    },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.unlock))
                }

                if (state.biometricEnabled && biometricAvailable) {
                    OutlinedButton(
                        onClick = {
                            biometricAttempted = true
                            launchBiometric()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                        Text(
                            text = stringResource(R.string.use_biometric),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
