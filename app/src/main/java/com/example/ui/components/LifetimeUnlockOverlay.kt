package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LicenseUiState
import com.example.data.UpiVerificationResult
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun LifetimeUnlockScreen(
    licenseState: LicenseUiState,
    upiQrPayload: String,
    createUpiPaymentIntent: () -> Pair<String, Intent>,
    onVerifyUpiResult: (resultCode: Int, data: Intent?, expectedTxnRef: String?) -> UpiVerificationResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingTxnRef by remember { mutableStateOf<String?>(null) }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val verification = onVerifyUpiResult(
            activityResult.resultCode,
            activityResult.data,
            pendingTxnRef
        )
        pendingTxnRef = null
        when (verification) {
            is UpiVerificationResult.Success -> {
                paymentStatusMessage = null
                onDismiss()
            }
            UpiVerificationResult.Cancelled -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_cancelled)
            }
            UpiVerificationResult.PendingOrSubmitted -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_pending)
            }
            is UpiVerificationResult.Failed -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_failed)
            }
        }
    }

    val launchUpiPaymentNow: () -> Unit = {
        paymentStatusMessage = null
        val (txnRef, upiIntent) = createUpiPaymentIntent()
        pendingTxnRef = txnRef
        val chooserTitle = context.getString(R.string.bn_upi_chooser_title)
        val chooser = Intent.createChooser(upiIntent, chooserTitle)
        try {
            upiLauncher.launch(chooser)
        } catch (_: ActivityNotFoundException) {
            pendingTxnRef = null
            paymentStatusMessage = context.getString(R.string.bn_upi_no_app_error)
        } catch (_: Exception) {
            pendingTxnRef = null
            paymentStatusMessage = context.getString(R.string.bn_upi_no_app_error)
        }
    }

    val copyDeviceId: () -> Unit = {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Device ID", licenseState.deviceId))
        Toast.makeText(context, context.getString(R.string.bn_device_id_copied), Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE006040D))
            .statusBarsPadding()
            .testTag("lifetime_unlock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianSurface)
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_close_optional_unlock")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.bn_btn_cancel),
                            tint = TextSecondary
                        )
                    }
                }

                // Glowing VIP Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonMagenta.copy(alpha = 0.24f), NeonCyan.copy(alpha = 0.2f))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(NeonMagenta, NeonCyan)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Glowing "Rakib official" Title & LIFETIME VIP • ONE-TIME PAYMENT (₹20) Banner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.studio_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = OrbitronFamily,
                            fontWeight = FontWeight.ExtraBold,
                            brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan))
                        ),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonMagenta.copy(alpha = 0.24f), NeonCyan.copy(alpha = 0.2f))
                                )
                            )
                            .border(
                                1.1.dp,
                                Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vip_one_time_badge),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = if (licenseState.isFreeStandardMode) {
                            stringResource(R.string.bn_unlock_expired_message)
                        } else {
                            stringResource(R.string.bn_unlock_active_trial_message)
                        },
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Clean Glassmorphic 6-Character Device ID Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xCC18102C), Color(0xCC13162D))
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.65f), NeonMagenta.copy(alpha = 0.45f))),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = copyDeviceId)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("unlock_device_id_box"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.bn_device_id_label),
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = licenseState.deviceId,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.2.sp,
                            color = NeonCyan,
                            modifier = Modifier.testTag("unlock_device_id_value")
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.bn_device_id_copied),
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Payment Status / Error Message (only rendered when present)
                if (!paymentStatusMessage.isNullOrBlank()) {
                    Text(
                        text = paymentStatusMessage.orEmpty(),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = NeonAmber,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonAmber.copy(alpha = 0.14f))
                            .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Vibrant Pink Primary UPI Payment Button (6296603748@okbizaxis in background intent)
                Button(
                    onClick = launchUpiPaymentNow,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMagenta,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("btn_official_upi_pay")
                ) {
                    Text(
                        text = stringResource(R.string.bn_btn_upi_pay),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Scannable Official Merchant UPI QR Code Card
                UpiQrCodeCard(upiPayload = upiQrPayload)

                // Dismiss back to Forever Free Classic Black Mode
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_continue_free_standard")
                ) {
                    Text(
                        text = stringResource(R.string.bn_btn_cancel),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun UpiQrCodeCard(
    upiPayload: String
) {
    val qrBitmap = remember(upiPayload) {
        generateQrBitmap(payload = upiPayload, sizePx = 420)
    }

    if (qrBitmap != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianCanvas)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .testTag("upi_qr_code_card"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.bn_upi_qr_title),
                fontFamily = NotoSansBengaliFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.bn_upi_qr_title),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = stringResource(R.string.bn_upi_qr_caption),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonCyan,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generateQrBitmap(payload: String, sizePx: Int): Bitmap? {
    return runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()
}
