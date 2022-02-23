package com.hemanth.speechAndVoiceRecognize.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.LinearLayout
import com.hemanth.speechAndVoiceRecognize.*
import com.hemanth.speechAndVoiceRecognize.ui.SpeechProgressView
import java.util.*

class BaseSpeechRecognitionEngine : SpeechRecognitionEngine {

    private var mContext: Context? = null
    private var mSpeechRecognizer: SpeechRecognizer? = null
    private var mDelegate: SpeechDelegate? = null
    private var mProgressView: SpeechProgressView? = null
    private var mCallingPackage: String? = null
    private var mUnstableData: String? = null
    private var mDelayedStopListening: DelayedOperation? = null
    private val mPartialData: MutableList<String?> = ArrayList()
    private var mLastPartialResults: List<String?>? = null
    private var mLocale = Locale.getDefault()
    private var mPreferOffline = false
    private var mGetPartialResults = true
    private var mIsListening = false
    private var mLastActionTimestamp: Long = 0
    private var mStopListeningDelayInMs: Long = 4000
    private var mTransitionMinimumDelay: Long = 1200

    override fun init(context: Context) {
        initDelayedStopListening(context)
    }

    override fun clear() {
        mPartialData.clear()
        mUnstableData = null
    }

    override fun onReadyForSpeech(bundle: Bundle) {
        mPartialData.clear()
        mUnstableData = null
    }

    override fun onBeginningOfSpeech() {
        if (mProgressView != null) mProgressView!!.onBeginningOfSpeech()
        mDelayedStopListening!!.start(object : DelayedOperation.Operation {
            override fun onDelayedOperation() {
                returnPartialResultsAndRecreateSpeechRecognizer()
            }

            override fun shouldExecuteDelayedOperation(): Boolean {
                return true
            }
        })
    }

    override fun onRmsChanged(v: Float) {
        try {
            if (mDelegate != null) mDelegate!!.onSpeechRmsChanged(v)
        } catch (exc: Throwable) {
            Logger.error(
                javaClass.simpleName,
                "Unhandled exception in delegate onSpeechRmsChanged", exc
            )
        }
        if (mProgressView != null) mProgressView!!.onRmsChanged(v)
    }

    override fun onPartialResults(bundle: Bundle) {
        mDelayedStopListening!!.resetTimer()
        val partialResults: List<String?>? =
            bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val unstableData: List<String>? =
            bundle.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
        if (partialResults != null && !partialResults.isEmpty()) {
            mPartialData.clear()
            mPartialData.addAll(partialResults)
            mUnstableData =
                if (unstableData != null && !unstableData.isEmpty()) unstableData[0] else null
            try {
                if (mLastPartialResults == null || mLastPartialResults != partialResults) {
                    if (mDelegate != null) mDelegate!!.onSpeechPartialResults(partialResults)
                    mLastPartialResults = partialResults
                }
            } catch (exc: Throwable) {
                Logger.error(
                    javaClass.simpleName,
                    "Unhandled exception in delegate onSpeechPartialResults", exc
                )
            }
        }
    }

    override fun onResults(bundle: Bundle) {
        mDelayedStopListening!!.cancel()
        val results: List<String?>? =
            bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val result: String?
        result = if (results != null && !results.isEmpty()
            && results[0] != null && !results[0]!!.isEmpty()
        ) {
            results[0]
        } else {
            Logger.info(javaClass.simpleName, "No speech results, getting partial")
            partialResultsAsString
        }
        mIsListening = false
        try {
            if (mDelegate != null) mDelegate!!.onSpeechResult(result!!.trim { it <= ' ' })
        } catch (exc: Throwable) {
            Logger.error(
                javaClass.simpleName,
                "Unhandled exception in delegate onSpeechResult", exc
            )
        }
        if (mProgressView != null) mProgressView!!.onResultOrOnError()
        initSpeechRecognizer(mContext!!)
    }

    override fun onError(code: Int) {
        Logger.error(LOG_TAG, "Speech recognition error", SpeechRecognitionException(code))
        returnPartialResultsAndRecreateSpeechRecognizer()
    }

    override fun onBufferReceived(bytes: ByteArray) {}
    override fun onEndOfSpeech() {
        if (mProgressView != null) mProgressView!!.onEndOfSpeech()
    }

    override fun onEvent(i: Int, bundle: Bundle) {}
    override fun getPartialResultsAsString(): String {
        val out = StringBuilder("")
        for (partial in mPartialData) {
            out.append(partial).append(" ")
        }
        if (mUnstableData != null && !mUnstableData!!.isEmpty()) out.append(mUnstableData)
        return out.toString().trim { it <= ' ' }
    }

