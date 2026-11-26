package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.PromoBannerConfig
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class PromoBannerSlide(
    val index: Int,
    val imageSource: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val accentColors: List<Color>,
    val glowColor: Color
)

/**
 * Tall & wide (`155.dp`) Top 2-Slide Rounded Glassmorphic Promo Banner Carousel connected in
 * real time to Firebase `/admin_config/banners` (`banner1Image`/`banner1Url`, `banner2Image`/`banner2Url`).
 * Supports BOTH HTTP/HTTPS image URLs (Coil) AND Base64 image strings, with flagship dark-RGB
 * defaults, smooth 4-second auto-slide, manual horizontal swipe, and indicator dots.
 */
@Composable
fun PromoBannerCarousel(
    banners: PromoBannerConfig,
    onBannerClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val slides = listOf(
        PromoBannerSlide(
            index = 0,
            imageSource = banners.resolvedBanner1Image,
            badge = "FLAGSHIP RGB • LIVE",
            title = banners.banner1Title.ifBlank { PromoBannerConfig.DEFAULT_BANNER1_TITLE },
            subtitle = banners.banner1Subtitle.ifBlank { PromoBannerConfig.DEFAULT_BANNER1_SUBTITLE },
            accentColors = listOf(Color(0xFF150B2E), Color(0xFF280E3C), Color(0xFF0A2A43)),
            glowColor = NeonCyan
        ),
        PromoBannerSlide(
            index = 1,
            imageSource = banners.resolvedBanner2Image,
            badge = "SPECIAL FEATURES • PRO",
            title = banners.banner2Title.ifBlank { PromoBannerConfig.DEFAULT_BANNER2_TITLE },
            subtitle = banners.banner2Subtitle.ifBlank { PromoBannerConfig.DEFAULT_BANNER2_SUBTITLE },
            accentColors = listOf(Color(0xFF1B0E2A), Color(0xFF3A1228), Color(0xFF261D09)),
            glowColor = RoyalGold
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })

    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            delay(4_000L)
            val next = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("promo_banner_carousel"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = slides[page]
            PromoBannerCard(
                slide = slide,
                onClick = { onBannerClick(slide.index) }
            )
        }

        // Indicator Dots (• •)
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("promo_banner_dots")
        ) {
            slides.indices.forEach { idx ->
                val isSelected = pagerState.currentPage == idx
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 7.dp,
                    label = "promo_dot_w_$idx"
                )
                Box(
                    modifier = Modifier
                        .height(7.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        Color.White.copy(alpha = 0.28f)
                                    )
                                )
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    slide: PromoBannerSlide,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val borderBrush = if (slide.index == 0) FlagshipCyanMagentaBrush else FlagshipGoldCyanBrush
    val trimmedSource = slide.imageSource.trim()
    val isHttpUrl = trimmedSource.startsWith("http://", ignoreCase = true) ||
        trimmedSource.startsWith("https://", ignoreCase = true)

    val decodedBase64Bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = trimmedSource) {
        value = if (trimmedSource.isNotBlank() && !isHttpUrl) {
            withContext(Dispatchers.Default) {
                decodeBase64ImageBitmap(trimmedSource)
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .luminousGlassCard(
                shape = shape,
                borderBrush = borderBrush,
                borderWidth = 1.5.dp,
                glowColor = slide.glowColor,
                glowElevation = 16.dp
            )
            .clickable(onClick = onClick)
            .testTag("promo_banner_slide_${slide.index}")
    ) {
        // Built-in rich dark-RGB aurora artwork (instant flagship default when image is blank or offline)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(slide.accentColors))
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (slide.index == 0) NeonMagenta else RoyalGold).copy(alpha = 0.36f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.22f),
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.85f, size.height * 0.22f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (slide.index == 0) NeonCyan else NeonViolet).copy(alpha = 0.32f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.14f, size.height * 0.92f),
                    radius = size.width * 0.44f
                ),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.14f, size.height * 0.92f)
            )
        }

        // Optional remote Firebase custom photo overlay (HTTP URL via Coil OR Base64 string decoded asynchronously)
        if (isHttpUrl) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(trimmedSource)
                    .crossfade(true)
                    .build(),
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (decodedBase64Bitmap != null) {
            Image(
                bitmap = decodedBase64Bitmap!!,
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Sleek dark-RGB readability scrim over custom photos or aurora backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x88090B16),
                        0.45f to Color(0x700D1022),
                        1.0f to Color(0xD6080A14)
                    )
                )
        )

        // Banner Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.50f))
                        .border(1.dp, slide.glowColor.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = slide.badge,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slide.index == 0) NeonCyan else RoyalGold,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (slide.index == 0) NeonMagenta else NeonAmber)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = if (slide.index == 0) OrbitronFamily else NotoSansBengaliFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = slide.subtitle,
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun decodeBase64ImageBitmap(rawSource: String): ImageBitmap? {
    return runCatching {
        val cleanBase64 = if (rawSource.contains(",")) {
            rawSource.substringAfter(",")
        } else {
            rawSource
        }.trim()
        if (cleanBase64.length < 32) return null
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        if (bytes.isEmpty()) return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}
