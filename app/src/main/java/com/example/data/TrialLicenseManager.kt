package com.example.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import androidx.compose.runtime.Immutable
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class LicenseUiState(
    val isLifetimeUnlocked: Boolean = false,
    val isAdminGrantedVip: Boolean = false,
    val isGlobalAllFree: Boolean = false,
    val isMonthlyActive: Boolean = false,
    val isWeeklyActive: Boolean = false,
    val vipExpiryTimestampMs: Long = 0L,
    val isBanglishLifetimeUnlocked: Boolean = false,
    val banglishExpiryTimestampMs: Long = 0L,
    val banglishRemainingSeconds: Long = 0L,
    val banglishTotalDurationSeconds: Long = TrialLicenseManager.TRIAL_DURATION_SECONDS,
    val vipStatusCode: String = TrialLicenseManager.STATUS_TRIAL,
    val isTrialExpired: Boolean = false,
    val remainingSeconds: Long = TrialLicenseManager.TRIAL_DURATION_SECONDS,
    val deviceId: String = "RK-00000000",
    val unlockMethod: String? = null,
    val unlockedTxnId: String? = null,
    val adminConfig: RemoteAdminConfig = RemoteAdminConfig(),
    val deviceOverrides: DeviceRemoteOverrides = DeviceRemoteOverrides(),
    val pendingPayment: PendingPaymentSnapshot? = null
) {
    /** True when admin unlocked everything globally (`globalAllFree`) or for this friend (`adminAllFree` / `adminFreeVip`). */
    val isGlobalOrAdminAllFree: Boolean
        get() = isGlobalAllFree || isAdminGrantedVip || deviceOverrides.hasFullAdminFreeAccess

    /** True when user purchased Card 1 (RGB & VIP Features) as Lifetime or active timed pass. */
    val isVipFeaturesPaidActive: Boolean
        get() = isLifetimeUnlocked || isMonthlyActive || isWeeklyActive

    /** True when user purchased Card 2 (Romanized Bengali) as Lifetime or active timed pass. */
    val isBanglishPaidActive: Boolean
        get() = isBanglishLifetimeUnlocked || banglishRemainingSeconds > 0L

    /** True when both Card 1 and Card 2 are permanently unlocked (or global/admin all-free is active). */
    val isAllUnlocked: Boolean
        get() = isGlobalOrAdminAllFree || (isLifetimeUnlocked && isBanglishLifetimeUnlocked)

    /** True when user holds Paid Card 1 VIP or Global/Admin All-Free. */
    val isPaidOrAdminVip: Boolean
        get() = isGlobalOrAdminAllFree || isVipFeaturesPaidActive

    /** True during the active trial window OR with Paid VIP / Global or Admin All-Free. */
    val isVipActive: Boolean
        get() = isPaidOrAdminVip || !isTrialExpired

    /** True after trial ends without active Card 1 VIP: keyboard defaults to Normal / Classic Black (No RGB). */
    val isFreeStandardMode: Boolean
        get() = !isPaidOrAdminVip && isTrialExpired

    private fun resolveVipFeatureAccess(deviceOverride: Boolean?, globalFree: Boolean): Boolean {
        if (deviceOverride != null) return deviceOverride
        if (isGlobalOrAdminAllFree) return true
        return isVipFeaturesPaidActive || !isTrialExpired || globalFree
    }

    /** Per-feature effective unlock checks honoring per-device overrides -> globalAllFree/adminAllFree -> Card 1 VIP / Trial -> globalFreeFeatures. */
    val isRgbThemesUnlocked: Boolean
        get() = resolveVipFeatureAccess(deviceOverrides.allowedFeatures.rgbThemes, adminConfig.globalFreeFeatures.rgbThemes)

    /**
     * Checks whether a specific theme combo, ambient animation, touch effect, banner, palette, or keycap
     * item is unlocked via permanent free flag, active VIP/trial/override, or `/admin_config/freeThemeIds`.
     */
    fun isThemeItemUnlocked(themeId: String, isPermanentlyFree: Boolean = false): Boolean {
        if (isPermanentlyFree) return true
        if (isRgbThemesUnlocked) return true
        return themeId.isNotBlank() && adminConfig.freeThemeIds.any { it.equals(themeId, ignoreCase = true) }
    }

    /** Independent Card 2 unlock check for Romanized Bengali (BN-EN): Card 1 VIP purchase excludes Banglish. */
    val isBanglishUnlocked: Boolean
        get() {
            val deviceOverride = deviceOverrides.allowedFeatures.banglish
            if (deviceOverride != null) return deviceOverride
            if (isGlobalOrAdminAllFree) return true
            return isBanglishPaidActive || !isTrialExpired || adminConfig.globalFreeFeatures.banglish
        }

    val isCallBarUnlocked: Boolean
        get() = resolveVipFeatureAccess(deviceOverrides.allowedFeatures.callBar, adminConfig.globalFreeFeatures.callBar)

    val isRgbStickerUnlocked: Boolean
        get() = resolveVipFeatureAccess(deviceOverrides.allowedFeatures.rgbSticker, adminConfig.globalFreeFeatures.rgbSticker)

    val isStylishFontsUnlocked: Boolean
        get() = resolveVipFeatureAccess(deviceOverrides.allowedFeatures.stylishFonts, adminConfig.globalFreeFeatures.stylishFonts)

    val isAiHubUnlocked: Boolean
        get() = resolveVipFeatureAccess(deviceOverrides.allowedFeatures.aiHub, adminConfig.globalFreeFeatures.aiHub)

    /** Effective remaining seconds for the Banglish live countdown timer bar (Paid Banglish or Free Trial). */
    val effectiveBanglishRemainingSeconds: Long
        get() = when {
            isGlobalOrAdminAllFree || isBanglishLifetimeUnlocked -> 0L
            banglishRemainingSeconds > 0L -> banglishRemainingSeconds
            !isTrialExpired -> remainingSeconds
            else -> 0L
        }

    /** Normalized 0f..1f progress fraction for the Banglish countdown progress bar. */
    val banglishCountdownProgress: Float
        get() = when {
            isGlobalOrAdminAllFree || isBanglishLifetimeUnlocked || adminConfig.globalFreeFeatures.banglish -> 1f
            banglishRemainingSeconds > 0L -> {
                val total = banglishTotalDurationSeconds.coerceAtLeast(banglishRemainingSeconds).coerceAtLeast(1L)
                (banglishRemainingSeconds.toFloat() / total.toFloat()).coerceIn(0.04f, 1f)
            }
            !isTrialExpired -> {
                val totalTrialSec = (adminConfig.trialDurationMs / 1000L).coerceAtLeast(1L)
                (remainingSeconds.toFloat() / totalTrialSec.toFloat()).coerceIn(0.04f, 1f)
            }
            else -> 0f
        }

    val formattedBanglishCountdownEn: String
        get() = when {
            isGlobalOrAdminAllFree -> "ALL FREE VIP"
            isBanglishLifetimeUnlocked -> "LIFETIME ACTIVE"
            deviceOverrides.allowedFeatures.banglish == true -> "FRIEND VIP ACTIVE"
            adminConfig.globalFreeFeatures.banglish -> "FREE EVENT ACTIVE"
            effectiveBanglishRemainingSeconds > 0L -> formatSecondsCompactEn(effectiveBanglishRemainingSeconds)
            else -> "0d 0h 0m · EXPIRED"
        }

    val formattedBanglishCountdownBn: String
        get() = when {
            isGlobalOrAdminAllFree -> "সবার জন্য ফ্রি VIP অ্যাক্টিভ"
            isBanglishLifetimeUnlocked -> "লাইফটাইম অ্যাক্টিভ"
            deviceOverrides.allowedFeatures.banglish == true -> "অ্যাডমিন ফ্রি অ্যাক্টিভ"
            adminConfig.globalFreeFeatures.banglish -> "গ্লোবাল ফ্রি অ্যাক্টিভ"
            effectiveBanglishRemainingSeconds > 0L -> formatSecondsCompactBn(effectiveBanglishRemainingSeconds)
            else -> "০ দিন ০ ঘণ্টা ০ মি. • মেয়াদ শেষ"
        }

    /** Days, Hours, Minutes strictly — NO seconds anywhere. */
    val remainingDays: Long
        get() = (remainingSeconds.coerceAtLeast(0L)) / 86_400L

    val remainingHours: Long
        get() = (remainingSeconds.coerceAtLeast(0L) % 86_400L) / 3_600L

    val remainingMinutes: Long
        get() {
            val clamped = remainingSeconds.coerceAtLeast(0L)
            if (clamped <= 0L) return 0L
            val rawMins = (clamped % 3_600L) / 60L
            return if (remainingDays == 0L && remainingHours == 0L && rawMins == 0L) 1L else rawMins
        }

    /** Exact Days, Hours & Minutes format ONLY (NO seconds): e.g., "6d 23h 59m", "0d 0h 0m" at expiry. */
    val formattedCompactEnglish: String
        get() = if (isTrialExpired && !isPaidOrAdminVip) {
            "0d 0h 0m"
        } else {
            "${remainingDays}d ${remainingHours}h ${remainingMinutes}m"
        }

    /** Flagship top-bar pill text reflecting Global Free, Admin VIP, Lifetime VIP, Timed VIP, Trial, or Expired. */
    val formattedVipPillText: String
        get() = when {
            isGlobalAllFree -> "👑 ALL FREE VIP"
            isAdminGrantedVip || deviceOverrides.hasFullAdminFreeAccess -> "👑 ADMIN VIP"
            isLifetimeUnlocked -> "👑 LIFETIME VIP"
            isMonthlyActive || isWeeklyActive -> "👑 VIP • $formattedCompactEnglish"
            isTrialExpired -> "🔒 0d 0h 0m · VIP"
            else -> "👑 VIP • $formattedCompactEnglish"
        }

    /** Bengali countdown strictly in Days, Hours & Minutes ONLY (NO seconds). */
    val formattedRemainingBangla: String
        get() {
            if (isTrialExpired && !isPaidOrAdminVip) {
                return "০ দিন ০ ঘণ্টা ০ মি."
            }
            return "${toBengaliDigits(remainingDays)} দিন ${toBengaliDigits(remainingHours)} ঘণ্টা ${toBengaliDigits(remainingMinutes)} মি."
        }

    /** Compact Bengali pill label in Days, Hours & Minutes ONLY (NO seconds). */
    val formattedCompactPillBangla: String
        get() = formattedRemainingBangla

    companion object {
        private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

        fun toBengaliDigits(value: Long): String {
            return value.toString().map { ch ->
                if (ch in '0'..'9') BENGALI_DIGITS[ch - '0'] else ch
            }.joinToString("")
        }

        fun formatSecondsCompactEn(totalSeconds: Long): String {
            val clamped = totalSeconds.coerceAtLeast(0L)
            val d = clamped / 86_400L
            val h = (clamped % 86_400L) / 3_600L
            val rawM = (clamped % 3_600L) / 60L
            val m = if (clamped > 0L && d == 0L && h == 0L && rawM == 0L) 1L else rawM
            return "${d}d ${h}h ${m}m"
        }

        fun formatSecondsCompactBn(totalSeconds: Long): String {
            val clamped = totalSeconds.coerceAtLeast(0L)
            val d = clamped / 86_400L
            val h = (clamped % 86_400L) / 3_600L
            val rawM = (clamped % 3_600L) / 60L
            val m = if (clamped > 0L && d == 0L && h == 0L && rawM == 0L) 1L else rawM
            return "${toBengaliDigits(d)} দিন ${toBengaliDigits(h)} ঘণ্টা ${toBengaliDigits(m)} মি."
        }
    }
}

