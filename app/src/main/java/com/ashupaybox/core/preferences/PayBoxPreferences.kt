package com.ashupaybox.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ashupaybox.core.util.AnnouncementFormat
import com.ashupaybox.core.util.VoiceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paybox_settings")

data class SoundBoxConfig(
    val isSoundBoxEnabled: Boolean = true,
    val isVoiceEnabled: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val announceAmount: Boolean = true,
    val announcePaymentMethod: Boolean = false,
    val announceMissedPayments: Boolean = false,
    val language: VoiceLanguage = VoiceLanguage.ENGLISH,
    val format: AnnouncementFormat = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT,
    val customPrefix: String = "",
    val customSuffix: String = "",
    val volume: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val minAmountPaise: Long = 100L, // ₹1 minimum
    val maxAmountPaise: Long = Long.MAX_VALUE,
    val isTestModeEnabled: Boolean = true,
    val includeTestInRevenue: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val keepServiceAlive: Boolean = false
)

class PayBoxPreferences(private val context: Context) {

    private object Keys {
        val SOUNDBOX_ENABLED = booleanPreferencesKey("soundbox_enabled")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ANNOUNCE_AMOUNT = booleanPreferencesKey("announce_amount")
        val ANNOUNCE_METHOD = booleanPreferencesKey("announce_method")
        val ANNOUNCE_MISSED = booleanPreferencesKey("announce_missed")
        val LANGUAGE = stringPreferencesKey("voice_language")
        val FORMAT = stringPreferencesKey("announcement_format")
        val CUSTOM_PREFIX = stringPreferencesKey("custom_prefix")
        val CUSTOM_SUFFIX = stringPreferencesKey("custom_suffix")
        val VOLUME = floatPreferencesKey("sound_volume")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val MIN_AMOUNT = longPreferencesKey("min_amount_paise")
        val MAX_AMOUNT = longPreferencesKey("max_amount_paise")
        val TEST_MODE = booleanPreferencesKey("test_mode_enabled")
        val INCLUDE_TEST_REVENUE = booleanPreferencesKey("include_test_revenue")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEEP_SERVICE_ALIVE = booleanPreferencesKey("keep_service_alive")
    }

    val configFlow: Flow<SoundBoxConfig> = context.dataStore.data.map { prefs ->
        val langId = prefs[Keys.LANGUAGE] ?: VoiceLanguage.ENGLISH.id
        val language = VoiceLanguage.entries.firstOrNull { it.id == langId } ?: VoiceLanguage.ENGLISH

        val formatId = prefs[Keys.FORMAT] ?: AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT.id
        val format = AnnouncementFormat.entries.firstOrNull { it.id == formatId }
            ?: AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT

        SoundBoxConfig(
            isSoundBoxEnabled = prefs[Keys.SOUNDBOX_ENABLED] ?: true,
            isVoiceEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            isNotificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            isVibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            announceAmount = prefs[Keys.ANNOUNCE_AMOUNT] ?: true,
            announcePaymentMethod = prefs[Keys.ANNOUNCE_METHOD] ?: false,
            announceMissedPayments = prefs[Keys.ANNOUNCE_MISSED] ?: false,
            language = language,
            format = format,
            customPrefix = prefs[Keys.CUSTOM_PREFIX] ?: "",
            customSuffix = prefs[Keys.CUSTOM_SUFFIX] ?: "",
            volume = prefs[Keys.VOLUME] ?: 1.0f,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            minAmountPaise = prefs[Keys.MIN_AMOUNT] ?: 100L,
            maxAmountPaise = prefs[Keys.MAX_AMOUNT] ?: Long.MAX_VALUE,
            isTestModeEnabled = prefs[Keys.TEST_MODE] ?: true,
            includeTestInRevenue = prefs[Keys.INCLUDE_TEST_REVENUE] ?: true,
            isOnboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            keepServiceAlive = prefs[Keys.KEEP_SERVICE_ALIVE] ?: false
        )
    }

    suspend fun setSoundBoxEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUNDBOX_ENABLED] = enabled }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAnnounceAmount(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANNOUNCE_AMOUNT] = enabled }
    }

    suspend fun setAnnouncePaymentMethod(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANNOUNCE_METHOD] = enabled }
    }

    suspend fun setAnnounceMissedPayments(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANNOUNCE_MISSED] = enabled }
    }

    suspend fun setLanguage(language: VoiceLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.id }
    }

    suspend fun setAnnouncementFormat(format: AnnouncementFormat) {
        context.dataStore.edit { it[Keys.FORMAT] = format.id }
    }

    suspend fun setCustomPrefix(prefix: String) {
        context.dataStore.edit { it[Keys.CUSTOM_PREFIX] = prefix }
    }

    suspend fun setCustomSuffix(suffix: String) {
        context.dataStore.edit { it[Keys.CUSTOM_SUFFIX] = suffix }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[Keys.VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setMinAmountPaise(minPaise: Long) {
        context.dataStore.edit { it[Keys.MIN_AMOUNT] = minPaise }
    }

    suspend fun setTestModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TEST_MODE] = enabled }
    }

    suspend fun setIncludeTestInRevenue(include: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_TEST_REVENUE] = include }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setKeepServiceAlive(alive: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SERVICE_ALIVE] = alive }
    }
}
