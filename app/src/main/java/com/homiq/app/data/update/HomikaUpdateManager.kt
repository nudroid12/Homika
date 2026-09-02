package com.homiq.app.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.homiq.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class HomikaUpdateManager(
    context: Context,
) {
    private val releaseClient = GitHubReleaseClient()
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private var activeJob: Job? = null

    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun checkForUpdates(manual: Boolean) {
        if (activeJob?.isActive == true) return
        if (!manual && !isAutomaticCheckDue()) return

        activeJob = scope.launch {
            _state.value = UpdateState.Checking(manual = manual)
            try {
                val release = releaseClient.latestRelease()
                preferences.edit()
                    .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                    .apply()

                if (VersionComparator.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                    _state.value = UpdateState.Available(release)
                } else {
                    _state.value = if (manual) {
                        UpdateState.UpToDate(release.versionName)
                    } else {
                        UpdateState.Idle
                    }
                }
            } catch (error: UpdateClientException) {
                _state.value = if (manual) {
                    UpdateState.Error(error.reason)
                } else {
                    UpdateState.Idle
                }
            } catch (error: Exception) {
                _state.value = if (manual) {
                    UpdateState.Error(UpdateFailureReason.NETWORK)
                } else {
                    UpdateState.Idle
                }
            }
        }
    }

    fun download(release: HomikaRelease) {
        if (activeJob?.isActive == true) return
        activeJob = scope.launch {
            val updateDir = File(appContext.cacheDir, UPDATE_DIR).apply { mkdirs() }
            val tempFile = File(updateDir, "download.tmp")
            val finalFile = File(updateDir, sanitizeApkName(release.apkName))
            tempFile.delete()
            finalFile.delete()

            try {
                downloadToFile(release, tempFile)
                if (release.sizeBytes > 0 && tempFile.length() != release.sizeBytes) {
                    throw UpdateClientException(UpdateFailureReason.DOWNLOAD_FAILED)
                }
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
                verifyDownloadedApk(finalFile)
                _state.value = UpdateState.Ready(release, finalFile)
            } catch (error: UpdateClientException) {
                tempFile.delete()
                finalFile.delete()
                _state.value = UpdateState.Error(error.reason)
            } catch (error: Exception) {
                tempFile.delete()
                finalFile.delete()
                _state.value = UpdateState.Error(UpdateFailureReason.DOWNLOAD_FAILED)
            }
        }
    }

    fun install() {
        val prepared = when (val current = _state.value) {
            is UpdateState.Ready -> current
            is UpdateState.PermissionRequired -> UpdateState.Ready(current.release, current.apk)
            else -> return
        }

        if (!appContext.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.PermissionRequired(prepared.release, prepared.apk)
            return
        }

        _state.value = UpdateState.Installing(prepared.release)
        scope.launch {
            try {
                commitInstall(prepared.apk)
            } catch (error: Exception) {
                _state.value = UpdateState.Error(UpdateFailureReason.INSTALL_FAILED)
            }
        }
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
            .onFailure {
                _state.value = UpdateState.Error(UpdateFailureReason.INSTALL_FAILED)
            }
    }

    fun onAppForeground() {
        val current = _state.value
        if (
            current is UpdateState.PermissionRequired &&
            appContext.packageManager.canRequestPackageInstalls()
        ) {
            _state.value = UpdateState.Ready(current.release, current.apk)
        }
    }

    fun onInstallStatus(
        status: Int,
        message: String?,
    ) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> _state.value = UpdateState.Idle
            PackageInstaller.STATUS_PENDING_USER_ACTION -> Unit
            else -> {
                @Suppress("UNUSED_VARIABLE")
                val ignoredMessage = message
                _state.value = UpdateState.Error(UpdateFailureReason.INSTALL_FAILED)
            }
        }
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading && _state.value !is UpdateState.Installing) {
            _state.value = UpdateState.Idle
        }
    }

    private fun isAutomaticCheckDue(): Boolean {
        val lastCheck = preferences.getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - lastCheck >= AUTO_CHECK_INTERVAL_MS
    }

    private fun downloadToFile(
        release: HomikaRelease,
        destination: File,
    ) {
        val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "Homika-Android-Updater")
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw UpdateClientException(UpdateFailureReason.DOWNLOAD_FAILED)
            }

            val expected = when {
                release.sizeBytes > 0 -> release.sizeBytes
                connection.contentLengthLong > 0 -> connection.contentLengthLong
                else -> 0L
            }
            var copied = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (expected > 0) {
                            _state.value = UpdateState.Downloading(
                                release = release,
                                progress = (copied.toDouble() / expected.toDouble())
                                    .toFloat()
                                    .coerceIn(0f, 1f),
                            )
                        } else {
                            _state.value = UpdateState.Downloading(release, 0f)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyDownloadedApk(apk: File) {
        val packageManager = appContext.packageManager
        val archive = packageInfoFromArchive(packageManager, apk)
            ?: throw UpdateClientException(UpdateFailureReason.APK_INVALID)
        val current = packageInfoForCurrentApp(packageManager)

        if (archive.packageName != appContext.packageName) {
            throw UpdateClientException(UpdateFailureReason.APK_INVALID)
        }
        if (longVersionCode(archive) <= longVersionCode(current)) {
            throw UpdateClientException(UpdateFailureReason.APK_INVALID)
        }

        val currentCertificates = certificateDigests(current)
        val archiveCertificates = certificateDigests(archive)
        if (
            currentCertificates.isEmpty() ||
            archiveCertificates.isEmpty() ||
            currentCertificates.intersect(archiveCertificates).isEmpty()
        ) {
            throw UpdateClientException(UpdateFailureReason.SIGNATURE_MISMATCH)
        }
    }

    @Suppress("DEPRECATION")
    private fun packageInfoFromArchive(
        packageManager: PackageManager,
        apk: File,
    ): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
    }

    @Suppress("DEPRECATION")
    private fun packageInfoForCurrentApp(packageManager: PackageManager): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return packageManager.getPackageInfo(appContext.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun longVersionCode(info: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }

    @Suppress("DEPRECATION")
    private fun certificateDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.toList()
            } else {
                signingInfo.signingCertificateHistory.toList()
            }
        } else {
            info.signatures?.toList().orEmpty()
        }

        return signatures.mapTo(linkedSetOf()) { signature ->
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
            digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
        }
    }

    private fun commitInstall(apk: File) {
        val installer = appContext.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(appContext.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite(APK_SESSION_NAME, 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }

                val callbackIntent = Intent(appContext, UpdateInstallReceiver::class.java).apply {
                    action = ACTION_INSTALL_STATUS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    sessionId,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (error: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw error
        }
    }

    private fun sanitizeApkName(name: String): String {
        val sanitized = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (sanitized.endsWith(".apk", ignoreCase = true)) sanitized else "Homika-update.apk"
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.homiq.app.UPDATE_INSTALL_STATUS"
        private const val PREFS_NAME = "homika_updater"
        private const val KEY_LAST_CHECK = "last_check_ms"
        private const val UPDATE_DIR = "updates"
        private const val APK_SESSION_NAME = "Homika-update.apk"
        private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
