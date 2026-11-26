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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BanglishPlanOption
import com.example.data.LicenseUiState
import com.example.data.RemoteAdminConfig
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.GlassCardSelected
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import com.example.ui.theme.midnightNavyIndigoWallpaper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

enum class UnlockPaywallFocus { VIP_FEATURES, BANGLISH }

@Composable
fun LifetimeUnlockScreen(
    licenseState: LicenseUiState,
    initialFocus: UnlockPaywallFocus = UnlockPaywallFocus.VIP_FEATURES,
    generateDiscountedAmount: (baseAmountInr: Int) -> String,
    registerPendingPayment: (planScope: String, baseAmountInr: Int, uniqueAmountFormatted: String, durationDays: Int) -> String,
    buildUpiQrPayload: (planScope: String, amountInr: Int, uniqueAmountFormatted: String, durationDays: Int) -> String,
    createUpiPaymentIntent: (planScope: String, amountInr: Int, uniqueAmountFormatted: String, durationDays: Int) -> Pair<String, Intent>,
    onVerifyUpiResult: (resultCode: Int, data: Intent?, expectedTxnRef: String?, planScope: String, purchasedDurationDays: Int?) -> UpiVerificationResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminConfig = licenseState.adminConfig
    val scrollState = rememberScrollState()

    val banglishOptions = remember(
        adminConfig.banglishPrice,
        adminConfig.banglishDurationDays,
        adminConfig.banglishDailyPrice,
        adminConfig.banglishWeeklyPrice,
        adminConfig.banglishMonthlyPrice,
        adminConfig.banglishCustomPlans
    ) {
        adminConfig.resolvedBanglishPlanOptions()
    }

    var selectedBanglishId by remember(banglishOptions) {
        val defaultOpt = banglishOptions.firstOrNull { it.durationDays == adminConfig.banglishDurationDays }
            ?: banglishOptions.lastOrNull()
        mutableStateOf(defaultOpt?.id.orEmpty())
    }
    val selectedBanglishOption: BanglishPlanOption = banglishOptions.firstOrNull { it.id == selectedBanglishId }
        ?: banglishOptions.lastOrNull()
        ?: BanglishPlanOption(
            id = "monthly",
            labelBn = "৩০ দিন",
            labelEn = "30 Days",
            priceInr = adminConfig.banglishPrice,
            durationDays = adminConfig.banglishDurationDays
        )

    // Customer-safe discounted unique-paisa amounts strictly LESS than base price (e.g., ₹20 -> ₹19.83)
    val vipDiscountedAmount = remember(adminConfig.vipPrice, adminConfig.vipDurationDays) {
        generateDiscountedAmount(adminConfig.vipPrice)
    }
    val banglishDiscountedAmount = remember(selectedBanglishOption.priceInr, selectedBanglishOption.durationDays) {
        generateDiscountedAmount(selectedBanglishOption.priceInr)
    }

    var showVipQr by remember { mutableStateOf(false) }
    var showBanglishQr by remember { mutableStateOf(initialFocus == UnlockPaywallFocus.BANGLISH) }

    var pendingTxnRef by remember { mutableStateOf<String?>(null) }
    var pendingPlanScope by remember { mutableStateOf(TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES) }
    var pendingDurationDays by remember { mutableStateOf<Int?>(null) }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }

    // Scroll smoothly to Card 2 when opened directly from Banglish "👑 VIP" button
    LaunchedEffect(initialFocus) {
        if (initialFocus == UnlockPaywallFocus.BANGLISH) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val vipQrPayload = remember(adminConfig.vipPrice, adminConfig.vipDurationDays, vipDiscountedAmount) {
        buildUpiQrPayload(
            TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
            adminConfig.vipPrice,
            vipDiscountedAmount,
            adminConfig.vipDurationDays
        )
    }
    val banglishQrPayload = remember(selectedBanglishOption, banglishDiscountedAmount) {
        buildUpiQrPayload(
            TrialLicenseManager.PLAN_SCOPE_BANGLISH,
            selectedBanglishOption.priceInr,
            banglishDiscountedAmount,
            selectedBanglishOption.durationDays
        )
    }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val verification = onVerifyUpiResult(
            activityResult.resultCode,
            activityResult.data,
            pendingTxnRef,
            pendingPlanScope,
            pendingDurationDays
        )
        pendingTxnRef = null
        pendingDurationDays = null
        when (verification) {
            is UpiVerificationResult.Success -> {
                paymentStatusMessage = null
                Toast.makeText(context, "✅ পেমেন্ট সফল! প্ল্যান আনলক হয়েছে", Toast.LENGTH_SHORT).show()
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

    val launchPlanUpiPayment: (String, Int, String, Int, String?) -> Unit = { scope, baseAmount, uniqueAmount, durationDays, targetPackage ->
        paymentStatusMessage = null
        val (txnRef, upiIntent) = createUpiPaymentIntent(scope, baseAmount, uniqueAmount, durationDays)
        pendingTxnRef = txnRef
        pendingPlanScope = scope
        pendingDurationDays = durationDays
        val chooserTitle = context.getString(R.string.bn_upi_chooser_title)
        val pm = context.packageManager

        val launched = runCatching {
            if (!targetPackage.isNullOrBlank()) {
                val targeted = Intent(upiIntent).setPackage(targetPackage)
                if (targeted.resolveActivity(pm) != null) {
                    upiLauncher.launch(targeted)
                    true
                } else {
                    val hasAnyUpi = pm.queryIntentActivities(upiIntent, 0).isNotEmpty()
                    if (hasAnyUpi) {
                        upiLauncher.launch(Intent.createChooser(upiIntent, chooserTitle))
                        true
                    } else {
                        false
                    }
                }
            } else {
                val hasAnyUpi = pm.queryIntentActivities(upiIntent, 0).isNotEmpty()
                if (hasAnyUpi) {
                    upiLauncher.launch(Intent.createChooser(upiIntent, chooserTitle))
                    true
                } else {
                    false
                }
            }
        }.getOrDefault(false)

        if (!launched) {
            pendingTxnRef = null
            pendingDurationDays = null
            if (scope == TrialLicenseManager.PLAN_SCOPE_BANGLISH) {
                showBanglishQr = true
            } else {
                showVipQr = true
            }
            paymentStatusMessage = "কোনো UPI অ্যাপ সরাসরি পাওয়া যায়নি — নিচের QR কোড স্ক্যান করে ₹$uniqueAmount পেমেন্ট করুন।"
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
            .midnightNavyIndigoWallpaper()
            .statusBarsPadding()
            .testTag("lifetime_unlock_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row: "Rakib official" VIP Hub + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.studio_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = OrbitronFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "VIP & Special Features Store",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GlassCardInner)
                            .border(1.dp, GlassBorder, CircleShape)
                            .testTag("btn_close_optional_unlock")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.bn_btn_cancel),
                            tint = Color.White
                        )
                    }
                }

                // Compact Hardware Device ID Pill (RK-XXXXXXXX)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardInner)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = copyDeviceId)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("unlock_device_id_box"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Device ID:",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(
                            text = licenseState.deviceId,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = RoyalGold,
                            modifier = Modifier.testTag("unlock_device_id_value")
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.bn_device_id_copied),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Live Firebase Auto-Payment Status Banner (if user initiated a discounted unique-paisa payment)
                val pending = licenseState.pendingPayment
                if (pending != null && (pending.isPending || pending.isApproved)) {
                    val isApproved = pending.isApproved
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isApproved) NeonEmerald.copy(alpha = 0.18f)
                                else NeonCyan.copy(alpha = 0.14f)
                            )
                            .border(
                                1.dp,
                                if (isApproved) NeonEmerald else NeonCyan.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("pending_payment_status_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isApproved) {
                                "✅ পেমেন্ট কনফার্ম (₹${pending.uniqueAmount})! আপনার প্ল্যান অটো আনলক হয়েছে।"
                            } else {
                                "⏳ অটো-ভেরিফিকেশন চালু: ₹${pending.uniqueAmount} পেমেন্ট সম্পন্ন হলেই সাথে সাথে অটো আনলক হবে।"
                            },
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Payment Error / Status Message
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

                // =========================================================================
                // CARD 1: ✨ RGB & VIP Features (Excludes Romanized Bengali) — Independent Pay + QR
                // =========================================================================
                IndependentVipFeaturesCard(
                    licenseState = licenseState,
                    basePriceInr = adminConfig.vipPrice,
                    durationDays = adminConfig.vipDurationDays,
                    discountedUniqueAmount = vipDiscountedAmount,
                    isHighlighted = initialFocus == UnlockPaywallFocus.VIP_FEATURES,
                    showQr = showVipQr,
                    qrPayload = vipQrPayload,
                    onToggleQr = {
                        val nextShow = !showVipQr
                        showVipQr = nextShow
                        if (nextShow) {
                            registerPendingPayment(
                                TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
                                adminConfig.vipPrice,
                                vipDiscountedAmount,
                                adminConfig.vipDurationDays
                            )
                        }
                    },
                    onPayClick = {
                        launchPlanUpiPayment(
                            TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
                            adminConfig.vipPrice,
                            vipDiscountedAmount,
                            adminConfig.vipDurationDays,
                            null
                        )
                    },
                    onPayWithPackage = { pkg ->
                        launchPlanUpiPayment(
                            TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
                            adminConfig.vipPrice,
                            vipDiscountedAmount,
                            adminConfig.vipDurationDays,
                            pkg
                        )
                    }
                )

                // =========================================================================
                // CARD 2: 🇮🇳 Romanized Bengali (Separate Subscription) — Independent Pay + QR + Live Timer Bar
                // =========================================================================
                IndependentBanglishCard(
                    licenseState = licenseState,
                    planOptions = banglishOptions,
                    selectedOption = selectedBanglishOption,
                    discountedUniqueAmount = banglishDiscountedAmount,
                    isHighlighted = initialFocus == UnlockPaywallFocus.BANGLISH,
                    showQr = showBanglishQr,
                    qrPayload = banglishQrPayload,
                    onSelectOption = { opt ->
                        selectedBanglishId = opt.id
                    },
                    onToggleQr = {
                        val nextShow = !showBanglishQr
                        showBanglishQr = nextShow
                        if (nextShow) {
                            registerPendingPayment(
                                TrialLicenseManager.PLAN_SCOPE_BANGLISH,
                                selectedBanglishOption.priceInr,
                                banglishDiscountedAmount,
                                selectedBanglishOption.durationDays
                            )
                        }
                    },
                    onPayClick = {
                        launchPlanUpiPayment(
                            TrialLicenseManager.PLAN_SCOPE_BANGLISH,
                            selectedBanglishOption.priceInr,
                            banglishDiscountedAmount,
                            selectedBanglishOption.durationDays,
                            null
                        )
                    },
                    onPayWithPackage = { pkg ->
                        launchPlanUpiPayment(
                            TrialLicenseManager.PLAN_SCOPE_BANGLISH,
                            selectedBanglishOption.priceInr,
                            banglishDiscountedAmount,
                            selectedBanglishOption.durationDays,
                            pkg
                        )
                    }
                )

                // Dismiss back to Forever Free Classic Mode
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
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun IndependentVipFeaturesCard(
    licenseState: LicenseUiState,
    basePriceInr: Int,
    durationDays: Int,
    discountedUniqueAmount: String,
    isHighlighted: Boolean,
    showQr: Boolean,
    qrPayload: String,
    onToggleQr: () -> Unit,
    onPayClick: () -> Unit,
    onPayWithPackage: (String) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val isUnlocked = licenseState.isPaidOrAdminVip
    val durationTextBn = when {
        durationDays <= 0 -> "লাইফটাইম"
        durationDays == 1 -> "১ দিন"
        durationDays == 7 -> "৭ দিন"
        durationDays == 30 -> "৩০ দিন"
        else -> "${RemoteAdminConfig.toBengaliDigits(durationDays)} দিন"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = shape,
                borderBrush = when {
                    isUnlocked -> FlagshipEmeraldCyanBrush
                    isHighlighted -> FlagshipCyanMagentaBrush
                    else -> FlagshipGoldAmberBrush
                },
                borderWidth = if (isHighlighted || isUnlocked) 1.7.dp else 1.2.dp,
                glowColor = if (isUnlocked) NeonEmerald else NeonCyan,
                glowElevation = if (isHighlighted) 18.dp else 12.dp
            )
            .padding(16.dp)
            .testTag("unlock_plan_all_vip"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1 Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "✨ RGB & VIP Features",
                    fontFamily = OrbitronFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )
                Text(
                    text = "RGB Themes • Sticker • Aa Fonts • AI Hub • Call Bar",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Text(
                    text = "(Romanized Bengali আলাদা প্ল্যান)",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 10.5.sp,
                    color = NeonAmber.copy(alpha = 0.92f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isUnlocked) NeonEmerald.copy(alpha = 0.22f) else GlassCardSelected)
                    .border(
                        1.dp,
                        if (isUnlocked) NeonEmerald else RoyalGold,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isUnlocked) "✅ ACTIVE" else durationTextBn,
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) NeonEmerald else RoyalGold
                )
            }
        }

        // Customer-Safe Discounted Unique-Paisa Price Strip
        DiscountedPriceStrip(
            basePriceInr = basePriceInr,
            discountedUniqueAmount = discountedUniqueAmount,
            planBadge = RemoteAdminConfig.formatPlanBadgeEn(basePriceInr, durationDays)
        )

        // Independent Pay Now + QR Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPayClick,
                enabled = !licenseState.isLifetimeUnlocked && !licenseState.isGlobalOrAdminAllFree,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMagenta,
                    contentColor = Color.White,
                    disabledContainerColor = NeonEmerald.copy(alpha = 0.28f),
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("btn_pay_vip_features")
            ) {
                if (licenseState.isLifetimeUnlocked || licenseState.isGlobalOrAdminAllFree) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIP আনলকড",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Pay ₹$discountedUniqueAmount ($durationTextBn)",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!licenseState.isLifetimeUnlocked && !licenseState.isGlobalOrAdminAllFree) {
                OutlinedButton(
                    onClick = onToggleQr,
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("btn_qr_vip_features")
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "VIP QR Code",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showQr) "Hide" else "QR",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (!licenseState.isLifetimeUnlocked && !licenseState.isGlobalOrAdminAllFree) {
            QuickUpiAppsRow(
                onPhonePeClick = { onPayWithPackage(PKG_PHONEPE) },
                onGPayClick = { onPayWithPackage(PKG_GPAY) }
            )
        }

        if (showQr && !licenseState.isLifetimeUnlocked && !licenseState.isGlobalOrAdminAllFree) {
            UpiQrCodeCard(
                upiPayload = qrPayload,
                uniqueAmountStr = discountedUniqueAmount,
                onPhonePeClick = { onPayWithPackage(PKG_PHONEPE) },
                onGPayClick = { onPayWithPackage(PKG_GPAY) }
            )
        }
    }
}

