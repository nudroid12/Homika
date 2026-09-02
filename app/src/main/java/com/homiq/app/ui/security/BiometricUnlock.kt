package com.homiq.app.ui.security

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

fun canUseHomiqBiometrics(activity: AppCompatActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK,
    ) == BiometricManager.BIOMETRIC_SUCCESS

fun showHomiqBiometricPrompt(
    activity: AppCompatActivity,
    title: String,
    subtitle: String,
    negativeButton: String,
    onSuccess: () -> Unit,
    onError: () -> Unit = {},
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                onSuccess()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError()
                }
            }
        },
    )

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setNegativeButtonText(negativeButton)
        .build()

    prompt.authenticate(info)
}
