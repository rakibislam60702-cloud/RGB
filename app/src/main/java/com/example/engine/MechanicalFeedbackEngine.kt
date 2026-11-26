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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * Parameterized procedural DSP synthesizer for 53+ mechanical, gaming, sci-fi, retro,
 * and ASMR keypress sound effects plus tactile haptics.
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
    private val pcmCache = ConcurrentHashMap<SwitchSoundProfile, ShortArray>()

    @Volatile
    private var lastSoundDispatchMs: Long = 0L

    private val sampleRate = 22050

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
        if (profile == SwitchSoundProfile.SILENT) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastSoundDispatchMs < 20L) return
        lastSoundDispatchMs = now

        runCatching {
            audioExecutor.execute {
                val pcm = pcmCache.getOrPut(profile) { synthesizeProfile(profile) }
                if (pcm.isEmpty()) return@execute
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
                    val clipDurationMs = ((pcm.size * 1000L) / sampleRate + 8L).coerceIn(22L, 80L)
                    Thread.sleep(clipDurationMs)
                    track.stop()
                    track.release()
                    true
                }.getOrDefault(false)

                if (!played) {
                    runCatching {
                        audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f)
                    }
                }
            }
        }
    }

    private enum class WaveShape { SINE, SQUARE, TRIANGLE, METALLIC }

    private data class SoundSynthSpec(
        val durationSec: Double,
        val startFreq: Double,
        val endFreq: Double,
        val harmonicRatio: Double,
        val harmonicMix: Double,
        val noiseMix: Double,
        val decayRate: Double,
        val secondPulseDelaySec: Double = 0.0,
        val secondPulseFreq: Double = 0.0,
        val secondPulseDecay: Double = 240.0,
        val gain: Double = 0.68,
        val waveShape: WaveShape = WaveShape.SINE,
        val seed: Int = 42
    )

    private fun synthesizeProfile(profile: SwitchSoundProfile): ShortArray {
        if (profile == SwitchSoundProfile.SILENT) return ShortArray(0)
        val spec = specFor(profile)
        val numSamples = (sampleRate * spec.durationSec).toInt().coerceAtLeast(120)
        val data = ShortArray(numSamples)
        val rng = Random(spec.seed + profile.ordinal * 31)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * spec.decayRate)
            val sweepProgress = 1.0 - exp(-t * 220.0)
            val freq = spec.startFreq + (spec.endFreq - spec.startFreq) * sweepProgress

            val rawPrimary = when (spec.waveShape) {
                WaveShape.SINE -> sin(2.0 * PI * freq * t)
                WaveShape.SQUARE -> sign(sin(2.0 * PI * freq * t)) * 0.72
                WaveShape.TRIANGLE -> {
                    val phase = (freq * t) % 1.0
                    2.0 * kotlin.math.abs(2.0 * phase - 1.0) - 1.0
                }
                WaveShape.METALLIC -> {
                    sin(2.0 * PI * freq * t) * 0.65 +
                        sin(2.0 * PI * (freq * 2.71) * t) * 0.35
                }
            }

            val harmonic = sin(2.0 * PI * (freq * spec.harmonicRatio) * t) * spec.harmonicMix
            val transient = (rng.nextDouble() * 2.0 - 1.0) * spec.noiseMix * exp(-t * 620.0)

            var secondary = 0.0
            if (spec.secondPulseDelaySec > 0.0 && t > spec.secondPulseDelaySec && spec.secondPulseFreq > 0.0) {
                val dt = t - spec.secondPulseDelaySec
                secondary = sin(2.0 * PI * spec.secondPulseFreq * dt) * 0.55 * exp(-dt * spec.secondPulseDecay)
            }

            val sample = ((rawPrimary * (1.0 - spec.harmonicMix * 0.4) + harmonic) * env + transient + secondary)
                .coerceIn(-1.0, 1.0)
            data[i] = (sample * Short.MAX_VALUE * spec.gain).toInt().toShort()
        }
        return data
    }

    private fun specFor(profile: SwitchSoundProfile): SoundSynthSpec = when (profile) {
        SwitchSoundProfile.SILENT -> SoundSynthSpec(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 100.0, gain = 0.0)

        // Mechanical (12)
        SwitchSoundProfile.SAMSUNG_SOFT -> SoundSynthSpec(0.015, 620.0, 290.0, 1.65, 0.16, 0.08, 360.0, gain = 0.42, seed = 10)
        SwitchSoundProfile.CLICKY_BLUE -> SoundSynthSpec(0.024, 3400.0, 1800.0, 1.59, 0.32, 0.45, 420.0, 0.004, 420.0, 240.0, 0.72, WaveShape.METALLIC, 42)
        SwitchSoundProfile.TACTILE_BROWN -> SoundSynthSpec(0.022, 980.0, 340.0, 0.42, 0.45, 0.24, 280.0, gain = 0.68, seed = 108)
        SwitchSoundProfile.LINEAR_RED -> SoundSynthSpec(0.020, 240.0, 165.0, 2.25, 0.28, 0.15, 225.0, gain = 0.75, seed = 77)
        SwitchSoundProfile.HOLY_PANDA_THOCK -> SoundSynthSpec(0.026, 460.0, 210.0, 1.85, 0.36, 0.18, 210.0, 0.003, 180.0, 190.0, 0.78, seed = 112)
        SwitchSoundProfile.BLACK_INK_V2 -> SoundSynthSpec(0.023, 210.0, 130.0, 2.40, 0.22, 0.12, 195.0, gain = 0.80, seed = 115)
        SwitchSoundProfile.GATERON_MILKY_YELLOW -> SoundSynthSpec(0.019, 390.0, 220.0, 1.72, 0.26, 0.14, 260.0, gain = 0.68, seed = 118)
        SwitchSoundProfile.BOX_JADE_CRYSTAL -> SoundSynthSpec(0.025, 4100.0, 2100.0, 1.48, 0.38, 0.50, 440.0, 0.005, 560.0, 260.0, 0.74, WaveShape.METALLIC, 121)
        SwitchSoundProfile.TOPRE_EC_THOCK -> SoundSynthSpec(0.024, 340.0, 145.0, 1.50, 0.25, 0.10, 205.0, gain = 0.76, seed = 124)
        SwitchSoundProfile.CHERRY_SILVER_SPEED -> SoundSynthSpec(0.016, 1250.0, 520.0, 1.90, 0.28, 0.22, 370.0, gain = 0.66, seed = 127)
        SwitchSoundProfile.IBM_BUCKLING_SPRING -> SoundSynthSpec(0.030, 2900.0, 1150.0, 2.35, 0.44, 0.42, 310.0, 0.006, 680.0, 180.0, 0.72, WaveShape.METALLIC, 130)
        SwitchSoundProfile.OIL_KING_DEEP -> SoundSynthSpec(0.024, 195.0, 110.0, 2.15, 0.24, 0.09, 185.0, gain = 0.82, seed = 133)

        // Gaming & Esports (12)
        SwitchSoundProfile.ESPORTS_PRO_TRIGGER -> SoundSynthSpec(0.017, 1950.0, 740.0, 1.80, 0.34, 0.32, 380.0, 0.003, 310.0, 260.0, 0.76, WaveShape.METALLIC, 201)
        SwitchSoundProfile.RAZOR_OPTO_PURPLE -> SoundSynthSpec(0.021, 3600.0, 1550.0, 1.62, 0.35, 0.44, 410.0, 0.004, 480.0, 240.0, 0.74, WaveShape.METALLIC, 204)
        SwitchSoundProfile.RAPID_FIRE_FPS -> SoundSynthSpec(0.016, 1450.0, 260.0, 2.10, 0.30, 0.48, 390.0, gain = 0.78, seed = 207)
        SwitchSoundProfile.SNIPER_BOLT_LOCK -> SoundSynthSpec(0.028, 2800.0, 920.0, 2.65, 0.40, 0.38, 300.0, 0.009, 1650.0, 280.0, 0.74, WaveShape.METALLIC, 210)
        SwitchSoundProfile.SHOTGUN_RELOAD_SNAP -> SoundSynthSpec(0.032, 520.0, 120.0, 2.30, 0.42, 0.55, 220.0, 0.012, 860.0, 290.0, 0.82, seed = 213)
        SwitchSoundProfile.HEADSHOT_PING -> SoundSynthSpec(0.034, 2450.0, 2380.0, 2.02, 0.38, 0.16, 150.0, gain = 0.68, waveShape = WaveShape.METALLIC, seed = 216)
        SwitchSoundProfile.KILLSTREAK_COMBO -> SoundSynthSpec(0.034, 880.0, 1420.0, 1.50, 0.32, 0.18, 175.0, 0.011, 1760.0, 190.0, 0.70, WaveShape.TRIANGLE, 219)
        SwitchSoundProfile.APEX_SHIELD_POP -> SoundSynthSpec(0.028, 680.0, 1620.0, 1.75, 0.30, 0.22, 220.0, gain = 0.72, waveShape = WaveShape.TRIANGLE, seed = 222)
        SwitchSoundProfile.VALO_TAP_CRISP -> SoundSynthSpec(0.019, 2150.0, 1180.0, 1.55, 0.30, 0.26, 330.0, gain = 0.70, seed = 225)
        SwitchSoundProfile.NITRO_BOOST_TURBO -> SoundSynthSpec(0.028, 1850.0, 620.0, 2.20, 0.28, 0.52, 235.0, gain = 0.72, seed = 228)
        SwitchSoundProfile.MECHANICAL_PARRY -> SoundSynthSpec(0.032, 3150.0, 1950.0, 2.45, 0.45, 0.30, 195.0, 0.005, 920.0, 220.0, 0.70, WaveShape.METALLIC, 231)
        SwitchSoundProfile.CRITICAL_HIT_DING -> SoundSynthSpec(0.032, 1980.0, 2640.0, 1.50, 0.36, 0.12, 165.0, gain = 0.68, waveShape = WaveShape.TRIANGLE, seed = 234)

        // Sci-Fi & Cyber (11)
        SwitchSoundProfile.CYBER_LASER_PULSE -> SoundSynthSpec(0.026, 2400.0, 380.0, 1.50, 0.24, 0.12, 210.0, gain = 0.68, seed = 301)
        SwitchSoundProfile.QUANTUM_BLASTER -> SoundSynthSpec(0.028, 1750.0, 210.0, 2.00, 0.32, 0.26, 195.0, gain = 0.74, waveShape = WaveShape.SQUARE, seed = 304)
        SwitchSoundProfile.PLASMA_SPARK -> SoundSynthSpec(0.022, 3200.0, 950.0, 2.80, 0.35, 0.58, 340.0, gain = 0.70, seed = 307)
        SwitchSoundProfile.HOLOGRAM_TAP -> SoundSynthSpec(0.024, 1320.0, 1480.0, 2.00, 0.22, 0.04, 215.0, gain = 0.62, seed = 310)
        SwitchSoundProfile.NEURAL_SYNAPSE -> SoundSynthSpec(0.020, 960.0, 1520.0, 1.62, 0.28, 0.10, 260.0, gain = 0.66, seed = 313)
        SwitchSoundProfile.MECH_SERVO_MICRO -> SoundSynthSpec(0.024, 780.0, 420.0, 3.10, 0.36, 0.28, 250.0, 0.007, 1120.0, 310.0, 0.70, WaveShape.SQUARE, 316)
        SwitchSoundProfile.STARSHIP_CONSOLE -> SoundSynthSpec(0.026, 1560.0, 1560.0, 1.50, 0.15, 0.03, 180.0, 0.010, 2080.0, 220.0, 0.64, seed = 319)
        SwitchSoundProfile.ION_CANNON_CLICK -> SoundSynthSpec(0.028, 520.0, 110.0, 2.50, 0.34, 0.22, 175.0, gain = 0.78, seed = 322)
        SwitchSoundProfile.MATRIX_DECRYPT -> SoundSynthSpec(0.016, 2650.0, 1900.0, 2.00, 0.20, 0.25, 410.0, gain = 0.64, waveShape = WaveShape.SQUARE, seed = 325)
        SwitchSoundProfile.CYBER_SONAR_PING -> SoundSynthSpec(0.036, 1680.0, 1670.0, 2.00, 0.12, 0.02, 115.0, gain = 0.62, seed = 328)
        SwitchSoundProfile.TRON_LIGHT_DISC -> SoundSynthSpec(0.026, 2100.0, 1340.0, 1.92, 0.34, 0.14, 190.0, gain = 0.68, waveShape = WaveShape.METALLIC, seed = 331)

        // Retro Arcade & Vintage (9)
        SwitchSoundProfile.RETRO_8BIT_BLIP -> SoundSynthSpec(0.022, 920.0, 1180.0, 2.00, 0.18, 0.04, 240.0, gain = 0.56, waveShape = WaveShape.SQUARE, seed = 401)
        SwitchSoundProfile.ARCADE_COIN_PING -> SoundSynthSpec(0.035, 988.0, 1319.0, 2.00, 0.20, 0.04, 150.0, 0.013, 1319.0, 140.0, 0.58, WaveShape.SQUARE, 404)
        SwitchSoundProfile.PIXEL_JUMP_TAP -> SoundSynthSpec(0.028, 440.0, 1120.0, 1.50, 0.18, 0.04, 185.0, gain = 0.58, waveShape = WaveShape.SQUARE, seed = 407)
        SwitchSoundProfile.NES_MENU_TICK -> SoundSynthSpec(0.014, 1480.0, 980.0, 2.00, 0.16, 0.06, 430.0, gain = 0.56, waveShape = WaveShape.SQUARE, seed = 410)
        SwitchSoundProfile.CHIPTUNE_PLUCK -> SoundSynthSpec(0.025, 580.0, 320.0, 2.00, 0.34, 0.06, 210.0, gain = 0.64, waveShape = WaveShape.TRIANGLE, seed = 413)
        SwitchSoundProfile.GAMEBOY_DPAD -> SoundSynthSpec(0.018, 390.0, 190.0, 2.10, 0.26, 0.18, 310.0, gain = 0.68, seed = 416)
        SwitchSoundProfile.SYNTHWAVE_TOM -> SoundSynthSpec(0.030, 380.0, 125.0, 1.60, 0.25, 0.26, 165.0, gain = 0.78, seed = 419)
        SwitchSoundProfile.TYPEWRITER_VINTAGE -> SoundSynthSpec(0.028, 2200.0, 480.0, 2.35, 0.42, 0.60, 285.0, 0.007, 680.0, 220.0, 0.76, WaveShape.METALLIC, 422)
        SwitchSoundProfile.TELETYPE_TERMINAL -> SoundSynthSpec(0.020, 1750.0, 620.0, 2.20, 0.36, 0.45, 340.0, gain = 0.72, waveShape = WaveShape.SQUARE, seed = 425)

        // Soft & ASMR (9)
        SwitchSoundProfile.GLASS_WATER_DROP -> SoundSynthSpec(0.026, 480.0, 1340.0, 2.00, 0.12, 0.02, 185.0, gain = 0.66, seed = 501)
        SwitchSoundProfile.CRYSTAL_ICE_CHIME -> SoundSynthSpec(0.034, 2850.0, 2790.0, 2.05, 0.30, 0.04, 145.0, gain = 0.58, waveShape = WaveShape.METALLIC, seed = 504)
        SwitchSoundProfile.WOOD_MARIMBA_TAP -> SoundSynthSpec(0.028, 520.0, 460.0, 3.98, 0.22, 0.08, 175.0, gain = 0.70, seed = 507)
        SwitchSoundProfile.CREAMY_PEBBLE_POP -> SoundSynthSpec(0.021, 560.0, 330.0, 1.50, 0.26, 0.09, 240.0, gain = 0.72, seed = 510)
        SwitchSoundProfile.BAMBOO_SHISHI_TAP -> SoundSynthSpec(0.026, 440.0, 370.0, 2.58, 0.28, 0.10, 195.0, gain = 0.72, seed = 513)
        SwitchSoundProfile.SILK_CLOUD_THOCK -> SoundSynthSpec(0.020, 260.0, 150.0, 1.50, 0.20, 0.06, 250.0, gain = 0.64, seed = 516)
        SwitchSoundProfile.VELVET_RUBBER_DOME -> SoundSynthSpec(0.018, 310.0, 180.0, 1.40, 0.18, 0.07, 290.0, gain = 0.60, seed = 519)
        SwitchSoundProfile.RAINDROP_RIPPLE -> SoundSynthSpec(0.025, 720.0, 1180.0, 1.75, 0.16, 0.14, 210.0, gain = 0.62, seed = 522)
        SwitchSoundProfile.KALIMBA_THUMB_PLUCK -> SoundSynthSpec(0.032, 880.0, 865.0, 2.00, 0.28, 0.04, 140.0, gain = 0.66, waveShape = WaveShape.TRIANGLE, seed = 525)
    }

    fun release() {
        runCatching { audioExecutor.shutdownNow() }
    }
}
