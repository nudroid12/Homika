package com.homiq.app.data.license

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LicensePreferences(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun read(): StoredLicense? {
        val encrypted = prefs.getString(KEY_SECURE_BLOB, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val raw = decrypt(encrypted) ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        val token = json.optString("activation_token").takeIf { it.isNotBlank() } ?: return null
        val expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() } ?: return null
        val expiryMillis = json.optLong("expires_at_millis", 0L)
        if (expiryMillis <= 0L) return null

        return StoredLicense(
            activationToken = token,
            licenseHint = json.optString("license_hint", "••••").ifBlank { "••••" },
            expiresAt = expiresAt,
            expiresAtEpochMillis = expiryMillis,
            maxDevices = json.optInt("max_devices", 3).coerceAtLeast(1),
            activeDevices = json.optInt("active_devices", 0).coerceAtLeast(0),
            lastValidatedAtMillis = json.optLong("last_validated_at", 0L),
            lastObservedAtMillis = json.optLong("last_observed_at", 0L),
        )
    }

    fun legacyLicenseKey(): String? =
        prefs.getString(KEY_LEGACY_LICENSE_KEY, null)
            ?.takeIf { it.isNotBlank() }

    fun saveValidated(
        activation: LicenseActivation,
        expiresAtEpochMillis: Long,
        nowMillis: Long,
    ): Boolean {
        val saved = saveStored(
            StoredLicense(
                activationToken = activation.activationToken,
                licenseHint = activation.licenseHint,
                expiresAt = activation.expiresAt,
                expiresAtEpochMillis = expiresAtEpochMillis,
                maxDevices = activation.maxDevices,
                activeDevices = activation.activeDevices,
                lastValidatedAtMillis = nowMillis,
                lastObservedAtMillis = nowMillis,
            ),
        )
        if (saved) clearLegacy()
        return saved
    }

    fun markObserved(nowMillis: Long) {
        val stored = read() ?: return
        saveStored(stored.copy(lastObservedAtMillis = nowMillis))
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SECURE_BLOB)
            .remove(KEY_LEGACY_LICENSE_KEY)
            .remove(KEY_LEGACY_EXPIRES_AT)
            .remove(KEY_LEGACY_EXPIRES_AT_MILLIS)
            .remove(KEY_LEGACY_MAX_DEVICES)
            .remove(KEY_LEGACY_ACTIVE_DEVICES)
            .remove(KEY_LEGACY_LAST_VALIDATED_AT)
            .remove(KEY_LEGACY_LAST_OBSERVED_AT)
            .apply()
    }

    private fun clearLegacy() {
        prefs.edit()
            .remove(KEY_LEGACY_LICENSE_KEY)
            .remove(KEY_LEGACY_EXPIRES_AT)
            .remove(KEY_LEGACY_EXPIRES_AT_MILLIS)
            .remove(KEY_LEGACY_MAX_DEVICES)
            .remove(KEY_LEGACY_ACTIVE_DEVICES)
            .remove(KEY_LEGACY_LAST_VALIDATED_AT)
            .remove(KEY_LEGACY_LAST_OBSERVED_AT)
            .apply()
    }

    private fun saveStored(stored: StoredLicense): Boolean {
        val json = JSONObject()
            .put("activation_token", stored.activationToken)
            .put("license_hint", stored.licenseHint)
            .put("expires_at", stored.expiresAt)
            .put("expires_at_millis", stored.expiresAtEpochMillis)
            .put("max_devices", stored.maxDevices)
            .put("active_devices", stored.activeDevices)
            .put("last_validated_at", stored.lastValidatedAtMillis)
            .put("last_observed_at", stored.lastObservedAtMillis)

        val encrypted = encrypt(json.toString()) ?: return false
        return prefs.edit().putString(KEY_SECURE_BLOB, encrypted).commit()
    }

    private fun encrypt(value: String): String? =
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(cipher.iv.size + ciphertext.size)
            System.arraycopy(cipher.iv, 0, combined, 0, cipher.iv.size)
            System.arraycopy(ciphertext, 0, combined, cipher.iv.size, ciphertext.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrNull()

    private fun decrypt(value: String): String? =
        runCatching {
            val combined = Base64.decode(value, Base64.DEFAULT)
            if (combined.size <= IV_SIZE_BYTES) return@runCatching null

            val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
            val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        ).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "homika_pro_license"
        const val KEY_SECURE_BLOB = "secure_activation_v2"

        const val KEY_LEGACY_LICENSE_KEY = "license_key"
        const val KEY_LEGACY_EXPIRES_AT = "expires_at"
        const val KEY_LEGACY_EXPIRES_AT_MILLIS = "expires_at_millis"
        const val KEY_LEGACY_MAX_DEVICES = "max_devices"
        const val KEY_LEGACY_ACTIVE_DEVICES = "active_devices"
        const val KEY_LEGACY_LAST_VALIDATED_AT = "last_validated_at"
        const val KEY_LEGACY_LAST_OBSERVED_AT = "last_observed_at"

        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "homika_pro_license_store_v2"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
