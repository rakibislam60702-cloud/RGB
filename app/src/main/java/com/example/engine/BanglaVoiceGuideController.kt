package com.example.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
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
    val activeScript: BanglaGuideScript? = null
)

/**
 * Interactive Bengali Text-to-Speech Voice Guide configured to `bn-BD` with automatic
 * fallback to `bn-IN`. Handles first-launch welcome, automatic step progression speech,
 * on-demand tap-to-listen playback, and mute/stop control.
 */
class BanglaVoiceGuideController(
    context: Context
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = null
    private var pendingScript: BanglaGuideScript? = null

    private val _state = MutableStateFlow(
        VoiceGuideState(
            isReady = false,
            isSpeaking = false,
            isMuted = prefs.getBoolean(KEY_MUTED, false),
            activeScript = null
        )
    )
    val state: StateFlow<VoiceGuideState> = _state.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(appContext, this)
        }
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            _state.value = _state.value.copy(isReady = false)
            return
        }

        val bdLocale = Locale("bn", "BD")
        val inLocale = Locale("bn", "IN")
        val genericBn = Locale("bn")

        val bdResult = runCatching { engine.setLanguage(bdLocale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (bdResult == TextToSpeech.LANG_MISSING_DATA || bdResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            val inResult = runCatching { engine.setLanguage(inLocale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (inResult == TextToSpeech.LANG_MISSING_DATA || inResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                runCatching { engine.setLanguage(genericBn) }
            }
        }

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
                _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
            }
        })

        _state.value = _state.value.copy(isReady = true)

        pendingScript?.let { queued ->
            pendingScript = null
            if (!_state.value.isMuted) {
                speakInternal(queued)
            }
        }
    }

    /**
     * Automatically speaks the warm welcome & Step 1 voice instruction the very first time
     * the user opens the app (if the keyboard isn't enabled yet and voice isn't muted).
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
     * Speaks a specific Bengali instruction script. When invoked by a user tapping
     * "🔊 বাংলায় শুনে নিন", `forceUnmute = true` ensures audio plays right away.
     */
    fun speak(script: BanglaGuideScript, forceUnmute: Boolean = true) {
        if (forceUnmute && _state.value.isMuted) {
            setMuted(false)
        }
        if (_state.value.isMuted) return

        if (!_state.value.isReady) {
            pendingScript = script
            return
        }
        speakInternal(script)
    }

    fun stopSpeaking() {
        pendingScript = null
        runCatching { tts?.stop() }
        _state.value = _state.value.copy(isSpeaking = false, activeScript = null)
    }

    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
        if (muted) {
            stopSpeaking()
        }
        _state.value = _state.value.copy(isMuted = muted)
    }

    /**
     * Top-right button action: if currently speaking, stops immediately; otherwise toggles mute,
     * and when unmuting replays the most relevant current step voice instruction.
     */
    fun toggleStopOrMute(recommendedScript: BanglaGuideScript) {
        if (_state.value.isSpeaking) {
            stopSpeaking()
            return
        }
        val nextMuted = !_state.value.isMuted
        setMuted(nextMuted)
        if (!nextMuted) {
            speak(recommendedScript, forceUnmute = false)
        }
    }

    private fun speakInternal(script: BanglaGuideScript) {
        val engine = tts ?: return
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, script.utteranceId)
        }
        runCatching {
            engine.stop()
            engine.speak(script.bengaliText, TextToSpeech.QUEUE_FLUSH, params, script.utteranceId)
            _state.value = _state.value.copy(isSpeaking = true, activeScript = script)
        }
    }

    fun release() {
        pendingScript = null
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
