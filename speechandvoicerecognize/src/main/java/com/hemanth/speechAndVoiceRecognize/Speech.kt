package com.hemanth.speechAndVoiceRecognize

import android.speech.tts.TextToSpeech.OnInitListener
import com.hemanth.speechAndVoiceRecognize.engine.SpeechRecognitionEngine
import com.hemanth.speechAndVoiceRecognize.engine.TextToSpeechEngine
import kotlin.jvm.Synchronized
import com.hemanth.speechAndVoiceRecognize.Speech
import kotlin.Throws
import com.hemanth.speechAndVoiceRecognize.SpeechRecognitionNotAvailable
import com.hemanth.speechAndVoiceRecognize.GoogleVoiceTypingDisabledException
import com.hemanth.speechAndVoiceRecognize.SpeechDelegate
import com.hemanth.speechAndVoiceRecognize.ui.SpeechProgressView
import kotlin.jvm.JvmOverloads
import com.hemanth.speechAndVoiceRecognize.TextToSpeechCallback
import android.speech.tts.Voice
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import com.hemanth.speechAndVoiceRecognize.SupportedLanguagesListener
import com.hemanth.speechAndVoiceRecognize.UnsupportedReason
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.BroadcastReceiver
import android.os.Bundle
import android.app.Activity
import android.content.Context
import com.hemanth.speechAndVoiceRecognize.engine.DummyOnInitListener
import com.hemanth.speechAndVoiceRecognize.engine.BaseSpeechRecognitionEngine
import com.hemanth.speechAndVoiceRecognize.engine.BaseTextToSpeechEngine
import java.util.*

/**
 * Helper class to easily work with Android speech recognition.
 */
