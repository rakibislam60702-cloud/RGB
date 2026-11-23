package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

enum class FeatureTourTarget {
    AI_HUB,
    RGB_STICKER,
    FONTS_AND_CLIPBOARD,
    VOICE_AND_GLOBE,
    TOUCH_SPARK_DECK
}

class FeatureTourCoordinates {
    val rects = mutableStateMapOf<FeatureTourTarget, Rect>()
    private var clipboardRect: Rect? = null
    private var fontsRect: Rect? = null

    fun register(target: FeatureTourTarget, rect: Rect) {
        if (rect.width > 1f && rect.height > 1f) {
            rects[target] = rect
        }
    }

    fun registerClipboard(rect: Rect) {
        if (rect.width > 1f && rect.height > 1f) {
            clipboardRect = rect
            updateFontsAndClipboardUnion()
        }
    }

    fun registerFonts(rect: Rect) {
        if (rect.width > 1f && rect.height > 1f) {
            fontsRect = rect
            updateFontsAndClipboardUnion()
        }
    }

    private fun updateFontsAndClipboardUnion() {
        val c = clipboardRect
        val f = fontsRect
        val combined = when {
            c != null && f != null -> Rect(
                left = minOf(c.left, f.left),
                top = minOf(c.top, f.top),
                right = maxOf(c.right, f.right),
                bottom = maxOf(c.bottom, f.bottom)
            )
            c != null -> c
            f != null -> f
            else -> null
        }
        if (combined != null) {
            rects[FeatureTourTarget.FONTS_AND_CLIPBOARD] = combined
        }
    }
}

@Immutable
private data class TourStepSpec(
    val target: FeatureTourTarget,
    val titleRes: Int,
    val subtitleRes: Int,
    val stepBadgeBn: String,
    val arrowColor: Color
)

private val NeonTourGreen = Color(0xFF00FF88)
private val NeonTourRed = Color(0xFFFF2E63)

private val featureTourSteps = listOf(
    TourStepSpec(
        target = FeatureTourTarget.AI_HUB,
        titleRes = R.string.tour_step_1_title,
        subtitleRes = R.string.tour_step_1_desc,
        stepBadgeBn = "১/৫",
        arrowColor = NeonTourGreen
    ),
    TourStepSpec(
        target = FeatureTourTarget.RGB_STICKER,
        titleRes = R.string.tour_step_2_title,
        subtitleRes = R.string.tour_step_2_desc,
        stepBadgeBn = "২/৫",
        arrowColor = NeonTourRed
    ),
    TourStepSpec(
        target = FeatureTourTarget.FONTS_AND_CLIPBOARD,
        titleRes = R.string.tour_step_3_title,
        subtitleRes = R.string.tour_step_3_desc,
        stepBadgeBn = "৩/৫",
        arrowColor = NeonTourGreen
    ),
    TourStepSpec(
        target = FeatureTourTarget.VOICE_AND_GLOBE,
        titleRes = R.string.tour_step_4_title,
        subtitleRes = R.string.tour_step_4_desc,
        stepBadgeBn = "৪/৫",
        arrowColor = NeonTourRed
    ),
    TourStepSpec(
        target = FeatureTourTarget.TOUCH_SPARK_DECK,
        titleRes = R.string.tour_step_5_title,
        subtitleRes = R.string.tour_step_5_desc,
        stepBadgeBn = "৫/৫",
        arrowColor = NeonTourGreen
    )
)

/**
 * Stage 2: 100% SILENT dark spotlight overlay with a glowing bouncing Neon Green (#00FF88) /
 * Neon Red (#FF2E63) arrow and compact Bengali label.
 * - Fixed "Skip ✕" pill stays pinned strictly at the top-right corner of the screen.
 * - User must tap the pointed spotlight area to advance step-by-step when not skipping.
 */
