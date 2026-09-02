package com.homiq.app.data.account

import android.app.PendingIntent
import android.content.Intent
import com.homiq.app.data.backup.BackupPreferences
import com.homiq.app.data.sync.DriveAuthorizationResult
import com.homiq.app.data.sync.GoogleDriveAuthorization
import com.homiq.app.data.sync.SyncFailureReason
import com.homiq.app.data.sync.SyncPreferences
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GoogleAccountProfile(
    val email: String?,
    val displayName: String?,
)

sealed interface GoogleAccountActionResult {
    data object Connected :
        GoogleAccountActionResult

    data class NeedsResolution(
        val pendingIntent: PendingIntent,
    ) : GoogleAccountActionResult

    data class Failure(
        val reason: SyncFailureReason,
    ) : GoogleAccountActionResult
}

class GoogleAccountService(
    private val authorization:
        GoogleDriveAuthorization,
    private val preferences:
        AccountPreferences,
    private val syncPreferences:
        SyncPreferences,
    private val backupPreferences:
        BackupPreferences,
) {
    init {
        if (
            syncPreferences.state().enabled &&
            !preferences.state.value.googleConnected
        ) {
            preferences.markGoogleConnected()
        }
    }

    suspend fun signIn():
        GoogleAccountActionResult =
        when (
            val auth = authorization.authorize()
        ) {
            is DriveAuthorizationResult.Authorized -> {
                rememberAuthorizedToken(
                    auth.accessToken,
                )
                GoogleAccountActionResult.Connected
            }

            is DriveAuthorizationResult.NeedsResolution ->
                GoogleAccountActionResult.NeedsResolution(
                    auth.pendingIntent,
                )

            is DriveAuthorizationResult.Failure ->
                GoogleAccountActionResult.Failure(
                    auth.reason,
                )
        }

    suspend fun completeSignIn(
        data: Intent?,
    ): GoogleAccountActionResult =
        when (
            val auth =
                authorization
                    .authorizationResultFromIntent(
                        data,
                    )
        ) {
            is DriveAuthorizationResult.Authorized -> {
                rememberAuthorizedToken(
                    auth.accessToken,
                )
                GoogleAccountActionResult.Connected
            }

            is DriveAuthorizationResult.NeedsResolution ->
                GoogleAccountActionResult.NeedsResolution(
                    auth.pendingIntent,
                )

            is DriveAuthorizationResult.Failure ->
                GoogleAccountActionResult.Failure(
                    auth.reason,
                )
        }

    suspend fun rememberAuthorizedToken(
        accessToken: String,
    ) {
        val profile =
            runCatching {
                GoogleAccountProfileClient()
                    .fetch(accessToken)
            }.getOrNull()

        if (profile == null) {
            preferences.markGoogleConnected()
        } else {
            preferences.setGoogleProfile(
                email = profile.email,
                displayName =
                    profile.displayName,
            )
        }
    }

    suspend fun signOut(): Boolean {
        val revoked = authorization.revoke()

        backupPreferences
            .setAutoBackupEnabled(false)
        backupPreferences
            .setAutoBackupPending(false)
        preferences.clearGoogleAccount()

        return revoked
    }
}

private class GoogleAccountProfileClient {
    suspend fun fetch(
        accessToken: String,
    ): GoogleAccountProfile =
        withContext(Dispatchers.IO) {
            val connection =
                URL(USER_INFO_URL)
                    .openConnection()
                    as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken",
                )
                connection.setRequestProperty(
                    "Accept",
                    "application/json",
                )

                val status = connection.responseCode
                val stream =
                    if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                val body =
                    if (stream == null) {
                        ""
                    } else {
                        BufferedReader(
                            InputStreamReader(
                                stream,
                                Charsets.UTF_8,
                            ),
                        ).use { it.readText() }
                    }

                if (status !in 200..299) {
                    error(
                        "Google user info HTTP $status",
                    )
                }

                val json = JSONObject(body)
                GoogleAccountProfile(
                    email =
                        json.optString("email")
                            .takeIf {
                                it.isNotBlank()
                            },
                    displayName =
                        json.optString("name")
                            .takeIf {
                                it.isNotBlank()
                            },
                )
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val USER_INFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo"
    }
}
