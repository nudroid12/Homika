package com.homiq.app.ui.util

import androidx.annotation.StringRes
import com.homiq.app.R
import com.homiq.app.data.sync.SyncFailureReason

@StringRes
fun SyncFailureReason.messageRes(): Int =
    when (this) {
        SyncFailureReason.AUTHORIZATION_FAILED ->
            R.string.sync_error_authorization
        SyncFailureReason.AUTHORIZATION_CANCELLED ->
            R.string.sync_error_cancelled
        SyncFailureReason.NETWORK_UNAVAILABLE ->
            R.string.sync_error_network
        SyncFailureReason.DRIVE_ACCESS_FAILED ->
            R.string.sync_error_drive
        SyncFailureReason.REMOTE_DATA_INVALID ->
            R.string.sync_error_remote_data
        SyncFailureReason.UNKNOWN ->
            R.string.sync_error_unknown
    }
