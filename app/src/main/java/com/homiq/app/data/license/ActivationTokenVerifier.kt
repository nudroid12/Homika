package com.homiq.app.data.license

import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object ActivationTokenVerifier {
    fun verify(
        token: String,
        expectedDeviceId: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): VerifiedActivationToken? {
        val parts = token.split('.')
        if (parts.size != 3) return null

        val header = decodeJson(parts[0]) ?: return null
        if (
            header.optString("alg") != "RS256" ||
            header.optString("typ") != "HAT" ||
            header.optInt("v", 0) != 1
        ) {
            return null
        }

        val signatureBytes = decodeUrl(parts[2]) ?: return null
        val signingInput = "${parts[0]}.${parts[1]}".toByteArray(Charsets.UTF_8)

        val publicKey = runCatching {
            val decoded = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decoded))
        }.getOrNull() ?: return null

        val signatureValid = runCatching {
            Signature.getInstance("SHA256withRSA").run {
                initVerify(publicKey)
                update(signingInput)
                verify(signatureBytes)
            }
        }.getOrDefault(false)

        if (!signatureValid) return null

        val claims = decodeJson(parts[1]) ?: return null
        if (
            claims.optInt("v", 0) != 1 ||
            claims.optString("iss") != "app-license-api" ||
            claims.optString("product_id") != "homika_pro"
        ) {
            return null
        }

        val licenseId = claims.optString("license_id").trim()
        if (licenseId.isBlank()) return null

        val serverDeviceHash = claims.optString("device_hash").trim()
        if (serverDeviceHash.isBlank() || serverDeviceHash != sha256(expectedDeviceId)) {
            return null
        }

        val planType = LicensePlanType.fromApi(claims.optString("plan_type", "annual"))
        val tokenExpirySeconds = claims.optLong("exp", 0L)
        val licenseExpirySeconds = claims.optLong("license_exp", 0L)
        if (tokenExpirySeconds <= 0L || licenseExpirySeconds <= 0L) return null

        val tokenExpiryMillis = runCatching {
            Math.multiplyExact(tokenExpirySeconds, 1000L)
        }.getOrNull() ?: return null

        val licenseExpiryMillis = runCatching {
            Math.multiplyExact(licenseExpirySeconds, 1000L)
        }.getOrNull() ?: return null

        if (nowMillis >= tokenExpiryMillis) return null

        return VerifiedActivationToken(
            licenseId = licenseId,
            planType = planType,
            expiresAtEpochMillis = licenseExpiryMillis,
            licenseHint = claims.optString("license_hint", "••••").ifBlank { "••••" },
            maxDevices = claims.optInt("max_devices", 3).coerceAtLeast(1),
        )
    }

    private fun decodeJson(value: String): JSONObject? =
        decodeUrl(value)
            ?.toString(Charsets.UTF_8)
            ?.let { raw -> runCatching { JSONObject(raw) }.getOrNull() }

    private fun decodeUrl(value: String): ByteArray? =
        runCatching {
            Base64.decode(
                value,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
        }.getOrNull()

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private const val PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsyzhDrq517vuIzP99flbfNSEPzrocJH/Dqlp07CNR+vNYadLpqpsVKGV+3SIBtF9ytcV6gB00d6dIVbfL5ORS0YY+XgKQhGjHAZ9/AWk1VqUCvXtavrZWA0kUMNy5kImzdtX/0cMclqH9WpC4kQxcsCgjpQp80mhdK3db1zmHsdi/4fH7Kxgcz1NTzFM3/8fLVXg1KdHw356vGmjJRoAxG8rg4rbymmgIRwFYnKUbyrG9xL4iBJ/J+D4zR5+DxQ3UCRKg5/576epGuWqkHARjxcR4IE1NEfsRHyqiRT4gXRoPdJfgSWB7nIGQ9Qvc8az6JQs4c7dV0wsMhUQ00XaewIDAQAB"
}
