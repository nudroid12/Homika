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

    suspend fun claimTrial(
        email: String,
        deviceId: String,
        deviceName: String,
    ): LicenseApiResult =
        postActivation(
            path = "/v1/trials/claim",
            payload = JSONObject()
                .put("email", email)
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
        val response = requestRaw(
            method = "POST",
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
                    LicenseDeactivateResult.Rejected(response.errorCode())
                }
            }
        }
    }

    suspend fun listDevices(
        activationToken: String,
        deviceId: String,
    ): LicenseDevicesResult = withContext(Dispatchers.IO) {
        val response = requestRaw(
            method = "GET",
            path = "/v1/licenses/devices",
            headers = authenticatedHeaders(activationToken, deviceId),
        )

        when (response) {
            RawResponse.NetworkError -> LicenseDevicesResult.NetworkError
            is RawResponse.Http -> {
                val json = response.json
                if (response.status in 200..299 && json?.optBoolean("ok", false) == true) {
                    val array = json.optJSONArray("devices")
                        ?: return@withContext LicenseDevicesResult.Rejected("invalid_server_response")
                    val devices = buildList {
                        for (index in 0 until array.length()) {
                            val item = array.optJSONObject(index) ?: continue
                            val hash = item.optString("device_hash").trim().lowercase()
                            if (!hash.matches(Regex("^[a-f0-9]{64}$"))) continue
                            add(
                                LicenseDeviceInfo(
                                    deviceHash = hash,
                                    deviceName = item.optString("device_name").trim(),
                                    activatedAt = item.optString("activated_at").trim(),
                                    lastSeenAt = item.optString("last_seen_at").trim(),
                                    isCurrentDevice = item.optBoolean("is_current_device", false),
                                ),
                            )
                        }
                    }
                    LicenseDevicesResult.Success(
                        maxDevices = json.optInt("max_devices", 3).coerceAtLeast(1),
                        activeDevices = json.optInt("active_devices", devices.size).coerceAtLeast(0),
                        devices = devices,
                    )
                } else if (response.status >= 500) {
                    LicenseDevicesResult.NetworkError
                } else {
                    LicenseDevicesResult.Rejected(response.errorCode())
                }
            }
        }
    }

    suspend fun deactivateOtherDevice(
        activationToken: String,
        deviceId: String,
        targetDeviceHash: String,
    ): LicenseRemoteDeviceDeactivateResult = withContext(Dispatchers.IO) {
        val response = requestRaw(
            method = "POST",
            path = "/v1/licenses/devices/deactivate",
            payload = JSONObject().put("device_hash", targetDeviceHash),
            headers = authenticatedHeaders(activationToken, deviceId),
        )

        when (response) {
            RawResponse.NetworkError -> LicenseRemoteDeviceDeactivateResult.NetworkError
            is RawResponse.Http -> {
                val json = response.json
                if (response.status in 200..299 && json?.optBoolean("ok", false) == true) {
                    LicenseRemoteDeviceDeactivateResult.Success(
                        deviceHash = json.optString("device_hash", targetDeviceHash)
                            .ifBlank { targetDeviceHash },
                        maxDevices = json.optInt("max_devices", 3).coerceAtLeast(1),
                        activeDevices = json.optInt("active_devices", 0).coerceAtLeast(0),
                    )
                } else if (response.status >= 500) {
                    LicenseRemoteDeviceDeactivateResult.NetworkError
                } else {
                    LicenseRemoteDeviceDeactivateResult.Rejected(response.errorCode())
                }
            }
        }
    }

    private suspend fun postActivation(
        path: String,
        payload: JSONObject,
    ): LicenseApiResult = withContext(Dispatchers.IO) {
        when (val response = requestRaw("POST", path, payload)) {
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
                            planType = LicensePlanType.fromApi(
                                activation.optString("plan_type", "annual"),
                            ),
                            expiresAt = expiresAt,
                            maxDevices = activation.optInt("max_devices", 3).coerceAtLeast(1),
                            activeDevices = activation.optInt("active_devices", 0).coerceAtLeast(0),
                        ),
                    )
                }

                if (response.status >= 500) {
                    return@withContext LicenseApiResult.NetworkError
                }

                LicenseApiResult.Rejected(
                    code = response.errorCode(),
                    planType = json?.optString("plan_type")
                        ?.takeIf { it.isNotBlank() }
                        ?.let(LicensePlanType::fromApi),
                    expiresAt = json?.optString("expires_at")?.takeIf { it.isNotBlank() },
                    maxDevices = json?.optInt("max_devices", -1)?.takeIf { it >= 0 },
                    activeDevices = json?.optInt("active_devices", -1)?.takeIf { it >= 0 },
                )
            }
        }
    }

    private fun authenticatedHeaders(
        activationToken: String,
        deviceId: String,
    ): Map<String, String> = mapOf(
        "Authorization" to "Bearer $activationToken",
        "X-Homika-Device-Id" to deviceId,
    )

    private fun requestRaw(
        method: String,
        path: String,
        payload: JSONObject? = null,
        headers: Map<String, String> = emptyMap(),
    ): RawResponse {
        val connection = runCatching {
            (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
                if (payload != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
        }.getOrElse {
            return RawResponse.NetworkError
        }

        return try {
            if (payload != null) {
                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
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

    private fun RawResponse.Http.errorCode(): String =
        json?.optString("error", "license_rejected")
            ?.ifBlank { "license_rejected" }
            ?: "invalid_server_response"

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
