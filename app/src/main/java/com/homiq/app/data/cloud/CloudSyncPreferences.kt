package com.homiq.app.data.cloud

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CloudSyncPreferences(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun cursor(licenseId: String): Long =
        preferences.getLong(key(KEY_CURSOR_PREFIX, licenseId), 0L).coerceAtLeast(0L)

    fun setCursor(
        licenseId: String,
        cursor: Long,
    ) {
        preferences.edit()
            .putLong(key(KEY_CURSOR_PREFIX, licenseId), cursor.coerceAtLeast(0L))
            .apply()
    }

    fun acknowledgedRevision(
        licenseId: String,
        type: CloudSyncEntityType,
        entityId: String,
    ): Long? =
        readAcknowledgements(licenseId)
            .optLong(recordKey(type, entityId), Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }

    fun setAcknowledgedRevision(
        licenseId: String,
        type: CloudSyncEntityType,
        entityId: String,
        revision: Long,
    ) {
        val acknowledgements = readAcknowledgements(licenseId)
        acknowledgements.put(recordKey(type, entityId), revision.coerceAtLeast(0L))
        preferences.edit()
            .putString(
                key(KEY_ACKNOWLEDGEMENTS_PREFIX, licenseId),
                acknowledgements.toString(),
            )
            .apply()
    }

    fun hasConflict(
        licenseId: String,
        type: CloudSyncEntityType,
        entityId: String,
    ): Boolean =
        conflicts(licenseId).any {
            it.type == type && it.entityId == entityId
        }

    fun conflicts(licenseId: String): List<CloudSyncConflictRecord> {
        val raw = preferences.getString(key(KEY_CONFLICTS_PREFIX, licenseId), null)
            ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val node = array.optJSONObject(index) ?: continue
                val type = CloudSyncEntityType.fromWireName(node.optString("entity_type")) ?: continue
                val entityId = node.optString("entity_id").trim()
                val remotePayload = node.optString("remote_payload_b64").trim()
                if (entityId.isBlank() || remotePayload.isBlank()) continue
                add(
                    CloudSyncConflictRecord(
                        type = type,
                        entityId = entityId,
                        localRevision = node.optLong("local_revision", 0L),
                        remoteRevision = node.optLong("remote_revision", 0L),
                        serverSequence = node.optLong("server_sequence", 0L),
                        remotePayloadBase64 = remotePayload,
                        remoteContentSha256 = node.optString("remote_content_sha256"),
                        reason = node.optString("reason", "revision_conflict"),
                        detectedAtEpochMillis = node.optLong(
                            "detected_at_epoch_millis",
                            System.currentTimeMillis(),
                        ),
                    ),
                )
            }
        }
    }

    fun recordConflict(
        licenseId: String,
        conflict: CloudSyncConflictRecord,
    ) {
        val updated = conflicts(licenseId)
            .filterNot {
                it.type == conflict.type && it.entityId == conflict.entityId
            }
            .plus(conflict)
            .sortedByDescending { it.detectedAtEpochMillis }
            .take(MAX_STORED_CONFLICTS)

        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("entity_type", item.type.wireName)
                    .put("entity_id", item.entityId)
                    .put("local_revision", item.localRevision)
                    .put("remote_revision", item.remoteRevision)
                    .put("server_sequence", item.serverSequence)
                    .put("remote_payload_b64", item.remotePayloadBase64)
                    .put("remote_content_sha256", item.remoteContentSha256)
                    .put("reason", item.reason)
                    .put("detected_at_epoch_millis", item.detectedAtEpochMillis),
            )
        }

        preferences.edit()
            .putString(key(KEY_CONFLICTS_PREFIX, licenseId), array.toString())
            .apply()
    }

    fun recordSuccess(
        licenseId: String,
        epochMillis: Long = System.currentTimeMillis(),
    ) {
        preferences.edit()
            .putLong(key(KEY_LAST_SUCCESS_PREFIX, licenseId), epochMillis)
            .apply()
    }

    fun lastSuccessEpochMillis(licenseId: String): Long? =
        preferences.getLong(key(KEY_LAST_SUCCESS_PREFIX, licenseId), 0L)
            .takeIf { it > 0L }

    private fun readAcknowledgements(licenseId: String): JSONObject {
        val raw = preferences.getString(key(KEY_ACKNOWLEDGEMENTS_PREFIX, licenseId), null)
        return raw?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
    }

    private fun recordKey(
        type: CloudSyncEntityType,
        entityId: String,
    ): String = "${type.wireName}|$entityId"

    private fun key(prefix: String, licenseId: String): String = "$prefix$licenseId"

    companion object {
        private const val PREFERENCES_NAME = "homika_cloud_sync"
        private const val KEY_CURSOR_PREFIX = "cursor_"
        private const val KEY_ACKNOWLEDGEMENTS_PREFIX = "ack_"
        private const val KEY_CONFLICTS_PREFIX = "conflicts_"
        private const val KEY_LAST_SUCCESS_PREFIX = "last_success_"
        private const val MAX_STORED_CONFLICTS = 100
    }
}
