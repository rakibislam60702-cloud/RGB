package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGoldCyanBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.OrbitronFamily
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.luminousGlassCard
import com.example.ui.theme.midnightNavyIndigoWallpaper
import com.example.model.ImeStatus

/**
 * Clean One-Time Startup Setup Stepper shown once on first launch (`setupCompleted == false`):
 *  - Step 1: Requests RECORD_AUDIO, READ_PHONE_STATE, ANSWER_PHONE_CALLS (API 26+),
 *            and POST_NOTIFICATIONS (API 33+) together in one tap.
 *  - Step 2: Guides enabling "AI Keyboard" in Android Language & Input settings.
 *  - Step 3: Guides selecting "AI Keyboard" as the active input method.
 * Once finished (or skipped), persists `setupCompleted = true` and never appears again.
 */
@Composable
fun StartupSetupStepper(
    imeStatus: ImeStatus,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    fun checkPermissionsGranted(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permPrefs = remember(context) {
        context.getSharedPreferences("rakib_permissions_prefs", android.content.Context.MODE_PRIVATE)
    }
    var permissionsGranted by remember { mutableStateOf(checkPermissionsGranted()) }
    var permissionsRequestedOnce by remember {
        mutableStateOf(permPrefs.getBoolean("mic_permission_prompted_once", false))
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsRequestedOnce = true
        permPrefs.edit()
            .putBoolean("mic_permission_prompted_once", true)
            .putBoolean("mic_activity_prompted_once", true)
            .apply()
        permissionsGranted = results.values.all { it } || checkPermissionsGranted()
    }

    val micGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    val phoneGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
    val step1Done = permissionsGranted || (micGranted && phoneGranted)

    Box(
        modifier = modifier
            .fillMaxSize()
            .midnightNavyIndigoWallpaper()
            .statusBarsPadding()
            .testTag("startup_setup_stepper"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flagship Welcome Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(RoyalGold.copy(alpha = 0.16f))
                        .border(1.dp, RoyalGold.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ ONE-TIME QUICK SETUP",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalGold
                    )
                }
                Text(
                    text = "AI Keyboard — Rakib official",
                    fontFamily = OrbitronFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "একবার সেটআপ করলেই ভয়েস টাইপিং, কল বার ও RGB কীবোর্ড সবসময় স্মুথভাবে চলবে।",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // STEP 1: One-Tap Permissions (Mic + Call Bar + Notifications)
            SetupStepCard(
                stepNumber = "1",
                title = "ভয়েস টাইপিং ও কল বার পারমিশন",
                subtitle = "Gboard-স্টাইল স্মুথ বাংলা/ইংরেজি ভয়েস টাইপিং (Mic) এবং ইনকামিং কল রিসিভ/কাট বার চালু করতে একবার Allow করুন।",
                isDone = step1Done,
                badgeText = if (step1Done) "✅ GRANTED" else "1-TAP ALLOW",
                buttonLabel = if (step1Done) "পারমিশন চালু আছে ✓" else "সব পারমিশন Allow করুন",
                accentColor = NeonCyan,
                testTag = "stepper_btn_permissions",
                onAction = {
                    val alreadyPromptedMic = permPrefs.getBoolean("mic_permission_prompted_once", false)
                    val permissionsToRequest = if (alreadyPromptedMic) {
                        requiredPermissions.filterNot { it == Manifest.permission.RECORD_AUDIO }.toTypedArray()
                    } else {
                        requiredPermissions
                    }
                    if (!alreadyPromptedMic) {
                        permissionsRequestedOnce = true
                        permPrefs.edit()
                            .putBoolean("mic_permission_prompted_once", true)
                            .putBoolean("mic_activity_prompted_once", true)
                            .apply()
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        multiPermissionLauncher.launch(permissionsToRequest)
                    }
                }
            )

            // STEP 2: Enable AI Keyboard in Android Settings
            SetupStepCard(
                stepNumber = "2",
                title = "AI Keyboard চালু করুন (Enable)",
                subtitle = "ফোনের Keyboard Settings-এ গিয়ে \"AI Keyboard\" সুইচটি ON করে ফিরে আসুন।",
                isDone = imeStatus.isEnabled,
                badgeText = if (imeStatus.isEnabled) "✅ ENABLED" else "REQUIRED",
                buttonLabel = if (imeStatus.isEnabled) "AI Keyboard চালু আছে ✓" else "Enable AI Keyboard",
                accentColor = RoyalGold,
                testTag = "stepper_btn_enable_ime",
                onAction = onOpenImeSettings
            )

            // STEP 3: Select AI Keyboard as Default Input Method
            SetupStepCard(
                stepNumber = "3",
                title = "AI Keyboard সিলেক্ট করুন (Select)",
                subtitle = "আপনার ডিফল্ট কীবোর্ড হিসেবে \"AI Keyboard\" বেছে নিন।",
                isDone = imeStatus.isSelected,
                badgeText = if (imeStatus.isSelected) "✅ ACTIVE" else "FINAL STEP",
                buttonLabel = if (imeStatus.isSelected) "AI Keyboard সিলেক্টেড ✓" else "Select AI Keyboard",
                accentColor = NeonMagenta,
                testTag = "stepper_btn_select_ime",
                onAction = onShowImePicker
            )

            // Primary Continue / Finish CTA
            val allReady = step1Done && imeStatus.isEnabled && imeStatus.isSelected
            Button(
                onClick = onCompleteSetup,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allReady) NeonEmerald else NeonCyan,
                    contentColor = Color(0xFF041418)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("stepper_btn_finish")
            ) {
                Text(
                    text = if (allReady) "🚀 সব প্রস্তুত! স্টুডিও শুরু করুন" else "সেটআপ সম্পন্ন / অ্যাপে প্রবেশ করুন",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (!allReady) {
                OutlinedButton(
                    onClick = onCompleteSetup,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("stepper_btn_skip")
                ) {
                    Text(
                        text = if (permissionsRequestedOnce) "পরে সেট করব (Skip)" else "এখন বাদ দিন (Skip)",
                        fontFamily = NotoSansBengaliFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    subtitle: String,
    isDone: Boolean,
    badgeText: String,
    buttonLabel: String,
    accentColor: Color,
    testTag: String,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val icon = when (stepNumber) {
        "1" -> Icons.Filled.Mic
        "2" -> Icons.Filled.Keyboard
        else -> Icons.Filled.PhoneInTalk
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = shape,
                borderBrush = if (isDone) FlagshipEmeraldCyanBrush else FlagshipGoldCyanBrush,
                borderWidth = if (isDone) 1.5.dp else 1.2.dp,
                glowColor = if (isDone) NeonEmerald else accentColor,
                glowElevation = 12.dp
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDone) NeonEmerald.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.18f))
                        .border(1.dp, if (isDone) NeonEmerald else accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Step $stepNumber • $title",
                    fontFamily = NotoSansBengaliFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDone) NeonEmerald.copy(alpha = 0.22f) else GlassCardInner)
                    .border(1.dp, if (isDone) NeonEmerald else GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) NeonEmerald else accentColor
                )
            }
        }

        Text(
            text = subtitle,
            fontFamily = NotoSansBengaliFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = Color.White.copy(alpha = 0.80f)
        )

        Button(
            onClick = onAction,
            enabled = !isDone,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color(0xFF070C1A),
                disabledContainerColor = NeonEmerald.copy(alpha = 0.22f),
                disabledContentColor = NeonEmerald
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buttonLabel,
                fontFamily = NotoSansBengaliFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
