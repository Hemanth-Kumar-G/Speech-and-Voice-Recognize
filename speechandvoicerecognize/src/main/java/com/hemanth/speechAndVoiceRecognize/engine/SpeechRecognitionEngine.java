package com.hemanth.speechAndVoiceRecognize.engine;

import android.content.Context;
import android.speech.RecognitionListener;


import com.hemanth.speechAndVoiceRecognize.GoogleVoiceTypingDisabledException;
import com.hemanth.speechAndVoiceRecognize.SpeechDelegate;
import com.hemanth.speechAndVoiceRecognize.SpeechRecognitionNotAvailable;
import com.hemanth.speechAndVoiceRecognize.ui.SpeechProgressView;

import java.util.Locale;

public interface SpeechRecognitionEngine extends RecognitionListener {

    void init(Context context);

    void clear();

    String getPartialResultsAsString();

    void initSpeechRecognizer(Context context);

    void startListening(SpeechProgressView progressView, SpeechDelegate delegate) throws SpeechRecognitionNotAvailable, GoogleVoiceTypingDisabledException;

    void stopListening();

    void returnPartialResultsAndRecreateSpeechRecognizer();

    void setPartialResults(boolean getPartialResults);

    void shutdown();

    boolean isListening();

    Locale getLocale();

    void setLocale(Locale locale);

    void setPreferOffline(boolean preferOffline);

    void setTransitionMinimumDelay(long milliseconds);

    void setStopListeningAfterInactivity(long milliseconds);

    void setCallingPackage(String callingPackage);

    void unregisterDelegate();
}