sealed interface UpiVerificationResult {
    data class Success(val txnId: String) : UpiVerificationResult
    data object Cancelled : UpiVerificationResult
    data object PendingOrSubmitted : UpiVerificationResult
    data class Failed(val reason: String) : UpiVerificationResult
}

/**
 * Manages:
 * - True Per-Device Hardware ID (`RK-XXXXXXXX`, 8 hex chars derived strictly from SHA-256 of
 *   `Settings.Secure.ANDROID_ID` + `Build.MODEL` — zero random/demo IDs)
 * - Anti-Tamper 7-Day (168h / 604,800s) Full VIP Trial persisted across BOTH `SharedPreferences`
 *   AND encrypted `Documents/.ai_keyboard_trial.dat` (with safe fallback to `context.filesDir`)
 *   so uninstalling or changing the phone date CANNOT reset the 7-day trial
 * - Official Merchant UPI ₹20 Lifetime VIP activation (`6296603748@okbizaxis`, `QUALITY FOODS`)
 * - AES-256 encrypted, device-bound persistent license in `Documents/.ai_keyboard_vip.lic`
 *   for silent automatic reinstall restore on the same device.
 *
 * Threading guarantee: constructor, `prefListener`, and `refreshState()` NEVER perform disk or
 * MediaStore I/O on the calling (main) thread and NEVER trigger recursive preference callbacks.
 * All file/MediaStore I/O runs exclusively on `Dispatchers.IO` inside safe `try-catch` blocks.
 */