@Composable
fun SilentFeatureTourOverlay(
    coordinates: FeatureTourCoordinates,
    onFinishOrSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = featureTourSteps[currentStepIndex.coerceIn(0, featureTourSteps.lastIndex)]
    val density = LocalDensity.current

    val bounceTransition = rememberInfiniteTransition(label = "tour_arrow_bounce")
    val bounceOffsetDp by bounceTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_bounce_y"
    )
    val ringPulseAlpha by bounceTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlight_ring_alpha"
    )

    val advanceStep: () -> Unit = {
        if (currentStepIndex < featureTourSteps.lastIndex) {
            currentStepIndex += 1
        } else {
            onFinishOrSkip()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Absorb background taps so only tapping the pointed spotlight area or top-right Skip advances
                detectTapGestures { }
            }
            .testTag("silent_feature_tour_overlay")
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 6.dp.toPx() }
        val cornerPx = with(density) { 14.dp.toPx() }

        val rawRect = coordinates.rects[step.target]
        val spotlightRect = if (rawRect != null && rawRect.width > 4f && rawRect.height > 4f) {
            Rect(
                left = (rawRect.left - padPx).coerceIn(4f, screenWidthPx - 48f),
                top = (rawRect.top - padPx).coerceIn(4f, screenHeightPx - 48f),
                right = (rawRect.right + padPx).coerceIn(52f, screenWidthPx - 4f),
                bottom = (rawRect.bottom + padPx).coerceIn(52f, screenHeightPx - 4f)
            )
        } else {
            // Safe fallback centered in lower half if coordinates are still settling
            val fallbackW = screenWidthPx * 0.72f
            val fallbackH = with(density) { 64.dp.toPx() }
            val left = (screenWidthPx - fallbackW) / 2f
            val top = screenHeightPx * 0.62f
            Rect(left, top, left + fallbackW, top + fallbackH)
        }

        // Dark spotlight scrim with rounded clear cutout + glowing Neon Green/Red border ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.78f))

            val rectTopLeft = Offset(spotlightRect.left, spotlightRect.top)
            val rectSize = Size(spotlightRect.width, spotlightRect.height)

            // Punch crisp spotlight window over the active feature
            drawRoundRect(
                color = Color.Transparent,
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                blendMode = BlendMode.Clear
            )

            // Outer soft neon halo
            drawRoundRect(
                color = step.arrowColor.copy(alpha = 0.34f * ringPulseAlpha),
                topLeft = Offset(rectTopLeft.x - 4f, rectTopLeft.y - 4f),
                size = Size(rectSize.width + 8f, rectSize.height + 8f),
                cornerRadius = CornerRadius(cornerPx + 4f, cornerPx + 4f),
                style = Stroke(width = 9f)
            )

            // Sharp neon spotlight border
            drawRoundRect(
                color = step.arrowColor.copy(alpha = ringPulseAlpha),
                topLeft = rectTopLeft,
                size = rectSize,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = 3.5f)
            )
        }

        // Interactive tap target strictly over the pointed feature area (requires tapping here to advance)
        val minHitPx = with(density) { 52.dp.toPx() }
        val hitW = maxOf(spotlightRect.width, minHitPx)
        val hitH = maxOf(spotlightRect.height, minHitPx)
        val hitLeft = (spotlightRect.center.x - hitW / 2f).coerceIn(0f, (screenWidthPx - hitW).coerceAtLeast(0f))
        val hitTop = (spotlightRect.center.y - hitH / 2f).coerceIn(0f, (screenHeightPx - hitH).coerceAtLeast(0f))

        Box(
            modifier = Modifier
                .offset { IntOffset(hitLeft.roundToInt(), hitTop.roundToInt()) }
                .width(with(density) { hitW.toDp() })
                .height(with(density) { hitH.toDp() })
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = advanceStep)
                .testTag("feature_tour_target_${step.target.name.lowercase()}")
        )

        // Position bouncing Neon Green / Neon Red Arrow + compact Bengali callout cleanly above (or below) the target
        val placeAbove = spotlightRect.top > with(density) { 185.dp.toPx() }
        val arrowSizeDp = 46.dp
        val arrowSizePx = with(density) { arrowSizeDp.toPx() }
        val bouncePx = with(density) { bounceOffsetDp.dp.toPx() }
        val gapPx = with(density) { 6.dp.toPx() }

        val arrowLeftPx = (spotlightRect.center.x - arrowSizePx / 2f)
            .coerceIn(with(density) { 12.dp.toPx() }, screenWidthPx - arrowSizePx - with(density) { 12.dp.toPx() })
        val arrowTopPx = if (placeAbove) {
            (spotlightRect.top - arrowSizePx - gapPx - bouncePx).coerceAtLeast(with(density) { 68.dp.toPx() })
        } else {
            (spotlightRect.bottom + gapPx + bouncePx).coerceAtMost(screenHeightPx - arrowSizePx - with(density) { 110.dp.toPx() })
        }

        // Glowing Bouncing Neon Arrow (pointing Down when above target, Up when below target)
        Box(
            modifier = Modifier
                .offset { IntOffset(arrowLeftPx.roundToInt(), arrowTopPx.roundToInt()) }
                .size(arrowSizeDp)
                .clickable(onClick = advanceStep)
                .testTag("feature_tour_bouncing_arrow")
        ) {
            NeonBouncingArrowCanvas(
                color = step.arrowColor,
                pointingDown = placeAbove,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Compact Bengali Label Card anchored cleanly next to the arrow (never overlapping top-right Skip)
        val cardMaxW = minOf(maxWidth - 32.dp, 296.dp)
        val cardWidthPx = with(density) { cardMaxW.toPx() }
        val cardLeftPx = (spotlightRect.center.x - cardWidthPx / 2f)
            .coerceIn(with(density) { 16.dp.toPx() }, (screenWidthPx - cardWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(16f))
        val estimatedCardHeightPx = with(density) { 98.dp.toPx() }
        val safeTopPx = with(density) { 78.dp.toPx() }
        val cardTopPx = if (placeAbove) {
            (arrowTopPx - estimatedCardHeightPx - with(density) { 6.dp.toPx() }).coerceAtLeast(safeTopPx)
        } else {
            (arrowTopPx + arrowSizePx + with(density) { 6.dp.toPx() }).coerceAtMost(screenHeightPx - estimatedCardHeightPx - with(density) { 16.dp.toPx() })
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(cardLeftPx.roundToInt(), cardTopPx.roundToInt()) }
                .width(cardMaxW)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xF0120B24))
                .border(
                    width = 1.5.dp,
                    color = step.arrowColor.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = advanceStep)
                .padding(horizontal = 13.dp, vertical = 10.dp)
                .testTag("feature_tour_label_card"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(step.titleRes),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = step.arrowColor,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(step.arrowColor.copy(alpha = 0.16f))
                        .border(1.dp, step.arrowColor.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = step.stepBadgeBn,
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.arrowColor
                    )
                }
            }
            Text(
                text = stringResource(step.subtitleRes),
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.tour_tap_pointed_area_hint),
                fontFamily = NotoSansBengaliFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }

        // Fixed Top-Right "Skip ✕" Pill Button (strictly pinned to screen top-right, never attached to moving arrow)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xE618102D))
                .border(
                    width = 1.25.dp,
                    brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonCyan)),
                    shape = RoundedCornerShape(999.dp)
                )
                .clickable(onClick = onFinishOrSkip)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("feature_tour_skip_btn"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.tour_skip_button),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun NeonBouncingArrowCanvas(
    color: Color,
    pointingDown: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val topY = size.height * 0.14f
        val bottomY = size.height * 0.88f
        val headWingX = size.width * 0.28f
        val headHeight = size.height * 0.30f

        val path = Path().apply {
            if (pointingDown) {
                moveTo(cx, topY)
                lineTo(cx, bottomY)
                moveTo(cx - headWingX, bottomY - headHeight)
                lineTo(cx, bottomY)
                lineTo(cx + headWingX, bottomY - headHeight)
            } else {
                moveTo(cx, bottomY)
                lineTo(cx, topY)
                moveTo(cx - headWingX, topY + headHeight)
                lineTo(cx, topY)
                lineTo(cx + headWingX, topY + headHeight)
            }
        }

        // Soft glowing neon aura stroke
        drawPath(
            path = path,
            color = color.copy(alpha = 0.34f),
            style = Stroke(
                width = size.width * 0.28f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Bright neon core stroke
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = size.width * 0.13f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Crisp inner highlight
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.8f),
            style = Stroke(
                width = size.width * 0.045f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Stage 3: Auto-disappearing glowing GTA VI neon Lifetime VIP banner shown in the center of the screen
 * right after the final tour step or top-right "Skip ✕" tap, fading out completely after 3 seconds
 * without blocking user interaction.
 */
@Composable
fun FinalLifetimeVipAutoBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(420)) + scaleOut(targetScale = 0.95f, animationSpec = tween(420))
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xF5190A2E),
                                Color(0xF5120822),
                                Color(0xF5101F30)
                            )
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .testTag("final_vip_auto_banner"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tour_final_vip_banner_text),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
