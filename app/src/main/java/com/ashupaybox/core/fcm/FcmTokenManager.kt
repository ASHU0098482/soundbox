package com.ashupaybox.core.fcm

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Firebase Cloud Messaging registration tokens:
 * - Detects token refreshes
 * - Persists active token locally in SharedPreferences
 * - Exposes token state and last updated timestamp for diagnostics
 * - Integrates with DeviceRegistrationRepository
 */
class FcmTokenManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "FcmTokenManager"
        private const val PREFS_NAME = "paybox_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_UPDATED_AT = "fcm_token_updated_at"
        private const val KEY_LAST_MESSAGE_AT = "fcm_last_message_at"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _fcmTokenFlow = MutableStateFlow<String?>(prefs.getString(KEY_FCM_TOKEN, null))
    val fcmTokenFlow: StateFlow<String?> = _fcmTokenFlow.asStateFlow()

    private val _lastUpdatedFlow = MutableStateFlow(prefs.getLong(KEY_TOKEN_UPDATED_AT, 0L))
    val lastUpdatedFlow: StateFlow<Long> = _lastUpdatedFlow.asStateFlow()

    private val _lastMessageAtFlow = MutableStateFlow(prefs.getLong(KEY_LAST_MESSAGE_AT, 0L))
    val lastMessageAtFlow: StateFlow<Long> = _lastMessageAtFlow.asStateFlow()

    val currentToken: String?
        get() = _fcmTokenFlow.value

    fun initialize() {
        // Attempt to fetch current token from Firebase Messaging
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        Log.d(TAG, "Fetched FCM registration token: $token")
                        saveToken(token)
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseMessaging not initialized or missing Google Play Services", e)
        }
    }

    fun onNewToken(newToken: String) {
        Log.i(TAG, "FCM onNewToken received")
        saveToken(newToken)
    }

    fun recordMessageReceived() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_MESSAGE_AT, now).apply()
        _lastMessageAtFlow.value = now
    }

    private fun saveToken(token: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .putLong(KEY_TOKEN_UPDATED_AT, now)
            .apply()

        _fcmTokenFlow.value = token
        _lastUpdatedFlow.value = now
    }
}
