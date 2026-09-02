package com.homiq.app.data.cloud

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CloudBackupCrypto {
    fun encrypt(
        rawJson: String,
        keyBase64: String,
    ): ByteArray {
        val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
        require(keyBytes.size == KEY_SIZE_BYTES) { "Invalid cloud key size." }

        val compressed = gzip(rawJson.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
        )
        cipher.updateAAD(AAD)
        val ciphertext = cipher.doFinal(compressed)
        val iv = cipher.iv
        require(iv.size == IV_SIZE_BYTES) { "Unexpected IV size." }

        return ByteBuffer.allocate(MAGIC.size + 1 + IV_SIZE_BYTES + ciphertext.size)
            .put(MAGIC)
            .put(FORMAT_VERSION)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun decrypt(
        encrypted: ByteArray,
        keyBase64: String,
    ): String {
        require(encrypted.size > MAGIC.size + 1 + IV_SIZE_BYTES) {
            "Cloud backup is too small."
        }

        val buffer = ByteBuffer.wrap(encrypted)
        val magic = ByteArray(MAGIC.size)
        buffer.get(magic)
        require(magic.contentEquals(MAGIC)) { "Invalid cloud backup magic." }

        val version = buffer.get()
        require(version == FORMAT_VERSION) { "Unsupported cloud backup version." }

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
        cipher.updateAAD(AAD)
        val compressed = cipher.doFinal(ciphertext)
        return ungzip(compressed).toString(Charsets.UTF_8)
    }

    private fun gzip(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(input) }
        return output.toByteArray()
    }

    private fun ungzip(input: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(input)).use { it.readBytes() }

    private const val KEY_SIZE_BYTES = 32
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'M'.code.toByte(), 'C'.code.toByte(), 'B'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private val AAD = "homika-cloud-backup-v1".toByteArray(Charsets.UTF_8)
}
