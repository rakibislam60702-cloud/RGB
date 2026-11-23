package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LicenseUiState
import com.example.model.AppUiLanguage
import com.example.model.KeyboardSettings
import com.example.model.LocalAppStrings
import com.example.model.SwitchSoundProfile
import com.example.ui.theme.BrandObsidianPill
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuxuryCyanStroke
import com.example.ui.theme.LuxuryPinkStroke
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonAmberContainer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldContainer
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RoyalGoldAccent
import com.example.ui.theme.RoyalGoldPillBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    appUiLanguage: AppUiLanguage,
    settings: KeyboardSettings,
    licenseState: LicenseUiState,
    onSelectAppUiLanguage: (AppUiLanguage) -> Unit,
    onSelectSwitchSound: (SwitchSoundProfile) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit,
    onSmartSuggestionsChange: (Boolean) -> Unit,
    onReplayFeatureGuide: () -> Unit,
    onOpenUnlockScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bodyFont = if (appUiLanguage == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
    var copiedDeviceId by remember { mutableStateOf(false) }

    LaunchedEffect(copiedDeviceId) {
        if (copiedDeviceId) {
            delay(2_000L)
            copiedDeviceId = false
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.settingsTitle,
                    fontFamily = if (appUiLanguage == AppUiLanguage.ENGLISH) OrbitronFamily else bodyFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = strings.settingsSubtitle,
                    fontFamily = bodyFont,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // 1. App Display Language (বাংলা / English / हिन्दी)
            SettingsCard(
                icon = Icons.Filled.Language,
                iconTint = NeonCyan,
                title = strings.settingsSectionLanguage,
                bodyFont = bodyFont
            ) {
                Text(
                    text = strings.settingsLanguageHint,
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppUiLanguage.entries.forEach { lang ->
                        val selected = lang == appUiLanguage
                        val pillFont = if (lang == AppUiLanguage.BENGALI) NotoSansBengaliFamily else FontFamily.Default
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) BrandObsidianPill else ObsidianCanvas)
                                .border(
                                    width = if (selected) 1.2.dp else 1.dp,
                                    brush = if (selected) {
                                        Brush.horizontalGradient(listOf(LuxuryPinkStroke, LuxuryCyanStroke))
                                    } else {
                                        Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectAppUiLanguage(lang) }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .testTag("settings_lang_${lang.code}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.nativeLabel,
                                fontFamily = pillFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 2. Keypress Sound (Soft Modern / Silent) & Key Vibration
            SettingsCard(
                icon = if (settings.switchSound == SwitchSoundProfile.SILENT) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                iconTint = NeonMagenta,
                title = strings.settingsSectionSoundHaptics,
                bodyFont = bodyFont
            ) {
                Text(
                    text = strings.settingsKeypressSoundLabel,
                    fontFamily = bodyFont,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val isSilent = settings.switchSound == SwitchSoundProfile.SILENT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoundOptionPill(
                        label = strings.settingsSoundSoftModern,
                        icon = Icons.Filled.VolumeUp,
                        selected = !isSilent,
                        accent = NeonCyan,
                        onClick = { onSelectSwitchSound(SwitchSoundProfile.SAMSUNG_SOFT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_sound_soft_modern")
                    )
                    SoundOptionPill(
                        label = strings.settingsSoundSilent,
                        icon = Icons.Filled.VolumeOff,
                        selected = isSilent,
                        accent = NeonMagenta,
                        onClick = { onSelectSwitchSound(SwitchSoundProfile.SILENT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_sound_silent")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .clickable { onHapticEnabledChange(!settings.hapticEnabled) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Vibration,
                        contentDescription = null,
                        tint = if (settings.hapticEnabled) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsKeyVibrationLabel,
                            fontFamily = bodyFont,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.settingsKeyVibrationDesc,
                            fontFamily = bodyFont,
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.hapticEnabled,
                        onCheckedChange = onHapticEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF001B22),
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurfaceVariant
                        ),
                        modifier = Modifier.testTag("settings_toggle_haptic")
                    )
                }
            }

            // 3. Smart Auto-Suggestions & Auto-Learn
            SettingsCard(
                icon = Icons.Filled.AutoAwesome,
                iconTint = NeonAmber,
                title = strings.settingsSectionSmartTyping,
                bodyFont = bodyFont
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .clickable { onSmartSuggestionsChange(!settings.smartSuggestionsEnabled) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsSmartSuggestionsLabel,
                            fontFamily = bodyFont,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.settingsSmartSuggestionsDesc,
                            fontFamily = bodyFont,
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.smartSuggestionsEnabled,
                        onCheckedChange = onSmartSuggestionsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF001B22),
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurfaceVariant
                        ),
                        modifier = Modifier.testTag("settings_toggle_smart_suggestions")
                    )
                }
            }

            // 4. Replay Interactive Feature Guide
            SettingsCard(
                icon = Icons.Filled.HelpOutline,
                iconTint = NeonCyan,
                title = strings.settingsSectionGuide,
                bodyFont = bodyFont
            ) {
                Text(
                    text = strings.settingsReplayGuideDesc,
                    fontFamily = bodyFont,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandObsidianPill)
                        .border(
                            1.15.dp,
                            Brush.horizontalGradient(listOf(LuxuryPinkStroke, LuxuryCyanStroke)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onReplayFeatureGuide)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("settings_btn_replay_feature_guide"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.settingsReplayGuideBtn,
                        fontFamily = bodyFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 5. VIP License Status & Device ID
            SettingsCard(
                icon = if (licenseState.isLifetimeUnlocked) Icons.Filled.Verified else Icons.Filled.Timer,
                iconTint = if (licenseState.isLifetimeUnlocked) NeonEmerald else RoyalGoldAccent,
                title = strings.settingsSectionVip,
                bodyFont = bodyFont
            ) {
                val statusColor = when {
                    licenseState.isLifetimeUnlocked -> NeonEmerald
                    licenseState.isFreeStandardMode -> Color(0xFFFF7AA8)
                    else -> RoyalGoldAccent
                }
                val statusBg = when {
                    licenseState.isLifetimeUnlocked -> Color(0xFF0E221C)
                    licenseState.isFreeStandardMode -> Color(0xFF231320)
                    else -> RoyalGoldPillBg
                }
                val statusLabel = when {
                    licenseState.isLifetimeUnlocked -> strings.settingsVipStatusLifetime
                    licenseState.isFreeStandardMode -> strings.settingsVipStatusFree
                    else -> "VIP Trial • ${licenseState.formattedCompactEnglish}"
                }

                // Status badge pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusBg)
                        .border(1.dp, statusColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (licenseState.isLifetimeUnlocked) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (!licenseState.isFreeStandardMode) {
                        Text(text = "👑", fontSize = 15.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = statusLabel,
                        fontFamily = if (!licenseState.isLifetimeUnlocked && !licenseState.isFreeStandardMode) {
                            FontFamily.Default
                        } else {
                            bodyFont
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // Device ID row with 1-tap copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsDeviceIdLabel,
                            fontFamily = bodyFont,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = licenseState.deviceId,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp,
                            color = NeonCyan
                        )
                    }

                    Row(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (copiedDeviceId) NeonEmerald.copy(alpha = 0.18f) else ObsidianSurfaceVariant)
                            .border(
                                1.dp,
                                if (copiedDeviceId) NeonEmerald else NeonCyan.copy(alpha = 0.55f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("AI Keyboard Device ID", licenseState.deviceId)
                                )
                                copiedDeviceId = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("settings_btn_copy_device_id"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedDeviceId) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = strings.settingsCopyIdBtn,
                            tint = if (copiedDeviceId) NeonEmerald else NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (copiedDeviceId) strings.settingsCopiedIdLabel else strings.settingsCopyIdBtn,
                            fontFamily = bodyFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (copiedDeviceId) NeonEmerald else NeonCyan
                        )
                    }
                }

                Text(
                    text = strings.settingsAutoRestoreNote,
                    fontFamily = bodyFont,
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                if (!licenseState.isLifetimeUnlocked) {
                    Button(
                        onClick = onOpenUnlockScreen,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonMagenta,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_btn_unlock_vip")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsUnlockVipBtn,
                            fontFamily = bodyFont,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    bodyFont: FontFamily,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontFamily = bodyFont,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        content()
    }
}

@Composable
private fun SoundOptionPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else ObsidianCanvas)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (selected) accent else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) accent else TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}
