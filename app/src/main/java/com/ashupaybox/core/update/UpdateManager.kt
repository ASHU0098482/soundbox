package com.ashupaybox.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.ashupaybox.core.preferences.PayBoxPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class InstallResult {
    object Launched : InstallResult()
    data class PermissionRequired(val settingsIntent: Intent) : InstallResult()
    data class Error(val message: String) : InstallResult()
}

class UpdateManager(
    private val context: Context,
    private val preferences: PayBoxPreferences,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "UPDATE_MANAGER"
    }

    private val updateService = UpdateService(context)

    private val _updateCheckState = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckState: StateFlow<UpdateCheckResult?> = _updateCheckState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _currentChannel = MutableStateFlow(UpdateChannel.STABLE)
    val currentChannel: StateFlow<UpdateChannel> = _currentChannel.asStateFlow()

    fun setChannel(channel: UpdateChannel) {
        _currentChannel.value = channel
    }

    /**
     * Executes non-blocking update check.
     */
    fun checkForUpdates(isManual: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            val installedCode = getInstalledVersionCode()
            val installedName = getInstalledVersionName()

            Log.d(TAG, "Checking for updates (installed: $installedName ($installedCode), channel: ${_currentChannel.value})")
            val result = updateService.checkForUpdates(
                installedVersionCode = installedCode,
                installedVersionName = installedName,
                channel = _currentChannel.value
            )
            _updateCheckState.value = result
        }
    }

    fun dismissUpdateDialog() {
        _updateCheckState.value = null
    }

    /**
     * Downloads the APK with progress and triggers verification.
     */
    fun startDownload(updateInfo: AppUpdateInfo) {
        scope.launch(Dispatchers.IO) {
            val installedCode = getInstalledVersionCode()
            updateService.downloadApk(updateInfo, installedCode).collect { progress ->
                _downloadProgress.value = progress
            }
        }
    }

    fun cancelDownload() {
        updateService.cancelDownload()
        _downloadProgress.value = DownloadProgress(status = DownloadStatus.CANCELLED)
    }

    /**
     * Initiates the standard Android PackageInstaller flow with FileProvider.
     */
    fun launchInstaller(apkFile: File): InstallResult {
        try {
            // Check unknown source install permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    return InstallResult.PermissionRequired(settingsIntent)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return InstallResult.Launched
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer", e)
            return InstallResult.Error("Could not launch package installer: ${e.localizedMessage}")
        }
    }

    fun getInstalledVersionCode(): Long {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getInstalledVersionName(): String {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            pi.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
