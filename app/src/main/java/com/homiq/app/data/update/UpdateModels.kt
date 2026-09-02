package com.homiq.app.data.update

import java.io.File

data class HomikaRelease(
    val tagName: String,
    val versionName: String,
    val notes: String,
    val apkName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

enum class UpdateFailureReason {
    NETWORK,
    RELEASE_INVALID,
    DOWNLOAD_FAILED,
    APK_INVALID,
    SIGNATURE_MISMATCH,
    INSTALL_FAILED,
}

sealed interface UpdateState {
    data object Idle : UpdateState

    data class Checking(
        val manual: Boolean,
    ) : UpdateState

    data class Available(
        val release: HomikaRelease,
    ) : UpdateState

    data class Downloading(
        val release: HomikaRelease,
        val progress: Float,
    ) : UpdateState

    data class Ready(
        val release: HomikaRelease,
        val apk: File,
    ) : UpdateState

    data class PermissionRequired(
        val release: HomikaRelease,
        val apk: File,
    ) : UpdateState

    data class Installing(
        val release: HomikaRelease,
    ) : UpdateState

    data class UpToDate(
        val latestVersion: String,
    ) : UpdateState

    data class Error(
        val reason: UpdateFailureReason,
    ) : UpdateState
}

object VersionComparator {
    private val versionRegex = Regex("""\d+(?:\.\d+){0,3}""")

    fun normalize(value: String): String {
        return versionRegex.find(value)?.value ?: value.trim().removePrefix("v").removePrefix("V")
    }

    fun isNewer(
        latest: String,
        current: String,
    ): Boolean {
        val latestParts = numericParts(latest)
        val currentParts = numericParts(current)
        val width = maxOf(latestParts.size, currentParts.size)

        for (index in 0 until width) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) {
                return latestPart > currentPart
            }
        }
        return false
    }

    private fun numericParts(value: String): List<Int> {
        val normalized = normalize(value)
        return normalized
            .split('.')
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0) }
    }
}
