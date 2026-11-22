package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LicenseUiState
import com.example.data.PinVerificationResult
import com.example.data.RestoreVipResult
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonError
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

private enum class PhoneHintPurpose { INITIAL, PAY_UPI, RESTORE_VIP }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LifetimeUnlockScreen(
    licenseState: LicenseUiState,
    upiQrPayload: String,
    createUpiPaymentIntent: () -> Pair<String, Intent>,
    onSaveMobileNumber: (String) -> String,
    onVerifyUpiResult: (resultCode: Int, data: Intent?, expectedTxnRef: String?, mobileNumber: String) -> UpiVerificationResult,
    onRestoreVipByMobile: (String) -> RestoreVipResult,
    onVerifyOwnerPin: (String) -> PinVerificationResult,
    onBuildWhatsAppUri: () -> Uri,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var pendingTxnRef by remember { mutableStateOf<String?>(null) }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }
    var isPaymentError by remember { mutableStateOf(false) }
    var showOwnerPinDialog by remember { mutableStateOf(false) }

    var selectedMobile by remember(licenseState.registeredMobileNumber) {
        mutableStateOf(licenseState.registeredMobileNumber)
    }
    var showManualMobileFallback by remember { mutableStateOf(false) }
    var activeHintPurpose by remember { mutableStateOf(PhoneHintPurpose.INITIAL) }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val verification = onVerifyUpiResult(
            activityResult.resultCode,
            activityResult.data,
            pendingTxnRef,
            selectedMobile
        )
        pendingTxnRef = null
        when (verification) {
            is UpiVerificationResult.Success -> {
                paymentStatusMessage = null
                isPaymentError = false
                onDismiss()
            }
            UpiVerificationResult.Cancelled -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_cancelled)
                isPaymentError = true
            }
            UpiVerificationResult.PendingOrSubmitted -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_pending)
                isPaymentError = true
            }
            is UpiVerificationResult.Failed -> {
                paymentStatusMessage = context.getString(R.string.bn_upi_failed)
                isPaymentError = true
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
            isPaymentError = true
        } catch (_: Exception) {
            pendingTxnRef = null
            paymentStatusMessage = context.getString(R.string.bn_upi_no_app_error)
            isPaymentError = true
        }
    }

    val handleRestoreForMobile: (String) -> Unit = { mobile ->
        when (onRestoreVipByMobile(mobile)) {
            is RestoreVipResult.Restored -> {
                paymentStatusMessage = context.getString(R.string.bn_restore_success)
                isPaymentError = false
                Toast.makeText(context, context.getString(R.string.bn_restore_success), Toast.LENGTH_LONG).show()
                onDismiss()
            }
            RestoreVipResult.WrongDevice -> {
                paymentStatusMessage = context.getString(R.string.bn_restore_wrong_device)
                isPaymentError = true
            }
            RestoreVipResult.NotFoundForDevice -> {
                paymentStatusMessage = context.getString(R.string.bn_restore_not_found)
                isPaymentError = true
            }
            RestoreVipResult.InvalidMobile -> {
                showManualMobileFallback = true
                paymentStatusMessage = context.getString(R.string.bn_restore_invalid_number)
                isPaymentError = true
            }
        }
    }

    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val rawHint = if (result.resultCode == Activity.RESULT_OK && result.data != null && activity != null) {
            runCatching {
                Identity.getSignInClient(activity).getPhoneNumberFromIntent(result.data)
            }.getOrNull()
        } else {
            null
        }

        val normalized = if (!rawHint.isNullOrBlank()) {
            onSaveMobileNumber(rawHint)
        } else {
            ""
        }

        if (normalized.length >= 10) {
            selectedMobile = normalized
            showManualMobileFallback = false
            when (activeHintPurpose) {
                PhoneHintPurpose.PAY_UPI -> launchUpiPaymentNow()
                PhoneHintPurpose.RESTORE_VIP -> handleRestoreForMobile(normalized)
                PhoneHintPurpose.INITIAL -> Unit
            }
        } else {
            // Clean fallback input box when SIM hint is dismissed or unavailable
            showManualMobileFallback = true
            if (activeHintPurpose == PhoneHintPurpose.PAY_UPI) {
                paymentStatusMessage = context.getString(R.string.bn_mobile_required_notice)
                isPaymentError = true
            }
        }
    }

    val triggerPhoneNumberHint: (PhoneHintPurpose) -> Unit = { purpose ->
        activeHintPurpose = purpose
        val hostActivity = activity
        if (hostActivity == null) {
            showManualMobileFallback = true
        } else {
            val request = GetPhoneNumberHintIntentRequest.builder().build()
            Identity.getSignInClient(hostActivity)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { pendingIntent ->
                    runCatching {
                        phoneHintLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent).build()
                        )
                    }.onFailure {
                        showManualMobileFallback = true
                        if (purpose == PhoneHintPurpose.PAY_UPI && selectedMobile.length >= 10) {
                            launchUpiPaymentNow()
                        } else if (purpose == PhoneHintPurpose.RESTORE_VIP && selectedMobile.length >= 10) {
                            handleRestoreForMobile(selectedMobile)
                        }
                    }
                }
                .addOnFailureListener {
                    // No SIM phone hint available on device/emulator -> show clean fallback input box
                    showManualMobileFallback = true
                    if (purpose == PhoneHintPurpose.PAY_UPI && selectedMobile.length >= 10) {
                        launchUpiPaymentNow()
                    } else if (purpose == PhoneHintPurpose.RESTORE_VIP && selectedMobile.length >= 10) {
                        handleRestoreForMobile(selectedMobile)
                    }
                }
        }
    }

    // Automatically trigger Android's native 1-tap SIM Phone Number Hint picker on open if no number is saved yet
    LaunchedEffect(Unit) {
        if (selectedMobile.length < 10) {
            triggerPhoneNumberHint(PhoneHintPurpose.INITIAL)
        }
    }

    val copyDeviceId: () -> Unit = {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Device ID", licenseState.deviceId))
        Toast.makeText(context, context.getString(R.string.bn_device_id_copied), Toast.LENGTH_SHORT).show()
    }

    if (showOwnerPinDialog) {
        OwnerPinFallbackDialog(
            deviceId = licenseState.deviceId,
            onCopyDeviceId = copyDeviceId,
            onOpenWhatsApp = {
                val uri = onBuildWhatsAppUri()
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            },
            onVerifyPin = { pin ->
                val res = onVerifyOwnerPin(pin)
                if (res is PinVerificationResult.Success) {
                    showOwnerPinDialog = false
                    onDismiss()
                }
                res
            },
            onDismiss = { showOwnerPinDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD05070E))
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
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianSurface)
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Close icon (user can always dismiss and keep using the Forever Free Standard Keyboard & Live Demo)
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

                // Glowing VIP Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.2f), NeonMagenta.copy(alpha = 0.24f))
                            )
                        )
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title, Glowing Vice City Branding & Exact English LIFETIME VIP • ONE-TIME PAYMENT (₹20) Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
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
                                1.dp,
                                Brush.horizontalGradient(listOf(NeonMagenta, NeonAmber, NeonCyan)),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
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
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Features Included Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bn_unlock_benefits_title),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                    Text(
                        text = stringResource(R.string.bn_unlock_benefit_1),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.bn_unlock_benefit_2),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.bn_unlock_benefit_3),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.bn_unlock_benefit_4),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                }

                // Clean Glassmorphic 6-Character Device ID Box directly above the Payment Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xCC18102C),
                                    Color(0xCC13162D)
                                )
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

                // 1-Tap SIM Mobile Number Card (auto-picked via Google Identity Phone Hint; clean fallback input if unavailable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, NeonCyan.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.bn_mobile_number_label),
                                fontFamily = NotoSansBengaliFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = if (selectedMobile.length >= 10) {
                                stringResource(R.string.bn_mobile_change_btn)
                            } else {
                                stringResource(R.string.bn_mobile_pick_sim_btn)
                            },
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { triggerPhoneNumberHint(PhoneHintPurpose.INITIAL) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("btn_pick_sim_mobile")
                        )
                    }

                    if (selectedMobile.length >= 10 && !showManualMobileFallback) {
                        Text(
                            text = selectedMobile,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary,
                            modifier = Modifier.testTag("unlock_selected_mobile_value")
                        )
                    } else if (showManualMobileFallback) {
                        OutlinedTextField(
                            value = selectedMobile,
                            onValueChange = { raw ->
                                val digits = TrialLicenseManager.normalizeMobileNumber(raw)
                                selectedMobile = digits
                                if (digits.length >= 10) {
                                    onSaveMobileNumber(digits)
                                }
                                paymentStatusMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.bn_mobile_input_placeholder),
                                    fontFamily = NotoSansBengaliFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan,
                                focusedContainerColor = ObsidianSurface,
                                unfocusedContainerColor = ObsidianSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fallback_mobile_number")
                        )
                    }
                }

                // Payment Status / Error Message
                if (!paymentStatusMessage.isNullOrBlank()) {
                    Text(
                        text = paymentStatusMessage.orEmpty(),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = if (isPaymentError) NeonAmber else NeonEmerald,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isPaymentError) NeonAmber.copy(alpha = 0.14f) else NeonEmerald.copy(alpha = 0.14f)
                            )
                            .border(
                                1.dp,
                                if (isPaymentError) NeonAmber.copy(alpha = 0.5f) else NeonEmerald.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Primary Official UPI Payment CTA Button (auto-triggers 1-tap SIM phone picker if not yet selected)
                Button(
                    onClick = {
                        val normalized = TrialLicenseManager.normalizeMobileNumber(selectedMobile)
                        if (normalized.length >= 10) {
                            selectedMobile = onSaveMobileNumber(normalized)
                            launchUpiPaymentNow()
                        } else {
                            triggerPhoneNumberHint(PhoneHintPurpose.PAY_UPI)
                        }
                    },
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Scannable Official Merchant UPI QR Code Card (merchant UPI ID hidden from UI)
                UpiQrCodeCard(upiPayload = upiQrPayload)

                // 1-Tap Automatic VIP Restore Button (Same Phone Only, via SIM Phone Hint Picker)
                OutlinedButton(
                    onClick = {
                        paymentStatusMessage = null
                        val normalized = TrialLicenseManager.normalizeMobileNumber(selectedMobile)
                        if (showManualMobileFallback && normalized.length >= 10) {
                            handleRestoreForMobile(normalized)
                        } else {
                            triggerPhoneNumberHint(PhoneHintPurpose.RESTORE_VIP)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.72f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_auto_restore_vip")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bn_btn_auto_restore_vip),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Secondary Helper Link -> Owner-Controlled Secret PIN Fallback Dialog
                Text(
                    text = stringResource(R.string.bn_owner_pin_helper_link),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showOwnerPinDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("btn_open_owner_pin_dialog")
                )

                // Always allow dismissing back to Forever Free Classic Black Keyboard + In-App Live Preview
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
private fun OwnerPinFallbackDialog(
    deviceId: String,
    onCopyDeviceId: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onVerifyPin: (String) -> PinVerificationResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.bn_owner_pin_dialog_title),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.bn_owner_pin_dialog_desc),
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )

                // Device ID copy card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCanvas)
                        .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onCopyDeviceId)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.bn_device_id_label),
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = deviceId,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = NeonCyan
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.bn_device_id_copied),
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // WhatsApp Receipt Button
                Button(
                    onClick = onOpenWhatsApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1FAF5A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_whatsapp_send_receipt")
                ) {
                    Text(
                        text = stringResource(R.string.bn_btn_whatsapp_receipt),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 6-Digit Owner Unlock PIN Input Field
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(6)
                        pinText = digits
                        errorMessage = null
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.bn_pin_input_label),
                            fontFamily = NotoSansBengaliFamily
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.bn_pin_input_placeholder),
                            fontFamily = NotoSansBengaliFamily,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan,
                        focusedContainerColor = ObsidianCanvas,
                        unfocusedContainerColor = ObsidianCanvas
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_owner_unlock_pin")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage.orEmpty(),
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.sp,
                        color = NeonError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (val result = onVerifyPin(pinText)) {
                        PinVerificationResult.Success -> {
                            errorMessage = null
                        }
                        PinVerificationResult.InvalidPin -> {
                            errorMessage = context.getString(R.string.bn_pin_invalid)
                        }
                        is PinVerificationResult.Cooldown -> {
                            errorMessage = context.getString(R.string.bn_pin_cooldown)
                        }
                    }
                },
                enabled = pinText.length == 6,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF001B22)
                ),
                modifier = Modifier.testTag("btn_verify_owner_pin")
            ) {
                Text(
                    text = stringResource(R.string.bn_btn_verify_pin),
                    fontFamily = NotoSansBengaliFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.bn_btn_cancel),
                    fontFamily = NotoSansBengaliFamily,
                    color = TextSecondary
                )
            }
        }
    )
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
                    .size(152.dp)
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
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun generateQrBitmap(payload: String, sizePx: Int): Bitmap? {
    return runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }.getOrNull()
}
