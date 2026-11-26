package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BannerThemePreset
import com.example.data.CustomBackgroundManager
import com.example.data.CustomPhotoTheme
import com.example.model.KeyboardSettings
import com.example.model.RgbEffect
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipGlassRimSubtle
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.luminousGlassCard
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
sealed interface BackdropEditTarget {
    data class PhotoUri(val uri: Uri) : BackdropEditTarget
    data class BannerPreset(val preset: BannerThemePreset) : BackdropEditTarget
    data class RemoteBanner(
        val title: String,
        val subtitle: String,
        val imageSource: String
    ) : BackdropEditTarget
}

/**
 * Unified 2-Step Photo Crop/Pan + "Adjust Brightness" Modal for BOTH:
 * - Gallery Photos (`PhotoUri`): Step 1 interactive Crop & Pan ("Pinch to scale, drag to move" + "Next")
 *   -> Step 2 "Adjust Brightness" (0%..100%) with live full keyboard preview & "Done".
 * - Special Banners (`BannerPreset` / `RemoteBanner`): Opens Step 2 "Adjust Brightness" (0%..100%)
 *   directly with live full keyboard preview & "Done".
 */
@Composable
fun BackdropBrightnessEditorDialog(
    target: BackdropEditTarget,
    settings: KeyboardSettings,
    customPhotoTheme: CustomPhotoTheme,
    timeSec: Float,
    onDismiss: () -> Unit,
    onApplyCroppedPhoto: (bitmap: Bitmap, dimAlpha: Float, showKeyBorders: Boolean) -> Unit,
    onApplyBannerPreset: (presetId: String, dimAlpha: Float, showKeyBorders: Boolean) -> Unit,
    onApplyRemoteBanner: (imageSource: String, dimAlpha: Float, showKeyBorders: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isGalleryFlow = target is BackdropEditTarget.PhotoUri
    var step by remember(target) { mutableIntStateOf(if (isGalleryFlow) 1 else 2) }

    var sourceBitmap by remember(target) { mutableStateOf<Bitmap?>(null) }
    var croppedBitmap by remember(target) { mutableStateOf<Bitmap?>(null) }
    var isLoadingPhoto by remember(target) { mutableStateOf(isGalleryFlow) }
    var loadError by remember(target) { mutableStateOf<String?>(null) }

    // Crop/pan transform state for Step 1
    var scale by remember(target) { mutableFloatStateOf(1f) }
    var offsetX by remember(target) { mutableFloatStateOf(0f) }
    var offsetY by remember(target) { mutableFloatStateOf(0f) }
    var cropViewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Step 2: User-facing Brightness 0%..100% (1f = brightest / 0.05 dim, 0f = darkest / 0.85 dim)
    var brightnessFraction by remember(target) {
        mutableFloatStateOf(CustomBackgroundManager.dimAlphaToBrightness(customPhotoTheme.dimAlpha))
    }
    var localKeyBorders by remember(target) { mutableStateOf(settings.showKeyBorders) }

    LaunchedEffect(target) {
        if (target is BackdropEditTarget.PhotoUri) {
            isLoadingPhoto = true
            loadError = null
            val loaded = withContext(Dispatchers.IO) {
                decodeOrientedSampledBitmap(context, target.uri, maxDim = 1440)
            }
            sourceBitmap = loaded
            if (loaded == null) {
                loadError = "Unable to load selected photo"
            }
            isLoadingPhoto = false
        }
    }

    val currentDimAlpha = remember(brightnessFraction) {
        CustomBackgroundManager.brightnessToDimAlpha(brightnessFraction)
    }

    val headerTitle = when {
        step == 1 -> "Scale & Crop Photo"
        target is BackdropEditTarget.BannerPreset -> target.preset.title
        target is BackdropEditTarget.RemoteBanner -> target.title
        else -> "Adjust Brightness"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .widthIn(max = 560.dp)
                .luminousGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    borderBrush = FlagshipCyanMagentaBrush,
                    borderWidth = 1.6.dp,
                    glowColor = NeonCyan,
                    glowElevation = 20.dp
                )
                .padding(16.dp)
                .testTag("backdrop_brightness_editor_dialog"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (step == 2 && isGalleryFlow) {
                        IconButton(
                            onClick = { step = 1 },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Crop",
                                tint = NeonCyan
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (step == 1) Icons.Filled.Crop else Icons.Filled.BrightnessHigh,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = headerTitle,
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (step == 1) "Pinch to scale, drag to move" else "Adjust Brightness & test live keys",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 12.sp,
                            color = NeonCyan.copy(alpha = 0.9f)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }
            }

            if (step == 1) {
                // STEP 1: Interactive Crop & Pan Frame matching Keyboard Aspect Ratio (~1.65:1)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.62f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0D16))
                        .border(1.4.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .onSizeChanged { cropViewportSize = it }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 4.5f)
                                val maxOffsetX = (cropViewportSize.width * (nextScale - 1f) / 2f).coerceAtLeast(80f)
                                val maxOffsetY = (cropViewportSize.height * (nextScale - 1f) / 2f).coerceAtLeast(80f)
                                scale = nextScale
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            }
                        }
                        .testTag("photo_crop_viewport"),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingPhoto -> CircularProgressIndicator(color = NeonCyan)
                        loadError != null -> Text(
                            text = loadError.orEmpty(),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        sourceBitmap != null -> {
                            val imgBitmap = remember(sourceBitmap) { sourceBitmap!!.asImageBitmap() }
                            Image(
                                bitmap = imgBitmap,
                                contentDescription = "Crop Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                            // Rule-of-thirds keyboard alignment grid overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val lineColor = Color.White.copy(alpha = 0.28f)
                                val thirdW = size.width / 3f
                                val thirdH = size.height / 3f
                                drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height), 1.dp.toPx())
                                drawLine(lineColor, Offset(thirdW * 2f, 0f), Offset(thirdW * 2f, size.height), 1.dp.toPx())
                                drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH), 1.dp.toPx())
                                drawLine(lineColor, Offset(0f, thirdH * 2f), Offset(size.width, thirdH * 2f), 1.dp.toPx())
                                drawRect(
                                    color = NeonCyan.copy(alpha = 0.5f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Step 1 Buttons: Cancel | Reset | Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("crop_btn_cancel")
                    ) {
                        Text(text = "Cancel", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            scope.launch {
                                val cropped = withContext(Dispatchers.Default) {
                                    renderCroppedKeyboardBitmap(
                                        source = bmp,
                                        viewportW = cropViewportSize.width.coerceAtLeast(1),
                                        viewportH = cropViewportSize.height.coerceAtLeast(1),
                                        userScale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY
                                    )
                                }
                                croppedBitmap = cropped
                                step = 2
                            }
                        },
                        enabled = sourceBitmap != null && !isLoadingPhoto,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF061020)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("crop_btn_next")
                    ) {
                        Text(text = "Next", fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                // STEP 2: Live Full Keyboard Preview + "Adjust Brightness" (0%..100%) + Key Borders + Done
                val croppedImageBitmap = remember(croppedBitmap) { croppedBitmap?.asImageBitmap() }

                FullMiniKeyboardPreview(
                    settings = settings,
                    timeSec = timeSec,
                    customPhotoTheme = customPhotoTheme,
                    overrideBannerPresetId = (target as? BackdropEditTarget.BannerPreset)?.preset?.id,
                    overrideRemoteImageUrl = (target as? BackdropEditTarget.RemoteBanner)?.imageSource,
                    overrideCustomBitmap = croppedImageBitmap,
                    overrideDimAlpha = currentDimAlpha,
                    overrideEffect = RgbEffect.NORMAL_DEFAULT,
                    overrideTouchEffect = settings.touchEffect,
                    overridePalette = settings.palette,
                    showKeyBorders = localKeyBorders,
                    forceDefaultBlack = false,
                    isInteractive = true,
                    compact = false
                )

                // Adjust Brightness Slider Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassCardInner)
                        .border(1.dp, FlagshipGlassRimSubtle, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adjust Brightness",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${(brightnessFraction * 100f).roundToInt()}%",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }
                    Slider(
                        value = brightnessFraction,
                        onValueChange = { brightnessFraction = it.coerceIn(0f, 1f) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_adjust_brightness")
                    )
                }

                // Key Borders Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassCardInner)
                        .border(1.dp, FlagshipGlassRimSubtle, RoundedCornerShape(16.dp))
                        .clickable { localKeyBorders = !localKeyBorders }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Key borders",
                        fontFamily = SpaceGroteskFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Switch(
                        checked = localKeyBorders,
                        onCheckedChange = { localKeyBorders = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RoyalBlueDeep,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = ObsidianCanvas,
                            uncheckedBorderColor = GlassBorderStrong
                        ),
                        modifier = Modifier.testTag("brightness_switch_key_borders")
                    )
                }

                // Cancel & Done Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("brightness_btn_cancel")
                    ) {
                        Text(text = "Cancel", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = {
                            when (target) {
                                is BackdropEditTarget.PhotoUri -> {
                                    val finalBmp = croppedBitmap ?: sourceBitmap
                                    if (finalBmp != null) {
                                        onApplyCroppedPhoto(finalBmp, currentDimAlpha, localKeyBorders)
                                    }
                                }
                                is BackdropEditTarget.BannerPreset -> {
                                    onApplyBannerPreset(target.preset.id, currentDimAlpha, localKeyBorders)
                                }
                                is BackdropEditTarget.RemoteBanner -> {
                                    onApplyRemoteBanner(target.imageSource, currentDimAlpha, localKeyBorders)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF061020)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("brightness_btn_done")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Done", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun decodeOrientedSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var inSample = 1
        while ((boundsOpts.outWidth / inSample) > maxDim || (boundsOpts.outHeight / inSample) > maxDim) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@runCatching null

        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) {
            raw
        } else {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
                if (it !== raw) raw.recycle()
            }
        }
    }.getOrNull()
}

private fun renderCroppedKeyboardBitmap(
    source: Bitmap,
    viewportW: Int,
    viewportH: Int,
    userScale: Float,
    offsetX: Float,
    offsetY: Float,
    outW: Int = 960,
    outH: Int = 592
): Bitmap {
    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val vw = viewportW.toFloat().coerceAtLeast(1f)
    val vh = viewportH.toFloat().coerceAtLeast(1f)
    val sw = source.width.toFloat().coerceAtLeast(1f)
    val sh = source.height.toFloat().coerceAtLeast(1f)

    // Replicate ContentScale.Crop base fit inside viewport, then apply user pinch/pan
    val cropBaseScale = max(vw / sw, vh / sh)
    val totalScaleInViewport = cropBaseScale * userScale
    val scaleToOutputX = outW / vw
    val scaleToOutputY = outH / vh

    val matrix = Matrix()
    matrix.postTranslate(-sw / 2f, -sh / 2f)
    matrix.postScale(
        totalScaleInViewport * scaleToOutputX,
        totalScaleInViewport * scaleToOutputY
    )
    matrix.postTranslate(
        outW / 2f + offsetX * scaleToOutputX,
        outH / 2f + offsetY * scaleToOutputY
    )

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return bmp
}
