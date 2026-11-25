package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.example.R
import com.example.data.LicenseUiState
import com.example.model.LanguageCatalog
import com.example.model.LanguagePackUiState
import com.example.ui.components.BanglishCountdownTimerBar
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldContainer
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.luminousGlassCard
import java.util.Locale

@Composable
fun LanguagesScreen(
    languagePacks: List<LanguagePackUiState>,
    licenseState: LicenseUiState,
    onDownloadAndEnable: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onOpenUnlockScreen: () -> Unit,
    onOpenBanglishVip: () -> Unit = onOpenUnlockScreen,
    onDeletePack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledPacks = languagePacks.filter { it.isEnabled }
    val downloadedBytes = languagePacks.filter { !it.catalog.isBuiltIn && it.isDownloaded }
        .sumOf { it.fileSizeBytes }
    val downloadedCount = languagePacks.count { !it.catalog.isBuiltIn && it.isDownloaded }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.languages_header),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.languages_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Active Globe Key Cycle Card + Local JSON Storage Readout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        borderBrush = FlagshipCyanMagentaBrush,
                        borderWidth = 1.3.dp,
                        glowColor = NeonCyan,
                        glowElevation = 12.dp
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.section_active_languages),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(ObsidianCanvas)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (downloadedCount == 0) {
                                "0 JSON packs"
                            } else {
                                val kb = String.format(Locale.US, "%.1f KB", downloadedBytes / 1024f)
                                "$downloadedCount saved · $kb"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(enabledPacks, key = { it.catalog.id }) { pack ->
                        val isVipLocked = pack.catalog.isVipOnly && !licenseState.isBanglishUnlocked
                        val isSelected = pack.isActive && !isVipLocked
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) NeonCyan.copy(alpha = 0.18f)
                                    else ObsidianSurfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 1.3.dp else 1.dp,
                                    color = when {
                                        isSelected -> NeonCyan
                                        isVipLocked -> NeonAmber.copy(alpha = 0.72f)
                                        else -> ObsidianBorder
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isVipLocked) onOpenUnlockScreen() else onSelectLanguage(pack.catalog.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("globe_cycle_chip_${pack.catalog.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> NeonCyan
                                            isVipLocked -> NeonAmber
                                            else -> NeonEmerald
                                        }
                                    )
                            )
                            Text(
                                text = pack.catalog.nativeName,
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                            if (isVipLocked) {
                                Text(
                                    text = "🔒 VIP",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.globe_cycle_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Built-in Cards (English + Indian Banglish BN-EN)
            val banglishPriceMeta = licenseState.adminConfig.formattedBanglishPriceBn()
            languagePacks.filter { it.catalog.isBuiltIn }.forEach { builtInPack ->
                val isVipLocked = builtInPack.catalog.isVipOnly && !licenseState.isBanglishUnlocked
                LanguagePackCard(
                    state = builtInPack,
                    licenseState = licenseState,
                    isVipLocked = isVipLocked,
                    vipPriceLabel = banglishPriceMeta,
                    onDownloadAndEnable = {},
                    onToggleEnabled = {},
                    onSelectLanguage = {
                        if (isVipLocked) onOpenBanglishVip() else onSelectLanguage(builtInPack.catalog.id)
                    },
                    onOpenBanglishVip = onOpenBanglishVip,
                    onDeletePack = {}
                )
            }

            // Downloadable Dynamic JSON Packs Section (Bengali, Hindi, Arabic, Urdu)
            SectionTitle(title = stringResource(R.string.section_downloadable_languages))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                languagePacks.filter { !it.catalog.isBuiltIn }.forEach { pack ->
                    val isVipLocked = pack.catalog.isVipOnly && !licenseState.isBanglishUnlocked
                    LanguagePackCard(
                        state = pack,
                        licenseState = licenseState,
                        isVipLocked = isVipLocked,
                        vipPriceLabel = banglishPriceMeta,
                        onDownloadAndEnable = { onDownloadAndEnable(pack.catalog.id) },
                        onToggleEnabled = { enabled -> onToggleEnabled(pack.catalog.id, enabled) },
                        onSelectLanguage = {
                            if (isVipLocked) onOpenBanglishVip() else onSelectLanguage(pack.catalog.id)
                        },
                        onOpenBanglishVip = onOpenBanglishVip,
                        onDeletePack = { onDeletePack(pack.catalog.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LanguagePackCard(
    state: LanguagePackUiState,
    licenseState: LicenseUiState,
    isVipLocked: Boolean = false,
    vipPriceLabel: String = "₹20/মাস",
    onDownloadAndEnable: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectLanguage: () -> Unit,
    onOpenBanglishVip: () -> Unit = {},
    onDeletePack: () -> Unit
) {
    val item = state.catalog
    val isBanglishCard = item.id == LanguageCatalog.BANGLISH_ID || item.isVipOnly
    val borderBrush = when {
        isVipLocked -> FlagshipGoldAmberBrush
        state.isActive -> FlagshipCyanMagentaBrush
        state.isDownloaded && state.isEnabled -> FlagshipEmeraldCyanBrush
        else -> FlagshipGlassRimSubtle
    }
    val glowColor = when {
        isVipLocked -> RoyalGold
        state.isActive -> NeonCyan
        state.isDownloaded && state.isEnabled -> NeonEmerald
        else -> NeonMagenta
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = RoundedCornerShape(20.dp),
                borderBrush = borderBrush,
                borderWidth = if (state.isActive || isVipLocked) 1.5.dp else 1.1.dp,
                glowColor = glowColor,
                glowElevation = if (state.isActive) 14.dp else 8.dp,
                surfaceColor = if (state.isActive) ObsidianSurfaceVariant else ObsidianSurface
            )
            .padding(16.dp)
            .testTag("language_pack_card_${item.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Luminous Native Script Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                glowColor.copy(alpha = 0.28f),
                                ObsidianCanvas
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = borderBrush,
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.shortBadge,
                    fontFamily = if (item.id == LanguageCatalog.ENGLISH_ID) JetBrainsMonoFamily else FontFamily.Default,
                    fontSize = if (item.shortBadge.length > 3) 12.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (item.id == LanguageCatalog.ENGLISH_ID) item.englishName else "${item.englishName} (${item.nativeName})",
                        fontFamily = if (item.id == "bn" || item.id == LanguageCatalog.BANGLISH_ID) NotoSansBengaliFamily else FontFamily.Default,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.layoutSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.isBuiltIn) {
                StatusPill(
                    label = when {
                        isVipLocked -> "🔒 VIP"
                        state.isActive -> stringResource(R.string.badge_active_layout)
                        else -> stringResource(R.string.badge_built_in)
                    },
                    accent = when {
                        isVipLocked -> NeonAmber
                        state.isActive -> NeonCyan
                        else -> TextSecondary
                    },
                    onClick = onSelectLanguage,
                    testTag = "btn_activate_${item.id}"
                )
            } else if (!state.isDownloaded) {
                Button(
                    onClick = onDownloadAndEnable,
                    enabled = !state.isDownloading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF001B22)
                    ),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("btn_download_lang_${item.id}")
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF001B22)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_downloading),
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_download_enable),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF001D24),
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = ObsidianCanvas,
                        uncheckedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.testTag("switch_enable_lang_${item.id}")
                )
            }
        }

        // Dedicated clickable 👑 VIP button + live countdown timer bar for Banglish (BN-EN) card
        if (isBanglishCard) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🇮🇳 Romanized Bengali VIP Plan",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    RoyalGold.copy(alpha = 0.28f),
                                    NeonAmber.copy(alpha = 0.24f)
                                )
                            )
                        )
                        .border(1.2.dp, FlagshipGoldAmberBrush, RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenBanglishVip)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("btn_banglish_vip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "👑 VIP",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            BanglishCountdownTimerBar(
                countdownLabelBn = licenseState.formattedBanglishCountdownBn,
                countdownLabelEn = licenseState.formattedBanglishCountdownEn,
                progress = licenseState.banglishCountdownProgress,
                isActive = licenseState.isBanglishUnlocked
            )
        }

        // Script Keycap Preview Bar + File Size / Action Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCanvas)
                .border(1.dp, Color(0xFF1D2234), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.scriptPreview,
                    fontFamily = FontFamily.Default,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val metaText = when {
                    isVipLocked -> "${item.locale} · 🔒 VIP Feature ($vipPriceLabel)"
                    item.isBuiltIn && item.isVipOnly -> "${item.locale} · 👑 VIP Active"
                    item.isBuiltIn -> "${item.locale} · Built-in free layout"
                    state.isDownloaded -> {
                        val kb = String.format(Locale.US, "%.1f KB", state.fileSizeBytes.coerceAtLeast(100L) / 1024f)
                        "filesDir/layouts/${item.id}.json · $kb"
                    }
                    else -> "${item.locale} · ${item.approxSizeLabel}"
                }
                Text(
                    text = metaText,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isVipLocked -> NeonAmber
                        state.isDownloaded && !item.isBuiltIn -> NeonEmerald
                        else -> TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (state.isDownloaded && !item.isBuiltIn) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        label = if (state.isActive) {
                            stringResource(R.string.badge_active_layout)
                        } else {
                            stringResource(R.string.action_use_layout)
                        },
                        accent = if (state.isActive) NeonEmerald else NeonCyan,
                        onClick = onSelectLanguage,
                        testTag = "btn_use_lang_${item.id}"
                    )

                    IconButton(
                        onClick = onDeletePack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_delete_lang_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_language),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when (accent) {
                    NeonEmerald -> Color(0x550E4F38)
                    NeonAmber -> NeonAmber.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.18f)
                }
            )
            .border(
                1.dp,
                if (accent == NeonAmber) NeonAmber else Color.White.copy(alpha = 0.72f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (accent == NeonEmerald) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
