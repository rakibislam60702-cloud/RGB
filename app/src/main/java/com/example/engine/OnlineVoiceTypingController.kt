package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.model.LanguageCatalog
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ultra-accurate direct same-language & Voice-to-Banglish controller backed by Android's
 * `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = false`, `EXTRA_PARTIAL_RESULTS = true`,
 * and `EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE = true`.
 *
 * Modes:
 * - Quick Single Tap (`lockedContinuous = false`): listens for one sentence and auto-stops cleanly.
 * - Hold / Long-Press (`lockedContinuous = true`): hands-free Continuous Voice Lock that never
 *   auto-stops across pauses or finished sentences until the user explicitly taps Mic again.
 * - Single subtle start chime on initial start only; suppresses system end-of-speech beeps.
 * - Maps `bn_en` (`বাংলিশ (BN-EN)`) to `bn-IN` and converts Bengali speech in real time into
 *   natural Indian / West Bengal Romanized Bengali via [BengaliTransliterator].
 */
class OnlineVoiceTypingController(
    private val context: Context,
    private val onStateChanged: (VoiceListeningState) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit
) {
    enum class VoiceLocale(val tag: String, val badge: String, val displayLabel: String) {
        BANGLISH_IN("bn-IN", "বাংলিশ", "বাংলিশ (BN-EN)"),
        BENGALI_BD("bn-BD", "বাংলা", "বাংলা (BD)"),
        BENGALI_IN("bn-IN", "বাংলা", "বাংলা (IN)"),
        ENGLISH_IN("en-IN", "EN", "English"),
        HINDI_IN("hi-IN", "हिन्दी", "हिन्दी"),
        ARABIC_SA("ar-SA", "العربية", "العربية"),
        URDU_PK("ur-PK", "اردو", "اردو");

        val isBengali: Boolean
            get() = this == BENGALI_BD || this == BENGALI_IN || this == BANGLISH_IN

        val isBanglish: Boolean
            get() = this == BANGLISH_IN

        val isHindi: Boolean
            get() = this == HINDI_IN
    }

    data class VoiceListeningState(
        val isListening: Boolean = false,
        val isLockedContinuous: Boolean = false,
        val locale: VoiceLocale = VoiceLocale.ENGLISH_IN,
        val partialTranscript: String = "",
        val errorMessage: String? = null
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLocale: VoiceLocale = VoiceLocale.ENGLISH_IN
    private var isListening = false
    private var isLockedContinuous = false
    private var isUserStopped = false
    private var triedBnFallback = false
    private var banglishRecognitionTag: String = "bn-IN"
    private var lastEmittedPartial = ""
    private var systemBeepMuted = false

    private val unmuteRunnable = Runnable { unmuteSystemBeepIfNeeded() }
    private val continuousRestartRunnable = Runnable {
        if (isLockedContinuous && !isUserStopped && hasAudioPermission()) {
            startSessionInternal(playChime = false)
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun getCurrentLocale(): VoiceLocale = currentLocale

    fun isCurrentlyListening(): Boolean = isListening || isLockedContinuous

    fun isContinuousLocked(): Boolean = isLockedContinuous

    fun startListening(locale: VoiceLocale, lockedContinuous: Boolean = false) {
        if (!hasAudioPermission()) {
            isLockedContinuous = false
            isListening = false
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    isLockedContinuous = false,
                    locale = locale,
                    errorMessage = "Microphone permission required"
                )
            )
            return
        }

        if (!isAvailable()) {
            isLockedContinuous = false
            isListening = false
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    isLockedContinuous = false,
                    locale = locale,
                    errorMessage = "Google Speech Recognition service unavailable"
                )
            )
            return
        }

        isUserStopped = false
        currentLocale = locale
        isLockedContinuous = lockedContinuous
        lastEmittedPartial = ""
        triedBnFallback = false
        banglishRecognitionTag = "bn-IN"

        mainHandler.removeCallbacks(continuousRestartRunnable)
        startSessionInternal(playChime = true)
    }

    /**
     * Upgrades an already-active listening session into Hands-Free Continuous Lock
     * without re-playing the start chime or dropping the active stream.
     */
    fun upgradeToContinuousLock() {
        if (!isListening || isUserStopped) return
        if (!isLockedContinuous) {
            isLockedContinuous = true
            onStateChanged(
                VoiceListeningState(
                    isListening = true,
                    isLockedContinuous = true,
                    locale = currentLocale,
                    partialTranscript = lastEmittedPartial,
                    errorMessage = null
                )
            )
        }
    }

    private fun startSessionInternal(playChime: Boolean) {
        stopInternal()

        // Suppress Android's default SpeechRecognizer start/end beep so only our single subtle chime plays
        muteSystemBeepBriefly(durationMs = 450L)
        if (playChime) {
            playSubtleStartChime()
        }

        val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (recognizer == null) {
            isListening = false
            isLockedContinuous = false
            unmuteSystemBeepIfNeeded()
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    isLockedContinuous = false,
                    locale = currentLocale,
                    errorMessage = "Unable to initialize voice recognizer"
                )
            )
            return
        }

        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (isUserStopped) return
                isListening = true
                onStateChanged(
                    VoiceListeningState(
                        isListening = true,
                        isLockedContinuous = isLockedContinuous,
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
                // Mute the recognizer's trailing end-of-speech chime
                muteSystemBeepBriefly(durationMs = 420L)
                if (!isLockedContinuous) {
                    isListening = false
                }
            }

            override fun onError(error: Int) {
                if (isUserStopped) return
                muteSystemBeepBriefly(durationMs = 360L)

                // Automatic bn-IN <-> bn-BD fallback if speech engine doesn't support one regional Bengali tag
                if ((currentLocale == VoiceLocale.BENGALI_IN ||
                        currentLocale == VoiceLocale.BENGALI_BD ||
                        currentLocale == VoiceLocale.BANGLISH_IN) &&
                    !triedBnFallback &&
                    (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                ) {
                    triedBnFallback = true
                    if (currentLocale == VoiceLocale.BANGLISH_IN) {
                        banglishRecognitionTag = if (banglishRecognitionTag == "bn-IN") "bn-BD" else "bn-IN"
                    } else {
                        currentLocale = if (currentLocale == VoiceLocale.BENGALI_IN) {
                            VoiceLocale.BENGALI_BD
                        } else {
                            VoiceLocale.BENGALI_IN
                        }
                    }
                    startSessionInternal(playChime = false)
                    return
                }

                // Salvage accurate partial speech before timeout/no-match
                if (lastEmittedPartial.isNotBlank() &&
                    (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                ) {
                    val salvaged = lastEmittedPartial
                    lastEmittedPartial = ""
                    onFinalText(salvaged)
                    if (isLockedContinuous) {
                        scheduleContinuousRestart()
                        return
                    }
                    isListening = false
                    onStateChanged(
                        VoiceListeningState(
                            isListening = false,
                            isLockedContinuous = false,
                            locale = currentLocale,
                            partialTranscript = "",
                            errorMessage = null
                        )
                    )
                    return
                }

                lastEmittedPartial = ""

                // In Hands-Free Continuous Lock mode, seamlessly keep listening across silences/pauses
                if (isLockedContinuous &&
                    error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
                    error != SpeechRecognizer.ERROR_NETWORK &&
                    error != SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                ) {
                    scheduleContinuousRestart()
                    return
                }

                isListening = false
                isLockedContinuous = false
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
                        isLockedContinuous = false,
                        locale = currentLocale,
                        partialTranscript = "",
                        errorMessage = msg
                    )
                )
            }

            override fun onResults(results: Bundle?) {
                if (isUserStopped) return
                muteSystemBeepBriefly(durationMs = 360L)

                val rawText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()

                val finalFormatted = if (rawText.isNotEmpty()) {
                    transformTranscript(rawText)
                } else {
                    lastEmittedPartial
                }
                lastEmittedPartial = ""

                if (finalFormatted.isNotEmpty()) {
                    onFinalText(finalFormatted)
                }

                if (isLockedContinuous) {
                    // Never auto-stop in locked continuous mode — immediately reopen silent stream
                    scheduleContinuousRestart()
                } else {
                    // Quick single tap auto-stops cleanly after the sentence finishes
                    isListening = false
                    stopInternal()
                    onStateChanged(
                        VoiceListeningState(
                            isListening = false,
                            isLockedContinuous = false,
                            locale = currentLocale,
                            partialTranscript = "",
                            errorMessage = null
                        )
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (isUserStopped) return
                val rawPartial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()
                if (rawPartial.isNotEmpty()) {
                    val formatted = transformTranscript(rawPartial)
                    lastEmittedPartial = formatted
                    onPartialText(formatted)
                    onStateChanged(
                        VoiceListeningState(
                            isListening = true,
                            isLockedContinuous = isLockedContinuous,
                            locale = currentLocale,
                            partialTranscript = formatted,
                            errorMessage = null
                        )
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val recognitionTag = if (currentLocale == VoiceLocale.BANGLISH_IN) {
            banglishRecognitionTag
        } else {
            currentLocale.tag
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (isLockedContinuous) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
            } else {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1100L)
            }
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        isListening = true
        onStateChanged(
            VoiceListeningState(
                isListening = true,
                isLockedContinuous = isLockedContinuous,
                locale = currentLocale,
                partialTranscript = "",
                errorMessage = null
            )
        )
        runCatching { recognizer.startListening(intent) }.onFailure {
            isListening = false
            isLockedContinuous = false
            unmuteSystemBeepIfNeeded()
            onStateChanged(
                VoiceListeningState(
                    isListening = false,
                    isLockedContinuous = false,
                    locale = currentLocale,
                    errorMessage = "Unable to start online voice recognition"
                )
            )
        }
    }

    private fun scheduleContinuousRestart() {
        if (!isLockedContinuous || isUserStopped) return
        isListening = true
        onStateChanged(
            VoiceListeningState(
                isListening = true,
                isLockedContinuous = true,
                locale = currentLocale,
                partialTranscript = "",
                errorMessage = null
            )
        )
        mainHandler.removeCallbacks(continuousRestartRunnable)
        mainHandler.postDelayed(continuousRestartRunnable, 180L)
    }

    private fun transformTranscript(rawText: String): String {
        return if (currentLocale == VoiceLocale.BANGLISH_IN) {
            BengaliTransliterator
                .transliterate(rawText, capitalizeSentences = true)
                .replace("।", ".")
                .replace("॥", ".")
                .trim()
        } else {
            rawText
        }
    }

    fun stopListening() {
        isUserStopped = true
        isLockedContinuous = false
        lastEmittedPartial = ""
        mainHandler.removeCallbacks(continuousRestartRunnable)
        muteSystemBeepBriefly(durationMs = 320L)
        stopInternal()
        onStateChanged(
            VoiceListeningState(
                isListening = false,
                isLockedContinuous = false,
                locale = currentLocale,
                partialTranscript = "",
                errorMessage = null
            )
        )
    }

    fun destroy() {
        isUserStopped = true
        isLockedContinuous = false
        lastEmittedPartial = ""
        mainHandler.removeCallbacks(continuousRestartRunnable)
        mainHandler.removeCallbacks(unmuteRunnable)
        stopInternal()
        unmuteSystemBeepIfNeeded()
    }

    private fun stopInternal() {
        isListening = false
        speechRecognizer?.let { rec ->
            runCatching { rec.cancel() }
            runCatching { rec.destroy() }
        }
        speechRecognizer = null
    }

    /**
     * Briefly mutes Android's notification/system recognition beep so end-of-speech and
     * continuous-lock restarts stay completely silent. Always restores cleanly.
     */
    private fun muteSystemBeepBriefly(durationMs: Long) {
        val am = audioManager ?: return
        mainHandler.removeCallbacks(unmuteRunnable)
        if (!systemBeepMuted) {
            runCatching {
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                systemBeepMuted = true
            }
        }
        mainHandler.postDelayed(unmuteRunnable, durationMs)
    }

    private fun unmuteSystemBeepIfNeeded() {
        val am = audioManager ?: return
        if (systemBeepMuted) {
            systemBeepMuted = false
            runCatching {
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
    }

    /**
     * Plays ONE crisp, subtle 55ms soft chime when voice listening starts.
     * Never plays on end-of-speech or during hands-free continuous lock restarts.
     */
    private fun playSubtleStartChime() {
        Thread {
            runCatching {
                val sampleRate = 22050
                val durationMs = 55
                val numSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    val progress = i.toDouble() / numSamples.toDouble()
                    // Gentle upward two-harmonic soft glass chime with smooth fade-in/out envelope
                    val freq = 680.0 + 180.0 * progress
                    val env = sin(PI * progress)
                    val sample = (sin(2.0 * PI * freq * t) * 0.72 + sin(2.0 * PI * (freq * 1.5) * t) * 0.28) * env
                    pcm[i] = (sample * Short.MAX_VALUE * 0.22).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(75L)
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }.start()
    }

    companion object {
        fun localeForLanguageId(langId: String): VoiceLocale = when (langId) {
            LanguageCatalog.BANGLISH_ID -> VoiceLocale.BANGLISH_IN
            "bn" -> VoiceLocale.BENGALI_IN
            "hi" -> VoiceLocale.HINDI_IN
            "ar" -> VoiceLocale.ARABIC_SA
            "ur" -> VoiceLocale.URDU_PK
            else -> VoiceLocale.ENGLISH_IN
        }
    }
}