class Speech private constructor(
    private val mContext: Context,
    callingPackage: String?,
    onInitListener: OnInitListener,
    private val speechRecognitionEngine: SpeechRecognitionEngine,
    textToSpeechEngine: TextToSpeechEngine
) {
    private val textToSpeechEngine: TextToSpeechEngine

    /**
     * Must be called inside Activity's onDestroy.
     */
    @Synchronized
    fun shutdown() {
        speechRecognitionEngine.shutdown()
        textToSpeechEngine.shutdown()
        instance = null
    }

    /**
     * Starts voice recognition.
     *
     * @param delegate delegate which will receive speech recognition events and status
     * @throws SpeechRecognitionNotAvailable      when speech recognition is not available on the device
     * @throws GoogleVoiceTypingDisabledException when google voice typing is disabled on the device
     */
    @Throws(SpeechRecognitionNotAvailable::class, GoogleVoiceTypingDisabledException::class)
    fun startListening(delegate: SpeechDelegate?) {
        startListening(null, delegate)
    }

    /**
     * Starts voice recognition.
     *
     * @param progressView view in which to draw speech animation
     * @param delegate     delegate which will receive speech recognition events and status
     * @throws SpeechRecognitionNotAvailable      when speech recognition is not available on the device
     * @throws GoogleVoiceTypingDisabledException when google voice typing is disabled on the device
     */
    @Throws(SpeechRecognitionNotAvailable::class, GoogleVoiceTypingDisabledException::class)
    fun startListening(progressView: SpeechProgressView?, delegate: SpeechDelegate?) {
        speechRecognitionEngine.startListening(progressView, delegate)
    }

    /**
     * Stops voice recognition listening.
     * This method does nothing if voice listening is not active
     */
    fun stopListening() {
        speechRecognitionEngine.stopListening()
    }

    /**
     * Check if voice recognition is currently active.
     *
     * @return true if the voice recognition is on, false otherwise
     */
    val isListening: Boolean
        get() = speechRecognitionEngine.isListening

    /**
     * Check if text to speak is currently speaking.
     *
     * @return true if the text to speak is speaking, false otherwise
     */
    val isSpeaking: Boolean
        get() = textToSpeechEngine.isSpeaking
    /**
     * Uses text to speech to transform a written message into a sound.
     *
     * @param message  message to play
     * @param callback callback which will receive progress status of the operation
     */
    /**
     * Uses text to speech to transform a written message into a sound.
     *
     * @param message message to play
     */
    @JvmOverloads
    fun say(message: String?, callback: TextToSpeechCallback? = null) {
        textToSpeechEngine.say(message, callback)
    }

    /**
     * Stops text to speech.
     */
    fun stopTextToSpeech() {
        textToSpeechEngine.stop()
    }

    /**
     * Set whether to only use an offline speech recognition engine.
     * The default is false, meaning that either network or offline recognition engines may be used.
     *
     * @param preferOffline true to prefer offline engine, false to use either one of the two
     * @return speech instance
     */
    fun setPreferOffline(preferOffline: Boolean): Speech {
        speechRecognitionEngine.setPreferOffline(preferOffline)
        return this
    }

    /**
     * Set whether partial results should be returned by the recognizer as the user speaks
     * (default is true). The server may ignore a request for partial results in some or all cases.
     *
     * @param getPartialResults true to get also partial recognition results, false otherwise
     * @return speech instance
     */
    fun setGetPartialResults(getPartialResults: Boolean): Speech {
        speechRecognitionEngine.setPartialResults(getPartialResults)
        return this
    }

    /**
     * Sets text to speech and recognition language.
     * Defaults to device language setting.
     *
     * @param locale new locale
     * @return speech instance
     */
    fun setLocale(locale: Locale?): Speech {
        speechRecognitionEngine.locale = locale
        textToSpeechEngine.setLocale(locale)
        return this
    }

    /**
     * Sets the speech rate. This has no effect on any pre-recorded speech.
     *
     * @param rate Speech rate. 1.0 is the normal speech rate, lower values slow down the speech
     * (0.5 is half the normal speech rate), greater values accelerate it
     * (2.0 is twice the normal speech rate).
     * @return speech instance
     */
    fun setTextToSpeechRate(rate: Float): Speech {
        textToSpeechEngine.setSpeechRate(rate)
        return this
    }

    /**
     * Sets the voice for the TextToSpeech engine.
     * This has no effect on any pre-recorded speech.
     *
     * @param voice Speech voice.
     * @return speech instance
     */
    fun setVoice(voice: Voice?): Speech {
        textToSpeechEngine.setVoice(voice)
        return this
    }

    /**
     * Sets the speech pitch for the TextToSpeech engine.
     * This has no effect on any pre-recorded speech.
     *
     * @param pitch Speech pitch. 1.0 is the normal pitch, lower values lower the tone of the
     * synthesized voice, greater values increase it.
     * @return speech instance
     */
    fun setTextToSpeechPitch(pitch: Float): Speech {
        textToSpeechEngine.setPitch(pitch)
        return this
    }

    /**
     * Sets the idle timeout after which the listening will be automatically stopped.
     *
     * @param milliseconds timeout in milliseconds
     * @return speech instance
     */
    fun setStopListeningAfterInactivity(milliseconds: Long): Speech {
        speechRecognitionEngine.setStopListeningAfterInactivity(milliseconds)
        speechRecognitionEngine.init(mContext)
        return this
    }

    /**
     * Sets the minimum interval between start/stop events. This is useful to prevent
     * monkey input from users.
     *
     * @param milliseconds minimum interval betweeb state change in milliseconds
     * @return speech instance
     */
    fun setTransitionMinimumDelay(milliseconds: Long): Speech {
        speechRecognitionEngine.setTransitionMinimumDelay(milliseconds)
        return this
    }

    /**
     * Sets the text to speech queue mode.
     * By default is TextToSpeech.QUEUE_FLUSH, which is faster, because it clears all the
     * messages before speaking the new one. TextToSpeech.QUEUE_ADD adds the last message
     * to speak in the queue, without clearing the messages that have been added.
     *
     * @param mode It can be either TextToSpeech.QUEUE_ADD or TextToSpeech.QUEUE_FLUSH.
     * @return speech instance
     */
    fun setTextToSpeechQueueMode(mode: Int): Speech {
        textToSpeechEngine.setTextToSpeechQueueMode(mode)
        return this
    }

    /**
     * Sets the audio stream type.
     * By default is TextToSpeech.Engine.DEFAULT_STREAM, which is equivalent to
     * AudioManager.STREAM_MUSIC.
     *
     * @param audioStream A constant from AudioManager.
     * e.g. [android.media.AudioManager.STREAM_VOICE_CALL]
     * @return speech instance
     */
    fun setAudioStream(audioStream: Int): Speech {
        textToSpeechEngine.setAudioStream(audioStream)
        return this
    }

    private val isGoogleAppInstalled: Boolean
        private get() {
            val packageManager = mContext.packageManager
            for (packageInfo in packageManager.getInstalledPackages(0)) {
                if (packageInfo.packageName.contains(GOOGLE_APP_PACKAGE)) {
                    return true
                }
            }
            return false
        }

    /**
     * Gets the list of the supported speech to text languages on this device
     * @param listener listner which will receive the results
     */
    fun getSupportedSpeechToTextLanguages(listener: SupportedLanguagesListener) {
        if (!isGoogleAppInstalled) {
            listener.onNotSupported(UnsupportedReason.GOOGLE_APP_NOT_FOUND)
            return
        }
        var intent = RecognizerIntent.getVoiceDetailsIntent(mContext)
        if (intent == null) {
            intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
            intent.setPackage(GOOGLE_APP_PACKAGE)
        }
        mContext.sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val extras = getResultExtras(true)
                if (extras != null && extras.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                    val languages: List<String>? =
                        extras.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                    if (languages == null || languages.isEmpty()) {
                        listener.onNotSupported(UnsupportedReason.EMPTY_SUPPORTED_LANGUAGES)
                    } else {
                        Collections.sort(languages)
                        listener.onSupportedLanguages(languages)
                    }
                } else {
                    listener.onNotSupported(UnsupportedReason.EMPTY_SUPPORTED_LANGUAGES)
                }
            }
        }, null, Activity.RESULT_OK, null, null)
    }

    /**
     * Gets the list of the supported Text to Speech languages on this device
     * @return list of locales on android API 23 and newer and empty list on lower Android, because native
     * TTS engine does not support querying voices on API lower than 23. Officially it's declared that
     * query voices support started on API 21, but in reality it started from 23.
     * If still skeptic about this, search the web and try on your own.
     */
    val supportedTextToSpeechVoices: List<Voice>
        get() = textToSpeechEngine.supportedVoices

    /**
     * Gets the locale used for speech recognition.
     * @return speech recognition locale
     */
    val speechToTextLanguage: Locale
        get() = speechRecognitionEngine.locale

    /**
     * Gets the current voice used for text to speech.
     * @return current voice on android API 23 or newer and null on lower Android, because native
     * TTS engine does not support querying voices on API lower than 23. Officially it's declared that
     * query voices support started on API 21, but in reality it started from 23.
     * If still skeptic about this, search the web and try on your own.
     */
    val textToSpeechVoice: Voice
        get() = textToSpeechEngine.currentVoice

    companion object {
        private var instance: Speech? = null
        var GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

        /**
         * Initializes speech recognition.
         *
         * @param context application context
         * @return speech instance
         */
        fun init(context: Context): Speech? {
            if (instance == null) {
                instance = Speech(
                    context,
                    null,
                    DummyOnInitListener(),
                    BaseSpeechRecognitionEngine(),
                    BaseTextToSpeechEngine()
                )
            }
            return instance
        }

        /**
         * Initializes speech recognition.
         *
         * @param context        application context
         * @param callingPackage The extra key used in an intent to the speech recognizer for
         * voice search. Not generally to be used by developers.
         * The system search dialog uses this, for example, to set a calling
         * package for identification by a voice search API.
         * If this extra is set by anyone but the system process,
         * it should be overridden by the voice search implementation.
         * By passing null or empty string (which is the default) you are
         * not overriding the calling package
         * @return speech instance
         */
        fun init(context: Context, callingPackage: String?): Speech? {
            if (instance == null) {
                instance = Speech(
                    context,
                    callingPackage,
                    DummyOnInitListener(),
                    BaseSpeechRecognitionEngine(),
                    BaseTextToSpeechEngine()
                )
            }
            return instance
        }

        fun init(
            context: Context,
            callingPackage: String?,
            onInitListener: OnInitListener
        ): Speech? {
            if (instance == null) {
                instance = Speech(
                    context,
                    callingPackage,
                    onInitListener,
                    BaseSpeechRecognitionEngine(),
                    BaseTextToSpeechEngine()
                )
            }
            return instance
        }

        fun init(
            context: Context,
            callingPackage: String?,
            onInitListener: OnInitListener,
            speechRecognitionEngine: SpeechRecognitionEngine,
            textToSpeechEngine: TextToSpeechEngine
        ): Speech? {
            if (instance == null) {
                instance = Speech(
                    context,
                    callingPackage,
                    onInitListener,
                    speechRecognitionEngine,
                    textToSpeechEngine
                )
            }
            return instance
        }

        /**
         * Gets speech recognition instance.
         *
         * @return SpeechRecognition instance
         */
        fun getInstance(): Speech? {
            checkNotNull(instance) { "Speech recognition has not been initialized! call init method first!" }
            return instance
        }
    }

    init {
        speechRecognitionEngine.setCallingPackage(callingPackage)
        speechRecognitionEngine.initSpeechRecognizer(mContext)
        this.textToSpeechEngine = textToSpeechEngine
        this.textToSpeechEngine.setOnInitListener(onInitListener)
        this.textToSpeechEngine.initTextToSpeech(mContext)
    }
}