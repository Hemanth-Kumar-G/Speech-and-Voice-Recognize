package com.hemanth.app

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.Voice
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hemanth.speechAndVoiceRecognize.*
import com.hemanth.speechAndVoiceRecognize.SpeechUtil.redirectUserToGoogleAppOnPlayStore
import com.hemanth.speechAndVoiceRecognize.ui.SpeechProgressView
import java.util.*

private const val PERMISSIONS_REQUEST = 1

class MainActivity : AppCompatActivity(), SpeechDelegate {

    private lateinit var button: ImageButton
    private lateinit var speak: Button
    private lateinit var text: TextView
    private lateinit var textToSpeech: EditText
    private lateinit var progress: SpeechProgressView
    private lateinit var linearLayout: LinearLayout

    private val mTttsInitListener = OnInitListener { status ->
        when (status) {
            TextToSpeech.SUCCESS -> Logger.info(LOG_TAG, "TextToSpeech engine successfully started")
            TextToSpeech.ERROR -> Logger.error(
                LOG_TAG, "Error while initializing TextToSpeech engine!"
            )
            else -> Logger.error(LOG_TAG, "Unknown TextToSpeech status: $status")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Speech.init(this, packageName, mTttsInitListener)

        linearLayout = findViewById(R.id.linearLayout)
        button = findViewById(R.id.button)
        button.setOnClickListener { onButtonClick() }
        speak = findViewById(R.id.speak)
        speak.setOnClickListener { onSpeakClick() }
        text = findViewById(R.id.text)
        textToSpeech = findViewById(R.id.textToSpeech)
        progress = findViewById(R.id.progress)

        val colors = intArrayOf(
            ContextCompat.getColor(this, android.R.color.black),
            ContextCompat.getColor(this, android.R.color.darker_gray),
            ContextCompat.getColor(this, android.R.color.black),
            ContextCompat.getColor(this, android.R.color.holo_orange_dark),
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
        progress.setColors(colors)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.supportedSTTLanguages -> {
                onSetSpeechToTextLanguage()
                true
            }
            R.id.supportedTTSLanguages -> {
                onSetTextToSpeechVoice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onSetSpeechToTextLanguage() {
        Speech.getInstance()
            ?.getSupportedSpeechToTextLanguages(object : SupportedLanguagesListener {

                override fun onSupportedLanguages(supportedLanguages: List<String>) {

                    val items = supportedLanguages.toTypedArray()

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Current language: " + Speech.getInstance()?.speechToTextLanguage)
                        .setItems(items) { _, i ->
                            val locale: Locale = Locale.forLanguageTag(supportedLanguages[i])

                            Speech.getInstance()?.setLocale(locale)
                            Toast.makeText(
                                this@MainActivity, "Selected: " + items[i], Toast.LENGTH_LONG
                            ).show()
                        }
                        .setPositiveButton("Cancel", null)
                        .create()
                        .show()
                }

                override fun onNotSupported(reason: UnsupportedReason) {
                    when (reason) {
                        UnsupportedReason.GOOGLE_APP_NOT_FOUND -> showSpeechNotSupportedDialog()
                        UnsupportedReason.EMPTY_SUPPORTED_LANGUAGES -> AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.set_stt_langs)
                            .setMessage(R.string.no_langs)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            })
    }

    private fun onSetTextToSpeechVoice() {
        val supportedVoices = Speech.getInstance()?.supportedTextToSpeechVoices
        if (supportedVoices?.isEmpty() == true) {
            AlertDialog.Builder(this)
                .setTitle(R.string.set_tts_voices)
                .setMessage(R.string.no_tts_voices)
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Sort TTS voices
        supportedVoices?.sortedWith { v1: Voice, v2: Voice ->
            v1.toString().compareTo(v2.toString())
        }

        val items = supportedVoices?.size?.let { arrayOfNulls<CharSequence>(it) }
        val iterator: Iterator<Voice>? = supportedVoices?.iterator()
        var i = 0
        while (iterator?.hasNext() == true) {
            val voice = iterator.next()
            items?.set(i, voice.toString())
            i++
        }
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Current: " + Speech.getInstance()?.textToSpeechVoice)
            .setItems(items) { _, index ->
                Speech.getInstance()?.setVoice(supportedVoices?.get(index))
                Toast.makeText(
                    this@MainActivity,
                    "Selected: " + (items?.get(index) ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
            .setPositiveButton("Cancel", null)
            .create()
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Speech.getInstance()?.shutdown()
    }

    private fun onButtonClick() {
        if (Speech.getInstance()?.isListening == true) {
            Speech.getInstance()?.stopListening()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onRecordAudioPermissionGranted()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSIONS_REQUEST
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != PERMISSIONS_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                onRecordAudioPermissionGranted()
            } else {
                // permission denied, boo!
                Toast.makeText(this@MainActivity, R.string.permission_required, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun onRecordAudioPermissionGranted() {
        button.visibility = View.GONE
        linearLayout.visibility = View.VISIBLE
        try {
            Speech.getInstance()?.stopTextToSpeech()
            Speech.getInstance()?.startListening(progress, this@MainActivity)
        } catch (exc: SpeechRecognitionNotAvailable) {
            showSpeechNotSupportedDialog()
        } catch (exc: GoogleVoiceTypingDisabledException) {
            showEnableGoogleVoiceTyping()
        }
    }

    private fun onSpeakClick() {
        if (textToSpeech.text.toString().trim { it <= ' ' }.isEmpty()) {
            Toast.makeText(this, R.string.input_something, Toast.LENGTH_LONG).show()
            return
        }
        Speech.getInstance()
            ?.say(textToSpeech.text.toString().trim { it <= ' ' }, object : TextToSpeechCallback {
                override fun onStart() {
                    Toast.makeText(this@MainActivity, "TTS onStart", Toast.LENGTH_SHORT).show()
                }

                override fun onCompleted() {
                    Toast.makeText(this@MainActivity, "TTS onCompleted", Toast.LENGTH_SHORT).show()
                }

                override fun onError() {
                    Toast.makeText(this@MainActivity, "TTS onError", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onStartOfSpeech() {}
    override fun onSpeechRmsChanged(value: Float) {
        //Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    override fun onSpeechResult(result: String?) {
        button.visibility = View.VISIBLE
        linearLayout.visibility = View.GONE
        text.text = result
        if (result!!.isEmpty()) {
            Speech.getInstance()?.say(getString(R.string.repeat))
        } else {
            Speech.getInstance()?.say(result)
        }
    }

    override fun onSpeechPartialResults(results: List<String?>?) {
        text.text = ""
        for (partial in results!!) {
            text.append("$partial ")
        }
    }

    private fun showSpeechNotSupportedDialog() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> redirectUserToGoogleAppOnPlayStore(this@MainActivity)
                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.speech_not_available)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, dialogClickListener)
            .setNegativeButton(R.string.no, dialogClickListener)
            .show()
    }

    private fun showEnableGoogleVoiceTyping() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.enable_google_voice_typing)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { _, _ -> }
            .show()
    }

    companion object {
        private val LOG_TAG = MainActivity::class.java.simpleName
    }
}