class TrialLicenseManager private constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val rawAndroidId: String = readHardwareAndroidId(context)
    private val deviceModel: String = Build.MODEL.orEmpty().trim().ifBlank { "UNKNOWN_MODEL" }

    /** Permanent hardware-derived 8-char hex Device ID formatted like `RK-8A4F92C1` strictly from ANDROID_ID. */
    val deviceId: String = deriveHardwareDeviceId(rawAndroidId)

    /** Kept strictly for silent backward-compatible migration of previously purchased license files. */
    private val legacyModelDeviceId: String = deriveLegacyModelDeviceId(rawAndroidId, deviceModel)
    private val legacySixCharDeviceId: String = deriveLegacySixCharDeviceId(rawAndroidId)

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isInternalPrefWrite = AtomicBoolean(false)
    private val isStorageSyncRunning = AtomicBoolean(false)

    @Volatile
    private var lastTrialPersistMs: Long = 0L

    @Volatile
    private var lastPrefCheckpointMs: Long = 0L

    @Volatile
    private var lastFirebaseSyncMs: Long = 0L

    @Volatile
    private var lastSyncedVipStatus: String = ""

    @Volatile
    private var latestPendingPayment: PendingPaymentSnapshot? = null

    @Volatile
    private var cachedCustomBanglishPlans: List<BanglishPlanOption> = emptyList()

    private val firebaseRemoteManager = FirebaseRemoteManager.getInstance(context)

    private val _licenseState = MutableStateFlow(computeFastStateFromPrefsOnly())
    val licenseState: StateFlow<LicenseUiState> = _licenseState.asStateFlow()

    /**
     * Listens ONLY for external / cross-process license state changes (e.g. MainActivity <-> IME).
     * Never writes to SharedPreferences and never touches MediaStore/disk, eliminating any loop risk.
     */
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (isInternalPrefWrite.get()) return@OnSharedPreferenceChangeListener
        if (key == KEY_IS_LIFETIME_UNLOCKED ||
            key == KEY_IS_BANGLISH_LIFETIME_UNLOCKED ||
            key == KEY_BANGLISH_EXPIRY_TIMESTAMP_MS ||
            key == KEY_BANGLISH_TOTAL_DURATION_SEC ||
            key == KEY_ADMIN_GRANTED_VIP ||
            key == KEY_ADMIN_ALL_FREE_DEVICE ||
            key == KEY_ADMIN_GLOBAL_ALL_FREE ||
            key == KEY_DEVICE_CUSTOM_EXPIRY_MS ||
            key == KEY_VIP_EXPIRY_TIMESTAMP_MS ||
            key == KEY_ACTIVE_SUB_PLAN ||
            key == KEY_TRIAL_EXPIRED_STICKY ||
            key == KEY_UNLOCK_TXN_ID ||
            key == KEY_UNLOCK_METHOD ||
            key == KEY_FIRST_INSTALL_TIMESTAMP_MS ||
            key == KEY_ADMIN_FREE_TRIAL_DAYS ||
            key == KEY_ADMIN_VIP_PRICE ||
            key == KEY_ADMIN_VIP_DURATION_DAYS ||
            key == KEY_ADMIN_PLAN_MODE ||
            key == KEY_ADMIN_BANGLISH_PRICE ||
            key == KEY_ADMIN_BANGLISH_DURATION_DAYS ||
            key == KEY_ADMIN_BANGLISH_PLAN_MODE ||
            key == KEY_ADMIN_BANGLISH_DAILY_PRICE ||
            key == KEY_ADMIN_BANGLISH_WEEKLY_PRICE ||
            key == KEY_ADMIN_BANGLISH_MONTHLY_PRICE ||
            key?.startsWith("admin_free_") == true ||
            key?.startsWith("admin_banner") == true ||
            key?.startsWith("device_allow_") == true
        ) {
            _licenseState.value = computeFastStateFromPrefsOnly()
        }
    }

    init {
        ensureLocalTrialSeededFast()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        val initial = computeFastStateFromPrefsOnly()
        _licenseState.value = initial
        firebaseRemoteManager.start(
            deviceId = deviceId,
            onAdminConfigChanged = { remoteCfg -> applyRemoteAdminConfig(remoteCfg) },
            onDeviceSnapshotChanged = { snapshot -> applyRemoteDeviceSnapshot(snapshot) },
            onPendingPaymentChanged = { pending -> handlePendingPaymentSnapshot(pending) }
        )
        syncDeviceToFirebase(state = initial, force = true)
        syncStorageInBackground(forcePersistToDocuments = true)
    }

    /**
     * Fast, main-thread-safe refresh: updates countdown immediately from SharedPreferences + clock
     * without blocking on disk/MediaStore, and triggers an asynchronous background storage & Firebase sync.
     */
    fun refreshState(): LicenseUiState {
        val state = computeFastStateFromPrefsOnly()
        val nowWall = currentServerOrUtcMs()
        if (!state.isPaidOrAdminVip && (state.isTrialExpired || nowWall - lastPrefCheckpointMs >= PREF_CHECKPOINT_INTERVAL_MS)) {
            checkpointLocalTrialFast(nowWall)
        }
        _licenseState.value = state
        syncDeviceToFirebase(state = state, force = false)
        syncStorageInBackground(forcePersistToDocuments = false)
        return state
    }

    private fun currentServerOrUtcMs(): Long = firebaseRemoteManager.serverNowMs()

    /**
     * Reconciles `Documents/.ai_keyboard_trial.dat` & auto-restores `Documents/.ai_keyboard_vip.lic`
     * safely on `Dispatchers.IO` so the main UI thread never hangs or crashes on startup.
     */
    fun syncStorageInBackground(forcePersistToDocuments: Boolean = false) {
        if (!isStorageSyncRunning.compareAndSet(false, true)) return
        ioScope.launch {
            try {
                reconcileAndPersistTrialStateOnIo(forcePersistToDocuments = forcePersistToDocuments)
                tryAutoRestoreFromLicenseFileOnIo()
                val updated = computeFastStateFromPrefsOnly()
                _licenseState.value = updated
                syncDeviceToFirebase(state = updated, force = false)
            } catch (_: Throwable) {
                // Never crash app startup or background sync due to OEM storage restrictions
            } finally {
                isStorageSyncRunning.set(false)
            }
        }
    }

    private fun applyRemoteAdminConfig(config: RemoteAdminConfig) {
        cachedCustomBanglishPlans = config.banglishCustomPlans
        editPrefsSafely {
            putInt(KEY_ADMIN_FREE_TRIAL_DAYS, config.freeTrialDays)
            putBoolean(KEY_ADMIN_GLOBAL_ALL_FREE, config.globalAllFree)
            putInt(KEY_ADMIN_VIP_PRICE, config.vipPrice)
            putInt(KEY_ADMIN_VIP_DURATION_DAYS, config.vipDurationDays)
            putString(KEY_ADMIN_PLAN_MODE, config.vipPlanType)
            putInt(KEY_ADMIN_BANGLISH_PRICE, config.banglishPrice)
            putInt(KEY_ADMIN_BANGLISH_DURATION_DAYS, config.banglishDurationDays)
            putString(KEY_ADMIN_BANGLISH_PLAN_MODE, config.banglishPlanType)
            putInt(KEY_ADMIN_BANGLISH_DAILY_PRICE, config.banglishDailyPrice)
            putInt(KEY_ADMIN_BANGLISH_WEEKLY_PRICE, config.banglishWeeklyPrice)
            putInt(KEY_ADMIN_BANGLISH_MONTHLY_PRICE, config.banglishMonthlyPrice)
            putBoolean(KEY_ADMIN_FREE_RGB_THEMES, config.globalFreeFeatures.rgbThemes)
            putBoolean(KEY_ADMIN_FREE_BANGLISH, config.globalFreeFeatures.banglish)
            putBoolean(KEY_ADMIN_FREE_CALL_BAR, config.globalFreeFeatures.callBar)
            putBoolean(KEY_ADMIN_FREE_RGB_STICKER, config.globalFreeFeatures.rgbSticker)
            putBoolean(KEY_ADMIN_FREE_STYLISH_FONTS, config.globalFreeFeatures.stylishFonts)
            putBoolean(KEY_ADMIN_FREE_AI_HUB, config.globalFreeFeatures.aiHub)
            putString(KEY_ADMIN_BANNER1_URL, config.banners.banner1Url)
            putString(KEY_ADMIN_BANNER1_IMAGE, config.banners.banner1Image)
            putString(KEY_ADMIN_BANNER1_TITLE, config.banners.banner1Title)
            putString(KEY_ADMIN_BANNER1_SUBTITLE, config.banners.banner1Subtitle)
            putString(KEY_ADMIN_BANNER2_URL, config.banners.banner2Url)
            putString(KEY_ADMIN_BANNER2_IMAGE, config.banners.banner2Image)
            putString(KEY_ADMIN_BANNER2_TITLE, config.banners.banner2Title)
            putString(KEY_ADMIN_BANNER2_SUBTITLE, config.banners.banner2Subtitle)
        }
        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        if (updated.vipStatusCode != lastSyncedVipStatus) {
            syncDeviceToFirebase(state = updated, force = true)
        }
    }

    private fun handlePendingPaymentSnapshot(pending: PendingPaymentSnapshot?) {
        latestPendingPayment = pending
        if (pending != null && pending.isApproved) {
            val approvalKey = pending.txnRef.ifBlank {
                "${pending.planType}_${pending.uniqueAmount}_${pending.createdAtMs}"
            }
            val alreadyConsumed = prefs.getString(KEY_LAST_APPROVED_PENDING_TXN, null)
            if (!approvalKey.equals(alreadyConsumed, ignoreCase = true)) {
                editPrefsSafely {
                    putString(KEY_LAST_APPROVED_PENDING_TXN, approvalKey)
                }
                val scope = pending.planType.ifBlank { PLAN_SCOPE_VIP_FEATURES }
                unlockPlanByScope(
                    planScope = scope,
                    durationDays = pending.durationDays,
                    method = "AUTO_UPI_APPROVED",
                    txnId = approvalKey
                )
                return
            }
        }
        _licenseState.value = computeFastStateFromPrefsOnly()
    }

    private fun applyRemoteDeviceSnapshot(snapshot: RemoteDeviceSnapshot) {
        val nowServer = currentServerOrUtcMs()
        val serverFirstInstall = snapshot.firstInstallTimestampMs?.takeIf { it > 0L }
        val statusLower = snapshot.vipStatus?.lowercase(Locale.US).orEmpty()
        val isAdminFree = snapshot.overrides.hasFullAdminFreeAccess || statusLower == STATUS_ADMIN_FREE_VIP
        val customExpiryMs = snapshot.overrides.customExpiryTimestampMs
        val isServerLifetime = statusLower == STATUS_LIFETIME_VIP
        val isServerTimedActive = (statusLower == STATUS_WEEKLY_ACTIVE ||
            statusLower == STATUS_MONTHLY_ACTIVE ||
            statusLower == STATUS_CUSTOM_ACTIVE) && snapshot.vipExpiryTimestampMs > nowServer
        val allowed = snapshot.overrides.allowedFeatures

        editPrefsSafely {
            // Strict One-Time Trial: server firstInstallTimestamp is authoritative and NEVER reset or extended
            if (serverFirstInstall != null) {
                val localFirst = prefs.getLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, serverFirstInstall)
                val authoritative = minOf(localFirst, serverFirstInstall)
                putLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, authoritative)
                putLong(KEY_FIRST_LAUNCH_WALL_MS, authoritative)
                val serverElapsed = (nowServer - authoritative).coerceAtLeast(0L)
                val prevMax = prefs.getLong(KEY_MAX_SEEN_ELAPSED_MS, 0L)
                putLong(KEY_MAX_SEEN_ELAPSED_MS, maxOf(prevMax, serverElapsed))
            }

            // Instant Remote Admin VIP Override (`adminAllFree` / `adminFreeVip`) & per-device `customExpiryTimestamp`
            putBoolean(KEY_ADMIN_GRANTED_VIP, isAdminFree)
            putBoolean(KEY_ADMIN_ALL_FREE_DEVICE, snapshot.overrides.adminAllFree)
            putLong(KEY_DEVICE_CUSTOM_EXPIRY_MS, customExpiryMs.coerceAtLeast(0L))
            if (isAdminFree) {
                putString(KEY_UNLOCK_METHOD, "ADMIN_REMOTE_VIP")
            }

            // Per-device individual feature grant/block overrides (-1 = unset, 0 = blocked, 1 = granted)
            putInt(KEY_DEVICE_ALLOW_RGB_THEMES, encodeTriState(allowed.rgbThemes))
            putInt(KEY_DEVICE_ALLOW_BANGLISH, encodeTriState(allowed.banglish))
            putInt(KEY_DEVICE_ALLOW_CALL_BAR, encodeTriState(allowed.callBar))
            putInt(KEY_DEVICE_ALLOW_RGB_STICKER, encodeTriState(allowed.rgbSticker))
            putInt(KEY_DEVICE_ALLOW_STYLISH_FONTS, encodeTriState(allowed.stylishFonts))
            putInt(KEY_DEVICE_ALLOW_AI_HUB, encodeTriState(allowed.aiHub))

            // Server-synced independent Banglish subscription expiry
            if (snapshot.banglishExpiryTimestampMs > nowServer) {
                val currentBnExpiry = prefs.getLong(KEY_BANGLISH_EXPIRY_TIMESTAMP_MS, 0L)
                putLong(KEY_BANGLISH_EXPIRY_TIMESTAMP_MS, maxOf(currentBnExpiry, snapshot.banglishExpiryTimestampMs))
            }

            // Server-granted permanent lifetime VIP
            if (isServerLifetime) {
                putBoolean(KEY_IS_LIFETIME_UNLOCKED, true)
                putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
                if (prefs.getString(KEY_UNLOCK_METHOD, null).isNullOrBlank()) {
                    putString(KEY_UNLOCK_METHOD, "FIREBASE_LIFETIME_VIP")
                }
            }

            // Server-synced active weekly / monthly / custom-day VIP expiry
            when {
                isServerTimedActive -> {
                    val currentExpiry = prefs.getLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, 0L)
                    putLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, maxOf(currentExpiry, snapshot.vipExpiryTimestampMs))
                    val planCode = when (statusLower) {
                        STATUS_WEEKLY_ACTIVE -> RemoteAdminConfig.PLAN_WEEKLY
                        STATUS_MONTHLY_ACTIVE -> RemoteAdminConfig.PLAN_MONTHLY
                        else -> RemoteAdminConfig.PLAN_CUSTOM
                    }
                    putString(KEY_ACTIVE_SUB_PLAN, planCode)
                }
                statusLower == STATUS_EXPIRED && !isAdminFree && !isServerLifetime && customExpiryMs <= nowServer -> {
                    putLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, 0L)
                    putString(KEY_ACTIVE_SUB_PLAN, "")
                }
            }
        }

        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        if (updated.vipStatusCode != lastSyncedVipStatus) {
            syncDeviceToFirebase(state = updated, force = true)
        }
    }

    private fun syncDeviceToFirebase(state: LicenseUiState, force: Boolean) {
        val now = currentServerOrUtcMs()
        val statusChanged = state.vipStatusCode != lastSyncedVipStatus
        if (!force && !statusChanged && (now - lastFirebaseSyncMs) < FIREBASE_SYNC_MIN_INTERVAL_MS) {
            return
        }
        lastFirebaseSyncMs = now
        lastSyncedVipStatus = state.vipStatusCode
        val firstInstallTs = prefs.getLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, 0L)
        firebaseRemoteManager.syncDeviceStatus(
            deviceId = deviceId,
            vipStatus = state.vipStatusCode,
            vipExpiryTimestampMs = state.vipExpiryTimestampMs,
            firstInstallTimestampMs = firstInstallTs,
            banglishExpiryTimestampMs = state.banglishExpiryTimestampMs
        )
    }

    private fun encodeTriState(value: Boolean?): Int = when (value) {
        true -> 1
        false -> 0
        null -> -1
    }

    private fun decodeTriState(key: String): Boolean? = when (prefs.getInt(key, -1)) {
        1 -> true
        0 -> false
        else -> null
    }

    private fun readCachedAdminConfig(): RemoteAdminConfig {
        val cachedVipPlan = prefs.getString(KEY_ADMIN_PLAN_MODE, RemoteAdminConfig.PLAN_LIFETIME)
            ?.takeIf { it.isNotBlank() } ?: RemoteAdminConfig.PLAN_LIFETIME
        val cachedVipDays = if (prefs.contains(KEY_ADMIN_VIP_DURATION_DAYS)) {
            prefs.getInt(KEY_ADMIN_VIP_DURATION_DAYS, RemoteAdminConfig.DEFAULT_VIP_DURATION_DAYS).coerceIn(0, 3650)
        } else {
            RemoteAdminConfig.planTypeToDefaultDays(cachedVipPlan)
        }
        val cachedBanglishPlan = prefs.getString(KEY_ADMIN_BANGLISH_PLAN_MODE, RemoteAdminConfig.PLAN_MONTHLY)
            ?.takeIf { it.isNotBlank() } ?: RemoteAdminConfig.PLAN_MONTHLY
        val cachedBanglishDays = if (prefs.contains(KEY_ADMIN_BANGLISH_DURATION_DAYS)) {
            prefs.getInt(KEY_ADMIN_BANGLISH_DURATION_DAYS, RemoteAdminConfig.DEFAULT_BANGLISH_DURATION_DAYS).coerceIn(0, 3650)
        } else {
            RemoteAdminConfig.planTypeToDefaultDays(cachedBanglishPlan)
        }
        val cachedBanglishPrice = prefs.getInt(KEY_ADMIN_BANGLISH_PRICE, RemoteAdminConfig.DEFAULT_BANGLISH_PRICE)
            .coerceAtLeast(1)

        return RemoteAdminConfig(
            freeTrialDays = prefs.getInt(KEY_ADMIN_FREE_TRIAL_DAYS, RemoteAdminConfig.DEFAULT_FREE_TRIAL_DAYS)
                .coerceIn(0, 365),
            globalAllFree = prefs.getBoolean(KEY_ADMIN_GLOBAL_ALL_FREE, false),
            vipPrice = prefs.getInt(KEY_ADMIN_VIP_PRICE, RemoteAdminConfig.DEFAULT_VIP_PRICE)
                .coerceAtLeast(1),
            vipDurationDays = cachedVipDays,
            vipPlanType = RemoteAdminConfig.durationDaysToPlanType(cachedVipDays),
            banglishPrice = cachedBanglishPrice,
            banglishDurationDays = cachedBanglishDays,
            banglishPlanType = RemoteAdminConfig.durationDaysToPlanType(cachedBanglishDays),
            banglishDailyPrice = prefs.getInt(KEY_ADMIN_BANGLISH_DAILY_PRICE, RemoteAdminConfig.DEFAULT_BANGLISH_DAILY_PRICE)
                .coerceAtLeast(1),
            banglishWeeklyPrice = prefs.getInt(KEY_ADMIN_BANGLISH_WEEKLY_PRICE, RemoteAdminConfig.DEFAULT_BANGLISH_WEEKLY_PRICE)
                .coerceAtLeast(1),
            banglishMonthlyPrice = prefs.getInt(KEY_ADMIN_BANGLISH_MONTHLY_PRICE, cachedBanglishPrice)
                .coerceAtLeast(1),
            banglishCustomPlans = cachedCustomBanglishPlans,
            globalFreeFeatures = FreeFeatureOverrides(
                rgbThemes = prefs.getBoolean(KEY_ADMIN_FREE_RGB_THEMES, false),
                banglish = prefs.getBoolean(KEY_ADMIN_FREE_BANGLISH, false),
                callBar = prefs.getBoolean(KEY_ADMIN_FREE_CALL_BAR, false),
                rgbSticker = prefs.getBoolean(KEY_ADMIN_FREE_RGB_STICKER, false),
                stylishFonts = prefs.getBoolean(KEY_ADMIN_FREE_STYLISH_FONTS, false),
                aiHub = prefs.getBoolean(KEY_ADMIN_FREE_AI_HUB, false)
            ),
            banners = PromoBannerConfig(
                banner1Url = prefs.getString(KEY_ADMIN_BANNER1_URL, "").orEmpty(),
                banner1Image = prefs.getString(KEY_ADMIN_BANNER1_IMAGE, "").orEmpty(),
                banner1Title = prefs.getString(KEY_ADMIN_BANNER1_TITLE, PromoBannerConfig.DEFAULT_BANNER1_TITLE)
                    ?.takeIf { it.isNotBlank() } ?: PromoBannerConfig.DEFAULT_BANNER1_TITLE,
                banner1Subtitle = prefs.getString(KEY_ADMIN_BANNER1_SUBTITLE, PromoBannerConfig.DEFAULT_BANNER1_SUBTITLE)
                    ?.takeIf { it.isNotBlank() } ?: PromoBannerConfig.DEFAULT_BANNER1_SUBTITLE,
                banner2Url = prefs.getString(KEY_ADMIN_BANNER2_URL, "").orEmpty(),
                banner2Image = prefs.getString(KEY_ADMIN_BANNER2_IMAGE, "").orEmpty(),
                banner2Title = prefs.getString(KEY_ADMIN_BANNER2_TITLE, PromoBannerConfig.DEFAULT_BANNER2_TITLE)
                    ?.takeIf { it.isNotBlank() } ?: PromoBannerConfig.DEFAULT_BANNER2_TITLE,
                banner2Subtitle = prefs.getString(KEY_ADMIN_BANNER2_SUBTITLE, PromoBannerConfig.DEFAULT_BANNER2_SUBTITLE)
                    ?.takeIf { it.isNotBlank() } ?: PromoBannerConfig.DEFAULT_BANNER2_SUBTITLE
            )
        )
    }

    private fun readCachedDeviceOverrides(): DeviceRemoteOverrides {
        return DeviceRemoteOverrides(
            adminFreeVip = prefs.getBoolean(KEY_ADMIN_GRANTED_VIP, false),
            adminAllFree = prefs.getBoolean(KEY_ADMIN_ALL_FREE_DEVICE, false),
            customExpiryTimestampMs = prefs.getLong(KEY_DEVICE_CUSTOM_EXPIRY_MS, 0L).coerceAtLeast(0L),
            allowedFeatures = DeviceFeatureAllowMap(
                rgbThemes = decodeTriState(KEY_DEVICE_ALLOW_RGB_THEMES),
                banglish = decodeTriState(KEY_DEVICE_ALLOW_BANGLISH),
                callBar = decodeTriState(KEY_DEVICE_ALLOW_CALL_BAR),
                rgbSticker = decodeTriState(KEY_DEVICE_ALLOW_RGB_STICKER),
                stylishFonts = decodeTriState(KEY_DEVICE_ALLOW_STYLISH_FONTS),
                aiHub = decodeTriState(KEY_DEVICE_ALLOW_AI_HUB)
            )
        )
    }

    private data class TrialRecord(
        val deviceId: String,
        val firstInstallTimestampMs: Long,
        val maxSeenElapsedMs: Long,
        val lastSeenWallMs: Long,
        val expired: Boolean
    )

    private inline fun editPrefsSafely(block: SharedPreferences.Editor.() -> Unit) {
        isInternalPrefWrite.set(true)
        try {
            prefs.edit().apply(block).apply()
        } finally {
            isInternalPrefWrite.set(false)
        }
    }

    private fun ensureLocalTrialSeededFast() {
        if (prefs.contains(KEY_FIRST_INSTALL_TIMESTAMP_MS)) return
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val existingLegacyFirst = if (prefs.contains(KEY_FIRST_LAUNCH_WALL_MS)) {
            prefs.getLong(KEY_FIRST_LAUNCH_WALL_MS, nowWall)
        } else {
            nowWall
        }
        val firstInstall = existingLegacyFirst.coerceAtMost(nowWall)
        val initialElapsed = (nowWall - firstInstall).coerceAtLeast(0L)
        val expired = prefs.getBoolean(KEY_TRIAL_EXPIRED_STICKY, false) || initialElapsed >= TRIAL_DURATION_MS
        editPrefsSafely {
            putInt(KEY_TRIAL_POLICY_VERSION, CURRENT_TRIAL_POLICY_VERSION)
            putLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, firstInstall)
            putLong(KEY_FIRST_LAUNCH_WALL_MS, firstInstall)
            putLong(KEY_FIRST_LAUNCH_ELAPSED_MS, nowElapsed)
            putLong(KEY_MAX_SEEN_ELAPSED_MS, initialElapsed)
            putLong(KEY_LAST_SEEN_WALL_MS, nowWall)
            putBoolean(KEY_TRIAL_EXPIRED_STICKY, expired)
        }
        lastPrefCheckpointMs = nowWall
    }

    private fun checkpointLocalTrialFast(nowWall: Long) {
        val nowElapsed = SystemClock.elapsedRealtime()
        val firstInstall = when {
            prefs.contains(KEY_FIRST_INSTALL_TIMESTAMP_MS) -> prefs.getLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, nowWall)
            prefs.contains(KEY_FIRST_LAUNCH_WALL_MS) -> prefs.getLong(KEY_FIRST_LAUNCH_WALL_MS, nowWall)
            else -> nowWall
        }.coerceAtMost(nowWall)
        val firstMono = if (prefs.contains(KEY_FIRST_LAUNCH_ELAPSED_MS)) {
            prefs.getLong(KEY_FIRST_LAUNCH_ELAPSED_MS, nowElapsed)
        } else {
            nowElapsed
        }
        val prevMaxElapsed = prefs.getLong(KEY_MAX_SEEN_ELAPSED_MS, 0L).coerceAtLeast(0L)
        val prevLastWall = prefs.getLong(KEY_LAST_SEEN_WALL_MS, firstInstall)
        val monoDelta = if (nowElapsed >= firstMono) (nowElapsed - firstMono) else 0L
        val wallDelta = (nowWall - firstInstall).coerceAtLeast(0L)
        val clockRolledBack = nowWall + CLOCK_ROLLBACK_TOLERANCE_MS < prevLastWall
        val mergedMaxElapsed = maxOf(prevMaxElapsed, wallDelta, monoDelta)
        val expired = prefs.getBoolean(KEY_TRIAL_EXPIRED_STICKY, false) ||
            clockRolledBack ||
            mergedMaxElapsed >= TRIAL_DURATION_MS

        editPrefsSafely {
            putLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, firstInstall)
            putLong(KEY_FIRST_LAUNCH_WALL_MS, firstInstall)
            putLong(KEY_FIRST_LAUNCH_ELAPSED_MS, firstMono)
            putLong(KEY_MAX_SEEN_ELAPSED_MS, if (expired) maxOf(mergedMaxElapsed, TRIAL_DURATION_MS) else mergedMaxElapsed)
            putLong(KEY_LAST_SEEN_WALL_MS, maxOf(prevLastWall, nowWall))
            putBoolean(KEY_TRIAL_EXPIRED_STICKY, expired)
        }
        lastPrefCheckpointMs = nowWall
    }

    /**
     * Pure, read-only state calculation from SharedPreferences + system clock.
     * Executes in <0.1ms with zero disk/MediaStore I/O and zero SharedPreferences writes.
     */
    private fun computeFastStateFromPrefsOnly(): LicenseUiState {
        val adminConfig = readCachedAdminConfig()
        val deviceOverrides = readCachedDeviceOverrides()
        val globalAllFree = adminConfig.globalAllFree
        val adminGranted = deviceOverrides.hasFullAdminFreeAccess
        val lifetimeUnlocked = prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)
        val banglishLifetimeUnlocked = prefs.getBoolean(KEY_IS_BANGLISH_LIFETIME_UNLOCKED, false)
        val unlockMethod = prefs.getString(KEY_UNLOCK_METHOD, null)
        val unlockedTxnId = prefs.getString(KEY_UNLOCK_TXN_ID, null)
        val nowServer = currentServerOrUtcMs()
        val pending = latestPendingPayment

        // Independent Romanized Bengali subscription countdown calculation
        val banglishExpiryMs = prefs.getLong(KEY_BANGLISH_EXPIRY_TIMESTAMP_MS, 0L)
        val banglishRemainingSec = if (banglishExpiryMs > nowServer) {
            ((banglishExpiryMs - nowServer) + 999L) / 1000L
        } else {
            0L
        }
        val banglishTotalDurationSec = prefs.getLong(
            KEY_BANGLISH_TOTAL_DURATION_SEC,
            (adminConfig.banglishDurationDays.coerceAtLeast(1).toLong() * 86_400L)
        ).coerceAtLeast(banglishRemainingSec.coerceAtLeast(1L))

        // Always compute true server/UTC trial status independently so Banglish card knows whether trial is active
        val nowElapsed = SystemClock.elapsedRealtime()
        val firstInstall = when {
            prefs.contains(KEY_FIRST_INSTALL_TIMESTAMP_MS) -> prefs.getLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, nowServer)
            prefs.contains(KEY_FIRST_LAUNCH_WALL_MS) -> prefs.getLong(KEY_FIRST_LAUNCH_WALL_MS, nowServer)
            else -> nowServer
        }.coerceAtMost(nowServer)
        val firstMono = if (prefs.contains(KEY_FIRST_LAUNCH_ELAPSED_MS)) {
            prefs.getLong(KEY_FIRST_LAUNCH_ELAPSED_MS, nowElapsed)
        } else {
            nowElapsed
        }
        val prevMaxElapsed = prefs.getLong(KEY_MAX_SEEN_ELAPSED_MS, 0L).coerceAtLeast(0L)
        val prevLastWall = prefs.getLong(KEY_LAST_SEEN_WALL_MS, firstInstall)

        val monoDelta = if (nowElapsed >= firstMono) (nowElapsed - firstMono) else 0L
        val wallDelta = (nowServer - firstInstall).coerceAtLeast(0L)
        val clockRolledBack = nowServer + CLOCK_ROLLBACK_TOLERANCE_MS < prevLastWall
        val effectiveElapsedMs = maxOf(prevMaxElapsed, wallDelta, monoDelta)
        val effectiveTrialDurationMs = adminConfig.trialDurationMs
        val trialRemainingMs = (effectiveTrialDurationMs - effectiveElapsedMs).coerceAtLeast(0L)
        val trialExpired = clockRolledBack || effectiveTrialDurationMs <= 0L || trialRemainingMs <= 0L
        val trialRemainingSec = if (trialExpired) 0L else (trialRemainingMs + 999L) / 1000L
        val trialEndTs = firstInstall + effectiveTrialDurationMs

        if (globalAllFree || adminGranted) {
            return LicenseUiState(
                isLifetimeUnlocked = lifetimeUnlocked,
                isAdminGrantedVip = adminGranted,
                isGlobalAllFree = globalAllFree,
                isMonthlyActive = false,
                isWeeklyActive = false,
                vipExpiryTimestampMs = 0L,
                isBanglishLifetimeUnlocked = banglishLifetimeUnlocked,
                banglishExpiryTimestampMs = banglishExpiryMs,
                banglishRemainingSeconds = banglishRemainingSec,
                banglishTotalDurationSeconds = banglishTotalDurationSec,
                vipStatusCode = STATUS_ADMIN_FREE_VIP,
                isTrialExpired = false,
                remainingSeconds = 0L,
                deviceId = deviceId,
                unlockMethod = unlockMethod ?: if (globalAllFree) "GLOBAL_ALL_FREE" else "ADMIN_REMOTE_VIP",
                unlockedTxnId = unlockedTxnId,
                adminConfig = adminConfig,
                deviceOverrides = deviceOverrides,
                pendingPayment = pending
            )
        }

        if (lifetimeUnlocked) {
            return LicenseUiState(
                isLifetimeUnlocked = true,
                isAdminGrantedVip = false,
                isGlobalAllFree = false,
                isMonthlyActive = false,
                isWeeklyActive = false,
                vipExpiryTimestampMs = 0L,
                isBanglishLifetimeUnlocked = banglishLifetimeUnlocked,
                banglishExpiryTimestampMs = banglishExpiryMs,
                banglishRemainingSeconds = banglishRemainingSec,
                banglishTotalDurationSeconds = banglishTotalDurationSec,
                vipStatusCode = STATUS_LIFETIME_VIP,
                isTrialExpired = trialExpired,
                remainingSeconds = trialRemainingSec,
                deviceId = deviceId,
                unlockMethod = unlockMethod,
                unlockedTxnId = unlockedTxnId,
                adminConfig = adminConfig,
                deviceOverrides = deviceOverrides,
                pendingPayment = pending
            )
        }

        val customOverrideExpiryMs = deviceOverrides.customExpiryTimestampMs
        val subExpiryMs = maxOf(prefs.getLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, 0L), customOverrideExpiryMs)
        if (subExpiryMs > nowServer) {
            val remSec = ((subExpiryMs - nowServer) + 999L) / 1000L
            val isFromCustomOverride = customOverrideExpiryMs > prefs.getLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, 0L)
            val subPlan = prefs.getString(KEY_ACTIVE_SUB_PLAN, RemoteAdminConfig.PLAN_MONTHLY).orEmpty()
            val isWeekly = !isFromCustomOverride && subPlan.equals(RemoteAdminConfig.PLAN_WEEKLY, ignoreCase = true)
            val statusCode = when {
                isFromCustomOverride -> STATUS_CUSTOM_ACTIVE
                isWeekly -> STATUS_WEEKLY_ACTIVE
                subPlan.equals(RemoteAdminConfig.PLAN_CUSTOM, ignoreCase = true) -> STATUS_CUSTOM_ACTIVE
                else -> STATUS_MONTHLY_ACTIVE
            }
            return LicenseUiState(
                isLifetimeUnlocked = false,
                isAdminGrantedVip = false,
                isGlobalAllFree = false,
                isMonthlyActive = !isWeekly,
                isWeeklyActive = isWeekly,
                vipExpiryTimestampMs = subExpiryMs,
                isBanglishLifetimeUnlocked = banglishLifetimeUnlocked,
                banglishExpiryTimestampMs = banglishExpiryMs,
                banglishRemainingSeconds = banglishRemainingSec,
                banglishTotalDurationSeconds = banglishTotalDurationSec,
                vipStatusCode = statusCode,
                isTrialExpired = trialExpired,
                remainingSeconds = remSec,
                deviceId = deviceId,
                unlockMethod = if (isFromCustomOverride) "ADMIN_CUSTOM_EXPIRY" else (unlockMethod ?: "UPI_TIMED"),
                unlockedTxnId = unlockedTxnId,
                adminConfig = adminConfig,
                deviceOverrides = deviceOverrides,
                pendingPayment = pending
            )
        }

        return LicenseUiState(
            isLifetimeUnlocked = false,
            isAdminGrantedVip = false,
            isGlobalAllFree = false,
            isMonthlyActive = false,
            isWeeklyActive = false,
            vipExpiryTimestampMs = if (trialExpired) 0L else trialEndTs,
            isBanglishLifetimeUnlocked = banglishLifetimeUnlocked,
            banglishExpiryTimestampMs = banglishExpiryMs,
            banglishRemainingSeconds = banglishRemainingSec,
            banglishTotalDurationSeconds = banglishTotalDurationSec,
            vipStatusCode = if (trialExpired) STATUS_EXPIRED else STATUS_TRIAL,
            isTrialExpired = trialExpired,
            remainingSeconds = trialRemainingSec,
            deviceId = deviceId,
            adminConfig = adminConfig,
            deviceOverrides = deviceOverrides,
            pendingPayment = pending
        )
    }

    /**
     * Runs strictly on `Dispatchers.IO`.
     * Reconciles `SharedPreferences` with encrypted `Documents/.ai_keyboard_trial.dat` (and
     * `context.filesDir` fallback). Always takes the earliest `first_install_timestamp` and
     * largest `max_seen_elapsed_ms` so reinstall or clock tampering cannot reset the 7-day trial.
     */
    private fun reconcileAndPersistTrialStateOnIo(forcePersistToDocuments: Boolean): TrialRecord {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val diskRecord = readEncryptedTrialRecord()?.takeIf {
            it.deviceId.equals(deviceId, ignoreCase = true)
        }

        val hasLocalFirst = prefs.contains(KEY_FIRST_INSTALL_TIMESTAMP_MS) || prefs.contains(KEY_FIRST_LAUNCH_WALL_MS)
        val localFirst = when {
            prefs.contains(KEY_FIRST_INSTALL_TIMESTAMP_MS) -> prefs.getLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, nowWall)
            prefs.contains(KEY_FIRST_LAUNCH_WALL_MS) -> prefs.getLong(KEY_FIRST_LAUNCH_WALL_MS, nowWall)
            else -> nowWall
        }
        val localMaxElapsed = prefs.getLong(KEY_MAX_SEEN_ELAPSED_MS, 0L).coerceAtLeast(0L)
        val localLastWall = prefs.getLong(KEY_LAST_SEEN_WALL_MS, localFirst)
        val localFirstMono = if (prefs.contains(KEY_FIRST_LAUNCH_ELAPSED_MS)) {
            prefs.getLong(KEY_FIRST_LAUNCH_ELAPSED_MS, nowElapsed)
        } else {
            nowElapsed
        }
        val localExpired = prefs.getBoolean(KEY_TRIAL_EXPIRED_STICKY, false)

        val mergedFirstInstall = when {
            diskRecord != null && hasLocalFirst -> minOf(diskRecord.firstInstallTimestampMs, localFirst)
            diskRecord != null -> diskRecord.firstInstallTimestampMs
            else -> localFirst
        }.coerceAtMost(nowWall)

        val mergedLastWall = maxOf(
            diskRecord?.lastSeenWallMs ?: mergedFirstInstall,
            localLastWall
        )

        val monoDelta = if (nowElapsed >= localFirstMono) (nowElapsed - localFirstMono) else 0L
        val wallDelta = (nowWall - mergedFirstInstall).coerceAtLeast(0L)
        val clockRolledBack = nowWall + CLOCK_ROLLBACK_TOLERANCE_MS < mergedLastWall

        val previousMaxElapsed = maxOf(diskRecord?.maxSeenElapsedMs ?: 0L, localMaxElapsed)
        val mergedMaxElapsed = maxOf(previousMaxElapsed, wallDelta, monoDelta)

        val isExpired = localExpired ||
            (diskRecord?.expired == true) ||
            clockRolledBack ||
            mergedMaxElapsed >= TRIAL_DURATION_MS

        val updatedLastWall = maxOf(mergedLastWall, nowWall)

        val record = TrialRecord(
            deviceId = deviceId,
            firstInstallTimestampMs = mergedFirstInstall,
            maxSeenElapsedMs = if (isExpired) maxOf(mergedMaxElapsed, TRIAL_DURATION_MS) else mergedMaxElapsed,
            lastSeenWallMs = updatedLastWall,
            expired = isExpired
        )

        editPrefsSafely {
            putInt(KEY_TRIAL_POLICY_VERSION, CURRENT_TRIAL_POLICY_VERSION)
            putLong(KEY_FIRST_INSTALL_TIMESTAMP_MS, record.firstInstallTimestampMs)
            putLong(KEY_FIRST_LAUNCH_WALL_MS, record.firstInstallTimestampMs)
            putLong(KEY_FIRST_LAUNCH_ELAPSED_MS, localFirstMono)
            putLong(KEY_MAX_SEEN_ELAPSED_MS, record.maxSeenElapsedMs)
            putLong(KEY_LAST_SEEN_WALL_MS, record.lastSeenWallMs)
            putBoolean(KEY_TRIAL_EXPIRED_STICKY, record.expired)
        }
        lastPrefCheckpointMs = nowWall

        val shouldWriteDisk = forcePersistToDocuments ||
            diskRecord == null ||
            record.expired != diskRecord.expired ||
            (nowWall - lastTrialPersistMs >= TRIAL_DISK_SYNC_INTERVAL_MS)

        if (shouldWriteDisk) {
            lastTrialPersistMs = nowWall
            writeEncryptedTrialFile(record)
        }

        return record
    }

    /**
     * Public non-blocking trigger for license file auto-restore.
     */
    fun tryAutoRestoreFromLicenseFile(): Boolean {
        if (prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            return true
        }
        syncStorageInBackground(forcePersistToDocuments = false)
        return false
    }

    /**
     * Runs strictly on `Dispatchers.IO`.
     * Silently reads and decrypts `Documents/.ai_keyboard_vip.lic` (with fallback to legacy
     * `.rakib_official_vip.lic` and `context.filesDir`) and validates it strictly against
     * this phone's hardware Device ID. If authentic, unlocks Lifetime VIP silently.
     */
    private fun tryAutoRestoreFromLicenseFileOnIo(): Boolean {
        if (prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            if (readEncryptedLicenseRecord(HIDDEN_LICENSE_FILENAME) == null) {
                writeEncryptedLicenseFile(
                    txnId = prefs.getString(KEY_UNLOCK_TXN_ID, "LIC_$deviceId").orEmpty()
                )
            }
            return true
        }

        // 1. Primary encrypted hardware-bound license file: Documents/.ai_keyboard_vip.lic
        val encryptedRecord = readEncryptedLicenseRecord(HIDDEN_LICENSE_FILENAME)
        if (encryptedRecord != null &&
            (encryptedRecord.deviceId.equals(deviceId, ignoreCase = true) ||
                encryptedRecord.deviceId.equals(legacyModelDeviceId, ignoreCase = true) ||
                encryptedRecord.deviceId.equals(legacySixCharDeviceId, ignoreCase = true)) &&
            verifyLicenseSignature(encryptedRecord.deviceId, encryptedRecord.txnId, encryptedRecord.signature)
        ) {
            val needsUpgrade = !encryptedRecord.deviceId.equals(deviceId, ignoreCase = true)
            markLifetimeUnlocked(
                method = "AUTO_RESTORE_ENCRYPTED_LIC",
                txnId = encryptedRecord.txnId.ifBlank { "LIC_$deviceId" },
                writeLicenseFile = needsUpgrade
            )
            return true
        }

        // 2. Seamless backward-compatibility for previously issued .rakib_official_vip.lic on same device
        val legacyRaw = readRawDocumentText(LEGACY_LICENSE_FILENAME)
        if (!legacyRaw.isNullOrBlank()) {
            val legacyDevice = extractPlainField(legacyRaw, "deviceId")
            val legacyKey = extractPlainField(legacyRaw, "vipKey")
            val legacyTxn = extractPlainField(legacyRaw, "txnId")
            val matchesDevice = legacyDevice.equals(deviceId, ignoreCase = true) ||
                legacyDevice.equals(legacyModelDeviceId, ignoreCase = true) ||
                legacyDevice.equals(legacySixCharDeviceId, ignoreCase = true)
            if (matchesDevice && legacyKey.length == 6) {
                markLifetimeUnlocked(
                    method = "AUTO_RESTORE_LEGACY_LIC",
                    txnId = legacyTxn.ifBlank { "LIC_$deviceId" },
                    writeLicenseFile = true
                )
                return true
            }
        }

        return false
    }

    /**
     * Generates a customer-safe discounted unique-paisa amount strictly LESS than `baseAmountInr`:
     * `(baseAmount - 1) + random(.01..0.99)` (e.g., ₹20 becomes `19.83`), ensuring the customer never
     * pays extra while enabling exact automatic payment matching at `/pending_payments/{deviceId}`.
     */
    fun generateDiscountedUniqueAmount(baseAmountInr: Int): String {
        val safeBase = baseAmountInr.coerceAtLeast(1)
        val rupees = (safeBase - 1).coerceAtLeast(0)
        val paisa = 1 + SecureRandom().nextInt(99) // 01..99 -> strictly < baseAmountInr
        return String.format(Locale.US, "%d.%02d", rupees, paisa)
    }

    /**
     * Submits `/pending_payments/{deviceId}` to Firebase RTDB so auto-verification can approve
     * this exact discounted amount for QR scans or UPI app payments.
     */
    fun registerPendingPayment(
        planScope: String,
        baseAmountInr: Int,
        uniqueAmountFormatted: String,
        durationDays: Int,
        txnRef: String = "AIKB_${System.currentTimeMillis()}"
    ): String {
        val normalizedScope = normalizePlanScope(planScope)
        firebaseRemoteManager.submitPendingPayment(
            deviceId = deviceId,
            planType = normalizedScope,
            baseAmount = baseAmountInr.coerceAtLeast(1),
            uniqueAmountFormatted = uniqueAmountFormatted,
            durationDays = durationDays.coerceAtLeast(0),
            txnRef = txnRef
        )
        return txnRef
    }

    /**
     * Builds the official RBI-compliant Merchant UPI Intent with the customer-safe discounted
     * unique-paisa amount (`am=${uniqueAmountFormatted}`) and writes `/pending_payments/{deviceId}`.
     */
    fun createOfficialUpiIntent(
        planScope: String = PLAN_SCOPE_VIP_FEATURES,
        overrideAmountInr: Int? = null,
        uniqueAmountFormatted: String? = null,
        overridePlanType: String? = null,
        overrideDurationDays: Int? = null
    ): Pair<String, Intent> {
        val cfg = readCachedAdminConfig()
        val normalizedScope = normalizePlanScope(planScope)
        val baseAmount = (overrideAmountInr ?: if (normalizedScope == PLAN_SCOPE_BANGLISH) cfg.banglishPrice else cfg.vipPrice)
            .coerceAtLeast(1)
        val amountFormatted = uniqueAmountFormatted?.takeIf { it.isNotBlank() }
            ?: generateDiscountedUniqueAmount(baseAmount)
        val fallbackDays = if (normalizedScope == PLAN_SCOPE_BANGLISH) cfg.banglishDurationDays else cfg.vipDurationDays
        val resolvedDays = resolveDurationDays(overrideDurationDays, overridePlanType, fallbackDays)
        val note = buildTransactionNote(normalizedScope, resolvedDays)
        val txnRef = "AIKB_${System.currentTimeMillis()}"

        registerPendingPayment(
            planScope = normalizedScope,
            baseAmountInr = baseAmount,
            uniqueAmountFormatted = amountFormatted,
            durationDays = resolvedDays,
            txnRef = txnRef
        )

        val uriString = "upi://pay" +
            "?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
            "&pn=${Uri.encode(MERCHANT_NAME)}" +
            "&mc=$MERCHANT_MCC" +
            "&tr=${Uri.encode(txnRef)}" +
            "&am=$amountFormatted" +
            "&cu=INR" +
            "&tn=${Uri.encode(note)}"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        return txnRef to intent
    }

    /**
     * Builds the scannable official UPI QR code URI for `6296603748@okbizaxis` using the exact
     * discounted unique-paisa amount (`am=${uniqueAmountFormatted}`).
     */
    fun buildUpiQrPayload(
        planScope: String = PLAN_SCOPE_VIP_FEATURES,
        overrideAmountInr: Int? = null,
        uniqueAmountFormatted: String? = null,
        overridePlanType: String? = null,
        overrideDurationDays: Int? = null
    ): String {
        val cfg = readCachedAdminConfig()
        val normalizedScope = normalizePlanScope(planScope)
        val baseAmount = (overrideAmountInr ?: if (normalizedScope == PLAN_SCOPE_BANGLISH) cfg.banglishPrice else cfg.vipPrice)
            .coerceAtLeast(1)
        val amountFormatted = uniqueAmountFormatted?.takeIf { it.isNotBlank() }
            ?: "$baseAmount.00"
        val fallbackDays = if (normalizedScope == PLAN_SCOPE_BANGLISH) cfg.banglishDurationDays else cfg.vipDurationDays
        val resolvedDays = resolveDurationDays(overrideDurationDays, overridePlanType, fallbackDays)
        val note = buildTransactionNote(normalizedScope, resolvedDays)
        return "upi://pay" +
            "?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
            "&pn=${Uri.encode(MERCHANT_NAME)}" +
            "&mc=$MERCHANT_MCC" +
            "&am=$amountFormatted" +
            "&cu=INR" +
            "&tn=${Uri.encode(note)}"
    }

    private fun buildTransactionNote(planScope: String, resolvedDays: Int): String {
        return if (planScope == PLAN_SCOPE_BANGLISH) {
            when {
                resolvedDays <= 0 -> "AI Keyboard Banglish Lifetime"
                resolvedDays == 1 -> "AI Keyboard Banglish 1-Day"
                resolvedDays == 7 -> "AI Keyboard Banglish 7-Day"
                resolvedDays == 30 -> "AI Keyboard Banglish 30-Day"
                else -> "AI Keyboard Banglish ${resolvedDays}D"
            }
        } else {
            when {
                resolvedDays <= 0 -> TRANSACTION_NOTE
                resolvedDays == 7 -> "AI Keyboard Weekly VIP"
                resolvedDays == 30 -> "AI Keyboard Monthly VIP"
                else -> "AI Keyboard ${resolvedDays}-Day VIP"
            }
        }
    }

    private fun normalizePlanScope(rawScope: String?): String {
        return if (rawScope.equals(PLAN_SCOPE_BANGLISH, ignoreCase = true) ||
            rawScope?.contains("banglish", ignoreCase = true) == true
        ) {
            PLAN_SCOPE_BANGLISH
        } else {
            PLAN_SCOPE_VIP_FEATURES
        }
    }

    private fun resolveDurationDays(
        explicitDays: Int?,
        planType: String?,
        fallbackDays: Int
    ): Int {
        if (explicitDays != null && explicitDays >= 0) return explicitDays
        if (!planType.isNullOrBlank()) {
            return RemoteAdminConfig.planTypeToDefaultDays(planType)
        }
        return fallbackDays.coerceAtLeast(0)
    }

    /**
     * Strictly verifies the authentic intent response returned by the UPI app and unlocks ONLY
     * the exact purchased plan scope (`vip_features` vs `banglish`).
     */
    fun verifyUpiIntentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?,
        planScope: String = PLAN_SCOPE_VIP_FEATURES,
        purchasedPlanType: String? = null,
        purchasedDurationDays: Int? = null
    ): UpiVerificationResult {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return UpiVerificationResult.Cancelled
        }

        val params = linkedMapOf<String, String>()

        val rawResponse = data.getStringExtra("response").orEmpty()
        if (rawResponse.isNotBlank()) {
            rawResponse.split("&").forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val key = pair.substring(0, idx).trim().lowercase(Locale.US)
                    val value = Uri.decode(pair.substring(idx + 1).trim())
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        params[key] = value
                    }
                }
            }
        }

        val extras = data.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                val lower = key.lowercase(Locale.US)
                if (lower == "response") continue
                val value = extras.getString(key)?.trim()
                if (!value.isNullOrEmpty() && !params.containsKey(lower)) {
                    params[lower] = value
                }
            }
        }

        val status = (params["status"] ?: params["txnstatus"]).orEmpty().trim()
        val txnId = params["txnid"].orEmpty().trim()
        val approvalRef = (params["approvalrefno"] ?: params["approvalref"]).orEmpty().trim()
        val returnedTxnRef = params["txnref"].orEmpty().trim()
        val responseCode = params["responsecode"].orEmpty().trim()

        if (status.isEmpty()) {
            return UpiVerificationResult.Cancelled
        }

        if (status.equals("SUBMITTED", ignoreCase = true) || status.equals("PENDING", ignoreCase = true)) {
            return UpiVerificationResult.PendingOrSubmitted
        }

        if (!status.equals("SUCCESS", ignoreCase = true)) {
            return UpiVerificationResult.Failed("Status=$status")
        }

        if (!expectedTxnRef.isNullOrBlank() && returnedTxnRef.isNotBlank() &&
            !returnedTxnRef.equals(expectedTxnRef, ignoreCase = true)
        ) {
            return UpiVerificationResult.Failed("Transaction reference mismatch")
        }

        if (responseCode.isNotBlank() && responseCode != "00" && responseCode != "0") {
            return UpiVerificationResult.Failed("ResponseCode=$responseCode")
        }

        val verifiedId = listOf(txnId, approvalRef).firstOrNull { isAuthenticTxnIdentifier(it) }
            ?: return UpiVerificationResult.Failed("Missing valid txnId / ApprovalRefNo")

        val normalizedScope = normalizePlanScope(planScope)
        val currentAdminCfg = readCachedAdminConfig()
        val fallbackDays = if (normalizedScope == PLAN_SCOPE_BANGLISH) {
            currentAdminCfg.banglishDurationDays
        } else {
            currentAdminCfg.vipDurationDays
        }
        val resolvedDays = resolveDurationDays(
            explicitDays = purchasedDurationDays,
            planType = purchasedPlanType,
            fallbackDays = fallbackDays
        )
        unlockPlanByScope(
            planScope = normalizedScope,
            durationDays = resolvedDays,
            method = if (resolvedDays <= 0) "UPI_OFFICIAL" else "UPI_${resolvedDays}D",
            txnId = verifiedId
        )
        return UpiVerificationResult.Success(verifiedId)
    }

    private fun unlockPlanByScope(
        planScope: String,
        durationDays: Int,
        method: String,
        txnId: String
    ) {
        val normalizedScope = normalizePlanScope(planScope)
        if (normalizedScope == PLAN_SCOPE_BANGLISH) {
            if (durationDays <= 0) {
                markBanglishLifetimeUnlocked(method = method, txnId = txnId)
            } else {
                markBanglishTimedUnlocked(
                    durationMs = durationDays.toLong() * 86_400_000L,
                    method = method,
                    txnId = txnId
                )
            }
        } else {
            if (durationDays <= 0) {
                markLifetimeUnlocked(
                    method = method,
                    txnId = txnId,
                    writeLicenseFile = true
                )
            } else {
                val planCode = RemoteAdminConfig.durationDaysToPlanType(durationDays)
                val durationMs = durationDays.toLong() * 86_400_000L
                markTimedSubscriptionUnlocked(
                    planType = planCode,
                    durationMs = durationMs,
                    method = method,
                    txnId = txnId
                )
            }
        }
    }

    private fun isAuthenticTxnIdentifier(candidate: String): Boolean {
        if (candidate.length < 6) return false
        val upper = candidate.uppercase(Locale.US)
        if (upper in setOf("NULL", "NONE", "NA", "N/A", "FAILED", "CANCELLED", "000000", "123456")) {
            return false
        }
        return candidate.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    }

    private fun markBanglishTimedUnlocked(
        durationMs: Long,
        method: String,
        txnId: String
    ) {
        val now = currentServerOrUtcMs()
        val existingExpiry = prefs.getLong(KEY_BANGLISH_EXPIRY_TIMESTAMP_MS, 0L)
        val baseTime = maxOf(now, existingExpiry)
        val newExpiry = baseTime + durationMs
        val totalDurationSec = ((newExpiry - now) / 1000L).coerceAtLeast(durationMs / 1000L)

        editPrefsSafely {
            putLong(KEY_BANGLISH_EXPIRY_TIMESTAMP_MS, newExpiry)
            putLong(KEY_BANGLISH_TOTAL_DURATION_SEC, totalDurationSec)
            putString(KEY_UNLOCK_METHOD, method)
            putString(KEY_UNLOCK_TXN_ID, txnId)
            putLong(KEY_UNLOCKED_AT_MS, now)
        }

        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        syncDeviceToFirebase(state = updated, force = true)
    }

    private fun markBanglishLifetimeUnlocked(
        method: String,
        txnId: String
    ) {
        editPrefsSafely {
            putBoolean(KEY_IS_BANGLISH_LIFETIME_UNLOCKED, true)
            putString(KEY_UNLOCK_METHOD, method)
            putString(KEY_UNLOCK_TXN_ID, txnId)
            putLong(KEY_UNLOCKED_AT_MS, currentServerOrUtcMs())
        }

        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        syncDeviceToFirebase(state = updated, force = true)
    }

    private fun markTimedSubscriptionUnlocked(
        planType: String,
        durationMs: Long,
        method: String,
        txnId: String
    ) {
        val now = currentServerOrUtcMs()
        val existingExpiry = prefs.getLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, 0L)
        val baseTime = maxOf(now, existingExpiry)
        val newExpiry = baseTime + durationMs

        editPrefsSafely {
            putLong(KEY_VIP_EXPIRY_TIMESTAMP_MS, newExpiry)
            putString(KEY_ACTIVE_SUB_PLAN, planType)
            putString(KEY_UNLOCK_METHOD, method)
            putString(KEY_UNLOCK_TXN_ID, txnId)
            putLong(KEY_UNLOCKED_AT_MS, now)
        }

        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        syncDeviceToFirebase(state = updated, force = true)
    }

    private fun markLifetimeUnlocked(
        method: String,
        txnId: String,
        writeLicenseFile: Boolean
    ) {
        editPrefsSafely {
            putBoolean(KEY_IS_LIFETIME_UNLOCKED, true)
            putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
            putString(KEY_UNLOCK_METHOD, method)
            putString(KEY_UNLOCK_TXN_ID, txnId)
            putLong(KEY_UNLOCKED_AT_MS, currentServerOrUtcMs())
        }

        val updated = computeFastStateFromPrefsOnly()
        _licenseState.value = updated
        syncDeviceToFirebase(state = updated, force = true)

        if (writeLicenseFile) {
            ioScope.launch {
                try {
                    writeEncryptedLicenseFile(txnId)
                } catch (_: Throwable) {
                    // Best-effort persistent backup; SharedPreferences already unlocked
                }
            }
        }
    }

    private data class EncryptedLicenseRecord(
        val deviceId: String,
        val txnId: String,
        val signature: String
    )

    private fun deriveDeviceAesKey(targetDeviceId: String = deviceId): SecretKeySpec {
        val keyBytes = sha256Bytes("AIKB_AES256:$rawAndroidId:$targetDeviceId:$LICENSE_SECRET_SALT")
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptWithAes(plain: String, key: SecretKeySpec, prefix: String): String {
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return prefix + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptWithAes(encoded: String, key: SecretKeySpec, prefix: String): String? {
        val trimmed = encoded.trim()
        if (!trimmed.startsWith(prefix)) return null
        val b64 = trimmed.removePrefix(prefix)
        return runCatching {
            val combined = Base64.decode(b64, Base64.DEFAULT)
            if (combined.size <= 16) return null
            val iv = combined.copyOfRange(0, 16)
            val cipherText = combined.copyOfRange(16, combined.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun encryptLicensePayload(txnId: String): String {
        val sig = computeLicenseSignature(deviceId, txnId)
        val plain = "v=3\ndeviceId=$deviceId\ntxnId=$txnId\nsig=$sig\n"
        return encryptWithAes(plain, deriveDeviceAesKey(deviceId), "AIKB_ENC_V2:")
    }

    private fun decryptLicensePayload(encoded: String): EncryptedLicenseRecord? {
        val plain = decryptWithAes(encoded, deriveDeviceAesKey(deviceId), "AIKB_ENC_V2:")
            ?: decryptWithAes(encoded, deriveDeviceAesKey(legacyModelDeviceId), "AIKB_ENC_V2:")
            ?: decryptWithAes(encoded, deriveDeviceAesKey(legacySixCharDeviceId), "AIKB_ENC_V2:")
            ?: return null

        val dev = extractPlainField(plain, "deviceId")
        val txn = extractPlainField(plain, "txnId")
        val sig = extractPlainField(plain, "sig")
        if (dev.isEmpty() || sig.isEmpty()) return null
        return EncryptedLicenseRecord(deviceId = dev, txnId = txn, signature = sig)
    }

    private fun encryptTrialPayload(record: TrialRecord): String {
        val sig = computeTrialSignature(
            record.deviceId,
            record.firstInstallTimestampMs,
            record.maxSeenElapsedMs,
            record.expired
        )
        val plain = "v=1\n" +
            "deviceId=${record.deviceId}\n" +
            "first_install_timestamp=${record.firstInstallTimestampMs}\n" +
            "max_seen_elapsed_ms=${record.maxSeenElapsedMs}\n" +
            "last_seen_wall_ms=${record.lastSeenWallMs}\n" +
            "expired=${record.expired}\n" +
            "sig=$sig\n"
        return encryptWithAes(plain, deriveDeviceAesKey(deviceId), "AIKB_TRIAL_V1:")
    }

    private fun readEncryptedTrialRecord(): TrialRecord? {
        val raw = readRawDocumentText(HIDDEN_TRIAL_FILENAME) ?: return null
        val plain = decryptWithAes(raw, deriveDeviceAesKey(deviceId), "AIKB_TRIAL_V1:") ?: return null
        val dev = extractPlainField(plain, "deviceId")
        val firstInstall = extractPlainField(plain, "first_install_timestamp").toLongOrNull() ?: return null
        val maxElapsed = extractPlainField(plain, "max_seen_elapsed_ms").toLongOrNull() ?: 0L
        val lastWall = extractPlainField(plain, "last_seen_wall_ms").toLongOrNull() ?: firstInstall
        val expired = extractPlainField(plain, "expired").equals("true", ignoreCase = true)
        val sig = extractPlainField(plain, "sig")
        val expectedSig = computeTrialSignature(dev, firstInstall, maxElapsed, expired)
        if (!MessageDigest.isEqual(sig.toByteArray(Charsets.UTF_8), expectedSig.toByteArray(Charsets.UTF_8))) {
            return null
        }
        return TrialRecord(
            deviceId = dev,
            firstInstallTimestampMs = firstInstall,
            maxSeenElapsedMs = maxElapsed.coerceAtLeast(0L),
            lastSeenWallMs = lastWall,
            expired = expired
        )
    }

    private fun extractPlainField(raw: String, key: String): String {
        return raw.lineSequence()
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }

    private fun writeEncryptedLicenseFile(txnId: String) {
        val payloadBytes = runCatching {
            encryptLicensePayload(txnId).toByteArray(Charsets.UTF_8)
        }.getOrNull() ?: return
        writeDocumentBytes(HIDDEN_LICENSE_FILENAME, payloadBytes)
    }

    private fun writeEncryptedTrialFile(record: TrialRecord) {
        val payloadBytes = runCatching {
            encryptTrialPayload(record).toByteArray(Charsets.UTF_8)
        }.getOrNull() ?: return
        writeDocumentBytes(HIDDEN_TRIAL_FILENAME, payloadBytes)
    }

    /**
     * Persists a file in `Documents/<fileName>` using zero-permission Scoped Storage `MediaStore.Files`
     * on Android 10+ (API 29+) and public `Documents/` fallback on older devices, plus app-private
     * external mirror and guaranteed `context.filesDir` internal fallback.
     * Every storage step is isolated in explicit `try-catch` (`SecurityException` + `Throwable`)
     * so storage/OEM restrictions can never crash the app.
     */
    private fun writeDocumentBytes(fileName: String, payloadBytes: ByteArray) {
        // 1. Scoped Storage MediaStore (Android 10+ / API 29+): survives uninstall with zero permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "${Environment.DIRECTORY_DOCUMENTS}%")

                var existingUri: Uri? = null
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        existingUri = Uri.withAppendedPath(collection, id.toString())
                    }
                }

                val targetUri = existingUri ?: resolver.insert(
                    collection,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/")
                    }
                )

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri, "wt")?.use { out ->
                        out.write(payloadBytes)
                        out.flush()
                    }
                }
            } catch (_: SecurityException) {
                // Scoped Storage restricted on this device; safely fall through
            } catch (_: Throwable) {
                // Ignore OEM MediaStore errors; fall through to fallback mirrors
            }
        }

        // 2. Direct public Documents/<fileName> fallback when writable
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (publicDocs != null) {
                if (!publicDocs.exists()) publicDocs.mkdirs()
                File(publicDocs, fileName).writeBytes(payloadBytes)
            }
        } catch (_: SecurityException) {
            // Expected on Android 11+ without legacy storage permission
        } catch (_: Throwable) {
            // Ignore IO errors; fall through
        }

        // 3. App-private external Documents mirror
        try {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (appDocs != null) {
                if (!appDocs.exists()) appDocs.mkdirs()
                File(appDocs, fileName).writeBytes(payloadBytes)
            }
        } catch (_: SecurityException) {
            // Ignore
        } catch (_: Throwable) {
            // Ignore
        }

        // 4. Guaranteed internal context.filesDir fallback (always accessible, zero permissions)
        try {
            val internalDir = context.filesDir
            if (!internalDir.exists()) internalDir.mkdirs()
            File(internalDir, fileName).writeBytes(payloadBytes)
        } catch (_: Throwable) {
            // Never crash on storage write
        }
    }

    private fun readEncryptedLicenseRecord(fileName: String): EncryptedLicenseRecord? {
        val raw = readRawDocumentText(fileName) ?: return null
        return decryptLicensePayload(raw)
    }

    private fun readRawDocumentText(fileName: String): String? {
        // 1. Query MediaStore Documents/<fileName> on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fromMediaStore = try {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "${Environment.DIRECTORY_DOCUMENTS}%")
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    } else null
                }
            } catch (_: SecurityException) {
                null
            } catch (_: Throwable) {
                null
            }

            if (!fromMediaStore.isNullOrBlank()) return fromMediaStore
        }

        // 2. Direct public Documents/<fileName>
        val fromPublicFile = try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = if (publicDocs != null) File(publicDocs, fileName) else null
            if (file != null && file.exists() && file.canRead()) {
                file.readText(Charsets.UTF_8)
            } else null
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }

        if (!fromPublicFile.isNullOrBlank()) return fromPublicFile

        // 3. App-private external Documents mirror
        val fromExternalMirror = try {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = if (appDocs != null) File(appDocs, fileName) else null
            if (file != null && file.exists() && file.canRead()) file.readText(Charsets.UTF_8) else null
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }

        if (!fromExternalMirror.isNullOrBlank()) return fromExternalMirror

        // 4. Guaranteed internal context.filesDir fallback
        val fromInternalFiles = try {
            val file = File(context.filesDir, fileName)
            if (file.exists() && file.canRead()) file.readText(Charsets.UTF_8) else null
        } catch (_: Throwable) {
            null
        }

        return fromInternalFiles?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val MERCHANT_UPI_ID = "6296603748@okbizaxis"
        const val MERCHANT_NAME = "QUALITY FOODS"
        const val MERCHANT_MCC = "5411"
        const val AMOUNT_INR = "20.00"
        const val TRANSACTION_NOTE = "AI Keyboard Lifetime VIP"

        const val TRIAL_DURATION_SECONDS = 604_800L // Default 7-day (168 hours) Full VIP trial
        const val TRIAL_DURATION_MS = TRIAL_DURATION_SECONDS * 1000L
        const val WEEKLY_DURATION_MS = 7L * 86_400L * 1000L // 7-day Weekly VIP plan
        const val MONTHLY_DURATION_MS = 30L * 86_400L * 1000L // 30-day Monthly VIP plan

        const val STATUS_TRIAL = "trial"
        const val STATUS_WEEKLY_ACTIVE = "weekly_active"
        const val PLAN_SCOPE_VIP_FEATURES = "vip_features"
        const val PLAN_SCOPE_BANGLISH = "banglish"

        const val STATUS_MONTHLY_ACTIVE = "monthly_active"
        const val STATUS_CUSTOM_ACTIVE = "custom_active"
        const val STATUS_LIFETIME_VIP = "lifetime_vip"
        const val STATUS_ADMIN_FREE_VIP = "admin_free_vip"
        const val STATUS_EXPIRED = "expired"

        private const val HIDDEN_LICENSE_FILENAME = ".ai_keyboard_vip.lic"
        private const val LEGACY_LICENSE_FILENAME = ".rakib_official_vip.lic"
        private const val HIDDEN_TRIAL_FILENAME = ".ai_keyboard_trial.dat"
        private const val LICENSE_SECRET_SALT = "RAKIB_QUALITY_FOODS_2026_SECRET"
        private const val CURRENT_TRIAL_POLICY_VERSION = 4
        private const val CLOCK_ROLLBACK_TOLERANCE_MS = 120_000L
        private const val TRIAL_DISK_SYNC_INTERVAL_MS = 60_000L
        private const val PREF_CHECKPOINT_INTERVAL_MS = 10_000L
        private const val FIREBASE_SYNC_MIN_INTERVAL_MS = 45_000L

        private const val PREFS_NAME = "rakib_trial_license_prefs"
        private const val KEY_TRIAL_POLICY_VERSION = "trial_policy_version"
        private const val KEY_FIRST_INSTALL_TIMESTAMP_MS = "first_install_timestamp"
        private const val KEY_FIRST_LAUNCH_WALL_MS = "first_launch_timestamp_ms"
        private const val KEY_FIRST_LAUNCH_ELAPSED_MS = "first_launch_elapsed_ms"
        private const val KEY_MAX_SEEN_ELAPSED_MS = "max_seen_elapsed_ms"
        private const val KEY_LAST_SEEN_WALL_MS = "last_seen_wall_ms"
        private const val KEY_TRIAL_EXPIRED_STICKY = "trial_expired_sticky"
        private const val KEY_IS_LIFETIME_UNLOCKED = "is_lifetime_unlocked"
        private const val KEY_IS_BANGLISH_LIFETIME_UNLOCKED = "is_banglish_lifetime_unlocked"
        private const val KEY_BANGLISH_EXPIRY_TIMESTAMP_MS = "banglish_expiry_timestamp_ms"
        private const val KEY_BANGLISH_TOTAL_DURATION_SEC = "banglish_total_duration_sec"
        private const val KEY_LAST_APPROVED_PENDING_TXN = "last_approved_pending_txn"
        private const val KEY_ADMIN_GRANTED_VIP = "admin_granted_vip"
        private const val KEY_ADMIN_ALL_FREE_DEVICE = "admin_all_free_device"
        private const val KEY_ADMIN_GLOBAL_ALL_FREE = "admin_global_all_free"
        private const val KEY_DEVICE_CUSTOM_EXPIRY_MS = "device_custom_expiry_ms"
        private const val KEY_VIP_EXPIRY_TIMESTAMP_MS = "vip_expiry_timestamp_ms"
        private const val KEY_ACTIVE_SUB_PLAN = "active_sub_plan"
        private const val KEY_UNLOCK_METHOD = "unlock_method"
        private const val KEY_UNLOCK_TXN_ID = "unlock_txn_id"
        private const val KEY_UNLOCKED_AT_MS = "unlocked_at_ms"
        private const val KEY_ADMIN_FREE_TRIAL_DAYS = "admin_free_trial_days"
        private const val KEY_ADMIN_VIP_PRICE = "admin_vip_price"
        private const val KEY_ADMIN_VIP_DURATION_DAYS = "admin_vip_duration_days"
        private const val KEY_ADMIN_PLAN_MODE = "admin_plan_mode"
        private const val KEY_ADMIN_BANGLISH_PRICE = "admin_banglish_price"
        private const val KEY_ADMIN_BANGLISH_DURATION_DAYS = "admin_banglish_duration_days"
        private const val KEY_ADMIN_BANGLISH_PLAN_MODE = "admin_banglish_plan_mode"
        private const val KEY_ADMIN_BANGLISH_DAILY_PRICE = "admin_banglish_daily_price"
        private const val KEY_ADMIN_BANGLISH_WEEKLY_PRICE = "admin_banglish_weekly_price"
        private const val KEY_ADMIN_BANGLISH_MONTHLY_PRICE = "admin_banglish_monthly_price"
        private const val KEY_ADMIN_FREE_RGB_THEMES = "admin_free_rgb_themes"
        private const val KEY_ADMIN_FREE_BANGLISH = "admin_free_banglish"
        private const val KEY_ADMIN_FREE_CALL_BAR = "admin_free_call_bar"
        private const val KEY_ADMIN_FREE_RGB_STICKER = "admin_free_rgb_sticker"
        private const val KEY_ADMIN_FREE_STYLISH_FONTS = "admin_free_stylish_fonts"
        private const val KEY_ADMIN_FREE_AI_HUB = "admin_free_ai_hub"
        private const val KEY_ADMIN_BANNER1_URL = "admin_banner1_url"
        private const val KEY_ADMIN_BANNER1_IMAGE = "admin_banner1_image"
        private const val KEY_ADMIN_BANNER1_TITLE = "admin_banner1_title"
        private const val KEY_ADMIN_BANNER1_SUBTITLE = "admin_banner1_subtitle"
        private const val KEY_ADMIN_BANNER2_URL = "admin_banner2_url"
        private const val KEY_ADMIN_BANNER2_IMAGE = "admin_banner2_image"
        private const val KEY_ADMIN_BANNER2_TITLE = "admin_banner2_title"
        private const val KEY_ADMIN_BANNER2_SUBTITLE = "admin_banner2_subtitle"
        private const val KEY_DEVICE_ALLOW_RGB_THEMES = "device_allow_rgb_themes"
        private const val KEY_DEVICE_ALLOW_BANGLISH = "device_allow_banglish"
        private const val KEY_DEVICE_ALLOW_CALL_BAR = "device_allow_call_bar"
        private const val KEY_DEVICE_ALLOW_RGB_STICKER = "device_allow_rgb_sticker"
        private const val KEY_DEVICE_ALLOW_STYLISH_FONTS = "device_allow_stylish_fonts"
        private const val KEY_DEVICE_ALLOW_AI_HUB = "device_allow_ai_hub"

        @Volatile
        private var instance: TrialLicenseManager? = null

        fun getInstance(context: Context): TrialLicenseManager {
            return instance ?: synchronized(this) {
                instance ?: TrialLicenseManager(context.applicationContext).also { instance = it }
            }
        }

        private fun computeLicenseSignature(deviceId: String, txnId: String): String {
            val payload = "AIKB_SIG_V2:$deviceId:$txnId:$LICENSE_SECRET_SALT"
            return sha256Bytes(payload).joinToString("") { "%02x".format(it) }.take(32)
        }

        private fun computeTrialSignature(
            deviceId: String,
            firstInstallMs: Long,
            maxSeenElapsedMs: Long,
            expired: Boolean
        ): String {
            val payload = "AIKB_TRIAL_SIG:$deviceId:$firstInstallMs:$maxSeenElapsedMs:$expired:$LICENSE_SECRET_SALT"
            return sha256Bytes(payload).joinToString("") { "%02x".format(it) }.take(32)
        }

        private fun verifyLicenseSignature(deviceId: String, txnId: String, providedSig: String): Boolean {
            val expected = computeLicenseSignature(deviceId, txnId)
            return MessageDigest.isEqual(
                providedSig.toByteArray(Charsets.UTF_8),
                expected.toByteArray(Charsets.UTF_8)
            )
        }

        private fun sha256Bytes(input: String): ByteArray {
            return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        }

        @SuppressLint("HardwareIds")
        private fun readHardwareAndroidId(context: Context): String {
            return runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull().orEmpty().ifBlank { "AIKB_HW_${Build.BOARD}_${Build.HARDWARE}" }
        }

        /**
         * Permanent 8-char uppercase hex hardware ID prefixed with `RK-` (e.g., `RK-8A4F92C1`),
         * derived strictly from `Settings.Secure.ANDROID_ID` so it remains identical across
         * app uninstall and reinstall on the same device.
         */
        private fun deriveHardwareDeviceId(androidId: String): String {
            val digest = sha256Bytes("RKBD_HW_ANDROID_ID_V3:$androidId")
            val hex8 = digest.joinToString("") { "%02X".format(it) }.take(8)
            return "RK-$hex8"
        }

        /** Backward-compatible fallback key for older cached `.ai_keyboard_vip.lic` files. */
        private fun deriveLegacyModelDeviceId(androidId: String, model: String): String {
            val digest = sha256Bytes("RKBD_HW_V2:$androidId:$model")
            val hex8 = digest.joinToString("") { "%02X".format(it) }.take(8)
            return "RK-$hex8"
        }

        private fun deriveLegacySixCharDeviceId(androidId: String): String {
            val digest = sha256Bytes("RKBD_DEV_$androidId")
            return digest.joinToString("") { "%02X".format(it) }.take(6)
        }
    }
}
