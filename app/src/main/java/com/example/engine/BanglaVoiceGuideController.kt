package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BanglaGuideScript(val utteranceId: String, val bengaliText: String) {
    WELCOME_STEP_1(
        utteranceId = "bn_guide_step_1",
        bengaliText = "রাকিব অফিশিয়াল কিবোর্ডে আপনাকে স্বাগতম। কিবোর্ডটি চালু করতে নিচের এক নম্বর বাটনে চাপ দিন এবং তালিকা থেকে রাকিব অফিশিয়াল অপশনটি অন করে দিন।"
    ),
    STEP_2(
        utteranceId = "bn_guide_step_2",
        bengaliText = "দারুণ! এবার দুই নম্বর বাটনে চাপ দিয়ে পপ-আপ তালিকা থেকে রাকিব অফিশিয়াল কিবোর্ডটি সিলেক্ট করুন।"
    ),
    COMPLETE(
        utteranceId = "bn_guide_complete",
        bengaliText = "অভিনন্দন! আপনার কিবোর্ড সম্পূর্ণ প্রস্তুত। পৃথিবী বাটনে চাপ দিয়ে বাংলা ও ইংরেজি পরিবর্তন করতে পারবেন এবং মাইক বাটনে চাপ দিয়ে বাংলায় ভয়েস টাইপিং করতে পারবেন।"
    ),
    USAGE_GUIDE(
        utteranceId = "bn_guide_usage",
        bengaliText = "রাকিব অফিশিয়াল কিবোর্ড ব্যবহার করা খুবই সহজ। পৃথিবী বাটনে চাপ দিয়ে বাংলা ও ইংরেজি পরিবর্তন করতে পারবেন। মাইক বাটনে চাপ দিয়ে বাংলায় ভয়েস টাইপিং করতে পারবেন। বাম দিকের তীর বাটনে দুবার চাপ দিয়ে ক্যাপস লক চালু করতে পারবেন, এবং ইফেক্টস ট্যাব থেকে ব্যানার বা গ্যালারির ছবি ব্যাকগ্রাউন্ডে বসাতে পারবেন।"
    ),
    PAYMENT_UNLOCKED(
        utteranceId = "bn_guide_payment_unlocked",
        bengaliText = "অভিনন্দন! আপনার পেমেন্ট সফল হয়েছে এবং রাকিব অফিশিয়াল কিবোর্ডের সমস্ত VIP ফিচার চিরজীবনের জন্য আনলক হয়েছে।"
    )
}

@Immutable
data class VoiceGuideState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val activeScript: BanglaGuideScript? = null,
    val usingOnlineFallback: Boolean = false
)

/**
 * 100% Reliable Automatic Bangla Voice Guide:
 * 1. Initializes Android TextToSpeech with `bn-BD` / `bn-IN` / `bn` and speaks automatically inside `onInit`.
 * 2. If offline Bengali TTS data is missing or unsupported (or fails at runtime), seamlessly falls back
 *    to streaming natural Bengali TTS audio via Android MediaPlayer (`translate.google.com/translate_tts`),
 *    chunking longer sentences cleanly so audio plays reliably on every device without extra downloads.
 */
