package com.homiq.app.data.license

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object LicenseDeviceIdentity {
    fun fingerprint(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )

        val stableDeviceValue = androidId
            ?.takeIf { it.isNotBlank() }
            ?: fallbackInstallId(context)

        return sha256(
            "${context.packageName}|$stableDeviceValue|homika-pro-license-v1",
        )
    }

    fun displayName(): String {
        val manufacturer = Build.MANUFACTURER
            .orEmpty()
            .trim()
            .replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty().trim()

        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { "Android device" }
            .take(120)
    }

    private fun fallbackInstallId(context: Context): String {
        val prefs = context.getSharedPreferences(
            "homika_pro_device_identity",
            Context.MODE_PRIVATE,
        )
        val current = prefs.getString("fallback_id", null)
        if (!current.isNullOrBlank()) return current

        val created = UUID.randomUUID().toString()
        prefs.edit().putString("fallback_id", created).apply()
        return created
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}
