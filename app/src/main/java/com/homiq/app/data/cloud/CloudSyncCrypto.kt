package com.homiq.app.data.cloud

import android.util.Base64
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CloudSyncCrypto {
    fun encrypt(
        rawJson: String,
        keyBase64: String,
        type: CloudSyncEntityType,
        entityId: String,
        revision: Long,
    ): String {
        val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
        require(keyBytes.size == KEY_SIZE_BYTES) { "Invalid cloud key size." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
        )
        cipher.updateAAD(aad(type, entityId, revision))
        val ciphertext = cipher.doFinal(rawJson.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == IV_SIZE_BYTES) { "Unexpected IV size." }

        val envelope = ByteBuffer.allocate(MAGIC.size + 1 + IV_SIZE_BYTES + ciphertext.size)
            .put(MAGIC)
            .put(FORMAT_VERSION)
            .put(iv)
            .put(ciphertext)
            .array()

        return Base64.encodeToString(envelope, Base64.NO_WRAP)
    }

    fun decrypt(
        payloadBase64: String,
        keyBase64: String,
        type: CloudSyncEntityType,
        entityId: String,
        revision: Long,
    ): String {
        val envelope = Base64.decode(payloadBase64, Base64.DEFAULT)
        require(envelope.size > MAGIC.size + 1 + IV_SIZE_BYTES) { "Cloud sync payload is too small." }

        val buffer = ByteBuffer.wrap(envelope)
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        require(magic.contentEquals(MAGIC)) { "Invalid cloud sync magic." }

        val version = buffer.get()
        require(version == FORMAT_VERSION) { "Unsupported cloud sync payload version." }

        val iv = ByteArray(IV_SIZE_BYTES)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
        require(keyBytes.size == KEY_SIZE_BYTES) { "Invalid cloud key size." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        cipher.updateAAD(aad(type, entityId, revision))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    fun contentSha256(rawJson: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawJson.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun aad(
        type: CloudSyncEntityType,
        entityId: String,
        revision: Long,
    ): ByteArray =
        "homika-cloud-sync-v1|${type.wireName}|$entityId|$revision"
            .toByteArray(Charsets.UTF_8)

    private const val KEY_SIZE_BYTES = 32
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'M'.code.toByte(), 'S'.code.toByte(), 'Y'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
}