    @Throws(SpeechRecognitionNotAvailable::class, GoogleVoiceTypingDisabledException::class)
    override fun startListening(progressView: SpeechProgressView, delegate: SpeechDelegate) {
        if (mIsListening) return
        if (mSpeechRecognizer == null) throw SpeechRecognitionNotAvailable()
        requireNotNull(delegate) { "delegate must be defined!" }
        if (throttleAction()) {
            Logger.debug(
                javaClass.simpleName,
                "Hey man calm down! Throttling start to prevent disaster!"
            )
            return
        }
        mProgressView = progressView
        mDelegate = delegate
        require(!(progressView != null && progressView.parent !is LinearLayout)) { "progressView must be put inside a LinearLayout!" }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, mGetPartialResults)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLocale.language)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        if (mCallingPackage != null && !mCallingPackage!!.isEmpty()) {
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mCallingPackage)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, mPreferOffline)
        }
        try {
            mSpeechRecognizer!!.startListening(intent)
        } catch (exc: SecurityException) {
            throw GoogleVoiceTypingDisabledException()
        }
        mIsListening = true
        updateLastActionTimestamp()
        try {
            if (mDelegate != null) mDelegate!!.onStartOfSpeech()
        } catch (exc: Throwable) {
            Logger.error(
                javaClass.simpleName,
                "Unhandled exception in delegate onStartOfSpeech", exc
            )
        }
    }

    override fun isListening(): Boolean {
        return mIsListening
    }

    override fun getLocale(): Locale {
        return mLocale
    }

    override fun setLocale(locale: Locale) {
        mLocale = locale
    }

    override fun stopListening() {
        if (!mIsListening) return
        if (throttleAction()) {
            Logger.debug(
                javaClass.simpleName,
                "Hey man calm down! Throttling stop to prevent disaster!"
            )
            return
        }
        mIsListening = false
        updateLastActionTimestamp()
        returnPartialResultsAndRecreateSpeechRecognizer()
    }

    override fun initSpeechRecognizer(context: Context) {
        mContext = context
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            if (mSpeechRecognizer != null) {
                try {
                    mSpeechRecognizer?.destroy()
                } catch (exc: Throwable) {
                    Logger.debug(
                        javaClass.simpleName,
                        "Non-Fatal error while destroying speech. " + exc.message
                    )
                } finally {
                    mSpeechRecognizer = null
                }
            }
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            mSpeechRecognizer?.setRecognitionListener(this)
            init(context)
        } else {
            mSpeechRecognizer = null
        }
        clear()
    }

    override fun returnPartialResultsAndRecreateSpeechRecognizer() {
        mIsListening = false
        try {
            if (mDelegate != null) mDelegate!!.onSpeechResult(partialResultsAsString)
        } catch (exc: Throwable) {
            Logger.error(
                javaClass.simpleName,
                "Unhandled exception in delegate onSpeechResult", exc
            )
        }
        if (mProgressView != null) mProgressView!!.onResultOrOnError()
        initSpeechRecognizer(mContext!!)
    }

    override fun setPartialResults(getPartialResults: Boolean) {
        mGetPartialResults = getPartialResults
    }

    override fun unregisterDelegate() {
        mProgressView = null
        mDelegate = null
    }

    override fun setPreferOffline(preferOffline: Boolean) {
        mPreferOffline = preferOffline
    }

    private fun initDelayedStopListening(context: Context) {
        if (mDelayedStopListening != null) {
            mDelayedStopListening!!.cancel()
            mDelayedStopListening = null
            stopDueToDelay()
        }
        mDelayedStopListening =
            DelayedOperation(context, "delayStopListening", mStopListeningDelayInMs)
    }

    protected fun stopDueToDelay() {}

    private fun updateLastActionTimestamp() {
        mLastActionTimestamp = Date().time
    }

    private fun throttleAction(): Boolean {
        return Date().time <= mLastActionTimestamp + mTransitionMinimumDelay
    }

    override fun setCallingPackage(callingPackage: String) {
        mCallingPackage = callingPackage
    }

    override fun setTransitionMinimumDelay(milliseconds: Long) {
        mTransitionMinimumDelay = milliseconds
    }

    override fun setStopListeningAfterInactivity(milliseconds: Long) {
        mStopListeningDelayInMs = milliseconds
    }

    override fun shutdown() {
        if (mSpeechRecognizer != null) {
            try {
                mSpeechRecognizer!!.stopListening()
                mSpeechRecognizer!!.destroy()
            } catch (exc: Exception) {
                Logger.error(
                    javaClass.simpleName,
                    "Warning while de-initing speech recognizer",
                    exc
                )
            }
        }
        unregisterDelegate()
    }

    companion object {
        private val LOG_TAG = BaseSpeechRecognitionEngine::class.java.simpleName
    }
}