package com.ashupaybox.data.repository

import android.content.Context
import android.os.Build
import com.ashupaybox.app.BuildConfig
import com.ashupaybox.core.fcm.FcmTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class DeviceRegistration(
    val installationId: String,
    val fcmToken: String?,
    val platform: String = "ANDROID",
    val appVersion: String,
    val versionCode: Int,
    val deviceModel: String,
    val androidVersion: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class BackendSyncStatus {
    NOT_CONNECTED_PHASE_3,
    REGISTERED,
    FAILED
}

class DeviceRegistrationRepository(
    private val context: Context,
    private val tokenManager: FcmTokenManager
) {
    companion object {
        private const val PREFS_NAME = "paybox_device_prefs"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val installationId: String
        get() {
            var id = prefs.getString(KEY_INSTALLATION_ID, null)
            if (id.isNullOrEmpty()) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_INSTALLATION_ID, id).apply()
            }
            return id
        }

    private val _syncStatusFlow = MutableStateFlow(BackendSyncStatus.NOT_CONNECTED_PHASE_3)
    val syncStatusFlow: StateFlow<BackendSyncStatus> = _syncStatusFlow.asStateFlow()

    fun getDeviceRegistration(): DeviceRegistration {
        return DeviceRegistration(
            installationId = installationId,
            fcmToken = tokenManager.currentToken,
            platform = "ANDROID",
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.SDK_INT,
            lastSeen = System.currentTimeMillis()
        )
    }

    /**
     * PHASE 3 PLACEHOLDER:
     * When Phase 3 secure website backend is deployed, this method will make
     * an authenticated POST request to: /api/device/register
     */
    suspend fun syncWithBackend(): Result<Unit> {
        // In Phase 2, backend registration remains intentionally offline / pending Phase 3 backend
        return Result.success(Unit)
    }
}
