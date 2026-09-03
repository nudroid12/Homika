package com.homiq.app.data.cloud

import android.content.Context

class CloudSnapshotSyncPreferences(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(
        "homika_cloud_snapshot_sync",
        Context.MODE_PRIVATE,
    )

    var localChangePending: Boolean
        get() = preferences.getBoolean(KEY_LOCAL_CHANGE_PENDING, false)
        set(value) {
            preferences.edit().putBoolean(KEY_LOCAL_CHANGE_PENDING, value).apply()
        }

    fun hasCompletedInitialSync(licenseId: String): Boolean =
        preferences.getBoolean(initialSyncKey(licenseId), false)

    fun markInitialSyncComplete(licenseId: String) {
        preferences.edit().putBoolean(initialSyncKey(licenseId), true).apply()
    }

    fun seenContentSha256(
        licenseId: String,
        deviceHash: String,
    ): String? =
        preferences.getString(seenKey(licenseId, deviceHash), null)
            ?.takeIf { it.length == 64 }

    fun markSeen(
        licenseId: String,
        deviceHash: String,
        contentSha256: String,
    ) {
        preferences.edit().putString(
            seenKey(licenseId, deviceHash),
            contentSha256,
        ).apply()
    }

    fun recordSuccess(
        licenseId: String,
        epochMillis: Long,
        conflictCount: Int,
        remoteDeviceCount: Int,
    ) {
        preferences.edit()
            .putLong(successKey(licenseId), epochMillis)
            .putInt(conflictKey(licenseId), conflictCount)
            .putInt(remoteDeviceKey(licenseId), remoteDeviceCount)
            .apply()
    }

    fun lastSuccessEpochMillis(licenseId: String): Long? =
        preferences.getLong(successKey(licenseId), 0L).takeIf { it > 0L }

    private fun initialSyncKey(licenseId: String) = "initial_${safe(licenseId)}"
    private fun successKey(licenseId: String) = "success_${safe(licenseId)}"
    private fun conflictKey(licenseId: String) = "conflicts_${safe(licenseId)}"
    private fun remoteDeviceKey(licenseId: String) = "remote_devices_${safe(licenseId)}"
    private fun seenKey(licenseId: String, deviceHash: String) =
        "seen_${safe(licenseId)}_${safe(deviceHash)}"

    private fun safe(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(160)

    private companion object {
        const val KEY_LOCAL_CHANGE_PENDING = "local_change_pending"
    }
}
