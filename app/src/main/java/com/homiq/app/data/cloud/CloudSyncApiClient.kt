package com.homiq.app.data.cloud

import com.homiq.app.data.license.LicenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CloudSyncApiClient(
    private val baseUrl: String = BASE_URL,
) {
    suspend fun push(
        credentials: LicenseRepository.CloudCredentials,
        changes: List<CloudSyncPushChange>,
    ): CloudSyncResult<CloudSyncPushResponse> = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("protocol", PROTOCOL_VERSION)
            .put(
                "changes",
                JSONArray().apply {
                    changes.forEach { change ->
                        put(
                            JSONObject()
                                .put("entity_type", change.type.wireName)
                                .put("entity_id", change.entityId)
                                .put("revision", change.revision)
                                .put("base_revision", change.baseRevision)
                                .put("updated_at_epoch_millis", change.updatedAtEpochMillis)
                                .put("is_deleted", change.isDeleted)
                                .put("payload_b64", change.payloadBase64)
                                .put("content_sha256", change.contentSha256),
                        )
                    }
                },
            )

        when (val response = requestJson("POST", "/v1/cloud/sync/push", credentials, body)) {
            RawJson.NetworkError -> CloudSyncResult.failure(CloudSyncFailureReason.NETWORK_UNAVAILABLE)
            is RawJson.Http -> {
                if (response.status in 200..299 && response.json?.optBoolean("ok", false) == true) {
                    parsePushResponse(response.json)
                        ?.let(CloudSyncResult.Companion::success)
                        ?: CloudSyncResult.failure(CloudSyncFailureReason.SERVER_ERROR)
                } else {
                    CloudSyncResult.failure(mapFailure(response.status, response.json))
                }
            }
        }
    }

    suspend fun pull(
        credentials: LicenseRepository.CloudCredentials,
        cursor: Long,
        limit: Int = 100,
    ): CloudSyncResult<CloudSyncPullResponse> = withContext(Dispatchers.IO) {
        val path = "/v1/cloud/sync/pull?cursor=${cursor.coerceAtLeast(0L)}&limit=${limit.coerceIn(1, 200)}"
        when (val response = requestJson("GET", path, credentials, null)) {
            RawJson.NetworkError -> CloudSyncResult.failure(CloudSyncFailureReason.NETWORK_UNAVAILABLE)
            is RawJson.Http -> {
                if (response.status in 200..299 && response.json?.optBoolean("ok", false) == true) {
                    parsePullResponse(response.json)
                        ?.let(CloudSyncResult.Companion::success)
                        ?: CloudSyncResult.failure(CloudSyncFailureReason.SERVER_ERROR)
                } else {
                    CloudSyncResult.failure(mapFailure(response.status, response.json))
                }
            }
        }
    }

    private fun requestJson(
        method: String,
        path: String,
        credentials: LicenseRepository.CloudCredentials,
        body: JSONObject?,
    ): RawJson {
        val connection = openConnection(path, method, credentials) ?: return RawJson.NetworkError
        return try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                }
            }
            val status = connection.responseCode
            RawJson.Http(status, readJsonBody(connection, status))
        } catch (_: Exception) {
            RawJson.NetworkError
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        path: String,
        method: String,
        credentials: LicenseRepository.CloudCredentials,
    ): HttpURLConnection? = runCatching {
        (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${credentials.activationToken}")
            setRequestProperty("X-Homika-Device-Id", credentials.deviceId)
        }
    }.getOrNull()

    private fun readJsonBody(
        connection: HttpURLConnection,
        status: Int,
    ): JSONObject? {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return raw.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    private fun parsePushResponse(json: JSONObject): CloudSyncPushResponse? {
        val acceptedArray = json.optJSONArray("accepted") ?: JSONArray()
        val conflictsArray = json.optJSONArray("conflicts") ?: JSONArray()

        val accepted = buildList {
            for (index in 0 until acceptedArray.length()) {
                val node = acceptedArray.optJSONObject(index) ?: return null
                val type = CloudSyncEntityType.fromWireName(node.optString("entity_type")) ?: return null
                val entityId = node.optString("entity_id").trim()
                if (entityId.isBlank()) return null
                add(
                    CloudSyncAcceptedChange(
                        type = type,
                        entityId = entityId,
                        revision = node.optLong("revision", -1L),
                        serverSequence = node.optLong("server_sequence", 0L),
                    ),
                )
            }
        }

        val conflicts = buildList {
            for (index in 0 until conflictsArray.length()) {
                val node = conflictsArray.optJSONObject(index) ?: return null
                val type = CloudSyncEntityType.fromWireName(node.optString("entity_type")) ?: return null
                val entityId = node.optString("entity_id").trim()
                if (entityId.isBlank()) return null
                add(
                    CloudSyncServerConflict(
                        type = type,
                        entityId = entityId,
                        localRevision = node.optLong("local_revision", 0L),
                        baseRevision = node.optLong("base_revision", 0L),
                        reason = node.optString("reason", "revision_conflict"),
                        current = node.optJSONObject("current")?.let(::parseRemoteChange),
                    ),
                )
            }
        }

        return CloudSyncPushResponse(
            accepted = accepted,
            conflicts = conflicts,
        )
    }

    private fun parsePullResponse(json: JSONObject): CloudSyncPullResponse? {
        if (json.optInt("protocol", PROTOCOL_VERSION) != PROTOCOL_VERSION) return null
        val array = json.optJSONArray("changes") ?: JSONArray()
        val changes = buildList {
            for (index in 0 until array.length()) {
                val node = array.optJSONObject(index) ?: return null
                add(parseRemoteChange(node) ?: return null)
            }
        }
        return CloudSyncPullResponse(
            nextCursor = json.optLong("next_cursor", 0L).coerceAtLeast(0L),
            hasMore = json.optBoolean("has_more", false),
            changes = changes,
        )
    }

    private fun parseRemoteChange(node: JSONObject): CloudSyncRemoteChange? {
        val type = CloudSyncEntityType.fromWireName(node.optString("entity_type")) ?: return null
        val entityId = node.optString("entity_id").trim()
        val payload = node.optString("payload_b64").trim()
        val contentSha = node.optString("content_sha256").trim()
        val revision = node.optLong("revision", -1L)
        if (entityId.isBlank() || payload.isBlank() || contentSha.length != 64 || revision < 0L) return null
        return CloudSyncRemoteChange(
            serverSequence = node.optLong("server_sequence", 0L).coerceAtLeast(0L),
            type = type,
            entityId = entityId,
            revision = revision,
            updatedAtEpochMillis = node.optLong("updated_at_epoch_millis", 0L),
            isDeleted = node.optBoolean("is_deleted", false),
            payloadBase64 = payload,
            contentSha256 = contentSha,
            sourceDeviceHash = node.optString("source_device_hash"),
        )
    }

    private fun mapFailure(
        status: Int,
        json: JSONObject?,
    ): CloudSyncFailureReason {
        val code = json?.optString("error").orEmpty()
        return when {
            code == "cloud_storage_not_configured" -> CloudSyncFailureReason.CLOUD_NOT_CONFIGURED
            status == 401 || status == 403 -> CloudSyncFailureReason.LICENSE_REQUIRED
            status >= 500 -> CloudSyncFailureReason.SERVER_ERROR
            else -> CloudSyncFailureReason.SERVER_REJECTED
        }
    }

    private sealed interface RawJson {
        data class Http(val status: Int, val json: JSONObject?) : RawJson
        data object NetworkError : RawJson
    }

    private companion object {
        const val BASE_URL = "https://app-license-api.nudroids.workers.dev"
        const val PROTOCOL_VERSION = 1
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
