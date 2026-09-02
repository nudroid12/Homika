package com.homiq.app.data.license

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LicenseApiClient(
    private val baseUrl: String = BASE_URL,
) {
    suspend fun activate(
        licenseKey: String,
        deviceId: String,
        deviceName: String,
    ): LicenseApiResult =
        post(
            path = "/v1/licenses/activate",
            payload = JSONObject()
                .put("license_key", licenseKey)
                .put("device_id", deviceId)
                .put("device_name", deviceName),
        )

    suspend fun validate(
        licenseKey: String,
        deviceId: String,
    ): LicenseApiResult =
        post(
            path = "/v1/licenses/validate",
            payload = JSONObject()
                .put("license_key", licenseKey)
                .put("device_id", deviceId),
        )

    private suspend fun post(
        path: String,
        payload: JSONObject,
    ): LicenseApiResult = withContext(Dispatchers.IO) {
        val connection = runCatching {
            (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
        }.getOrElse {
            return@withContext LicenseApiResult.NetworkError
        }

        try {
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val body = runCatching {
                val source = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                source?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }.getOrDefault("")

            if (body.isBlank()) {
                return@withContext if (status in 500..599) {
                    LicenseApiResult.NetworkError
                } else {
                    LicenseApiResult.Rejected("invalid_server_response")
                }
            }

            val json = runCatching { JSONObject(body) }.getOrNull()
                ?: return@withContext LicenseApiResult.Rejected("invalid_server_response")

            if (status in 200..299 && json.optBoolean("ok", false)) {
                val activation = json.optJSONObject("activation")
                    ?: return@withContext LicenseApiResult.Rejected("invalid_server_response")

                val expiresAt = activation.optString("expires_at").trim()
                if (expiresAt.isBlank()) {
                    return@withContext LicenseApiResult.Rejected("invalid_server_response")
                }

                return@withContext LicenseApiResult.Success(
                    LicenseActivation(
                        expiresAt = expiresAt,
                        maxDevices = activation.optInt("max_devices", 3).coerceAtLeast(1),
                        activeDevices = activation.optInt("active_devices", 0).coerceAtLeast(0),
                    ),
                )
            }

            val error = json.optString("error", "license_rejected")
                .ifBlank { "license_rejected" }

            if (status >= 500) {
                return@withContext LicenseApiResult.NetworkError
            }

            LicenseApiResult.Rejected(
                code = error,
                expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() },
                maxDevices = json.optInt("max_devices", -1).takeIf { it >= 0 },
                activeDevices = json.optInt("active_devices", -1).takeIf { it >= 0 },
            )
        } catch (_: Exception) {
            LicenseApiResult.NetworkError
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE_URL = "https://app-license-api.nudroids.workers.dev"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
    }
}
