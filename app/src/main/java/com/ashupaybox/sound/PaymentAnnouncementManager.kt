package com.ashupaybox.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.preferences.SoundBoxConfig
import com.ashupaybox.core.util.IndianVoiceFormatter
import com.ashupaybox.core.util.VoiceLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class PaymentAnnouncementManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val speechQueue = ConcurrentLinkedQueue<String>()
    private var isSpeaking = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            setupTtsEngine()
        } else {
            isTtsInitialized = false
        }
    }

    private fun setupTtsEngine() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                processNextInQueue()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                processNextInQueue()
            }
        })
    }

    /**
     * Plays a pleasant merchant SoundBox chime tone (like POS terminal / SoundBox).
     */
    fun playChimeTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            scope.launch(Dispatchers.Default) {
                delay(300)
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Announce a payment event according to merchant config.
     */
    fun announcePayment(event: PaymentEvent, config: SoundBoxConfig) {
        if (!config.isSoundBoxEnabled || !config.isVoiceEnabled) {
            return
        }

        // Check min / max amount filters
        if (event.amountPaise < config.minAmountPaise || event.amountPaise > config.maxAmountPaise) {
            return
        }

        val methodName = if (config.announcePaymentMethod) event.paymentMethod.displayName else null

        val announcementText = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = event.amountPaise,
            language = config.language,
            format = config.format,
            customPrefix = config.customPrefix,
            customSuffix = config.customSuffix,
            paymentMethodName = methodName
        )

        announcePhrase(announcementText, config)
    }

    /**
     * Announces an explicit phrase (used for Test Sound button and diagnostics).
     */
    fun announcePhrase(phrase: String, config: SoundBoxConfig) {
        playChimeTone()

        scope.launch(Dispatchers.Main) {
            delay(280) // Let the chime finish playing cleanly
            enqueueSpeech(phrase, config)
        }
    }

    private fun enqueueSpeech(text: String, config: SoundBoxConfig) {
        speechQueue.offer(text)
        if (!isSpeaking) {
            processNextInQueue(config)
        }
    }

    private fun processNextInQueue(config: SoundBoxConfig? = null) {
        val nextText = speechQueue.poll() ?: return
        speakInternal(nextText, config)
    }

    private fun speakInternal(text: String, config: SoundBoxConfig?) {
        val engine = tts
        if (engine == null || !isTtsInitialized) {
            return
        }

        requestAudioFocus()

        val language = config?.language ?: VoiceLanguage.ENGLISH
        val locale = when (language) {
            VoiceLanguage.ENGLISH -> Locale("en", "IN")
            VoiceLanguage.HINDI, VoiceLanguage.HINGLISH -> Locale("hi", "IN")
        }

        try {
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to standard English
                engine.setLanguage(Locale.US)
            }
        } catch (e: Exception) {
            engine.setLanguage(Locale.US)
        }

        engine.setSpeechRate(config?.speechRate ?: 1.0f)

        val utteranceId = "paybox_speech_${System.currentTimeMillis()}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, config?.volume ?: 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }
            engine.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                put(TextToSpeech.Engine.KEY_PARAM_VOLUME, (config?.volume ?: 1.0f).toString())
            }
            @Suppress("DEPRECATION")
            engine.speak(text, TextToSpeech.QUEUE_ADD, params)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* handle ducking */ }
                .build()

            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    fun isEngineReady(): Boolean = isTtsInitialized

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
