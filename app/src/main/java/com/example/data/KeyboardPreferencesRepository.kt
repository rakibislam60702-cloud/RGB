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
import com.example.model.TouchTypingEffect
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
    private val syncPrefs = context.getSharedPreferences(SYNC_CACHE_PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var lastKnownSettings: KeyboardSettings? = null

    private object Keys {
        val LANGUAGE = stringPreferencesKey("keyboard_language")
        val ACTIVE_LANGUAGE_ID = stringPreferencesKey("active_language_id")
        val ENABLED_LANGUAGE_IDS = stringPreferencesKey("enabled_language_ids")
        val EFFECT = stringPreferencesKey("rgb_effect")
        val TOUCH_EFFECT = stringPreferencesKey("touch_typing_effect")
        val PALETTE = stringPreferencesKey("rgb_palette")
        val KEYCAP_STYLE = stringPreferencesKey("keycap_style")
        val SWITCH_SOUND = stringPreferencesKey("switch_sound")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius")
        val SHOW_KEY_BORDERS = booleanPreferencesKey("show_key_borders")
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
        val SOUND_MIGRATION_V1 = booleanPreferencesKey("sound_default_migrated_v1")
        val TRIAL_DEFAULTS_SEEDED_V1 = booleanPreferencesKey("trial_defaults_seeded_v1")
        val POST_TRIAL_REVERTED_V1 = booleanPreferencesKey("post_trial_reverted_v1")
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
            val soundMigrated = prefs[Keys.SOUND_MIGRATION_V1] == true
            val resolvedSound = if (soundMigrated) {
                prefs[Keys.SWITCH_SOUND]?.let { name ->
                    SwitchSoundProfile.entries.find { it.name == name }
                } ?: SwitchSoundProfile.SILENT
            } else {
                SwitchSoundProfile.SILENT
            }

            KeyboardSettings(
                language = legacyLang,
                activeLanguageId = activeId,
                enabledLanguageIds = enabledIds,
                effect = RgbEffect.fromSavedName(prefs[Keys.EFFECT]),
                touchEffect = TouchTypingEffect.fromSavedName(prefs[Keys.TOUCH_EFFECT]),
                palette = prefs[Keys.PALETTE]?.let { name ->
                    RgbPalette.entries.find { it.name == name }
                } ?: RgbPalette.VICE_CITY_NEON,
                keycapStyle = prefs[Keys.KEYCAP_STYLE]?.let { name ->
                    KeycapStyle.entries.find { it.name == name }
                } ?: KeycapStyle.PUDDING,
                switchSound = resolvedSound,
                animationSpeed = (prefs[Keys.ANIMATION_SPEED] ?: 1.0f).coerceIn(0.2f, 2.5f),
                glowIntensity = (prefs[Keys.GLOW_INTENSITY] ?: 0.90f).coerceIn(0.2f, 1.0f),
                keyCornerRadiusDp = (prefs[Keys.CORNER_RADIUS] ?: 6f).coerceIn(2f, 16f),
                showKeyBorders = prefs[Keys.SHOW_KEY_BORDERS] ?: true,
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
            ).also { resolved ->
                lastKnownSettings = resolved
                persistSnapshotToSyncCache(resolved)
            }
        }

    /**
     * Reads the latest persisted keyboard settings synchronously from SharedPreferences cache
     * so IME restarts and app switches render immediately with zero default flash.
     */
    fun getCachedSettingsSnapshot(): KeyboardSettings {
        if (!syncPrefs.getBoolean(SYNC_KEY_INITIALIZED, false)) {
            return lastKnownSettings ?: KeyboardSettings()
        }
        val activeId = syncPrefs.getString(Keys.ACTIVE_LANGUAGE_ID.name, null)
            ?.takeIf { it.isNotBlank() }
            ?: LanguageCatalog.ENGLISH_ID
        val enabledIds = parseEnabledLanguageIds(syncPrefs.getString(Keys.ENABLED_LANGUAGE_IDS.name, null))
        val legacyLang = if (activeId == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH
        return KeyboardSettings(
            language = legacyLang,
            activeLanguageId = activeId,
            enabledLanguageIds = enabledIds,
            effect = RgbEffect.fromSavedName(syncPrefs.getString(Keys.EFFECT.name, null)),
            touchEffect = TouchTypingEffect.fromSavedName(syncPrefs.getString(Keys.TOUCH_EFFECT.name, null)),
            palette = syncPrefs.getString(Keys.PALETTE.name, null)?.let { name ->
                RgbPalette.entries.find { it.name == name }
            } ?: RgbPalette.VICE_CITY_NEON,
            keycapStyle = syncPrefs.getString(Keys.KEYCAP_STYLE.name, null)?.let { name ->
                KeycapStyle.entries.find { it.name == name }
            } ?: KeycapStyle.PUDDING,
            switchSound = syncPrefs.getString(Keys.SWITCH_SOUND.name, null)?.let { name ->
                SwitchSoundProfile.entries.find { it.name == name }
            } ?: SwitchSoundProfile.SILENT,
            animationSpeed = syncPrefs.getFloat(Keys.ANIMATION_SPEED.name, 1.0f).coerceIn(0.2f, 2.5f),
            glowIntensity = syncPrefs.getFloat(Keys.GLOW_INTENSITY.name, 0.90f).coerceIn(0.2f, 1.0f),
            keyCornerRadiusDp = syncPrefs.getFloat(Keys.CORNER_RADIUS.name, 6f).coerceIn(2f, 16f),
            showKeyBorders = syncPrefs.getBoolean(Keys.SHOW_KEY_BORDERS.name, true),
            illuminatedLegends = syncPrefs.getBoolean(Keys.ILLUMINATED_LEGENDS.name, true),
            chassisAura = syncPrefs.getBoolean(Keys.CHASSIS_AURA.name, true),
            hapticEnabled = syncPrefs.getBoolean(Keys.HAPTIC_ENABLED.name, false),
            hapticStrength = syncPrefs.getFloat(Keys.HAPTIC_STRENGTH.name, 0.45f).coerceIn(0.15f, 1.0f),
            heightPreset = syncPrefs.getString(Keys.HEIGHT_PRESET.name, null)?.let { name ->
                KeyboardHeightPreset.entries.find { it.name == name }
            } ?: KeyboardHeightPreset.STANDARD,
            showNumberRow = syncPrefs.getBoolean(Keys.SHOW_NUMBER_ROW.name, false),
            showToolbar = syncPrefs.getBoolean(Keys.SHOW_TOOLBAR.name, true),
            autoCapitalize = syncPrefs.getBoolean(Keys.AUTO_CAPITALIZE.name, true),
            doubleSpacePeriod = syncPrefs.getBoolean(Keys.DOUBLE_SPACE_PERIOD.name, true),
            smartSuggestionsEnabled = syncPrefs.getBoolean(Keys.SMART_SUGGESTIONS.name, true)
        ).also { lastKnownSettings = it }
    }

    private fun persistSnapshotToSyncCache(settings: KeyboardSettings) {
        syncPrefs.edit()
            .putBoolean(SYNC_KEY_INITIALIZED, true)
            .putString(Keys.ACTIVE_LANGUAGE_ID.name, settings.activeLanguageId)
            .putString(Keys.LANGUAGE.name, settings.language.name)
            .putString(Keys.ENABLED_LANGUAGE_IDS.name, settings.enabledLanguageIds.joinToString(","))
            .putString(Keys.EFFECT.name, settings.effect.name)
            .putString(Keys.TOUCH_EFFECT.name, settings.touchEffect.name)
            .putString(Keys.PALETTE.name, settings.palette.name)
            .putString(Keys.KEYCAP_STYLE.name, settings.keycapStyle.name)
            .putString(Keys.SWITCH_SOUND.name, settings.switchSound.name)
            .putFloat(Keys.ANIMATION_SPEED.name, settings.animationSpeed)
            .putFloat(Keys.GLOW_INTENSITY.name, settings.glowIntensity)
            .putFloat(Keys.CORNER_RADIUS.name, settings.keyCornerRadiusDp)
            .putBoolean(Keys.SHOW_KEY_BORDERS.name, settings.showKeyBorders)
            .putBoolean(Keys.ILLUMINATED_LEGENDS.name, settings.illuminatedLegends)
            .putBoolean(Keys.CHASSIS_AURA.name, settings.chassisAura)
            .putBoolean(Keys.HAPTIC_ENABLED.name, settings.hapticEnabled)
            .putFloat(Keys.HAPTIC_STRENGTH.name, settings.hapticStrength)
            .putString(Keys.HEIGHT_PRESET.name, settings.heightPreset.name)
            .putBoolean(Keys.SHOW_NUMBER_ROW.name, settings.showNumberRow)
            .putBoolean(Keys.SHOW_TOOLBAR.name, settings.showToolbar)
            .putBoolean(Keys.AUTO_CAPITALIZE.name, settings.autoCapitalize)
            .putBoolean(Keys.DOUBLE_SPACE_PERIOD.name, settings.doubleSpacePeriod)
            .putBoolean(Keys.SMART_SUGGESTIONS.name, settings.smartSuggestionsEnabled)
            .apply()
    }

    /** Synchronously caches the active language before async DataStore persistence completes. */
    fun cacheActiveLanguageSync(langId: String) {
        val cleanId = langId.ifBlank { LanguageCatalog.ENGLISH_ID }
        val base = getCachedSettingsSnapshot()
        val updatedEnabled = if (cleanId in base.enabledLanguageIds) {
            base.enabledLanguageIds
        } else {
            base.enabledLanguageIds + cleanId
        }
        val legacyLang = if (cleanId == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH
        val updated = base.copy(
            language = legacyLang,
            activeLanguageId = cleanId,
            enabledLanguageIds = updatedEnabled
        )
        lastKnownSettings = updated
        persistSnapshotToSyncCache(updated)
    }

    suspend fun updateLanguage(language: KeyboardLanguage) {
        val targetId = if (language == KeyboardLanguage.BENGALI) "bn" else LanguageCatalog.ENGLISH_ID
        updateActiveLanguage(targetId)
    }

    suspend fun updateActiveLanguage(langId: String) {
        val cleanId = langId.ifBlank { LanguageCatalog.ENGLISH_ID }
        cacheActiveLanguageSync(cleanId)
        context.rgbKeyboardDataStore.edit { prefs ->
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
        if (LanguageCatalog.isBuiltInId(langId)) return
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
        val current = getCachedSettingsSnapshot()
        val list = current.enabledLanguageIds.toMutableList()
        var nextActive = current.activeLanguageId
        if (enabled) {
            if (langId !in list) list.add(langId)
        } else {
            list.remove(langId)
            if (nextActive == langId) nextActive = LanguageCatalog.ENGLISH_ID
        }
        persistSnapshotToSyncCache(
            current.copy(
                activeLanguageId = nextActive,
                language = if (nextActive == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH,
                enabledLanguageIds = list
            )
        )
    }

    suspend fun cycleActiveLanguage(
        downloadedIds: Set<String>,
        includeVipLanguages: Boolean = true
    ): String {
        var chosen = LanguageCatalog.ENGLISH_ID
        context.rgbKeyboardDataStore.edit { prefs ->
            val enabled = parseEnabledLanguageIds(prefs[Keys.ENABLED_LANGUAGE_IDS])
                .filter {
                    (LanguageCatalog.isBuiltInId(it) || it in downloadedIds) &&
                        (includeVipLanguages || it != LanguageCatalog.BANGLISH_ID)
                }
                .ifEmpty { listOf(LanguageCatalog.ENGLISH_ID) }
            val current = prefs[Keys.ACTIVE_LANGUAGE_ID] ?: LanguageCatalog.ENGLISH_ID
            val idx = enabled.indexOf(current)
            chosen = enabled[(idx + 1) % enabled.size]
            prefs[Keys.ACTIVE_LANGUAGE_ID] = chosen
            prefs[Keys.LANGUAGE] = if (chosen == "bn") KeyboardLanguage.BENGALI.name else KeyboardLanguage.ENGLISH.name
        }
        cacheActiveLanguageSync(chosen)
        return chosen
    }

    private fun parseEnabledLanguageIds(raw: String?): List<String> {
        if (raw == null) {
            return listOf(LanguageCatalog.ENGLISH_ID, LanguageCatalog.BANGLISH_ID, "bn", "hi")
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
        if (LanguageCatalog.BANGLISH_ID !in parsed) {
            parsed.add(1, LanguageCatalog.BANGLISH_ID)
        }
        return parsed
    }

    suspend fun updateEffect(effect: RgbEffect) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(effect = effect))
        context.rgbKeyboardDataStore.edit {
            it[Keys.EFFECT] = effect.name
            it[Keys.TRIAL_DEFAULTS_SEEDED_V1] = true
        }
    }

    suspend fun updateTouchEffect(touchEffect: TouchTypingEffect) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(touchEffect = touchEffect))
        context.rgbKeyboardDataStore.edit {
            it[Keys.TOUCH_EFFECT] = touchEffect.name
            it[Keys.TRIAL_DEFAULTS_SEEDED_V1] = true
        }
    }

    suspend fun updatePalette(palette: RgbPalette) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(palette = palette))
        context.rgbKeyboardDataStore.edit { it[Keys.PALETTE] = palette.name }
    }

    suspend fun cycleEffect() {
        var nextEffect = RgbEffect.NORMAL_DEFAULT
        context.rgbKeyboardDataStore.edit { prefs ->
            val current = RgbEffect.fromSavedName(prefs[Keys.EFFECT])
            nextEffect = current.next()
            prefs[Keys.EFFECT] = nextEffect.name
        }
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(effect = nextEffect))
    }

    suspend fun cyclePalette() {
        var nextPalette = RgbPalette.SPECTRUM_PRISM
        context.rgbKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.PALETTE]?.let { n -> RgbPalette.entries.find { it.name == n } }
                ?: RgbPalette.SPECTRUM_PRISM
            nextPalette = current.next()
            prefs[Keys.PALETTE] = nextPalette.name
        }
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(palette = nextPalette))
    }

    suspend fun updateKeycapStyle(style: KeycapStyle) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(keycapStyle = style))
        context.rgbKeyboardDataStore.edit { it[Keys.KEYCAP_STYLE] = style.name }
    }

    suspend fun updateSwitchSound(profile: SwitchSoundProfile) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(switchSound = profile))
        context.rgbKeyboardDataStore.edit { prefs ->
            prefs[Keys.SWITCH_SOUND] = profile.name
            prefs[Keys.SOUND_MIGRATION_V1] = true
        }
    }

    suspend fun ensureSoundDefaultMigrated() {
        context.rgbKeyboardDataStore.edit { prefs ->
            if (prefs[Keys.SOUND_MIGRATION_V1] != true) {
                prefs[Keys.SWITCH_SOUND] = SwitchSoundProfile.SILENT.name
                prefs[Keys.SOUND_MIGRATION_V1] = true
            }
        }
    }

    suspend fun seedInitialTrialTouchDefaultsIfNeeded() {
        context.rgbKeyboardDataStore.edit { prefs ->
            if (prefs[Keys.TRIAL_DEFAULTS_SEEDED_V1] != true) {
                val existingTouch = prefs[Keys.TOUCH_EFFECT]
                if (existingTouch == null || existingTouch == TouchTypingEffect.NONE.name) {
                    prefs[Keys.TOUCH_EFFECT] = TouchTypingEffect.TOUCH_SHOCKWAVE_RING.name
                }
                if (!prefs.contains(Keys.SHOW_KEY_BORDERS)) {
                    prefs[Keys.SHOW_KEY_BORDERS] = true
                }
                prefs[Keys.TRIAL_DEFAULTS_SEEDED_V1] = true
            }
        }
    }

    suspend fun resetPostTrialRevertFlag() {
        context.rgbKeyboardDataStore.edit { prefs ->
            if (prefs[Keys.POST_TRIAL_REVERTED_V1] == true) {
                prefs[Keys.POST_TRIAL_REVERTED_V1] = false
            }
        }
    }

    suspend fun revertToFreeStandardDefaultsOnce() {
        var didRevert = false
        context.rgbKeyboardDataStore.edit { prefs ->
            if (prefs[Keys.POST_TRIAL_REVERTED_V1] != true) {
                prefs[Keys.EFFECT] = RgbEffect.NORMAL_DEFAULT.name
                prefs[Keys.TOUCH_EFFECT] = TouchTypingEffect.NONE.name
                if (prefs[Keys.ACTIVE_LANGUAGE_ID] == LanguageCatalog.BANGLISH_ID) {
                    prefs[Keys.ACTIVE_LANGUAGE_ID] = LanguageCatalog.ENGLISH_ID
                    prefs[Keys.LANGUAGE] = KeyboardLanguage.ENGLISH.name
                }
                prefs[Keys.POST_TRIAL_REVERTED_V1] = true
                didRevert = true
            }
        }
        if (didRevert) {
            val current = getCachedSettingsSnapshot()
            val nextLangId = if (current.activeLanguageId == LanguageCatalog.BANGLISH_ID) {
                LanguageCatalog.ENGLISH_ID
            } else {
                current.activeLanguageId
            }
            persistSnapshotToSyncCache(
                current.copy(
                    effect = RgbEffect.NORMAL_DEFAULT,
                    touchEffect = TouchTypingEffect.NONE,
                    activeLanguageId = nextLangId,
                    language = if (nextLangId == "bn") KeyboardLanguage.BENGALI else KeyboardLanguage.ENGLISH
                )
            )
        }
    }

    suspend fun updateAnimationSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.2f, 2.5f)
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(animationSpeed = clamped))
        context.rgbKeyboardDataStore.edit { it[Keys.ANIMATION_SPEED] = clamped }
    }

    suspend fun updateGlowIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.2f, 1.0f)
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(glowIntensity = clamped))
        context.rgbKeyboardDataStore.edit { it[Keys.GLOW_INTENSITY] = clamped }
    }

    suspend fun updateCornerRadius(radiusDp: Float) {
        val clamped = radiusDp.coerceIn(2f, 16f)
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(keyCornerRadiusDp = clamped))
        context.rgbKeyboardDataStore.edit { it[Keys.CORNER_RADIUS] = clamped }
    }

    suspend fun updateShowKeyBorders(show: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(showKeyBorders = show))
        context.rgbKeyboardDataStore.edit { it[Keys.SHOW_KEY_BORDERS] = show }
    }

    suspend fun updateIlluminatedLegends(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(illuminatedLegends = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.ILLUMINATED_LEGENDS] = enabled }
    }

    suspend fun updateChassisAura(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(chassisAura = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.CHASSIS_AURA] = enabled }
    }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(hapticEnabled = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun updateHapticStrength(strength: Float) {
        val clamped = strength.coerceIn(0.15f, 1.0f)
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(hapticStrength = clamped))
        context.rgbKeyboardDataStore.edit { it[Keys.HAPTIC_STRENGTH] = clamped }
    }

    suspend fun updateHeightPreset(preset: KeyboardHeightPreset) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(heightPreset = preset))
        context.rgbKeyboardDataStore.edit { it[Keys.HEIGHT_PRESET] = preset.name }
    }

    suspend fun updateShowNumberRow(show: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(showNumberRow = show))
        context.rgbKeyboardDataStore.edit { it[Keys.SHOW_NUMBER_ROW] = show }
    }

    suspend fun updateShowToolbar(show: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(showToolbar = show))
        context.rgbKeyboardDataStore.edit { it[Keys.SHOW_TOOLBAR] = show }
    }

    suspend fun updateAutoCapitalize(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(autoCapitalize = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.AUTO_CAPITALIZE] = enabled }
    }

    suspend fun updateDoubleSpacePeriod(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(doubleSpacePeriod = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.DOUBLE_SPACE_PERIOD] = enabled }
    }

    suspend fun updateSmartSuggestionsEnabled(enabled: Boolean) {
        persistSnapshotToSyncCache(getCachedSettingsSnapshot().copy(smartSuggestionsEnabled = enabled))
        context.rgbKeyboardDataStore.edit { it[Keys.SMART_SUGGESTIONS] = enabled }
    }

    suspend fun resetDefaults() {
        val defaults = KeyboardSettings(switchSound = SwitchSoundProfile.SILENT)
        lastKnownSettings = defaults
        persistSnapshotToSyncCache(defaults)
        context.rgbKeyboardDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.SWITCH_SOUND] = SwitchSoundProfile.SILENT.name
            prefs[Keys.SOUND_MIGRATION_V1] = true
        }
    }

    companion object {
        private const val SYNC_CACHE_PREFS_NAME = "pro_rgb_keyboard_sync_cache"
        private const val SYNC_KEY_INITIALIZED = "sync_cache_initialized_v1"

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