@Composable
private fun IndependentBanglishCard(
    licenseState: LicenseUiState,
    planOptions: List<BanglishPlanOption>,
    selectedOption: BanglishPlanOption,
    discountedUniqueAmount: String,
    isHighlighted: Boolean,
    showQr: Boolean,
    qrPayload: String,
    onSelectOption: (BanglishPlanOption) -> Unit,
    onToggleQr: () -> Unit,
    onPayClick: () -> Unit,
    onPayWithPackage: (String) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val isPermanent = licenseState.isGlobalOrAdminAllFree || licenseState.isBanglishLifetimeUnlocked

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = shape,
                borderBrush = if (isHighlighted) FlagshipGoldCyanBrush else FlagshipGoldAmberBrush,
                borderWidth = if (isHighlighted) 1.8.dp else 1.3.dp,
                glowColor = RoyalGold,
                glowElevation = if (isHighlighted) 20.dp else 14.dp
            )
            .padding(16.dp)
            .testTag("unlock_plan_banglish"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 2 Header: Indian Flag & Romanized Bengali Branding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "🇮🇳 Romanized Bengali",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalGold
                )
                Text(
                    text = "বাংলা → English অক্ষর ইঞ্জিন (আলাদা সাবস্ক্রিপশন)",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (licenseState.isBanglishUnlocked) NeonEmerald.copy(alpha = 0.22f)
                        else NeonAmber.copy(alpha = 0.20f)
                    )
                    .border(
                        1.dp,
                        if (licenseState.isBanglishUnlocked) NeonEmerald else NeonAmber,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (licenseState.isBanglishUnlocked) "👑 ACTIVE" else "🔒 VIP",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (licenseState.isBanglishUnlocked) NeonEmerald else NeonAmber
                )
            }
        }

        // Live Countdown Timer Bar for Romanized Bengali
        BanglishCountdownTimerBar(
            countdownLabelBn = licenseState.formattedBanglishCountdownBn,
            countdownLabelEn = licenseState.formattedBanglishCountdownEn,
            progress = licenseState.banglishCountdownProgress,
            isActive = licenseState.isBanglishUnlocked
        )

        // Selectable Remote Daily / Weekly / Monthly / Custom-Day Plan Chips
        if (!isPermanent && planOptions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banglish_plan_options_row")
            ) {
                items(planOptions, key = { it.id }) { option ->
                    val isSelected = option.id == selectedOption.id
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GlassCardSelected else GlassCardInner)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                brush = if (isSelected) FlagshipGoldCyanBrush else FlagshipGoldAmberBrush,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectOption(option) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("banglish_plan_chip_${option.id}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = option.labelBn,
                            fontFamily = NotoSansBengaliFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonCyan else Color.White
                        )
                        Text(
                            text = "₹${option.priceInr}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalGold
                        )
                    }
                }
            }
        }

        // Customer-Safe Discounted Unique-Paisa Price Strip
        if (!isPermanent) {
            DiscountedPriceStrip(
                basePriceInr = selectedOption.priceInr,
                discountedUniqueAmount = discountedUniqueAmount,
                planBadge = "BANGLISH • ${selectedOption.labelEn.uppercase()} (₹${selectedOption.priceInr})"
            )
        }

        // Independent Pay Now + QR Toggle Row for Romanized Bengali
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPayClick,
                enabled = !isPermanent,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = Color(0xFF140E02),
                    disabledContainerColor = NeonEmerald.copy(alpha = 0.28f),
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .testTag("btn_pay_banglish")
            ) {
                if (isPermanent) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Romanized Bengali অ্যাক্টিভ",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Pay ₹$discountedUniqueAmount (${selectedOption.labelBn})",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isPermanent) {
                OutlinedButton(
                    onClick = onToggleQr,
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("btn_qr_banglish")
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = "Banglish QR Code",
                        tint = RoyalGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showQr) "Hide" else "QR",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (!isPermanent) {
            QuickUpiAppsRow(
                onPhonePeClick = { onPayWithPackage(PKG_PHONEPE) },
                onGPayClick = { onPayWithPackage(PKG_GPAY) }
            )
        }

        if (showQr && !isPermanent) {
            UpiQrCodeCard(
                upiPayload = qrPayload,
                uniqueAmountStr = discountedUniqueAmount,
                onPhonePeClick = { onPayWithPackage(PKG_PHONEPE) },
                onGPayClick = { onPayWithPackage(PKG_GPAY) }
            )
        }
    }
}

