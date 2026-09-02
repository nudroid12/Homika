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
        postActivation(
            path = "/v1/licenses/activate",
            payload = JSONObject()
                .put("license_key", licenseKey)
                .put("device_id", deviceId)
                .put("device_name", deviceName),
        )

    suspend fun validate(
        activationToken: String,
        deviceId: String,
    ): LicenseApiResult =
        postActivation(
            path = "/v1/licenses/validate",
            payload = JSONObject()
                .put("activation_token", activationToken)
                .put("device_id", deviceId),
        )

    suspend fun deactivate(
        activationToken: String,
        deviceId: String,
    ): LicenseDeactivateResult = withContext(Dispatchers.IO) {
        val response = postRaw(
            path = "/v1/licenses/deactivate",
            payload = JSONObject()
                .put("activation_token", activationToken)
                .put("device_id", deviceId),
        )

        when (response) {
            RawResponse.NetworkError -> LicenseDeactivateResult.NetworkError
            is RawResponse.Http -> {
                val json = response.json
                if (response.status in 200..299 && json?.optBoolean("ok", false) == true) {
                    LicenseDeactivateResult.Success(
                        maxDevices = json.optInt("max_devices", 3).coerceAtLeast(1),
                        activeDevices = json.optInt("active_devices", 0).coerceAtLeast(0),
                    )
                } else if (response.status >= 500) {
                    LicenseDeactivateResult.NetworkError
                } else {
                    LicenseDeactivateResult.Rejected(
                        code = json?.optString("error", "license_rejected")
                            ?.ifBlank { "license_rejected" }
                            ?: "invalid_server_response",
                    )
                }
            }
        }
    }

    private suspend fun postActivation(
        path: String,
        payload: JSONObject,
    ): LicenseApiResult = withContext(Dispatchers.IO) {
        when (val response = postRaw(path, payload)) {
            RawResponse.NetworkError -> LicenseApiResult.NetworkError
            is RawResponse.Http -> {
                val json = response.json

                if (response.status in 200..299 && json?.optBoolean("ok", false) == true) {
                    val activation = json.optJSONObject("activation")
                        ?: return@withContext LicenseApiResult.Rejected("invalid_server_response")

                    val token = activation.optString("activation_token").trim()
                    val expiresAt = activation.optString("expires_at").trim()
                    if (token.isBlank() || expiresAt.isBlank()) {
                        return@withContext LicenseApiResult.Rejected("invalid_server_response")
                    }

                    return@withContext LicenseApiResult.Success(
                        LicenseActivation(
                            activationToken = token,
                            licenseHint = activation.optString("license_hint", "••••")
                                .ifBlank { "••••" },
                            expiresAt = expiresAt,
                            maxDevices = activation.optInt("max_devices", 3).coerceAtLeast(1),
                            activeDevices = activation.optInt("active_devices", 0).coerceAtLeast(0),
                        ),
                    )
                }

                if (response.status >= 500) {
                    return@withContext LicenseApiResult.NetworkError
                }

                val error = json?.optString("error", "license_rejected")
                    ?.ifBlank { "license_rejected" }
                    ?: "invalid_server_response"

                LicenseApiResult.Rejected(
                    code = error,
                    expiresAt = json?.optString("expires_at")?.takeIf { it.isNotBlank() },
                    maxDevices = json?.optInt("max_devices", -1)?.takeIf { it >= 0 },
                    activeDevices = json?.optInt("active_devices", -1)?.takeIf { it >= 0 },
                )
            }
        }
    }

    private fun postRaw(
        path: String,
        payload: JSONObject,
    ): RawResponse {
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
            return RawResponse.NetworkError
        }

        return try {
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

            val json = body
                .takeIf { it.isNotBlank() }
                ?.let { raw -> runCatching { JSONObject(raw) }.getOrNull() }

            RawResponse.Http(status = status, json = json)
        } catch (_: Exception) {
            RawResponse.NetworkError
        } finally {
            connection.disconnect()
        }
    }

    private sealed interface RawResponse {
        data class Http(
            val status: Int,
            val json: JSONObject?,
        ) : RawResponse

        data object NetworkError : RawResponse
    }

    private companion object {
        const val BASE_URL = "https://app-license-api.nudroids.workers.dev"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
    }
}
