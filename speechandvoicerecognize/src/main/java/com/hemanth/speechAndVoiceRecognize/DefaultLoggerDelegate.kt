package com.hemanth.speechAndVoiceRecognize

import android.util.Log
import com.hemanth.speechAndVoiceRecognize.Logger.LoggerDelegate


class DefaultLoggerDelegate : LoggerDelegate {
    override fun error(tag: String?, message: String?) {
        Log.e(TAG, "$tag - $message")
    }

    override fun error(tag: String?, message: String?, exception: Throwable?) {
        Log.e(TAG, "$tag - $message", exception)
    }

    override fun debug(tag: String?, message: String?) {
        Log.d(TAG, "$tag - $message")
    }

    override fun info(tag: String?, message: String?) {
        Log.i(TAG, "$tag - $message")
    }

    companion object {
        private val TAG = Speech::class.java.simpleName
    }
}