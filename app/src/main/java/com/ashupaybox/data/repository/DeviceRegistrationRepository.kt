package com.ashupaybox.data.repository

import android.content.Context
import android.os.Build
import com.ashupaybox.app.BuildConfig
import com.ashupaybox.core.fcm.FcmTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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
    PAIRING_REQUIRED,
    CONNECTING,
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
        private const val KEY_DEVICE_TOKEN = "soundbox_device_token"
        private const val KEY_LAST_REGISTERED_AT = "soundbox_registered_at"
        private const val KEY_LAST_ERROR = "soundbox_last_error"
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

    private val _lastErrorFlow = MutableStateFlow(prefs.getString(KEY_LAST_ERROR, "") ?: "")
    val lastErrorFlow: StateFlow<String> = _lastErrorFlow.asStateFlow()

    val backendUrl: String
        get() = BuildConfig.PAYBOX_BACKEND_URL.trim().trimEnd('/')

    val isPaired: Boolean
        get() = !prefs.getString(KEY_DEVICE_TOKEN, null).isNullOrBlank()

    val lastRegisteredAt: Long
        get() = prefs.getLong(KEY_LAST_REGISTERED_AT, 0L)

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
     * PHASE 3:
     * Registers this Android device's current FCM token with the secure backend
     * so verified Razorpay webhook events can be sent through FCM.
     */
    suspend fun syncWithBackend(pairingCode: String? = null): Result<Unit> {
        val baseUrl = backendUrl
        if (baseUrl.isEmpty()) {
            _syncStatusFlow.value = BackendSyncStatus.NOT_CONNECTED_PHASE_3
            return Result.success(Unit)
        }

        val registration = getDeviceRegistration()
        if (registration.fcmToken.isNullOrBlank()) {
            _syncStatusFlow.value = BackendSyncStatus.FAILED
            return Result.failure(IllegalStateException("FCM token is not available yet"))
        }

        val savedDeviceToken = prefs.getString(KEY_DEVICE_TOKEN, null)?.trim().orEmpty()
        val cleanPairingCode = pairingCode?.trim().orEmpty()
        if (savedDeviceToken.isEmpty() && cleanPairingCode.isEmpty()) {
            _syncStatusFlow.value = BackendSyncStatus.PAIRING_REQUIRED
            return Result.failure(IllegalStateException("Pairing code required"))
        }

        _syncStatusFlow.value = BackendSyncStatus.CONNECTING

        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL("$baseUrl/api/soundbox/device/register").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    if (savedDeviceToken.isNotEmpty()) {
                        setRequestProperty("Authorization", "Bearer $savedDeviceToken")
                    }
                }

                val payload = JSONObject().apply {
                    if (savedDeviceToken.isEmpty()) {
                        put("pairingCode", cleanPairingCode)
                    }
                    put("installationId", registration.installationId)
                    put("fcmToken", registration.fcmToken)
                    put("platform", registration.platform)
                    put("appVersion", registration.appVersion)
                    put("versionCode", registration.versionCode)
                    put("deviceModel", registration.deviceModel)
                    put("androidVersion", registration.androidVersion)
                    put("lastSeen", registration.lastSeen)
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }

                val responseCode = connection.responseCode
                val responseBody = runCatching {
                    val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    stream?.use { InputStreamReader(it, Charsets.UTF_8).readText() }.orEmpty()
                }.getOrDefault("")
                connection.disconnect()
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Device registration failed with HTTP $responseCode")
                }
                if (savedDeviceToken.isEmpty()) {
                    val responseJson = JSONObject(responseBody)
                    val deviceToken = responseJson.optString("deviceToken")
                    if (deviceToken.isBlank()) {
                        throw IllegalStateException("Backend did not return a device token")
                    }
                    prefs.edit().putString(KEY_DEVICE_TOKEN, deviceToken).apply()
                }
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(KEY_LAST_REGISTERED_AT, now)
                    .putString(KEY_LAST_ERROR, "")
                    .apply()
                _lastErrorFlow.value = ""
                _syncStatusFlow.value = BackendSyncStatus.REGISTERED
            }.onFailure {
                val message = it.message ?: "Backend connection failed"
                prefs.edit().putString(KEY_LAST_ERROR, message).apply()
                _lastErrorFlow.value = message
                _syncStatusFlow.value = BackendSyncStatus.FAILED
            }
        }
    }

    fun disconnect() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_LAST_REGISTERED_AT)
            .remove(KEY_LAST_ERROR)
            .apply()
        _lastErrorFlow.value = ""
        _syncStatusFlow.value = BackendSyncStatus.PAIRING_REQUIRED
    }
}
