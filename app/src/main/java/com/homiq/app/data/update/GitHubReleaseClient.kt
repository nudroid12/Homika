package com.homiq.app.data.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal class GitHubReleaseClient(
    private val latestReleaseUrl: String = LATEST_RELEASE_URL,
) {
    fun latestRelease(): HomikaRelease {
        val connection = open(latestReleaseUrl)
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw UpdateClientException(UpdateFailureReason.NETWORK)
            }

            val payload = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            parseRelease(payload)
        } catch (error: UpdateClientException) {
            throw error
        } catch (error: Exception) {
            throw UpdateClientException(UpdateFailureReason.NETWORK, error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(payload: String): HomikaRelease {
        return try {
            val json = JSONObject(payload)
            val tagName = json.optString("tag_name").trim()
            val versionName = VersionComparator.normalize(tagName)
            val notes = json.optString("body").trim()
            val assets = json.optJSONArray("assets")
                ?: throw UpdateClientException(UpdateFailureReason.RELEASE_INVALID)

            val apkCandidates = buildList {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name").trim()
                    val url = asset.optString("browser_download_url").trim()
                    if (!name.endsWith(".apk", ignoreCase = true) || url.isBlank()) continue
                    add(
                        ApkCandidate(
                            name = name,
                            url = url,
                            size = asset.optLong("size", 0L),
                        ),
                    )
                }
            }

            val selected = apkCandidates
                .sortedByDescending(::scoreAsset)
                .firstOrNull()
                ?: throw UpdateClientException(UpdateFailureReason.RELEASE_INVALID)

            if (tagName.isBlank() || versionName.isBlank()) {
                throw UpdateClientException(UpdateFailureReason.RELEASE_INVALID)
            }

            HomikaRelease(
                tagName = tagName,
                versionName = versionName,
                notes = notes,
                apkName = selected.name,
                downloadUrl = selected.url,
                sizeBytes = selected.size,
            )
        } catch (error: UpdateClientException) {
            throw error
        } catch (error: Exception) {
            throw UpdateClientException(UpdateFailureReason.RELEASE_INVALID, error)
        }
    }

    private fun scoreAsset(candidate: ApkCandidate): Int {
        val name = candidate.name.lowercase()
        var score = 0
        if ("homika" in name) score += 8
        if ("release" in name) score += 4
        if ("universal" in name) score += 2
        if ("debug" in name) score -= 20
        return score
    }

    private fun open(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Homika-Android-Updater")
        }
    }

    private data class ApkCandidate(
        val name: String,
        val url: String,
        val size: Long,
    )

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/nudroid12/Homika/releases/latest"
    }
}

internal class UpdateClientException(
    val reason: UpdateFailureReason,
    cause: Throwable? = null,
) : Exception(cause)
