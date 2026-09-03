package com.homiq.app.data.cloud

import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.license.LicenseRepository
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CloudSnapshotSyncApiClient(
    private val baseUrl: String = BASE_URL,
) {
    suspend fun listSnapshots(
        credentials: LicenseRepository.CloudCredentials,
    ): CloudSnapshotSyncResult<List<CloudSnapshotMetadata>> = withContext(Dispatchers.IO) {
        val raw = requestJson(
            method = "GET",
            path = "/v1/cloud/sync/snapshots",
            credentials = credentials,
        )
        when (raw) {
            RawJson.NetworkError -> CloudSnapshotSyncResult.failure(
                CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE,
            )
            is RawJson.Http -> {
                if (raw.status in 200..299 && raw.json?.optBoolean("ok", false) == true) {
                    val array = raw.json.optJSONArray("snapshots")
                    val result = buildList {
                        if (array != null) {
                            for (index in 0 until array.length()) {
                                val item = parseMetadata(array.optJSONObject(index))
                                    ?: return@withContext CloudSnapshotSyncResult.failure(
                                        CloudSnapshotSyncFailureReason.SERVER_ERROR,
                                    )
                                add(item)
                            }
                        }
                    }
                    CloudSnapshotSyncResult.success(result)
                } else {
                    CloudSnapshotSyncResult.failure(mapFailure(raw.status, raw.json))
                }
            }
        }
    }

    suspend fun uploadCurrent(
        credentials: LicenseRepository.CloudCredentials,
        encrypted: ByteArray,
        contentSha256: String,
        createdAtEpochMillis: Long,
        recordCount: Int,
    ): CloudSnapshotSyncResult<CloudSnapshotMetadata> = withContext(Dispatchers.IO) {
        val connection = openConnection(
            path = "/v1/cloud/sync/snapshots/current",
            method = "PUT",
            credentials = credentials,
        ) ?: return@withContext CloudSnapshotSyncResult.failure(
            CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE,
        )

        return@withContext try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty("X-Homika-Sync-Updated-At", createdAtEpochMillis.toString())
            connection.setRequestProperty("X-Homika-Record-Count", recordCount.coerceAtLeast(0).toString())
            connection.setRequestProperty("X-Homika-Content-Sha256", contentSha256)
            connection.setRequestProperty("X-Homika-Format-Version", HomiqBackupCodec.FORMAT_VERSION.toString())
            connection.setRequestProperty(
                "X-Homika-Database-Schema-Version",
                HomiqBackupCodec.DATABASE_SCHEMA_VERSION.toString(),
            )
            connection.outputStream.use { it.write(encrypted) }

            val status = connection.responseCode
            val json = readJsonBody(connection, status)
            if (status in 200..299 && json?.optBoolean("ok", false) == true) {
                val metadata = parseMetadata(json.optJSONObject("snapshot"))
                    ?: return@withContext CloudSnapshotSyncResult.failure(
                        CloudSnapshotSyncFailureReason.SERVER_ERROR,
                    )
                CloudSnapshotSyncResult.success(metadata)
            } else {
                CloudSnapshotSyncResult.failure(mapFailure(status, json))
            }
        } catch (_: Exception) {
            CloudSnapshotSyncResult.failure(CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadSnapshot(
        credentials: LicenseRepository.CloudCredentials,
        deviceHash: String,
    ): CloudSnapshotSyncResult<ByteArray> = withContext(Dispatchers.IO) {
        val encodedHash = URLEncoder.encode(deviceHash, Charsets.UTF_8.name())
        val connection = openConnection(
            path = "/v1/cloud/sync/snapshots/content?device_hash=$encodedHash",
            method = "GET",
            credentials = credentials,
        ) ?: return@withContext CloudSnapshotSyncResult.failure(
            CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE,
        )

        return@withContext try {
            val status = connection.responseCode
            if (status in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) {
                    CloudSnapshotSyncResult.failure(CloudSnapshotSyncFailureReason.INVALID_REMOTE_SNAPSHOT)
                } else {
                    CloudSnapshotSyncResult.success(bytes)
                }
            } else {
                val json = readJsonBody(connection, status)
                CloudSnapshotSyncResult.failure(mapFailure(status, json))
            }
        } catch (_: Exception) {
            CloudSnapshotSyncResult.failure(CloudSnapshotSyncFailureReason.NETWORK_UNAVAILABLE)
        } finally {
            connection.disconnect()
        }
    }

    private fun requestJson(
        method: String,
        path: String,
        credentials: LicenseRepository.CloudCredentials,
    ): RawJson {
        val connection = openConnection(path, method, credentials) ?: return RawJson.NetworkError
        return try {
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
            setRequestProperty("Accept", "application/json, application/octet-stream")
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

    private fun parseMetadata(node: JSONObject?): CloudSnapshotMetadata? {
        if (node == null) return null
        val deviceHash = node.optString("device_hash").trim()
        val updatedAt = node.optLong("updated_at_epoch_millis", 0L)
        val sha256 = node.optString("sha256").trim()
        val contentSha256 = node.optString("content_sha256").trim()
        if (
            deviceHash.length != 64 ||
            updatedAt <= 0L ||
            sha256.length != 64 ||
            contentSha256.length != 64
        ) {
            return null
        }
        return CloudSnapshotMetadata(
            deviceHash = deviceHash,
            updatedAtEpochMillis = updatedAt,
            recordCount = node.optInt("record_count", 0).coerceAtLeast(0),
            byteSize = node.optLong("byte_size", 0L).coerceAtLeast(0L),
            sha256 = sha256,
            contentSha256 = contentSha256,
            isCurrentDevice = node.optBoolean("is_current_device", false),
        )
    }

    private fun mapFailure(
        status: Int,
        json: JSONObject?,
    ): CloudSnapshotSyncFailureReason {
        val code = json?.optString("error").orEmpty()
        return when {
            code == "cloud_storage_not_configured" -> CloudSnapshotSyncFailureReason.CLOUD_NOT_CONFIGURED
            code == "sync_snapshot_too_large" -> CloudSnapshotSyncFailureReason.SNAPSHOT_TOO_LARGE
            status == 401 || status == 403 -> CloudSnapshotSyncFailureReason.LICENSE_REQUIRED
            status >= 500 -> CloudSnapshotSyncFailureReason.SERVER_ERROR
            else -> CloudSnapshotSyncFailureReason.SERVER_REJECTED
        }
    }

    private sealed interface RawJson {
        data class Http(val status: Int, val json: JSONObject?) : RawJson
        data object NetworkError : RawJson
    }

    private companion object {
        const val BASE_URL = "https://app-license-api.nudroids.workers.dev"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
