package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontFamily
import com.example.R
import com.example.data.LicenseUiState
import com.example.model.ImeStatus
import com.example.model.KeyboardHeightPreset
import com.example.model.KeyboardSettings
import com.example.model.LanguagePackUiState
import com.example.model.SwitchSoundProfile
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGlassRimGoldSubtle
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.luminousGlassCard

@Composable
fun HardwareScreen(
    settings: KeyboardSettings,
    imeStatus: ImeStatus,
    licenseState: LicenseUiState,
    languagePacks: List<LanguagePackUiState>,
    onSelectSwitchSound: (SwitchSoundProfile) -> Unit,
    onAuditionSwitch: (SwitchSoundProfile) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
    onHapticStrengthChange: (Float) -> Unit,
    onSelectLanguageById: (String) -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onOpenLanguagesTab: () -> Unit,
    onHeightPresetChange: (KeyboardHeightPreset) -> Unit,
    onShowToolbarChange: (Boolean) -> Unit,
    onAutoCapsChange: (Boolean) -> Unit,
    onDoubleSpacePeriodChange: (Boolean) -> Unit,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.feel_header),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.feel_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Switch Acoustics Section + Interactive Switch Audition Cap
            SectionTitle(title = stringResource(R.string.section_switch_acoustics))
            SwitchAuditionDeck(
                activeProfile = settings.switchSound,
                onAudition = { onAuditionSwitch(settings.switchSound) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchSoundProfile.entries.forEach { profile ->
                    val selected = settings.switchSound == profile
                    val accent = Color(profile.accentHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .luminousGlassCard(
                                shape = RoundedCornerShape(16.dp),
                                borderBrush = if (selected) FlagshipCyanMagentaBrush else FlagshipGlassRimSubtle,
                                borderWidth = if (selected) 1.5.dp else 1.dp,
                                glowColor = accent,
                                glowElevation = if (selected) 12.dp else 6.dp,
                                surfaceColor = if (selected) ObsidianSurfaceVariant else ObsidianSurface
                            )
                            .clickable { onSelectSwitchSound(profile) }
                            .padding(14.dp)
                            .testTag("switch_profile_${profile.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accent.copy(alpha = 0.34f), ObsidianCanvas)
                                    )
                                )
                                .border(1.1.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = profile.actuationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                        }
                    }
                }
            }

            // Haptics Card
            SectionTitle(title = stringResource(R.string.section_haptics))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipGlassRimSubtle,
                        borderWidth = 1.2.dp,
                        glowColor = NeonCyan,
                        glowElevation = 10.dp
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleSettingRow(
                    title = stringResource(R.string.label_haptic_enabled),
                    subtitle = "Crisp motor impulse on every keystroke actuation",
                    checked = settings.hapticEnabled,
                    onCheckedChange = onHapticEnabledChange,
                    testTag = "switch_haptic_enabled"
                )
                if (settings.hapticEnabled) {
                    TuningSliderRow(
                        label = stringResource(R.string.label_haptic_strength),
                        valueLabel = "${(settings.hapticStrength * 100).toInt()}%",
                        value = settings.hapticStrength,
                        valueRange = 0.15f..1.0f,
                        onValueChange = onHapticStrengthChange,
                        testTag = "slider_haptic_strength"
                    )
                }
            }

            // Ergonomics & Layout Card
            SectionTitle(title = stringResource(R.string.section_layout))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipGlassRimGoldSubtle,
                        borderWidth = 1.2.dp,
                        glowColor = RoyalGold,
                        glowElevation = 10.dp
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_default_language),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(languagePacks.filter { it.isEnabled }, key = { it.catalog.id }) { pack ->
                            val isVipLocked = pack.catalog.isVipOnly && !licenseState.isBanglishUnlocked
                            val selected = pack.isActive && !isVipLocked
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) NeonCyan.copy(alpha = 0.18f) else ObsidianCanvas)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = when {
                                            selected -> NeonCyan
                                            isVipLocked -> NeonAmber.copy(alpha = 0.72f)
                                            else -> ObsidianBorder
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isVipLocked) onOpenUnlockScreen() else onSelectLanguageById(pack.catalog.id)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("hardware_lang_${pack.catalog.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = pack.catalog.nativeName,
                                    fontFamily = FontFamily.Default,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isVipLocked) {
                                    Text(
                                        text = "🔒 VIP",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber
                                    )
                                }
                            }
                        }

                        item(key = "hardware_more_languages") {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenLanguagesTab)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("hardware_btn_more_languages"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.add_languages_chip),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.label_keyboard_height),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyboardHeightPreset.entries.forEach { preset ->
                            val selected = settings.heightPreset == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) ObsidianSurfaceVariant else ObsidianCanvas)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) Color.White else ObsidianBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onHeightPresetChange(preset) }
                                    .testTag("height_preset_${preset.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                ToggleSettingRow(
                    title = stringResource(R.string.label_show_toolbar),
                    subtitle = stringResource(R.string.label_show_toolbar_desc),
                    checked = settings.showToolbar,
                    onCheckedChange = onShowToolbarChange,
                    testTag = "switch_show_toolbar"
                )
                ToggleSettingRow(
                    title = stringResource(R.string.label_auto_caps),
                    subtitle = stringResource(R.string.label_auto_caps_desc),
                    checked = settings.autoCapitalize,
                    onCheckedChange = onAutoCapsChange,
                    testTag = "switch_auto_caps"
                )
                ToggleSettingRow(
                    title = stringResource(R.string.label_double_space_period),
                    subtitle = stringResource(R.string.label_double_space_period_desc),
                    checked = settings.doubleSpacePeriod,
                    onCheckedChange = onDoubleSpacePeriodChange,
                    testTag = "switch_double_space_period"
                )
            }

            // System IME Setup Checklist Card
            SectionTitle(title = stringResource(R.string.section_setup_guide))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipCyanMagentaBrush,
                        borderWidth = 1.25.dp,
                        glowColor = NeonCyan,
                        glowElevation = 12.dp
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupStepRow(
                    title = stringResource(R.string.setup_step_1_title),
                    description = stringResource(R.string.setup_step_1_desc),
                    completed = imeStatus.isEnabled
                )
                SetupStepRow(
                    title = stringResource(R.string.setup_step_2_title),
                    description = stringResource(R.string.setup_step_2_desc),
                    completed = imeStatus.isSelected
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenImeSettings,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0D1B3E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("guide_btn_settings")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.action_open_settings), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onShowImePicker,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("guide_btn_picker")
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.action_select_ime), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Reset Defaults Button
            OutlinedButton(
                onClick = onResetDefaults,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_reset_defaults")
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reset_defaults),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SwitchAuditionDeck(
    activeProfile: SwitchSoundProfile,
    onAudition: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "switch_cap_scale"
    )
    val accent = Color(activeProfile.accentHex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = RoundedCornerShape(20.dp),
                borderBrush = FlagshipGoldCyanBrush,
                borderWidth = 1.35.dp,
                glowColor = accent,
                glowElevation = 14.dp
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tactile sculpted 3D mechanical test keycap
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(bottom = 5.dp, start = 3.dp, end = 3.dp, top = 2.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (pressed) Color(0xFF21273A) else Color(0xFF11141E))
                .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(13.dp))
                .pointerInput(activeProfile) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        tapCount += 1
                        onAudition()
                        waitForUpOrCancellation()
                        pressed = false
                    }
                }
                .testTag("switch_audition_cap"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = stringResource(R.string.switch_test_prompt),
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "TEST",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = activeProfile.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.switch_test_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = "${activeProfile.actuationLabel} · $tapCount actuations",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    title: String,
    description: String,
    completed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) NeonEmerald else Color.White,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.86f)
            )
        }
    }
}
