package com.homiq.app.data.security

import java.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinHash(
    val saltBase64: String,
    val hashBase64: String,
)

object AppLockHasher {
    private const val ITERATIONS = 90_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun hashNew(pin: String): PinHash {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return PinHash(
            saltBase64 = Base64.getEncoder().withoutPadding().encodeToString(salt),
            hashBase64 = Base64.getEncoder().withoutPadding().encodeToString(derive(pin, salt)),
        )
    }

    fun verify(
        pin: String,
        saltBase64: String,
        hashBase64: String,
    ): Boolean = runCatching {
        val salt = Base64.getDecoder().decode(saltBase64)
        val expected = Base64.getDecoder().decode(hashBase64)
        val actual = derive(pin, salt)
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
