package com.ashupaybox.core.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class UpdateService(private val context: Context) {

    companion object {
        private const val TAG = "UPDATE_SERVICE"
    }

    private val isCancelled = AtomicBoolean(false)

    /**
     * Checks GitHub Releases for a newer version.
     */
    suspend fun checkForUpdates(
        installedVersionCode: Long,
        installedVersionName: String,
        channel: UpdateChannel
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val urlString = if (channel == UpdateChannel.BETA) {
                UpdateConfig.ALL_RELEASES_API_URL
            } else {
                UpdateConfig.LATEST_RELEASE_API_URL
            }

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "AshuPayBox-Updater/$installedVersionName")
                connectTimeout = UpdateConfig.CONNECT_TIMEOUT_MS
                readTimeout = UpdateConfig.READ_TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "GitHub API returned code $responseCode: $errorBody")
                return@withContext UpdateCheckResult.Error("GitHub returned response code: $responseCode")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = if (channel == UpdateChannel.BETA) {
                val array = JSONArray(responseBody)
                if (array.length() == 0) null else array.getJSONObject(0)
            } else {
                JSONObject(responseBody)
            }

            if (releaseObj == null) {
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }

            val isDraft = releaseObj.optBoolean("draft", false)
            if (isDraft) {
                // Drafts must never be installed
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }

            val isPrerelease = releaseObj.optBoolean("prerelease", false)
            if (isPrerelease && channel != UpdateChannel.BETA) {
                // Ignore prereleases on stable channel
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }

            val tagName = releaseObj.optString("tag_name", "").removePrefix("v").trim()
            val releaseName = releaseObj.optString("name", tagName)
            val changelog = releaseObj.optString("body", "General improvements and bug fixes.")
            val publishedAt = releaseObj.optString("published_at", "")

            // Locate production APK asset
            val assetsArray = releaseObj.optJSONArray("assets")
            var apkAsset: JSONObject? = null
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkAsset = asset
                        break
                    }
                }
            }

            if (apkAsset == null) {
                Log.w(TAG, "Release $tagName does not have an attached production APK asset")
                return@withContext UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }

            val downloadUrl = apkAsset.optString("browser_download_url", "")
            val assetName = apkAsset.optString("name", "AshuPayBox.apk")
            val assetSize = apkAsset.optLong("size", 0L)

            // Parse versionCode: Look for embedded pattern <!-- versionCode: 12 --> or asset name or fallback from tag
            val remoteVersionCode = parseVersionCode(changelog, assetName, tagName)
            val isForceUpdate = changelog.contains("forceUpdate: true", ignoreCase = true) ||
                    changelog.contains("force_update: true", ignoreCase = true)

            if (remoteVersionCode > installedVersionCode) {
                val updateInfo = AppUpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = tagName.ifBlank { releaseName },
                    downloadUrl = downloadUrl,
                    apkFileName = assetName,
                    apkSizeBytes = assetSize,
                    changelog = cleanChangelog(changelog),
                    isForceUpdate = isForceUpdate,
                    publishedAt = publishedAt,
                    isPrerelease = isPrerelease
                )
                UpdateCheckResult.UpdateAvailable(
                    updateInfo = updateInfo,
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            } else {
                UpdateCheckResult.UpToDate(
                    currentVersion = installedVersionName,
                    currentVersionCode = installedVersionCode.toInt()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates from GitHub", e)
            UpdateCheckResult.Error("Failed to check for updates: ${e.localizedMessage}", e)
        }
    }

    /**
     * Streams APK download with live progress.
     */
    fun downloadApk(
        updateInfo: AppUpdateInfo,
        installedVersionCode: Long
    ): Flow<DownloadProgress> = flow {
        isCancelled.set(false)
        emit(DownloadProgress(status = DownloadStatus.DOWNLOADING, totalBytes = updateInfo.apkSizeBytes))

        val updatesDir = File(context.cacheDir, "exports/updates").apply { mkdirs() }
        val targetFile = File(updatesDir, updateInfo.apkFileName)
        if (targetFile.exists()) targetFile.delete()

        var connection: HttpURLConnection? = null
        try {
            val url = URL(updateInfo.downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "AshuPayBox-Updater")
                connectTimeout = UpdateConfig.CONNECT_TIMEOUT_MS
                readTimeout = UpdateConfig.READ_TIMEOUT_MS
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else updateInfo.apkSizeBytes
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isCancelled.get()) {
                            targetFile.delete()
                            emit(DownloadProgress(status = DownloadStatus.CANCELLED))
                            return@flow
                        }
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        val fraction = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
                        emit(
                            DownloadProgress(
                                status = DownloadStatus.DOWNLOADING,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                progressFraction = fraction
                            )
                        )
                    }
                }
            }

            // Verify Security
            emit(DownloadProgress(status = DownloadStatus.VERIFYING, bytesDownloaded = bytesDownloaded, totalBytes = totalBytes, progressFraction = 1f))
            val verification = UpdateSecurityValidator.verifyDownloadedApk(
                context = context,
                apkFile = targetFile,
                expectedSha256 = updateInfo.sha256,
                installedVersionCode = installedVersionCode
            )

            when (verification) {
                is ValidationResult.Success -> {
                    emit(
                        DownloadProgress(
                            status = DownloadStatus.READY_TO_INSTALL,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            progressFraction = 1f,
                            downloadedFile = targetFile
                        )
                    )
                }
                is ValidationResult.Failed -> {
                    emit(
                        DownloadProgress(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Update security verification failed: ${verification.reason}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            targetFile.delete()
            Log.e(TAG, "Download error", e)
            emit(
                DownloadProgress(
                    status = DownloadStatus.FAILED,
                    errorMessage = "Download failed: ${e.localizedMessage}"
                )
            )
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    fun cancelDownload() {
        isCancelled.set(true)
    }

    private fun parseVersionCode(changelog: String, assetName: String, tagName: String): Int {
        // 1. Try to find <!-- versionCode: 15 --> in changelog
        val codeRegex = Regex("<!--\\s*versionCode:\\s*(\\d+)\\s*-->")
        codeRegex.find(changelog)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // 2. Try to find -b15 or -c15 or -v15 in asset name
        val assetRegex = Regex("[_-][bc](\\d+)", RegexOption.IGNORE_CASE)
        assetRegex.find(assetName)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // 3. Fallback: Parse major.minor.patch as (major * 10000 + minor * 100 + patch)
        val tagDigits = tagName.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        if (tagDigits.isNotEmpty()) {
            val major = tagDigits.getOrElse(0) { 0 }
            val minor = tagDigits.getOrElse(1) { 0 }
            val patch = tagDigits.getOrElse(2) { 0 }
            return (major * 10000) + (minor * 100) + patch
        }

        return 1
    }

    private fun cleanChangelog(raw: String): String {
        return raw.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "").trim()
    }
}