class BanglaVoiceGuideController(
    context: Context
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized: Boolean = false
    private var hasOfflineBengaliTts: Boolean = false
    private var pendingScript: BanglaGuideScript? = null
    private var onlineChunkQueue: ArrayDeque<String> = ArrayDeque()
    private var activeOnlineScript: BanglaGuideScript? = null
    private var playbackGeneration: Int = 0

    private val _state = MutableStateFlow(
        VoiceGuideState(
            isReady = false,
            isSpeaking = false,
            isMuted = prefs.getBoolean(KEY_MUTED, false),
            activeScript = null,
            usingOnlineFallback = false
        )
    )
    val state: StateFlow<VoiceGuideState> = _state.asStateFlow()

    init {
        val created = runCatching {
            TextToSpeech(appContext, this)
        }.getOrNull()

        if (created != null) {
            tts = created
        } else {
            // If TextToSpeech constructor throws on an unusual ROM, activate online MediaPlayer fallback immediately
            isInitialized = true
            hasOfflineBengaliTts = false
            _state.value = _state.value.copy(isReady = true, usingOnlineFallback = true)
        }
    }

    override fun onInit(status: Int) {
        isInitialized = true
        val engine = tts
        var bengaliAvailable = false

        if (status == TextToSpeech.SUCCESS && engine != null) {
            val localesToTry = listOf(
                Locale("bn", "BD"),
                Locale("bn", "IN"),
                Locale("bn")
            )
            for (locale in localesToTry) {
                val res = runCatching { engine.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    bengaliAvailable = true
                    break
                }
            }

            if (bengaliAvailable) {
                engine.setSpeechRate(0.95f)
                engine.setPitch(1.0f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val script = BanglaGuideScript.entries.firstOrNull { it.utteranceId == utteranceId }
                        _state.value = _state.value.copy(isSpeaking = true, activeScript = script)
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        val failedScript = BanglaGuideScript.entries.firstOrNull { it.utteranceId == utteranceId }
                        fallbackToOnlineIfPossible(failedScript)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        val failedScript = BanglaGuideScript.entries.firstOrNull { it.utteranceId == utteranceId }
                        fallbackToOnlineIfPossible(failedScript)
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
                    }
                })
            }
        }

        hasOfflineBengaliTts = bengaliAvailable
        _state.value = _state.value.copy(
            isReady = true,
            usingOnlineFallback = !bengaliAvailable
        )

        // Speak queued first-launch or progression script automatically right inside onInit
        val queued = pendingScript
        pendingScript = null
        if (queued != null && !_state.value.isMuted) {
            speakNow(queued)
        }
    }

    private fun fallbackToOnlineIfPossible(script: BanglaGuideScript?) {
        mainHandler.post {
            hasOfflineBengaliTts = false
            _state.value = _state.value.copy(usingOnlineFallback = true)
            if (script != null && !_state.value.isMuted) {
                speakViaOnlineMediaPlayer(script)
            } else {
                _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            }
        }
    }

    /**
     * Automatically speaks the warm Bengali welcome & Step 1 instruction on first launch
     * (or Step 2 / Complete depending on current keyboard setup status).
     */
    fun triggerFirstLaunchWelcomeIfNeeded(isEnabled: Boolean, isSelected: Boolean) {
        if (_state.value.isMuted) return
        val alreadySpoken = prefs.getBoolean(KEY_FIRST_WELCOME_SPOKEN, false)
        if (alreadySpoken) return

        prefs.edit().putBoolean(KEY_FIRST_WELCOME_SPOKEN, true).apply()
        val script = when {
            !isEnabled -> BanglaGuideScript.WELCOME_STEP_1
            !isSelected -> BanglaGuideScript.STEP_2
            else -> BanglaGuideScript.COMPLETE
        }
        speak(script, forceUnmute = false)
    }

    /**
     * Speaks a specific Bengali instruction script via offline Android TTS if Bengali is installed,
     * or seamlessly streams natural Bengali TTS audio via MediaPlayer fallback.
     */
    fun speak(script: BanglaGuideScript, forceUnmute: Boolean = true) {
        if (forceUnmute && _state.value.isMuted) {
            setMuted(false)
        }
        if (_state.value.isMuted) return

        if (!isInitialized) {
            pendingScript = script
            return
        }
        speakNow(script)
    }

    private fun speakNow(script: BanglaGuideScript) {
        if (hasOfflineBengaliTts && tts != null) {
            val spokeOffline = speakOfflineInternal(script)
            if (!spokeOffline) {
                hasOfflineBengaliTts = false
                _state.value = _state.value.copy(usingOnlineFallback = true)
                speakViaOnlineMediaPlayer(script)
            }
        } else {
            speakViaOnlineMediaPlayer(script)
        }
    }

    private fun speakOfflineInternal(script: BanglaGuideScript): Boolean {
        stopOnlinePlayerOnly()
        val engine = tts ?: return false
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, script.utteranceId)
        }
        return runCatching {
            engine.stop()
            val result = engine.speak(script.bengaliText, TextToSpeech.QUEUE_FLUSH, params, script.utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                _state.value = _state.value.copy(isSpeaking = true, activeScript = script)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun speakViaOnlineMediaPlayer(script: BanglaGuideScript) {
        runCatching { tts?.stop() }
        stopOnlinePlayerOnly()

        val chunks = splitIntoTtsChunks(script.bengaliText)
        if (chunks.isEmpty()) return

        activeOnlineScript = script
        onlineChunkQueue = ArrayDeque(chunks)
        val gen = ++playbackGeneration
        _state.value = _state.value.copy(
            isSpeaking = true,
            activeScript = script,
            usingOnlineFallback = true
        )
        playNextOnlineChunk(gen)
    }

    private fun playNextOnlineChunk(generation: Int) {
        if (generation != playbackGeneration || _state.value.isMuted) {
            stopOnlinePlayerOnly()
            _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            return
        }

        val nextText = onlineChunkQueue.removeFirstOrNull()
        if (nextText == null) {
            activeOnlineScript = null
            _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            return
        }

        val encoded = runCatching { URLEncoder.encode(nextText, "UTF-8") }.getOrNull()
        if (encoded.isNullOrBlank()) {
            playNextOnlineChunk(generation)
            return
        }

        val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=bn&client=tw-ob&q=$encoded"

        releaseMediaPlayerInstance()
        val player = MediaPlayer()
        mediaPlayer = player

        val started = runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build()
            )
            player.setDataSource(
                appContext,
                Uri.parse(ttsUrl),
                mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            )
            player.setOnPreparedListener { mp ->
                if (generation == playbackGeneration && !_state.value.isMuted) {
                    runCatching { mp.start() }
                } else {
                    releaseMediaPlayerInstance()
                }
            }
            player.setOnCompletionListener {
                if (generation == playbackGeneration) {
                    playNextOnlineChunk(generation)
                }
            }
            player.setOnErrorListener { _, _, _ ->
                if (generation == playbackGeneration) {
                    stopOnlinePlayerOnly()
                    _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
                }
                true
            }
            player.prepareAsync()
            true
        }.getOrDefault(false)

        if (!started) {
            stopOnlinePlayerOnly()
            _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
        }
    }

    private fun splitIntoTtsChunks(text: String, maxLen: Int = 175): List<String> {
        val sentences = text.split("।", "!", "?")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val result = mutableListOf<String>()
        for (sentence in sentences) {
            if (sentence.length <= maxLen) {
                result.add(sentence)
            } else {
                var current = StringBuilder()
                for (word in sentence.split(" ")) {
                    if (current.length + word.length + 1 > maxLen && current.isNotEmpty()) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(word)
                }
                if (current.isNotEmpty()) {
                    result.add(current.toString().trim())
                }
            }
        }
        return result
    }

    fun stopSpeaking() {
        pendingScript = null
        playbackGeneration++
        runCatching { tts?.stop() }
        stopOnlinePlayerOnly()
        _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
    }

    private fun stopOnlinePlayerOnly() {
        onlineChunkQueue.clear()
        activeOnlineScript = null
        releaseMediaPlayerInstance()
    }

    private fun releaseMediaPlayerInstance() {
        val existing = mediaPlayer
        mediaPlayer = null
        if (existing != null) {
            runCatching {
                if (existing.isPlaying) existing.stop()
                existing.reset()
                existing.release()
            }
        }
    }

    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
        if (muted) {
            stopSpeaking()
        }
        _state.value = _state.value.copy(isMuted = muted)
    }

    /**
     * Compact hero icon action:
     * - If currently speaking: stops immediately.
     * - If muted: unmutes and speaks the most relevant step instruction.
     * - If unmuted & idle: replays the current step instruction (long/second tap stops, or toggles mute).
     */
    fun toggleStopOrMute(recommendedScript: BanglaGuideScript) {
        if (_state.value.isSpeaking) {
            stopSpeaking()
            return
        }
        if (_state.value.isMuted) {
            setMuted(false)
            speak(recommendedScript, forceUnmute = false)
        } else {
            // Replay current step guidance immediately when tapped while idle
            speak(recommendedScript, forceUnmute = true)
        }
    }

    fun release() {
        pendingScript = null
        playbackGeneration++
        stopOnlinePlayerOnly()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    companion object {
        private const val PREFS_NAME = "rakib_bangla_voice_guide_prefs"
        private const val KEY_MUTED = "voice_guide_muted"
        private const val KEY_FIRST_WELCOME_SPOKEN = "first_welcome_spoken"
    }
}
