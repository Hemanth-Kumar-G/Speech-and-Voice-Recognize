package com.hemanth.speechAndVoiceRecognize

/**
 * Contains the methods which are called to notify text to speech progress status.
 */
interface TextToSpeechCallback {
    fun onStart()
    fun onCompleted()
    fun onError()
}