package com.ashupaybox.core.update

import java.io.File

enum class UpdateChannel(val displayName: String) {
    STABLE("Stable"),
    BETA("Beta")
}

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val minimumSupportedVersionCode: Int = 1,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val sha256: String? = null,
    val changelog: String = "",
    val isForceUpdate: Boolean = false,
    val publishedAt: String = "",
    val isPrerelease: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpToDate(
        val currentVersion: String,
        val currentVersionCode: Int,
        val lastCheckedAt: Long = System.currentTimeMillis()
    ) : UpdateCheckResult()

    data class UpdateAvailable(
        val updateInfo: AppUpdateInfo,
        val currentVersion: String,
        val currentVersionCode: Int
    ) : UpdateCheckResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UpdateCheckResult()
}

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    VERIFYING,
    READY_TO_INSTALL,
    FAILED,
    CANCELLED
}

data class DownloadProgress(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progressFraction: Float = 0f,
    val downloadedFile: File? = null,
    val errorMessage: String? = null
)
