package com.homiq.app.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

fun signingCertificateSha1(context: Context): String? {
    return runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            ).signatures.orEmpty()
        }

        val certificate = signatures.firstOrNull()?.toByteArray()
        if (certificate == null) {
            null
        } else {
            MessageDigest.getInstance("SHA-1")
                .digest(certificate)
                .joinToString(":") { byte ->
                    "%02X".format(byte)
                }
        }
    }.getOrNull()
}