@Composable
private fun QuickUpiAppsRow(
    onPhonePeClick: () -> Unit,
    onGPayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPhonePeClick,
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5F259F),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = "1-Tap PhonePe",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Button(
            onClick = onGPayClick,
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = "1-Tap GPay",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DiscountedPriceStrip(
    basePriceInr: Int,
    discountedUniqueAmount: String,
    planBadge: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardInner)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = planBadge,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = "অটো-ভেরিফাইড ডিসকাউন্ট পেমেন্ট",
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.80f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "₹$basePriceInr",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f),
                textDecoration = TextDecoration.LineThrough
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonEmerald.copy(alpha = 0.22f))
                    .border(1.dp, NeonEmerald.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "₹$discountedUniqueAmount",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonEmerald
                )
            }
        }
    }
}

@Composable
fun BanglishCountdownTimerBar(
    countdownLabelBn: String,
    countdownLabelEn: String,
    progress: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA0C1022))
            .border(
                1.dp,
                if (isActive) RoyalGold.copy(alpha = 0.55f) else NeonAmber.copy(alpha = 0.45f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .testTag("banglish_countdown_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isActive) "⏱ $countdownLabelBn" else "🔒 $countdownLabelBn",
                fontFamily = NotoSansBengaliFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else NeonAmber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = countdownLabelEn,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) NeonCyan else NeonAmber
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            val clampedProgress = progress.coerceIn(0f, 1f)
            if (clampedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (isActive) {
                                Brush.horizontalGradient(listOf(RoyalGold, NeonCyan, NeonEmerald))
                            } else {
                                Brush.horizontalGradient(listOf(NeonAmber, NeonMagenta))
                            }
                        )
                )
            }
        }
    }
}

private const val PKG_PHONEPE = "com.phonepe.app"
private const val PKG_GPAY = "com.google.android.apps.nbu.paisa.user"

@Composable
private fun UpiQrCodeCard(
    upiPayload: String,
    uniqueAmountStr: String,
    onPhonePeClick: () -> Unit,
    onGPayClick: () -> Unit
) {
    val qrBitmap = remember(upiPayload) {
        generateQrBitmap(payload = upiPayload, sizePx = 420)
    }

    if (qrBitmap != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassCardInner)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .testTag("upi_qr_code_card"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Verified Merchant QR ₹$uniqueAmountStr",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "Verified Merchant: QUALITY FOODS ✅",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonEmerald,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("verified_merchant_label")
            )
            Text(
                text = "ঠিক ₹$uniqueAmountStr পেমেন্ট করুন",
                fontFamily = NotoSansBengaliFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RoyalGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("exact_payable_amount_label")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPhonePeClick,
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5F259F),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("btn_pay_phonepe")
                ) {
                    Text(
                        text = "PhonePe",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Button(
                    onClick = onGPayClick,
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("btn_pay_gpay")
                ) {
                    Text(
                        text = "GPay",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
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
