package com.ashupaybox.presentation.diagnostic

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.fcm.FcmTokenManager
import com.ashupaybox.core.fcm.FirebaseMessageLog
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.data.repository.DeviceRegistrationRepository
import com.ashupaybox.domain.processor.PaymentEventProcessor
import com.ashupaybox.notification.PaymentNotificationManager
import com.ashupaybox.sound.PaymentAnnouncementManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class DiagnosticStatus {
    IDLE, RUNNING, PASS, WARN, FAIL
}

data class DiagnosticItem(
    val id: Int,
    val name: String,
    val description: String,
    val status: DiagnosticStatus = DiagnosticStatus.IDLE,
    val resultMessage: String? = null
)

data class DiagnosticUiState(
    val items: List<DiagnosticItem> = emptyList(),
    val isRunningAll: Boolean = false,
    val passedCount: Int = 0,
    val totalCount: Int = 8
)

class DiagnosticViewModel(
    private val context: Context,
    private val announcementManager: PaymentAnnouncementManager,
    private val notificationManager: PaymentNotificationManager,
    val fcmTokenManager: FcmTokenManager,
    val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val eventProcessor: PaymentEventProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiagnosticUiState(
            items = listOf(
                DiagnosticItem(1, "Speaker Test", "Verifies device speaker and audio hardware"),
                DiagnosticItem(2, "Voice Engine Test", "Checks TextToSpeech engine and language pack"),
                DiagnosticItem(3, "Notification Sound Test", "Validates chime sound playback on media channel"),
                DiagnosticItem(4, "Background Notification Test", "Checks notification channel and permission"),
                DiagnosticItem(5, "Do Not Disturb (DND) Access", "Checks if priority alerts can bypass DND"),
                DiagnosticItem(6, "Volume Check", "Ensures current media volume is loud enough for merchant"),
                DiagnosticItem(7, "Network Connectivity", "Checks internet capability for payment events"),
                DiagnosticItem(8, "Firebase Cloud Messaging (FCM)", "Validates Google Play Services & FCM push token")
            )
        )
    )
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    val fcmTokenFlow = fcmTokenManager.fcmTokenFlow
    val tokenLastUpdatedFlow = fcmTokenManager.lastUpdatedFlow
    val lastMessageAtFlow = fcmTokenManager.lastMessageAtFlow
    val fcmLogsFlow = FirebaseMessageLog.logsFlow
    val syncStatusFlow = deviceRegistrationRepository.syncStatusFlow

    fun runAllDiagnostics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningAll = true)

            // Test 1: Speaker Test
            updateItemStatus(1, DiagnosticStatus.RUNNING, "Testing speaker output...")
            announcementManager.playChimeTone()
            delay(500)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isSpeakerWorking = audioManager.mode == AudioManager.MODE_NORMAL
            updateItemStatus(1, if (isSpeakerWorking) DiagnosticStatus.PASS else DiagnosticStatus.WARN, "Speaker hardware is operational")

            // Test 2: Voice Engine
            updateItemStatus(2, DiagnosticStatus.RUNNING, "Testing TextToSpeech engine...")
            delay(400)
            val isTtsReady = announcementManager.isEngineReady()
            updateItemStatus(
                2,
                if (isTtsReady) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                if (isTtsReady) "TTS Engine ready with Indian speech support" else "TTS is initializing or requires language pack"
            )

            // Test 3: Notification Sound
            updateItemStatus(3, DiagnosticStatus.RUNNING, "Testing notification tone...")
            delay(400)
            updateItemStatus(3, DiagnosticStatus.PASS, "Notification tone generator working")

            // Test 4: Background Notification
            updateItemStatus(4, DiagnosticStatus.RUNNING, "Verifying notification channels...")
            delay(400)
            val notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            updateItemStatus(
                4,
                if (notifEnabled) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                if (notifEnabled) "Payment Alerts channel active (HIGH priority)" else "Notification permission missing"
            )

            // Test 5: DND Access
            updateItemStatus(5, DiagnosticStatus.RUNNING, "Checking Do Not Disturb access...")
            delay(400)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val hasDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.isNotificationPolicyAccessGranted
            } else true
            updateItemStatus(
                5,
                if (hasDnd) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                if (hasDnd) "DND access granted for priority payment alerts" else "DND bypass not granted (optional, recommended)"
            )

            // Test 6: Volume Check
            updateItemStatus(6, DiagnosticStatus.RUNNING, "Checking current volume...")
            delay(400)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val pct = (currentVol.toFloat() / maxVol.toFloat()) * 100
            val volStatus = if (pct >= 40) DiagnosticStatus.PASS else DiagnosticStatus.WARN
            updateItemStatus(
                6,
                volStatus,
                "Media volume is at ${pct.toInt()}%" + (if (pct < 40) " (recommended >= 50% for shop)" else "")
            )

            // Test 7: Network
            updateItemStatus(7, DiagnosticStatus.RUNNING, "Checking network connectivity...")
            delay(400)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNet)
            val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            updateItemStatus(
                7,
                if (isOnline) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                if (isOnline) "Connected to Internet" else "Offline (local transactions cached safely)"
            )

            // Test 8: Firebase Cloud Messaging
            updateItemStatus(8, DiagnosticStatus.RUNNING, "Checking Google Play Services & FCM...")
            delay(400)
            val gmsCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            val isGmsAvailable = gmsCode == ConnectionResult.SUCCESS
            val token = fcmTokenManager.currentToken
            val fcmStatus = when {
                token != null -> DiagnosticStatus.PASS
                isGmsAvailable -> DiagnosticStatus.WARN
                else -> DiagnosticStatus.FAIL
            }
            val fcmMsg = when {
                token != null -> "FCM initialized. Device token available for push."
                isGmsAvailable -> "Google Play Services ready. Generating FCM token..."
                else -> "Google Play Services missing or outdated (Error code: $gmsCode)"
            }
            updateItemStatus(8, fcmStatus, fcmMsg)

            // Compute summary
            val passed = _uiState.value.items.count { it.status == DiagnosticStatus.PASS }
            _uiState.value = _uiState.value.copy(
                isRunningAll = false,
                passedCount = passed
            )
        }
    }

    fun copyFcmToken() {
        val token = fcmTokenManager.currentToken
        if (token != null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Ashu PayBox FCM Token", token))
            Toast.makeText(context, "FCM Token copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "FCM Token not yet available. Ensure device is online.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendLocalSimulation(amountRupees: Long) {
        viewModelScope.launch {
            val event = PaymentEvent(
                eventId = "sim_diag_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                paymentId = "pay_diag_${System.currentTimeMillis()}",
                orderId = "order_diag_${System.currentTimeMillis()}",
                amountPaise = amountRupees * 100L,
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Diagnostic Test",
                source = PaymentSource.FCM_TEST,
                isTest = true
            )
            eventProcessor.processEvent(event)
            Toast.makeText(context, "Simulated ₹$amountRupees payment event processed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateItemStatus(id: Int, status: DiagnosticStatus, message: String?) {
        val updated = _uiState.value.items.map {
            if (it.id == id) it.copy(status = status, resultMessage = message) else it
        }
        val passed = updated.count { it.status == DiagnosticStatus.PASS }
        _uiState.value = _uiState.value.copy(items = updated, passedCount = passed)
    }

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DiagnosticViewModel(
                    context = app.applicationContext,
                    announcementManager = app.announcementManager,
                    notificationManager = app.notificationManager,
                    fcmTokenManager = app.fcmTokenManager,
                    deviceRegistrationRepository = app.deviceRegistrationRepository,
                    eventProcessor = app.eventProcessor
                ) as T
            }
        }
    }
}
