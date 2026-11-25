package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.KeyboardSettings
import com.example.model.SwitchSoundProfile
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesizes realistic low-latency mechanical switch acoustics and tactile haptics.
 * Safely degrades on devices/emulators without audio or vibrator hardware.
 */
class MechanicalFeedbackEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioExecutor = Executors.newSingleThreadExecutor()
    @Volatile
    private var lastSoundDispatchMs: Long = 0L

    private val sampleRate = 22050
    private val samsungSoftPcm: ShortArray by lazy { synthesizeSamsungSoft() }
    private val clickyPcm: ShortArray by lazy { synthesizeClickyBlue() }
    private val tactilePcm: ShortArray by lazy { synthesizeTactileBrown() }
    private val linearPcm: ShortArray by lazy { synthesizeLinearRed() }

    init {
        // Pre-warm default acoustic PCM off the main UI thread
        runCatching { audioExecutor.execute { samsungSoftPcm } }
    }

    fun triggerKeyFeedback(settings: KeyboardSettings, isModifier: Boolean = false) {
        if (settings.hapticEnabled) {
            triggerHaptic(settings.hapticStrength, isModifier)
        }
        if (settings.switchSound != SwitchSoundProfile.SILENT) {
            triggerSwitchSound(settings.switchSound)
        }
    }

    fun triggerSwitchAudition(profile: SwitchSoundProfile, hapticEnabled: Boolean, hapticStrength: Float) {
        if (hapticEnabled) {
            triggerHaptic(hapticStrength, isModifier = false)
        }
        if (profile != SwitchSoundProfile.SILENT) {
            triggerSwitchSound(profile)
        }
    }

    private fun triggerHaptic(strength: Float, isModifier: Boolean) {
        val v = vibrator ?: return
        runCatching {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && strength <= 0.55f && !isModifier) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            }
            val durationMs = (7L + (strength * if (isModifier) 15f else 11f).toLong()).coerceIn(6L, 26L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (38 + (strength * 185f).toInt()).coerceIn(1, 255)
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun triggerSwitchSound(profile: SwitchSoundProfile) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastSoundDispatchMs < 20L) return
        lastSoundDispatchMs = now

        runCatching {
            audioExecutor.execute {
                val pcm = when (profile) {
                    SwitchSoundProfile.SAMSUNG_SOFT -> samsungSoftPcm
                    SwitchSoundProfile.CLICKY_BLUE -> clickyPcm
                    SwitchSoundProfile.TACTILE_BROWN -> tactilePcm
                    SwitchSoundProfile.LINEAR_RED -> linearPcm
                    SwitchSoundProfile.SILENT -> return@execute
                }
            val played = runCatching {
                val bufferBytes = pcm.size * 2
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(28L)
                track.stop()
                track.release()
                true
            }.getOrDefault(false)

                if (!played) {
                    runCatching {
                        audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.25f)
                    }
                }
            }
        }
    }

    /**
     * Subtle, low-volume Soft Modern keypress tap: gentle rounded pitch droplet with damped body.
     */
    private fun synthesizeSamsungSoft(): ShortArray {
        val numSamples = (sampleRate * 0.013).toInt()
        val data = ShortArray(numSamples)
        val rng = Random(2026)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 390.0)
            val freq = 580.0 - 320.0 * (1.0 - exp(-t * 300.0))
            val body = sin(2.0 * PI * freq * t) * 0.74 + sin(2.0 * PI * (freq * 1.65) * t) * 0.14
            val softTap = (rng.nextDouble() * 2.0 - 1.0) * 0.07 * exp(-t * 980.0)
            val sample = (body * env + softTap).coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE * 0.24).toInt().toShort()
        }
        return data
    }

    private fun synthesizeClickyBlue(): ShortArray {
        val numSamples = (sampleRate * 0.022).toInt()
        val data = ShortArray(numSamples)
        val rng = Random(42)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Crisp high-frequency metallic click jacket + light housing thock
            val clickEnv = exp(-t * 420.0)
            val snap = sin(2.0 * PI * 3200.0 * t) * 0.55 + sin(2.0 * PI * 5100.0 * t) * 0.3
            val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.45
            val thockEnv = if (t > 0.004) exp(-(t - 0.004) * 240.0) else 0.0
            val thock = sin(2.0 * PI * 420.0 * t) * 0.5 * thockEnv
            val sample = ((snap + noise) * clickEnv + thock).coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        return data
    }

    private fun synthesizeTactileBrown(): ShortArray {
        val numSamples = (sampleRate * 0.020).toInt()
        val data = ShortArray(numSamples)
        val rng = Random(108)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 280.0)
            val bump = sin(2.0 * PI * 980.0 * t) * 0.45 + sin(2.0 * PI * 340.0 * t) * 0.55
            val transient = (rng.nextDouble() * 2.0 - 1.0) * 0.25 * exp(-t * 600.0)
            val sample = (bump * env + transient).coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE * 0.62).toInt().toShort()
        }
        return data
    }

    private fun synthesizeLinearRed(): ShortArray {
        val numSamples = (sampleRate * 0.018).toInt()
        val data = ShortArray(numSamples)
        val rng = Random(77)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 230.0)
            // Deep PBT bottom-out thock
            val thock = sin(2.0 * PI * 220.0 * t) * 0.72 + sin(2.0 * PI * 510.0 * t) * 0.28
            val softNoise = (rng.nextDouble() * 2.0 - 1.0) * 0.14 * exp(-t * 500.0)
            val sample = (thock * env + softNoise).coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE * 0.68).toInt().toShort()
        }
        return data
    }

    fun release() {
        runCatching { audioExecutor.shutdownNow() }
    }
}
