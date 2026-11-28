package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LicenseUiState

/**
 * Manages polite, low-frequency VIP notifications:
 * 1. **VIP / Trial Expiry Alert (1–2 days before expiry):**
 *    Bilingual Bengali + English reminder sent at most once per expiry window.
 * 2. **Gentle Non-VIP Everyday Offer Notice (every 4–7 days, default 5):**
 *    Uses admin-configured `/admin_config/offerTitle`, `offerMessageBn`, `offerMessageEn`
 *    when available, otherwise shows a gentle bilingual VIP unlock offer.
 *
 * Strictly checks `POST_NOTIFICATIONS` permission cleanly without prompting inside background/IME flows.
 */
object VipNotificationHelper {

    private const val PREFS_NAME = "rakib_vip_notifications_prefs"
    private const val KEY_EXPIRY_ALERT_CYCLE_ID = "expiry_alert_cycle_id"
    private const val KEY_LAST_OFFER_NOTIFY_MS = "last_offer_notify_ms"
    private const val KEY_FIRST_OBSERVED_MS = "first_observed_ms"

    const val CHANNEL_EXPIRY_ID = "vip_expiry_channel"
    const val CHANNEL_OFFERS_ID = "vip_offers_channel"
    private const val NOTIFY_ID_EXPIRY = 7101
    private const val NOTIFY_ID_OFFER = 7102

    private const val TWO_DAYS_SECONDS = 2L * 86_400L
    private const val DAY_MS = 86_400_000L

    fun evaluateAndNotify(
        context: Context,
        state: LicenseUiState,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!hasNotificationPermission(context)) return
        if (state.isDeviceBlocked) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val firstObserved = prefs.getLong(KEY_FIRST_OBSERVED_MS, 0L).let { existing ->
            if (existing > 0L) existing else nowMs.also {
                prefs.edit().putLong(KEY_FIRST_OBSERVED_MS, it).apply()
            }
        }

        // 1. Bilingual expiry reminder 1-2 days before trial or timed VIP ends
        if (shouldSendExpiryAlert(state, prefs.getString(KEY_EXPIRY_ALERT_CYCLE_ID, null))) {
            val cycleId = buildExpiryCycleKey(state)
            prefs.edit().putString(KEY_EXPIRY_ALERT_CYCLE_ID, cycleId).apply()
            showExpiryNotification(context, state)
            return
        }

        // 2. Gentle periodic offer notification for non-VIP expired users every 4..7 days (never daily)
        val intervalDays = state.adminConfig.offerIntervalDays.coerceIn(4, 7)
        val lastOfferMs = prefs.getLong(KEY_LAST_OFFER_NOTIFY_MS, 0L)
        if (shouldSendOfferNotification(state, nowMs, lastOfferMs, firstObserved, intervalDays)) {
            prefs.edit().putLong(KEY_LAST_OFFER_NOTIFY_MS, nowMs).apply()
            showOfferNotification(context, state)
        }
    }

    internal fun shouldSendExpiryAlert(state: LicenseUiState, alreadySentCycleId: String?): Boolean {
        if (state.isDeviceBlocked) return false
        if (state.isGlobalOrAdminAllFree || state.isLifetimeUnlocked) return false
        val isExpiringTrial = !state.isPaidOrAdminVip && !state.isTrialExpired
        val isExpiringTimedVip = state.isMonthlyActive || state.isWeeklyActive
        if (!isExpiringTrial && !isExpiringTimedVip) return false
        if (state.remainingSeconds !in 1L..TWO_DAYS_SECONDS) return false
        val cycleId = buildExpiryCycleKey(state)
        return cycleId != alreadySentCycleId
    }

    internal fun shouldSendOfferNotification(
        state: LicenseUiState,
        nowMs: Long,
        lastOfferMs: Long,
        firstObservedMs: Long,
        intervalDays: Int
    ): Boolean {
        if (state.isDeviceBlocked || !state.isFreeStandardMode) return false
        val cooldownMs = intervalDays.coerceIn(4, 7).toLong() * DAY_MS
        if ((nowMs - firstObservedMs) < DAY_MS) return false
        return lastOfferMs <= 0L || (nowMs - lastOfferMs) >= cooldownMs
    }

    private fun buildExpiryCycleKey(state: LicenseUiState): String {
        val bucket = state.vipExpiryTimestampMs / (12L * 3_600_000L)
        return "${state.vipStatusCode}_$bucket"
    }

    private fun showExpiryNotification(context: Context, state: LicenseUiState) {
        ensureChannels(context)
        val daysLeft = state.remainingDays
        val hoursLeft = state.remainingHours
        val title = if (state.isPaidOrAdminVip) {
            "⏰ VIP প্ল্যান শীঘ্রই শেষ হচ্ছে • VIP Expiring Soon"
        } else {
            "⏳ ফ্রি VIP ট্রায়াল শেষ হতে চলেছে • Trial Ending Soon"
        }
        val body = "আর মাত্র ${daysLeft}d ${hoursLeft}h বাকি! VIP, Touch FX ও Banglish চালু রাখতে এখনই আনলক করুন। " +
            "Only ${daysLeft}d ${hoursLeft}h left — unlock Lifetime VIP for ₹${state.adminConfig.vipPrice}."

        postNotification(
            context = context,
            channelId = CHANNEL_EXPIRY_ID,
            notificationId = NOTIFY_ID_EXPIRY,
            title = title,
            body = body
        )
    }

    private fun showOfferNotification(context: Context, state: LicenseUiState) {
        ensureChannels(context)
        val cfg = state.adminConfig
        val title = cfg.offerTitle.takeIf { it.isNotBlank() }
            ?: "✨ Special VIP Offer • AI Keyboard"
        val customBn = cfg.offerMessageBn.trim()
        val customEn = cfg.offerMessageEn.trim()
        val body = when {
            customBn.isNotEmpty() && customEn.isNotEmpty() -> "$customBn\n$customEn"
            customBn.isNotEmpty() -> customBn
            customEn.isNotEmpty() -> customEn
            else -> "সব VIP থিম, 106+ Touch FX ও Banglish আনলক করুন মাত্র ₹${cfg.vipPrice}-এ! " +
                "Unlock all VIP themes & Touch FX from just ₹${cfg.vipPrice}."
        }

        postNotification(
            context = context,
            channelId = CHANNEL_OFFERS_ID,
            notificationId = NOTIFY_ID_OFFER,
            title = title,
            body = body
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_UNLOCK_SCREEN, true)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(context, notificationId, openIntent, pendingFlags)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_EXPIRY_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_EXPIRY_ID,
                    "VIP & Trial Expiry Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies 1-2 days before your free trial or VIP pass expires"
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_OFFERS_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OFFERS_ID,
                    "VIP Special Offers",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Periodic special offers for AI Keyboard VIP features"
                }
            )
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
