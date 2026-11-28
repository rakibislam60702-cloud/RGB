package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGlassRimGoldSubtle
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.GlassCardSelected
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.luminousGlassCard

enum class StudioEffectsCategory(
    val title: String,
    val subtitle: String,
    val testTag: String
) {
    PHOTOS(
        title = "Photos",
        subtitle = "Gallery & HD Photo Backgrounds",
        testTag = "category_card_photos"
    ),
    BANNERS(
        title = "Banners",
        subtitle = "Featured Banner Wallpapers",
        testTag = "category_card_banners"
    ),
    ANIMATIONS(
        title = "Animations",
        subtitle = "Live Ambient & Motion Effects",
        testTag = "category_card_animations"
    ),
    TOUCH_RESPONSES(
        title = "Touch Responses",
        subtitle = "100+ AI & Physics Key-Tap Effects",
        testTag = "category_card_touch_responses"
    ),
    THEMES(
        title = "Themes",
        subtitle = "Complete Luxury Keyboard Themes",
        testTag = "category_card_themes"
    )
}

@Composable
fun LuxuryStudioCategoryGrid(
    selectedCategory: StudioEffectsCategory?,
    photosBadge: String,
    bannersBadge: String,
    animationsBadge: String,
    touchResponsesBadge: String,
    themesBadge: String,
    onSelectCategory: (StudioEffectsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Photos & Banners
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LuxuryCategoryCard(
                category = StudioEffectsCategory.PHOTOS,
                badge = photosBadge,
                icon = Icons.Filled.Collections,
                accentColor = NeonCyan,
                accentBrush = FlagshipEmeraldCyanBrush,
                selected = selectedCategory == StudioEffectsCategory.PHOTOS,
                onClick = { onSelectCategory(StudioEffectsCategory.PHOTOS) },
                modifier = Modifier.weight(1f)
            )
            LuxuryCategoryCard(
                category = StudioEffectsCategory.BANNERS,
                badge = bannersBadge,
                icon = Icons.Filled.Wallpaper,
                accentColor = RoyalGold,
                accentBrush = FlagshipGoldAmberBrush,
                selected = selectedCategory == StudioEffectsCategory.BANNERS,
                onClick = { onSelectCategory(StudioEffectsCategory.BANNERS) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Animations & Touch Responses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LuxuryCategoryCard(
                category = StudioEffectsCategory.ANIMATIONS,
                badge = animationsBadge,
                icon = Icons.Filled.AutoAwesome,
                accentColor = NeonViolet,
                accentBrush = FlagshipCyanMagentaBrush,
                selected = selectedCategory == StudioEffectsCategory.ANIMATIONS,
                onClick = { onSelectCategory(StudioEffectsCategory.ANIMATIONS) },
                modifier = Modifier.weight(1f)
            )
            LuxuryCategoryCard(
                category = StudioEffectsCategory.TOUCH_RESPONSES,
                badge = touchResponsesBadge,
                icon = Icons.Filled.TouchApp,
                accentColor = NeonMagenta,
                accentBrush = FlagshipCyanMagentaBrush,
                selected = selectedCategory == StudioEffectsCategory.TOUCH_RESPONSES,
                onClick = { onSelectCategory(StudioEffectsCategory.TOUCH_RESPONSES) },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Full-Width Flagship Themes Hero Card
        val themesSelected = selectedCategory == StudioEffectsCategory.THEMES
        val heroShape = RoundedCornerShape(22.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .luminousGlassCard(
                    shape = heroShape,
                    borderBrush = if (themesSelected) FlagshipGoldCyanBrush else FlagshipGlassRimGoldSubtle,
                    borderWidth = if (themesSelected) 2.dp else 1.3.dp,
                    glowColor = RoyalGold,
                    glowElevation = if (themesSelected) 18.dp else 10.dp,
                    surfaceColor = if (themesSelected) GlassCardSelected else GlassCardSurface
                )
                .clickable { onSelectCategory(StudioEffectsCategory.THEMES) }
                .padding(horizontal = 16.dp, vertical = 15.dp)
                .testTag(StudioEffectsCategory.THEMES.testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2B200B), Color(0xFF102438))
                        )
                    )
                    .border(1.2.dp, FlagshipGoldCyanBrush, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = StudioEffectsCategory.THEMES.title,
                    tint = RoyalGold,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = StudioEffectsCategory.THEMES.title,
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    CategoryCountBadge(
                        text = themesBadge,
                        accentColor = RoyalGold
                    )
                }
                Text(
                    text = StudioEffectsCategory.THEMES.subtitle,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (themesSelected) Brush.horizontalGradient(listOf(RoyalGold, NeonCyan))
                        else Brush.horizontalGradient(listOf(Color(0xFF231D36), Color(0xFF16243A)))
                    )
                    .border(1.dp, FlagshipGoldCyanBrush, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open ${StudioEffectsCategory.THEMES.title}",
                    tint = if (themesSelected) Color(0xFF061020) else NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LuxuryCategoryCard(
    category: StudioEffectsCategory,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    accentBrush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .heightIn(min = 132.dp)
            .luminousGlassCard(
                shape = cardShape,
                borderBrush = if (selected) accentBrush else FlagshipGlassRimSubtle,
                borderWidth = if (selected) 1.9.dp else 1.1.dp,
                glowColor = accentColor,
                glowElevation = if (selected) 16.dp else 8.dp,
                surfaceColor = if (selected) GlassCardSelected else GlassCardSurface
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(category.testTag),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.68f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            CategoryCountBadge(
                text = badge,
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = category.title,
                fontFamily = SpaceGroteskFamily,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category.subtitle,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryCountBadge(
    text: String,
    accentColor: Color
) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GlassCardInner)
            .border(0.9.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
