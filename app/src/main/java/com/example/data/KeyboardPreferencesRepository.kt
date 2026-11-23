package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardSettings
import com.example.model.KeycapStyle
import com.example.model.LanguageCatalog
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.SwitchSoundProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.rgbKeyboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pro_rgb_keyboard_prefs"
)

class KeyboardPreferencesRepository private constructor(
    private val context: Context
) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("keyboard_language")
        val ACTIVE_LANGUAGE_ID = stringPreferencesKey("active_language_id")
        val ENABLED_LANGUAGE_IDS = stringPreferencesKey("enabled_language_ids")
        val EFFECT = stringPreferencesKey("rgb_effect")
        val PALETTE = stringPreferencesKey("rgb_palette")
        val KEYCAP_STYLE = stringPreferencesKey("keycap_style")
        val SWITCH_SOUND = stringPreferencesKey("switch_sound")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val ILLUMINATED_LEGENDS = booleanPreferencesKey("illuminated_legends")
        val CHASSIS_AURA = booleanPreferencesKey("chassis_aura")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_STRENGTH = floatPreferencesKey("haptic_strength")
        val HEIGHT_PRESET = stringPreferencesKey("height_preset")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val SHOW_TOOLBAR = booleanPreferencesKey("show_toolbar")
        val AUTO_CAPITALIZE = booleanPreferencesKey("auto_capitalize")
        val DOUBLE_SPACE_PERIOD = booleanPreferencesKey("double_space_period")
        val SMART_SUGGESTIONS = booleanPreferencesKey("smart_suggestions_enabled")
    }

    val settingsFlow: Flow<KeyboardSettings> = context.rgbKeyboardDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val enabledIds = parseEnabledLanguageIds(prefs[Keys.ENABLED_LANGUAGE_IDS])
            val activeId = prefs[Keys.ACTIVE_LANGUAGE_ID]?.takeIf { it.isNotBlank() }
                ?: LanguageCatalog.ENGLISH_ID
            val legacyLang = if (activeId == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH

            KeyboardSettings(
                language = legacyLang,
                activeLanguageId = activeId,
                enabledLanguageIds = enabledIds,
                effect = prefs[Keys.EFFECT]?.let { name ->
                    RgbEffect.entries.find { it.name == name }
                } ?: RgbEffect.TOUCH_SPARK,
                palette = prefs[Keys.PALETTE]?.let { name ->
                    RgbPalette.entries.find { it.name == name }
                } ?: RgbPalette.VICE_CITY_NEON,
                keycapStyle = prefs[Keys.KEYCAP_STYLE]?.let { name ->
                    KeycapStyle.entries.find { it.name == name }
                } ?: KeycapStyle.PUDDING,
                switchSound = prefs[Keys.SWITCH_SOUND]?.let { name ->
                    SwitchSoundProfile.entries.find { it.name == name }
                } ?: SwitchSoundProfile.SAMSUNG_SOFT,
                animationSpeed = (prefs[Keys.ANIMATION_SPEED] ?: 1.0f).coerceIn(0.2f, 2.5f),
                glowIntensity = (prefs[Keys.GLOW_INTENSITY] ?: 0.90f).coerceIn(0.2f, 1.0f),
                keyCornerRadiusDp = (prefs[Keys.CORNER_RADIUS] ?: 9f).coerceIn(2f, 16f),
                illuminatedLegends = prefs[Keys.ILLUMINATED_LEGENDS] ?: true,
                chassisAura = prefs[Keys.CHASSIS_AURA] ?: true,
                hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: false,
                hapticStrength = (prefs[Keys.HAPTIC_STRENGTH] ?: 0.45f).coerceIn(0.15f, 1.0f),
                heightPreset = prefs[Keys.HEIGHT_PRESET]?.let { name ->
                    KeyboardHeightPreset.entries.find { it.name == name }
                } ?: KeyboardHeightPreset.STANDARD,
                showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: false,
                showToolbar = prefs[Keys.SHOW_TOOLBAR] ?: true,
                autoCapitalize = prefs[Keys.AUTO_CAPITALIZE] ?: true,
                doubleSpacePeriod = prefs[Keys.DOUBLE_SPACE_PERIOD] ?: true,
                smartSuggestionsEnabled = prefs[Keys.SMART_SUGGESTIONS] ?: true
            )
        }

    suspend fun updateLanguage(language: KeyboardLanguage) {
        val targetId = if (language == KeyboardLanguage.BENGALI) "bn" else LanguageCatalog.ENGLISH_ID
        updateActiveLanguage(targetId)
    }

    suspend fun updateActiveLanguage(langId: String) {
        context.rgbKeyboardDataStore.edit { prefs ->
            val cleanId = langId.ifBlank { LanguageCatalog.ENGLISH_ID }
            prefs[Keys.ACTIVE_LANGUAGE_ID] = cleanId
            prefs[Keys.LANGUAGE] = if (cleanId == "bn") KeyboardLanguage.BENGALI.name else KeyboardLanguage.ENGLISH.name
            val currentEnabled = parseEnabledLanguageIds(prefs[Keys.ENABLED_LANGUAGE_IDS]).toMutableList()
            if (cleanId !in currentEnabled) {
                currentEnabled.add(cleanId)
                prefs[Keys.ENABLED_LANGUAGE_IDS] = currentEnabled.joinToString(",")
            }
        }
    }

    suspend fun setLanguageEnabled(langId: String, enabled: Boolean) {
        if (langId == LanguageCatalog.ENGLISH_ID) return
        context.rgbKeyboardDataStore.edit { prefs ->
            val list = parseEnabledLanguageIds(prefs[Keys.ENABLED_LANGUAGE_IDS]).toMutableList()
            if (enabled) {
                if (langId !in list) list.add(langId)
            } else {
                list.remove(langId)
                if (prefs[Keys.ACTIVE_LANGUAGE_ID] == langId) {
                    prefs[Keys.ACTIVE_LANGUAGE_ID] = LanguageCatalog.ENGLISH_ID
                    prefs[Keys.LANGUAGE] = KeyboardLanguage.ENGLISH.name
                }
            }
            prefs[Keys.ENABLED_LANGUAGE_IDS] = list.joinToString(",")
        }
    }

    suspend fun cycleActiveLanguage(downloadedIds: Set<String>): String {
        var chosen = LanguageCatalog.ENGLISH_ID
        context.rgbKeyboardDataStore.edit { prefs ->
            val enabled = parseEnabledLanguageIds(prefs[Keys.ENABLED_LANGUAGE_IDS])
                .filter { it == LanguageCatalog.ENGLISH_ID || it in downloadedIds }
                .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID) }
            val current = prefs[Keys.ACTIVE_LANGUAGE_ID] ?: LanguageCatalog.ENGLISH_ID
            val idx = enabled.indexOf(current)
            chosen = enabled[(idx + 1) % enabled.size]
            prefs[Keys.ACTIVE_LANGUAGE_ID] = chosen
            prefs[Keys.LANGUAGE] = if (chosen == "bn") KeyboardLanguage.BENGALI.name else KeyboardLanguage.ENGLISH.name
        }
        return chosen
    }

    private fun parseEnabledLanguageIds(raw: String?): List<String> {
        if (raw == null) {
            return listOf(LanguageCatalog.ENGLISH_ID, "bn", "hi")
        }
        val parsed = raw
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toMutableList()
        if (LanguageCatalog.ENGLISH_ID !in parsed) {
            parsed.add(0, LanguageCatalog.ENGLISH_ID)
        }
        return parsed
    }

    suspend fun updateEffect(effect: RgbEffect) {
        context.rgbKeyboardDataStore.edit { it[Keys.EFFECT] = effect.name }
    }

    suspend fun updatePalette(palette: RgbPalette) {
        context.rgbKeyboardDataStore.edit { it[Keys.PALETTE] = palette.name }
    }

    suspend fun cycleEffect() {
        context.rgbKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.EFFECT]?.let { n -> RgbEffect.entries.find { it.name == n } }
                ?: RgbEffect.TOUCH_SPARK
            prefs[Keys.EFFECT] = current.next().name
        }
    }

    suspend fun cyclePalette() {
        context.rgbKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.PALETTE]?.let { n -> RgbPalette.entries.find { it.name == n } }
                ?: RgbPalette.SPECTRUM_PRISM
            prefs[Keys.PALETTE] = current.next().name
        }
    }

    suspend fun updateKeycapStyle(style: KeycapStyle) {
        context.rgbKeyboardDataStore.edit { it[Keys.KEYCAP_STYLE] = style.name }
    }

    suspend fun updateSwitchSound(profile: SwitchSoundProfile) {
        context.rgbKeyboardDataStore.edit { it[Keys.SWITCH_SOUND] = profile.name }
    }

    suspend fun updateAnimationSpeed(speed: Float) {
        context.rgbKeyboardDataStore.edit { it[Keys.ANIMATION_SPEED] = speed.coerceIn(0.2f, 2.5f) }
    }

    suspend fun updateGlowIntensity(intensity: Float) {
        context.rgbKeyboardDataStore.edit { it[Keys.GLOW_INTENSITY] = intensity.coerceIn(0.2f, 1.0f) }
    }

    suspend fun updateCornerRadius(radiusDp: Float) {
        context.rgbKeyboardDataStore.edit { it[Keys.CORNER_RADIUS] = radiusDp.coerceIn(2f, 16f) }
    }

    suspend fun updateIlluminatedLegends(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.ILLUMINATED_LEGENDS] = enabled }
    }

    suspend fun updateChassisAura(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.CHASSIS_AURA] = enabled }
    }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun updateHapticStrength(strength: Float) {
        context.rgbKeyboardDataStore.edit { it[Keys.HAPTIC_STRENGTH] = strength.coerceIn(0.15f, 1.0f) }
    }

    suspend fun updateHeightPreset(preset: KeyboardHeightPreset) {
        context.rgbKeyboardDataStore.edit { it[Keys.HEIGHT_PRESET] = preset.name }
    }

    suspend fun updateShowNumberRow(show: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.SHOW_NUMBER_ROW] = show }
    }

    suspend fun updateShowToolbar(show: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.SHOW_TOOLBAR] = show }
    }

    suspend fun updateAutoCapitalize(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.AUTO_CAPITALIZE] = enabled }
    }

    suspend fun updateDoubleSpacePeriod(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.DOUBLE_SPACE_PERIOD] = enabled }
    }

    suspend fun updateSmartSuggestionsEnabled(enabled: Boolean) {
        context.rgbKeyboardDataStore.edit { it[Keys.SMART_SUGGESTIONS] = enabled }
    }

    suspend fun resetDefaults() {
        context.rgbKeyboardDataStore.edit { it.clear() }
    }

    companion object {
        @Volatile
        private var instance: KeyboardPreferencesRepository? = null

        fun getInstance(context: Context): KeyboardPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: KeyboardPreferencesRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
