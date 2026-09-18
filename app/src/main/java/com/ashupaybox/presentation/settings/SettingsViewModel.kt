package com.ashupaybox.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.core.preferences.SoundBoxConfig
import com.ashupaybox.core.util.AnnouncementFormat
import com.ashupaybox.core.util.DeviceIdentifier
import com.ashupaybox.core.util.VoiceLanguage
import com.ashupaybox.data.repository.PaymentRepository
import com.ashupaybox.data.repository.DeviceRegistrationRepository
import com.ashupaybox.sound.PaymentAnnouncementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: PayBoxPreferences,
    private val repository: PaymentRepository,
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val announcementManager: PaymentAnnouncementManager,
    private val appContext: Context
) : ViewModel() {

    val config: StateFlow<SoundBoxConfig> = preferences.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SoundBoxConfig()
    )

    val deviceId: String by lazy {
        DeviceIdentifier.getOrCreateInstallationId(appContext)
    }

    val backendSyncStatus = deviceRegistrationRepository.syncStatusFlow
    val backendLastError = deviceRegistrationRepository.lastErrorFlow
    val backendUrl: String
        get() = deviceRegistrationRepository.backendUrl.ifBlank { "Not configured" }
    val isBackendPaired: Boolean
        get() = deviceRegistrationRepository.isPaired

    fun setSoundBoxEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setSoundBoxEnabled(enabled)
    }

    fun setVoiceEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setVoiceEnabled(enabled)
    }

    fun setNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotificationEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setVibrationEnabled(enabled)
    }

    fun setAnnounceAmount(enabled: Boolean) = viewModelScope.launch {
        preferences.setAnnounceAmount(enabled)
    }

    fun setAnnouncePaymentMethod(enabled: Boolean) = viewModelScope.launch {
        preferences.setAnnouncePaymentMethod(enabled)
    }

    fun setAnnounceMissedPayments(enabled: Boolean) = viewModelScope.launch {
        preferences.setAnnounceMissedPayments(enabled)
    }

    fun setLanguage(language: VoiceLanguage) = viewModelScope.launch {
        preferences.setLanguage(language)
    }

    fun setAnnouncementFormat(format: AnnouncementFormat) = viewModelScope.launch {
        preferences.setAnnouncementFormat(format)
    }

    fun setCustomPrefix(prefix: String) = viewModelScope.launch {
        preferences.setCustomPrefix(prefix)
    }

    fun setCustomSuffix(suffix: String) = viewModelScope.launch {
        preferences.setCustomSuffix(suffix)
    }

    fun setVolume(volume: Float) = viewModelScope.launch {
        preferences.setVolume(volume)
    }

    fun setSpeechRate(rate: Float) = viewModelScope.launch {
        preferences.setSpeechRate(rate)
    }

    fun setMinAmountRupees(rupees: Long) = viewModelScope.launch {
        preferences.setMinAmountPaise(rupees * 100L)
    }

    fun setTestModeEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setTestModeEnabled(enabled)
    }

    fun setIncludeTestInRevenue(include: Boolean) = viewModelScope.launch {
        preferences.setIncludeTestInRevenue(include)
    }

    fun setKeepServiceAlive(alive: Boolean) = viewModelScope.launch {
        preferences.setKeepServiceAlive(alive)
    }

    fun testAnnouncement() {
        val currentConfig = config.value
        val phrase = when (currentConfig.language) {
            VoiceLanguage.ENGLISH -> "Payment received. Three hundred rupees."
            VoiceLanguage.HINDI -> "भुगतान प्राप्त हुआ। तीन सौ रुपये।"
            VoiceLanguage.HINGLISH -> "Payment received. Teen sau rupaye."
        }
        announcementManager.announcePhrase(phrase, currentConfig)
    }

    fun connectSoundBox(pairingCode: String, onComplete: (Boolean, String) -> Unit) = viewModelScope.launch {
        val result = deviceRegistrationRepository.syncWithBackend(pairingCode)
        onComplete(
            result.isSuccess,
            result.exceptionOrNull()?.message ?: if (result.isSuccess) "SoundBox connected" else "Connection failed"
        )
    }

    fun syncSoundBox(onComplete: (Boolean, String) -> Unit) = viewModelScope.launch {
        val result = deviceRegistrationRepository.syncWithBackend()
        onComplete(
            result.isSuccess,
            result.exceptionOrNull()?.message ?: if (result.isSuccess) "Backend synced" else "Sync failed"
        )
    }

    fun disconnectSoundBox() {
        deviceRegistrationRepository.disconnect()
    }

    fun clearTestData(onComplete: (Int) -> Unit) = viewModelScope.launch {
        val count = repository.clearTestData()
        onComplete(count)
    }

    fun seedDemoTransactions(onComplete: () -> Unit) = viewModelScope.launch {
        repository.seedDemoTransactions()
        onComplete()
    }

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    preferences = app.preferences,
                    repository = app.repository,
                    deviceRegistrationRepository = app.deviceRegistrationRepository,
                    announcementManager = app.announcementManager,
                    appContext = app.applicationContext
                ) as T
            }
        }
    }
}
