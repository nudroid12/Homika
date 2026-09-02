package com.homiq.app.data.cloud

import com.homiq.app.data.backup.BackupPreview
import com.homiq.app.data.backup.HomiqBackupCodec
import com.homiq.app.data.license.LicenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CloudBackupApiClient(
    private val baseUrl: String = BASE_URL,
) {
    suspend fun fetchCloudKey(
        credentials: LicenseRepository.CloudCredentials,
    ): CloudBackupResult<String> = withContext(Dispatchers.IO) {
        when (val response = requestJson("GET", "/v1/cloud/key", credentials)) {
            RawJson.NetworkError -> CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)
            is RawJson.Http -> {
                if (response.status in 200..299 && response.json?.optBoolean("ok", false) == true) {
                    val key = response.json.optJSONObject("cloud")
                        ?.optString("key_b64")
                        ?.trim()
                        .orEmpty()
                    if (key.isBlank()) {
                        CloudBackupResult.failure(CloudBackupFailureReason.SERVER_ERROR)
                    } else {
                        CloudBackupResult.success(key)
                    }
                } else {
                    CloudBackupResult.failure(mapFailure(response.status, response.json))
                }
            }
        }
    }

    suspend fun upload(
        credentials: LicenseRepository.CloudCredentials,
        encrypted: ByteArray,
        preview: BackupPreview,
    ): CloudBackupResult<CloudBackupMetadata> = withContext(Dispatchers.IO) {
        val connection = openConnection("/v1/cloud/backups", "POST", credentials)
            ?: return@withContext CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)

        return@withContext try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setRequestProperty(
                "X-Homika-Backup-Created-At",
                preview.createdAtEpochMillis.toString(),
            )
            connection.setRequestProperty("X-Homika-Record-Count", preview.totalRecordCount.toString())
            connection.setRequestProperty("X-Homika-Format-Version", HomiqBackupCodec.FORMAT_VERSION.toString())
            connection.setRequestProperty(
                "X-Homika-Database-Schema-Version",
                HomiqBackupCodec.DATABASE_SCHEMA_VERSION.toString(),
            )
            connection.outputStream.use { it.write(encrypted) }

            val status = connection.responseCode
            val json = readJsonBody(connection, status)
            if (status in 200..299 && json?.optBoolean("ok", false) == true) {
                val metadata = parseMetadata(json.optJSONObject("backup"))
                if (metadata == null) {
                    CloudBackupResult.failure(CloudBackupFailureReason.SERVER_ERROR)
                } else {
                    CloudBackupResult.success(metadata)
                }
            } else {
                CloudBackupResult.failure(mapFailure(status, json))
            }
        } catch (_: Exception) {
            CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun latest(
        credentials: LicenseRepository.CloudCredentials,
    ): CloudBackupResult<CloudBackupMetadata?> = withContext(Dispatchers.IO) {
        when (val response = requestJson("GET", "/v1/cloud/backups/latest", credentials)) {
            RawJson.NetworkError -> CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)
            is RawJson.Http -> {
                if (response.status in 200..299 && response.json?.optBoolean("ok", false) == true) {
                    val node = response.json.optJSONObject("backup")
                    if (node == null) {
                        CloudBackupResult.success<CloudBackupMetadata?>(null)
                    } else {
                        val metadata = parseMetadata(node)
                        if (metadata == null) {
                            CloudBackupResult.failure(CloudBackupFailureReason.SERVER_ERROR)
                        } else {
                            CloudBackupResult.success<CloudBackupMetadata?>(metadata)
                        }
                    }
                } else {
                    CloudBackupResult.failure(mapFailure(response.status, response.json))
                }
            }
        }
    }

    suspend fun downloadLatest(
        credentials: LicenseRepository.CloudCredentials,
    ): CloudBackupResult<ByteArray> = withContext(Dispatchers.IO) {
        val connection = openConnection(
            path = "/v1/cloud/backups/latest/content",
            method = "GET",
            credentials = credentials,
        ) ?: return@withContext CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)

        return@withContext try {
            val status = connection.responseCode
            if (status in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) {
                    CloudBackupResult.failure(CloudBackupFailureReason.INVALID_CLOUD_BACKUP)
                } else {
                    CloudBackupResult.success(bytes)
                }
            } else {
                val json = readJsonBody(connection, status)
                CloudBackupResult.failure(mapFailure(status, json))
            }
        } catch (_: Exception) {
            CloudBackupResult.failure(CloudBackupFailureReason.NETWORK_UNAVAILABLE)
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

    private fun parseMetadata(node: JSONObject?): CloudBackupMetadata? {
        if (node == null) return null
        val id = node.optString("id").trim()
        val createdAt = node.optLong("created_at_epoch_millis", 0L)
        if (id.isBlank() || createdAt <= 0L) return null
        return CloudBackupMetadata(
            id = id,
            createdAtEpochMillis = createdAt,
            recordCount = node.optInt("record_count", 0).coerceAtLeast(0),
            byteSize = node.optLong("byte_size", 0L).coerceAtLeast(0L),
            sha256 = node.optString("sha256").trim(),
        )
    }

    private fun mapFailure(status: Int, json: JSONObject?): CloudBackupFailureReason {
        val code = json?.optString("error").orEmpty()
        return when {
            code == "cloud_storage_not_configured" -> CloudBackupFailureReason.CLOUD_NOT_CONFIGURED
            code == "cloud_backup_not_found" || code == "cloud_backup_missing" ->
                CloudBackupFailureReason.BACKUP_NOT_FOUND
            code == "backup_too_large" -> CloudBackupFailureReason.BACKUP_TOO_LARGE
            status == 401 || status == 403 -> CloudBackupFailureReason.LICENSE_REQUIRED
            status >= 500 -> CloudBackupFailureReason.SERVER_ERROR
            else -> CloudBackupFailureReason.SERVER_REJECTED
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
