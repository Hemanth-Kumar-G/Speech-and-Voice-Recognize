package com.hemanth.speechAndVoiceRecognize

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hemanth.speechAndVoiceRecognize.Speech

/**
 * Utility methods.
 */
object SpeechUtil {
    /**
     * Opens the Google App page on Play Store
     * @param context application context
     */
    fun redirectUserToGoogleAppOnPlayStore(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("market://details?id=" + Speech.GOOGLE_APP_PACKAGE))
        )
    }
}