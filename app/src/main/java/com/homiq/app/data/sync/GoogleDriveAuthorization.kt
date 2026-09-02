package com.homiq.app.data.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface DriveAuthorizationResult {
    data class Authorized(
        val accessToken: String,
    ) : DriveAuthorizationResult

    data class NeedsResolution(
        val pendingIntent: PendingIntent,
    ) : DriveAuthorizationResult

    data class Failure(
        val reason: SyncFailureReason,
    ) : DriveAuthorizationResult
}

class GoogleDriveAuthorization(
    context: Context,
) {
    private val client =
        Identity.getAuthorizationClient(
            context.applicationContext,
        )

    private val scopes =
        listOf(
            Scope(Scopes.DRIVE_APPFOLDER),
            Scope("openid"),
            Scope("email"),
            Scope("profile"),
        )

    private val request =
        AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .build()

    suspend fun authorize():
        DriveAuthorizationResult =
        runCatching {
            client.authorize(request)
                .awaitTask()
        }.fold(
            onSuccess = { result ->
                if (result.hasResolution()) {
                    val pending =
                        result.pendingIntent
                    if (pending != null) {
                        DriveAuthorizationResult
                            .NeedsResolution(
                                pending,
                            )
                    } else {
                        DriveAuthorizationResult
                            .Failure(
                                SyncFailureReason
                                    .AUTHORIZATION_FAILED,
                            )
                    }
                } else {
                    val token =
                        result.accessToken
                    if (token.isNullOrBlank()) {
                        DriveAuthorizationResult
                            .Failure(
                                SyncFailureReason
                                    .AUTHORIZATION_FAILED,
                            )
                    } else {
                        DriveAuthorizationResult
                            .Authorized(token)
                    }
                }
            },
            onFailure = {
                DriveAuthorizationResult.Failure(
                    SyncFailureReason
                        .AUTHORIZATION_FAILED,
                )
            },
        )

    fun authorizationResultFromIntent(
        data: Intent?,
    ): DriveAuthorizationResult {
        if (data == null) {
            return DriveAuthorizationResult
                .Failure(
                    SyncFailureReason
                        .AUTHORIZATION_CANCELLED,
                )
        }

        return runCatching {
            client.getAuthorizationResultFromIntent(
                data,
            )
        }.fold(
            onSuccess = { result ->
                val token =
                    result.accessToken
                if (token.isNullOrBlank()) {
                    DriveAuthorizationResult
                        .Failure(
                            SyncFailureReason
                                .AUTHORIZATION_FAILED,
                        )
                } else {
                    DriveAuthorizationResult
                        .Authorized(token)
                }
            },
            onFailure = {
                DriveAuthorizationResult.Failure(
                    SyncFailureReason
                        .AUTHORIZATION_FAILED,
                )
            },
        )
    }

    suspend fun revoke(): Boolean =
        runCatching {
            client.revokeAccess(
                RevokeAccessRequest.builder()
                    .setScopes(scopes)
                    .build(),
            ).awaitTask()
            true
        }.getOrDefault(false)
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) {
                continuation.resume(value)
            }
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(
                    error,
                )
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
