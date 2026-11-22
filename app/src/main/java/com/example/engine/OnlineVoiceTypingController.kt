package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * Ultra-accurate direct same-language voice typing controller backed by Android's
 * `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = false`, `EXTRA_PARTIAL_RESULTS = true`,
 * and `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE = true` (zero unwanted auto-translation).
 *
 * Maps the active keyboard language directly:
 * - Bengali (`bn`) -> `bn-BD` (with `bn-IN` fallback if unavailable)
 * - English (`en`) -> `en-IN`
 * - Hindi (`hi`) -> `hi-IN`
 */
class OnlineVoiceTypingController(
    private val context: Context,
    private val onStateChanged: (VoiceListeningState) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit
) {
    enum class VoiceLocale(val tag: String, val badge: String, val displayLabel: String) {
        BENGALI_BD("bn-BD", "বাংলা", "বাংলা (BD)"),
        BENGALI_IN("bn-IN", "বাংলা", "বাংলা (IN)"),
        ENGLISH_IN("en-IN", "EN", "English"),
        HINDI_IN("hi-IN", "हिन्दी", "हिन्दी"),
        ARABIC_SA("ar-SA", "العربية", "العربية"),
        URDU_PK("ur-PK", "اردو", "اردو");

        val isBengali: Boolean
            get() = this == BENGALI_BD || this == BENGALI_IN

        val isHindi: Boolean
            get() = this == HINDI_IN
    }

    data class VoiceListeningState(
        val isListening: Boolean = false,
        val locale: VoiceLocale = VoiceLocale.ENGLISH_IN,
        val partialTranscript: String = "",
        val errorMessage: String? = null
    )

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLocale: VoiceLocale = VoiceLocale.ENGLISH_IN
    private var isListening = false
    private var triedBnFallback = false
    private var lastEmittedPartial = ""

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun getCurrentLocale(): VoiceLocale = currentLocale

    fun isCurrentlyListening(): Boolean = isListening

    fun startListening(locale: VoiceLocale) {
        if (!hasAudioPermission()) {
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    locale = locale,
                    errorMessage = "Microphone permission required"
                )
            )
            return
        }

        if (!isAvailable()) {
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    locale = locale,
                    errorMessage = "Google Speech Recognition service unavailable"
                )
            )
            return
        }

        currentLocale = locale
        lastEmittedPartial = ""
        if (locale != VoiceLocale.BENGALI_IN) {
            triedBnFallback = false
        }

        stopInternal()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onStateChanged(
                    VoiceListeningState(
                        isListening = true,
                        locale = currentLocale,
                        partialTranscript = "",
                        errorMessage = null
                    )
                )
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                // Automatic bn-BD -> bn-IN fallback only if speech engine doesn't support bn-BD
                if (currentLocale == VoiceLocale.BENGALI_BD &&
                    !triedBnFallback &&
                    (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                ) {
                    triedBnFallback = true
                    startListening(VoiceLocale.BENGALI_IN)
                    return
                }

                // If we already captured accurate partial speech before a timeout/no-match, commit it cleanly
                if (lastEmittedPartial.isNotBlank() &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                ) {
                    val salvaged = lastEmittedPartial
                    lastEmittedPartial = ""
                    onFinalText(salvaged)
                    onStateChanged(
                        VoiceListeningState(
                            isListening = false,
                            locale = currentLocale,
                            partialTranscript = "",
                            errorMessage = null
                        )
                    )
                    return
                }

                lastEmittedPartial = ""
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for online voice typing"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard — tap mic to retry"
                    else -> "Voice typing paused — tap mic to retry"
                }
                onStateChanged(
                    VoiceListeningState(
                        isListening = false,
                        locale = currentLocale,
                        partialTranscript = "",
                        errorMessage = msg
                    )
                )
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()
                    .ifEmpty { lastEmittedPartial }
                lastEmittedPartial = ""
                if (text.isNotEmpty()) {
                    onFinalText(text)
                }
                onStateChanged(
                    VoiceListeningState(
                        isListening = false,
                        locale = currentLocale,
                        partialTranscript = "",
                        errorMessage = null
                    )
                )
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    lastEmittedPartial = partial
                    onPartialText(partial)
                    onStateChanged(
                        VoiceListeningState(
                            isListening = true,
                            locale = currentLocale,
                            partialTranscript = partial,
                            errorMessage = null
                        )
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.tag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.tag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        isListening = true
        onStateChanged(
            VoiceListeningState(
                isListening = true,
                locale = currentLocale,
                partialTranscript = "",
                errorMessage = null
            )
        )
        runCatching { recognizer.startListening(intent) }.onFailure {
            isListening = false
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    locale = currentLocale,
                    errorMessage = "Unable to start online voice recognition"
                )
            )
        }
    }

    fun stopListening() {
        lastEmittedPartial = ""
        stopInternal()
        onStateChanged(
            VoiceListeningState(
                isListening = false,
                locale = currentLocale,
                partialTranscript = "",
                errorMessage = null
            )
        )
    }

    fun destroy() {
        lastEmittedPartial = ""
        stopInternal()
    }

    private fun stopInternal() {
        isListening = false
        speechRecognizer?.let { rec ->
            runCatching { rec.cancel() }
            runCatching { rec.destroy() }
        }
        speechRecognizer = null
    }

    companion object {
        fun localeForLanguageId(langId: String): VoiceLocale = when (langId) {
            "bn" -> VoiceLocale.BENGALI_BD
            "hi" -> VoiceLocale.HINDI_IN
            "ar" -> VoiceLocale.ARABIC_SA
            "ur" -> VoiceLocale.URDU_PK
            else -> VoiceLocale.ENGLISH_IN
        }
    }
}
