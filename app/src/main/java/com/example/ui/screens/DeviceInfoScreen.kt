package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRemoteManager
import com.example.data.LicenseUiState
import com.example.ui.components.BlueVerifiedBadge
import com.example.ui.theme.FlagshipCyanMagentaBrush
import com.example.ui.theme.FlagshipEmeraldCyanBrush
import com.example.ui.theme.FlagshipGoldAmberBrush
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardInner
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.luminousGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceInfoScreen(
    licenseState: LicenseUiState,
    isFirebaseConnected: Boolean,
    lastFirebaseSyncMs: Long,
    appVersionLabel: String,
    onOpenUnlockScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copiedDeviceId by remember { mutableStateOf(false) }

    val manufacturer = remember { FirebaseRemoteManager.normalizeManufacturerName(Build.MANUFACTURER) }
    val model = remember { Build.MODEL.orEmpty().trim().ifBlank { "UNKNOWN_MODEL" } }
    val cleanDeviceName = remember { FirebaseRemoteManager.buildCleanDeviceName(Build.MANUFACTURER, model) }
    val androidOsLabel = remember { "Android ${Build.VERSION.RELEASE ?: "Unknown"} (API ${Build.VERSION.SDK_INT})" }
    val appVersionCode = remember {
        runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        }.getOrDefault(1L)
    }

    val vipActiveOrVerified = !licenseState.isDeviceBlocked && !licenseState.isFreeStandardMode
    val remainingLabel = when {
        licenseState.isDeviceBlocked -> "Access Blocked by Admin"
        licenseState.isGlobalAllFree -> "Unlimited (Global All Free Active)"
        licenseState.isAdminGrantedVip || licenseState.deviceOverrides.hasFullAdminFreeAccess ->
            "Unlimited (Admin VIP Active)"
        licenseState.isLifetimeUnlocked -> "Lifetime Permanent VIP"
        licenseState.isMonthlyActive || licenseState.isWeeklyActive ->
            "${licenseState.formattedCompactEnglish} remaining"
        !licenseState.isTrialExpired ->
            "${licenseState.formattedCompactEnglish} (${licenseState.effectiveTrialDays}-Day Free Trial)"
        else -> "0d 0h 0m · Free Classic Black Mode"
    }

    val lastSyncFormatted = remember(lastFirebaseSyncMs) {
        if (lastFirebaseSyncMs <= 0L) {
            "Auto-syncing on connection..."
        } else {
            runCatching {
                val fmt = SimpleDateFormat("hh:mm:ss a", Locale.US)
                fmt.format(Date(lastFirebaseSyncMs))
            }.getOrDefault("Just now")
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .luminousGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        borderBrush = if (isFirebaseConnected) FlagshipEmeraldCyanBrush else FlagshipGoldAmberBrush,
                        borderWidth = 1.4.dp,
                        glowColor = if (isFirebaseConnected) NeonEmerald else NeonAmber,
                        glowElevation = 14.dp,
                        surfaceColor = GlassCardSurface
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Device Info",
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (vipActiveOrVerified) {
                            BlueVerifiedBadge(
                                size = 18.dp,
                                contentDescription = "VIP Verified"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isFirebaseConnected) NeonEmerald.copy(alpha = 0.2f)
                                else NeonAmber.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isFirebaseConnected) NeonEmerald else NeonAmber,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("device_info_cloud_status_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseConnected) NeonEmerald else NeonAmber)
                        )
                        Text(
                            text = if (isFirebaseConnected) "Connected & Auto-Syncing" else "Offline",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirebaseConnected) NeonEmerald else NeonAmber,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "Hardware identity, VIP trial tracking & live cloud auto-sync status",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // 1. Hardware Device ID Card
            DeviceInfoSectionCard(
                icon = Icons.Filled.PermDeviceInformation,
                title = "Device ID (RK-ID)",
                borderBrush = FlagshipCyanMagentaBrush
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardInner)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Persistent Hardware RK-ID",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = licenseState.deviceId,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp,
                                color = NeonCyan,
                                modifier = Modifier.testTag("device_info_rk_id")
                            )
                            if (vipActiveOrVerified) {
                                BlueVerifiedBadge(size = 16.dp, contentDescription = "Verified Device")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (copiedDeviceId) NeonEmerald.copy(alpha = 0.24f) else GlassCardSurface)
                            .border(
                                1.dp,
                                if (copiedDeviceId) NeonEmerald else Color.White.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("AI Keyboard RK-ID", licenseState.deviceId)
                                )
                                copiedDeviceId = true
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("device_info_copy_id_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedDeviceId) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copy Device ID",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (copiedDeviceId) "Copied" else "Copy",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Phone Manufacturer, Model, OS & App Version Card
            DeviceInfoSectionCard(
                icon = Icons.Filled.PhoneAndroid,
                title = "Hardware & System",
                borderBrush = FlagshipGoldAmberBrush
            ) {
                DeviceInfoKeyValueRow(label = "Device Name", value = cleanDeviceName)
                DeviceInfoKeyValueRow(label = "Manufacturer", value = manufacturer)
                DeviceInfoKeyValueRow(label = "Phone Model", value = model)
                DeviceInfoKeyValueRow(label = "Android OS Version", value = androidOsLabel)
                DeviceInfoKeyValueRow(
                    label = "App Version",
                    value = "v$appVersionLabel (Build $appVersionCode)"
                )
            }

            // 3. VIP / Trial Status & Exact Remaining Days/Time
            DeviceInfoSectionCard(
                icon = Icons.Filled.WorkspacePremium,
                title = "VIP & Trial Status",
                borderBrush = if (vipActiveOrVerified) FlagshipEmeraldCyanBrush else FlagshipCyanMagentaBrush
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (vipActiveOrVerified) Color(0x550E4F38) else Color(0x552E1024)
                        )
                        .border(
                            1.dp,
                            if (vipActiveOrVerified) NeonEmerald else NeonMagenta,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (vipActiveOrVerified) {
                        BlueVerifiedBadge(size = 18.dp, contentDescription = "VIP Verified")
                    }
                    Text(
                        text = licenseState.formattedVipPillText,
                        fontFamily = FontFamily.Default,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("device_info_vip_badge")
                    )
                }

                DeviceInfoKeyValueRow(label = "Remaining Time", value = remainingLabel)
                DeviceInfoKeyValueRow(
                    label = "Trial Countdown",
                    value = if (licenseState.isTrueLifetimeDisplay) {
                        "Permanent Lifetime"
                    } else {
                        "${licenseState.formattedCompactEnglish} • ${licenseState.formattedRemainingBangla}"
                    }
                )

                if (!licenseState.isAllUnlocked) {
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
                            .testTag("device_info_btn_unlock_vip")
                    ) {
                        Text(
                            text = "👑 Unlock Lifetime VIP (₹${licenseState.adminConfig.vipPrice})",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 4. Live Cloud Auto-Sync Card (No Manual Send Button)
            DeviceInfoSectionCard(
                icon = if (isFirebaseConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                title = "Cloud Auto-Sync Status",
                borderBrush = if (isFirebaseConnected) FlagshipEmeraldCyanBrush else FlagshipGoldAmberBrush
            ) {
                DeviceInfoKeyValueRow(
                    label = "Connection Status",
                    value = if (isFirebaseConnected) "Connected & Auto-Syncing" else "Offline",
                    valueColor = if (isFirebaseConnected) NeonEmerald else NeonAmber
                )
                DeviceInfoKeyValueRow(
                    label = "Sync Channel",
                    value = "Real-time Encrypted Auto-Sync"
                )
                DeviceInfoKeyValueRow(
                    label = "Last Auto-Sync",
                    value = lastSyncFormatted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCardInner)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Device details, model info, timestamps & VIP/trial status sync automatically on launch, keyboard start, resume, and periodic heartbeat.",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DeviceInfoSectionCard(
    icon: ImageVector,
    title: String,
    borderBrush: Brush,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .luminousGlassCard(
                shape = RoundedCornerShape(20.dp),
                borderBrush = borderBrush,
                borderWidth = 1.25.dp,
                glowColor = NeonCyan,
                glowElevation = 10.dp,
                surfaceColor = GlassCardSurface
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontFamily = PlusJakartaSansFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        content()
    }
}

@Composable
private fun DeviceInfoKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardInner)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
