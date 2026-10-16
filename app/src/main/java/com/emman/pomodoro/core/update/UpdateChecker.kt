package com.emman.pomodoro.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val apkUrl: String?
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GhAsset> = emptyList()
)

@Serializable
private data class GhAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = ""
)

@Singleton
class UpdateChecker @Inject constructor() {

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(RELEASES_API).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TIMEOUT_MS
                    connection.readTimeout = TIMEOUT_MS
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.setRequestProperty("User-Agent", "Pomodoro-Android")
                    if (connection.responseCode !in 200..299) return@runCatching UpdateCheckResult.Failed
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val info = parseLatestRelease(body, currentVersion)
                    if (info != null) UpdateCheckResult.Available(info) else UpdateCheckResult.UpToDate
                } finally {
                    runCatching { connection.disconnect() }
                }
            }.getOrElse { UpdateCheckResult.Failed }
        }

    companion object {
        private const val RELEASES_API =
            "https://api.github.com/repos/MREmmanuel09/pomodoro/releases/latest"
        private const val TIMEOUT_MS = 10_000

        private val json = Json { ignoreUnknownKeys = true }

        fun parseLatestRelease(body: String, currentVersion: String): UpdateInfo? {
            val release = runCatching { json.decodeFromString<GhRelease>(body) }.getOrNull()
                ?: return null
            val latest = release.tagName.removePrefix("v").trim()
            if (latest.isBlank()) return null
            if (!isNewer(latest, currentVersion)) return null
            val apkUrl = release.assets
                .firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?.downloadUrl
                ?.takeIf { it.isNotBlank() }
            return UpdateInfo(version = latest, releaseUrl = release.htmlUrl, apkUrl = apkUrl)
        }

        fun isNewer(latest: String, current: String): Boolean {
            val latestParts = parseVersion(latest) ?: return false
            val currentParts = parseVersion(current) ?: return false
            for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l != c) return l > c
            }
            return false
        }

        private fun parseVersion(version: String): List<Int>? {
            if (version.isBlank()) return null
            val parts = version.split('.')
            return parts.map { it.toIntOrNull() ?: return null }
        }
    }
}
