package com.hemanth.speechAndVoiceRecognize.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.hemanth.speechAndVoiceRecognize.Logger
import com.hemanth.speechAndVoiceRecognize.TextToSpeechCallback
import com.hemanth.speechAndVoiceRecognize.TtsProgressListener
import java.util.*

class BaseTextToSpeechEngine : TextToSpeechEngine {

    private var mTextToSpeech: TextToSpeech? = null
    private var mTttsInitListener: OnInitListener? = null
    private var mTtsProgressListener: UtteranceProgressListener? = null
    private var mTtsRate = 1.0f
    private var mTtsPitch = 1.0f
    private var mLocale = Locale.getDefault()
    private var voice: Voice? = null
    private var mTtsQueueMode = TextToSpeech.QUEUE_FLUSH
    private var mAudioStream = TextToSpeech.Engine.DEFAULT_STREAM
    private val mTtsCallbacks: MutableMap<String, TextToSpeechCallback> = HashMap()

    override fun initTextToSpeech(context: Context) {
        if (mTextToSpeech != null) {
            return
        }
        mTtsProgressListener = TtsProgressListener(context, mTtsCallbacks)
        mTextToSpeech = TextToSpeech(context.applicationContext, mTttsInitListener)
        mTextToSpeech?.setOnUtteranceProgressListener(mTtsProgressListener)
        mTextToSpeech?.language = mLocale
        mTextToSpeech?.setPitch(mTtsPitch)
        mTextToSpeech?.setSpeechRate(mTtsRate)

        if (voice == null) {
            voice = mTextToSpeech!!.defaultVoice
        }
        mTextToSpeech!!.voice = voice

    }

    override fun isSpeaking(): Boolean {
        return if (mTextToSpeech == null) {
            false
        } else mTextToSpeech!!.isSpeaking
    }

    override fun setOnInitListener(onInitListener: OnInitListener) {
        mTttsInitListener = onInitListener
    }

    override fun setLocale(locale: Locale) {
        mLocale = locale
        if (mTextToSpeech != null) {
            mTextToSpeech!!.language = locale
        }
    }

    override fun say(message: String, callback: TextToSpeechCallback) {
        val utteranceId = UUID.randomUUID().toString()
        if (callback != null) {
            mTtsCallbacks[utteranceId] = callback
        }
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, mAudioStream.toString())
        mTextToSpeech!!.speak(message, mTtsQueueMode, params, utteranceId)
    }

    override fun shutdown() {
        if (mTextToSpeech != null) {
            try {
                mTtsCallbacks.clear()
                mTextToSpeech!!.stop()
                mTextToSpeech!!.shutdown()
            } catch (exc: Exception) {
                Logger.error(javaClass.simpleName, "Warning while de-initing text to speech", exc)
            }
        }
    }

    override fun setTextToSpeechQueueMode(mode: Int) {
        mTtsQueueMode = mode
    }

    override fun setAudioStream(audioStream: Int) {
        mAudioStream = audioStream
    }

    override fun stop() {
        if (mTextToSpeech != null) {
            mTextToSpeech!!.stop()
        }
    }

    override fun setPitch(pitch: Float) {
        mTtsPitch = pitch
        if (mTextToSpeech != null) {
            mTextToSpeech!!.setPitch(pitch)
        }
    }

    override fun setSpeechRate(rate: Float) {
        mTtsRate = rate
        if (mTextToSpeech != null) {
            mTextToSpeech!!.setSpeechRate(rate)
        }
    }

    override fun setVoice(voice: Voice) {
        this.voice = voice
        if (mTextToSpeech != null) {
            mTextToSpeech!!.voice = voice
        }
    }

    override fun getSupportedVoices(): List<Voice> {
        if (mTextToSpeech != null && Build.VERSION.SDK_INT >= 23) {
            val voices = mTextToSpeech!!.voices
            val voicesList = ArrayList<Voice>(voices.size)
            voicesList.addAll(voices)
            return voicesList
        }
        return ArrayList(1)
    }

    override fun getCurrentVoice(): Voice? {
        return if (mTextToSpeech != null && Build.VERSION.SDK_INT >= 23) {
            mTextToSpeech!!.voice
        } else null
    }
}