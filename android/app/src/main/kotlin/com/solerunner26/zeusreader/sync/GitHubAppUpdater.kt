package com.solerunner26.zeusreader.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.solerunner26.zeusreader.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: String
)

/**
 * Checks GitHub Releases (solerunner26/ZeusReader) for newer private APK builds
 * and downloads + triggers in-place installation only upon explicit user action.
 */
@Singleton
class GitHubAppUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "ZeusReader/$currentVersion")
            }
            try {
                if (conn.responseCode == 404) return@runCatching null
                if (conn.responseCode !in 200..299) {
                    error("GitHub check failed (HTTP ${conn.responseCode})")
                }
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                parseLatestReleaseJson(raw, currentVersion)
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun downloadAndInstallUpdate(
        update: AppUpdateInfo,
        onProgressPercent: (Int) -> Unit = {}
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(permIntent)
                return@runCatching false
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val tempApk = File(updatesDir, "zeusreader-update.apk.tmp")
            val finalApk = File(updatesDir, "zeusreader-update.apk")

            val conn = (URL(update.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ZeusReader/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    error("APK download failed (HTTP ${conn.responseCode})")
                }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.apkSizeBytes
                var downloaded = 0L
                conn.inputStream.use { input ->
                    FileOutputStream(tempApk).use { output ->
                        val buf = ByteArray(16_384)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgressPercent(((downloaded * 100L) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            if (finalApk.exists()) finalApk.delete()
            if (!tempApk.renameTo(finalApk)) {
                tempApk.copyTo(finalApk, overwrite = true)
                tempApk.delete()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                finalApk
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true
        }
    }

    companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/solerunner26/ZeusReader/releases/latest"
        private val json = Json { ignoreUnknownKeys = true }

        @Serializable
        private data class GitHubAssetDto(
            val name: String = "",
            val size: Long = 0L,
            val browser_download_url: String = ""
        )

        @Serializable
        private data class GitHubReleaseDto(
            val tag_name: String = "",
            val name: String = "",
            val body: String = "",
            val draft: Boolean = false,
            val published_at: String = "",
            val assets: List<GitHubAssetDto> = emptyList()
        )

        internal fun parseLatestReleaseJson(rawJson: String, currentVersion: String): AppUpdateInfo? {
            val rel = json.decodeFromString(GitHubReleaseDto.serializer(), rawJson)
            if (rel.draft) return null
            val tag = rel.tag_name.trim()
            if (tag.isBlank() || !isNewerVersion(tag, currentVersion)) return null

            val apkAsset = rel.assets.firstOrNull {
                it.name.lowercase().endsWith(".apk") && it.browser_download_url.isNotBlank()
            } ?: return null

            return AppUpdateInfo(
                tagName = tag,
                title = rel.name.ifBlank { tag },
                releaseNotes = rel.body.ifBlank { "Bug fixes and improvements." },
                apkDownloadUrl = apkAsset.browser_download_url,
                apkSizeBytes = apkAsset.size,
                publishedAt = rel.published_at.take(10)
            )
        }

        internal fun parseSemVer(version: String): Triple<Int, Int, Int>? {
            val match = Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(version) ?: return null
            val (major, minor, patch) = match.destructured
            return Triple(major.toInt(), minor.toInt(), patch.toInt())
        }

        internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
            val remote = parseSemVer(remoteTag) ?: return false
            val current = parseSemVer(currentVersion) ?: return false
            if (remote.first != current.first) return remote.first > current.first
            if (remote.second != current.second) return remote.second > current.second
            return remote.third > current.third
        }
    }
}
