package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class PromoBannerConfig(
    val banner1Url: String = "",
    val banner1Image: String = "",
    val banner1Title: String = DEFAULT_BANNER1_TITLE,
    val banner1Subtitle: String = DEFAULT_BANNER1_SUBTITLE,
    val banner2Url: String = "",
    val banner2Image: String = "",
    val banner2Title: String = DEFAULT_BANNER2_TITLE,
    val banner2Subtitle: String = DEFAULT_BANNER2_SUBTITLE
) {
    /** Prefers `banner1Image` (HTTP URL or Base64) and falls back to `banner1Url`. */
    val resolvedBanner1Image: String
        get() = banner1Image.trim().ifBlank { banner1Url.trim() }

    /** Prefers `banner2Image` (HTTP URL or Base64) and falls back to `banner2Url`. */
    val resolvedBanner2Image: String
        get() = banner2Image.trim().ifBlank { banner2Url.trim() }

    companion object {
        const val DEFAULT_BANNER1_TITLE = "AI Keyboard — Rakib official"
        const val DEFAULT_BANNER1_SUBTITLE = "Touch Spark RGB • 170+ AI Shayari • Voice Lock"
        const val DEFAULT_BANNER2_TITLE = "✨ Special Features"
        const val DEFAULT_BANNER2_SUBTITLE = "Custom Photo Themes, 106 Touch FX, Stylish Fonts & AI Hub"
    }
}

@Immutable
data class FreeFeatureOverrides(
    val rgbThemes: Boolean = false,
    val banglish: Boolean = false,
    val callBar: Boolean = false,
    val rgbSticker: Boolean = false,
    val stylishFonts: Boolean = false,
    val aiHub: Boolean = false
)

@Immutable
data class BanglishPlanOption(
    val id: String,
    val labelBn: String,
    val labelEn: String,
    val priceInr: Int,
    val durationDays: Int
)

@Immutable
data class PendingPaymentSnapshot(
    val deviceId: String = "",
    val planType: String = "",
    val baseAmount: Int = 0,
    val uniqueAmount: String = "",
    val durationDays: Int = 0,
    val txnRef: String = "",
    val status: String = "",
    val createdAtMs: Long = 0L
) {
    val isPending: Boolean
        get() = status.equals("pending", ignoreCase = true)

    val isApproved: Boolean
        get() = status.equals("approved", ignoreCase = true)
}

@Immutable
data class CloudThemeConfig(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String = "cyber",
    val imageUrl: String = "",
    val effectId: String = "quantum_vortex",
    val touchEffectId: String = "touch_spark",
    val paletteName: String = "VICE_CITY_NEON",
    val previewColors: List<Long> = listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF0C1022),
    val isFree: Boolean = false
)

@Immutable
data class CloudAnimationConfig(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val baseEffectId: String = "quantum_vortex",
    val paletteName: String = "SPECTRUM_PRISM",
    val isFree: Boolean = false
)

@Immutable
data class RemoteAdminConfig(
    val freeTrialDays: Int = DEFAULT_FREE_TRIAL_DAYS,
    val globalAllFree: Boolean = false,
    val vipPrice: Int = DEFAULT_VIP_PRICE,
    val vipDurationDays: Int = DEFAULT_VIP_DURATION_DAYS,
    val vipPlanType: String = PLAN_LIFETIME,
    val banglishPrice: Int = DEFAULT_BANGLISH_PRICE,
    val banglishDurationDays: Int = DEFAULT_BANGLISH_DURATION_DAYS,
    val banglishPlanType: String = PLAN_MONTHLY,
    val banglishDailyPrice: Int = DEFAULT_BANGLISH_DAILY_PRICE,
    val banglishWeeklyPrice: Int = DEFAULT_BANGLISH_WEEKLY_PRICE,
    val banglishMonthlyPrice: Int = DEFAULT_BANGLISH_PRICE,
    val banglishCustomPlans: List<BanglishPlanOption> = emptyList(),
    val globalFreeFeatures: FreeFeatureOverrides = FreeFeatureOverrides(),
    val banners: PromoBannerConfig = PromoBannerConfig(),
    val freeThemeIds: Set<String> = emptySet(),
    val cloudThemes: List<CloudThemeConfig> = emptyList(),
    val cloudAnimations: List<CloudAnimationConfig> = emptyList(),
    val offerTitle: String = "",
    val offerMessageBn: String = "",
    val offerMessageEn: String = "",
    val offerIntervalDays: Int = DEFAULT_OFFER_INTERVAL_DAYS
) {
    val trialDurationMs: Long
        get() = (if (freeTrialDays > 0) freeTrialDays else DEFAULT_FREE_TRIAL_DAYS)
            .coerceIn(1, 365).toLong() * 86_400_000L

    val isWeeklyPlan: Boolean
        get() = vipDurationDays == 7 || (vipDurationDays < 0 && vipPlanType.equals(PLAN_WEEKLY, ignoreCase = true))

    val isMonthlyPlan: Boolean
        get() = vipDurationDays == 30 || (vipDurationDays < 0 && vipPlanType.equals(PLAN_MONTHLY, ignoreCase = true))

    fun formattedVipPriceBn(): String = formatPriceByDaysBn(vipPrice, vipDurationDays, vipPlanType)

    fun formattedBanglishPriceBn(): String = formatPriceByDaysBn(banglishPrice, banglishDurationDays, banglishPlanType)

    /**
     * Resolves selectable Romanized Bengali subscription durations (Daily / Weekly / Monthly / Custom)
     * from Firebase `/admin_config` without duplicating days.
     */
    fun resolvedBanglishPlanOptions(): List<BanglishPlanOption> {
        if (banglishCustomPlans.isNotEmpty()) {
            return banglishCustomPlans
        }
        val byDays = linkedMapOf<Int, BanglishPlanOption>()
        byDays[1] = BanglishPlanOption(
            id = "daily",
            labelBn = "১ দিন",
            labelEn = "1 Day",
            priceInr = banglishDailyPrice.coerceAtLeast(1),
            durationDays = 1
        )
        byDays[7] = BanglishPlanOption(
            id = "weekly",
            labelBn = "৭ দিন",
            labelEn = "7 Days",
            priceInr = banglishWeeklyPrice.coerceAtLeast(1),
            durationDays = 7
        )
        byDays[30] = BanglishPlanOption(
            id = "monthly",
            labelBn = "৩০ দিন",
            labelEn = "30 Days",
            priceInr = banglishMonthlyPrice.coerceAtLeast(1),
            durationDays = 30
        )
        // Always honor admin's primary `banglishPrice` + `banglishDurationDays` (supports custom days or lifetime=0)
        val primaryDays = banglishDurationDays.coerceAtLeast(0)
        byDays[primaryDays] = BanglishPlanOption(
            id = when {
                primaryDays <= 0 -> "lifetime"
                primaryDays == 1 -> "daily"
                primaryDays == 7 -> "weekly"
                primaryDays == 30 -> "monthly"
                else -> "custom_${primaryDays}d"
            },
            labelBn = when {
                primaryDays <= 0 -> "লাইফটাইম"
                primaryDays == 1 -> "১ দিন"
                primaryDays == 7 -> "৭ দিন"
                primaryDays == 30 -> "৩০ দিন"
                else -> "${toBengaliDigits(primaryDays)} দিন"
            },
            labelEn = when {
                primaryDays <= 0 -> "Lifetime"
                primaryDays == 1 -> "1 Day"
                else -> "$primaryDays Days"
            },
            priceInr = banglishPrice.coerceAtLeast(1),
            durationDays = primaryDays
        )
        return byDays.values.toList()
    }

    companion object {
        const val DEFAULT_FREE_TRIAL_DAYS = 7
        const val DEFAULT_OFFER_INTERVAL_DAYS = 5
        const val DEFAULT_VIP_PRICE = 20
        const val DEFAULT_VIP_DURATION_DAYS = 0 // 0 = Lifetime
        const val DEFAULT_BANGLISH_PRICE = 20
        const val DEFAULT_BANGLISH_DURATION_DAYS = 30
        const val DEFAULT_BANGLISH_DAILY_PRICE = 5
        const val DEFAULT_BANGLISH_WEEKLY_PRICE = 10
        const val PLAN_LIFETIME = "lifetime"
        const val PLAN_MONTHLY = "monthly"
        const val PLAN_WEEKLY = "weekly"
        const val PLAN_CUSTOM = "custom"

        private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

        fun toBengaliDigits(value: Int): String {
            return value.toString().map { ch ->
                if (ch in '0'..'9') BN_DIGITS[ch - '0'] else ch
            }.joinToString("")
        }

        fun durationDaysToPlanType(days: Int): String = when {
            days <= 0 -> PLAN_LIFETIME
            days == 7 -> PLAN_WEEKLY
            days == 30 -> PLAN_MONTHLY
            else -> PLAN_CUSTOM
        }

        fun planTypeToDefaultDays(planType: String): Int = when {
            planType.equals(PLAN_WEEKLY, ignoreCase = true) -> 7
            planType.equals(PLAN_MONTHLY, ignoreCase = true) -> 30
            else -> 0
        }

        fun formatPriceByDaysBn(price: Int, durationDays: Int, fallbackPlanType: String = PLAN_LIFETIME): String {
            val safePrice = price.coerceAtLeast(1)
            val resolvedDays = if (durationDays >= 0) durationDays else planTypeToDefaultDays(fallbackPlanType)
            return when {
                resolvedDays <= 0 -> "₹$safePrice লাইফটাইম"
                resolvedDays == 7 -> "₹$safePrice/সপ্তাহ"
                resolvedDays == 30 -> "₹$safePrice/মাস"
                resolvedDays == 1 -> "₹$safePrice/১ দিন"
                else -> "₹$safePrice/${toBengaliDigits(resolvedDays)} দিন"
            }
        }

        fun formatPlanBadgeEn(price: Int, durationDays: Int): String {
            val safePrice = price.coerceAtLeast(1)
            return when {
                durationDays <= 0 -> "LIFETIME VIP • ONE-TIME (₹$safePrice)"
                durationDays == 7 -> "WEEKLY VIP • ₹$safePrice / 7 DAYS"
                durationDays == 30 -> "MONTHLY VIP • ₹$safePrice / 30 DAYS"
                durationDays == 1 -> "1-DAY VIP PASS • ₹$safePrice"
                else -> "${durationDays}-DAY VIP • ₹$safePrice"
            }
        }
    }
}

@Immutable
data class DeviceFeatureAllowMap(
    val rgbThemes: Boolean? = null,
    val banglish: Boolean? = null,
    val callBar: Boolean? = null,
    val rgbSticker: Boolean? = null,
    val stylishFonts: Boolean? = null,
    val aiHub: Boolean? = null
)

@Immutable
data class DeviceRemoteOverrides(
    val adminFreeVip: Boolean = false,
    val adminAllFree: Boolean = false,
    val customExpiryTimestampMs: Long = 0L,
    val isBlocked: Boolean = false,
    val allowedFeatures: DeviceFeatureAllowMap = DeviceFeatureAllowMap()
) {
    /** True when admin granted full free VIP access via either `adminAllFree` or `adminFreeVip` (unless device is blocked). */
    val hasFullAdminFreeAccess: Boolean
        get() = !isBlocked && (adminAllFree || adminFreeVip)
}

@Immutable
data class RemoteSecurityFlags(
    val reinstallDetected: Boolean = false,
    val reinstallCount: Int = 0,
    val clockRollbackDetected: Boolean = false,
    val clockRollbackCount: Int = 0,
    val localTamperDetected: Boolean = false,
    val tamperBlocked: Boolean = false
)

@Immutable
data class RemoteDeviceSnapshot(
    val exists: Boolean = false,
    val firstInstallTimestampMs: Long? = null,
    val firstInstallAtMs: Long? = null,
    val trialStartedAtMs: Long? = null,
    val trialExpiresAtMs: Long? = null,
    val trialUsed: Boolean? = null,
    val trialPolicyVersion: Int = 0,
    val vipStatus: String? = null,
    val explicitLifetime: Boolean? = null,
    val vipExpiryTimestampMs: Long = 0L,
    val banglishExpiryTimestampMs: Long = 0L,
    val isBlocked: Boolean = false,
    val overrides: DeviceRemoteOverrides = DeviceRemoteOverrides(),
    val securityFlags: RemoteSecurityFlags = RemoteSecurityFlags()
)

/**
 * Real-time Firebase Realtime Database synchronization manager for Remote Admin Control:
 * - Database URL: `https://rakib-ai-keyboard-default-rtdb.firebaseio.com/`
 * - Tracks `.info/serverTimeOffset` so trial expiry & VIP expiry use tamper-resistant server/UTC time.
 * - Listens to `/admin_config` (`freeTrialDays`, `globalAllFree`, `vipPrice`, `vipDurationDays`,
 *   `banglishPrice`, `banglishDurationDays`, daily/weekly/monthly/custom plans, `globalFreeFeatures`,
 *   and `banners` supporting both HTTP URLs and Base64 image strings).
 * - Tracks `/devices/{deviceId}` (`deviceId`, `deviceModel`, `firstInstallTimestamp` written ONCE
 *   atomically via `runTransaction(ServerValue.TIMESTAMP)`, `installDateText`, `lastActive`,
 *   `isOnline` with `onDisconnect()`, `vipStatus`, `vipExpiryTimestamp`, `banglishExpiryTimestamp`)
 *   and obeys `/devices/{deviceId}/overrides` (`adminAllFree`, `adminFreeVip`, `customExpiryTimestamp`,
 *   `allowedFeatures`) within 1 second.
 * - Submits and listens in real time to `/pending_payments/{deviceId}` for customer-safe discounted
 *   unique-paisa auto-payment verification (`status == "approved"`).
 */
class FirebaseRemoteManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val started = AtomicBoolean(false)
    private val firstInstallTransactionAttempted = AtomicBoolean(false)
    private val hasSyncedServerOffset = AtomicBoolean(false)
    private val serverTimeOffsetMs = AtomicLong(0L)
    private val sdkConnected = AtomicBoolean(false)
    private val restReachable = AtomicBoolean(false)
    private val restSyncInFlight = AtomicBoolean(false)
    private val networkCallbackRegistered = AtomicBoolean(false)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val _hasNetwork = MutableStateFlow(checkActiveNetwork())
    val hasNetwork: StateFlow<Boolean> = _hasNetwork.asStateFlow()
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _lastSyncTimestampMs = MutableStateFlow(0L)
    val lastSyncTimestampMs: StateFlow<Long> = _lastSyncTimestampMs.asStateFlow()

    @Volatile
    private var activeDeviceId: String = ""

    @Volatile
    private var activeAdminConfigCallback: ((RemoteAdminConfig) -> Unit)? = null

    @Volatile
    private var activeDeviceSnapshotCallback: ((RemoteDeviceSnapshot) -> Unit)? = null

    @Volatile
    private var activePendingPaymentCallback: ((PendingPaymentSnapshot?) -> Unit)? = null

    @Volatile
    private var activeHandshakeCallback: ((Boolean, Boolean) -> Unit)? = null

    @Volatile
    private var database: FirebaseDatabase? = null

    @Volatile
    private var adminConfigRef: DatabaseReference? = null

    @Volatile
    private var deviceRef: DatabaseReference? = null

    @Volatile
    private var pendingPaymentRef: DatabaseReference? = null

    @Volatile
    private var offsetRef: DatabaseReference? = null

    @Volatile
    private var connectedRef: DatabaseReference? = null

    @Volatile
    private var adminConfigListener: ValueEventListener? = null

    @Volatile
    private var deviceListener: ValueEventListener? = null

    @Volatile
    private var pendingPaymentListener: ValueEventListener? = null

    @Volatile
    private var offsetListener: ValueEventListener? = null

    @Volatile
    private var connectedListener: ValueEventListener? = null

    fun hasServerTimeOffset(): Boolean = hasSyncedServerOffset.get()

    fun getServerTimeOffsetMs(): Long = serverTimeOffsetMs.get()

    fun checkActiveNetwork(): Boolean {
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val active = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(active) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        }.getOrDefault(false).also { online ->
            _hasNetwork.value = online
            if (!online) {
                restReachable.set(false)
                _isConnected.value = false
            }
        }
    }

    private fun updateConnectedState() {
        val onlineNet = checkActiveNetwork()
        _isConnected.value = onlineNet && (sdkConnected.get() || restReachable.get())
    }

    private fun registerNetworkMonitorOnce() {
        if (!networkCallbackRegistered.compareAndSet(false, true)) return
        runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _hasNetwork.value = true
                        runCatching { database?.goOnline() }
                        triggerImmediateSync()
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        val hasNet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        _hasNetwork.value = hasNet
                        if (hasNet) {
                            runCatching { database?.goOnline() }
                            if (!_isConnected.value) {
                                triggerImmediateSync()
                            }
                        } else {
                            restReachable.set(false)
                            _isConnected.value = false
                        }
                    }

                    override fun onLost(network: Network) {
                        val stillOnline = checkActiveNetwork()
                        if (!stillOnline) {
                            sdkConnected.set(false)
                            restReachable.set(false)
                            _isConnected.value = false
                        }
                    }
                })
            }
        }.onFailure {
            networkCallbackRegistered.set(false)
        }
    }

    /** Current server/UTC timestamp in milliseconds (local wall clock corrected by verified server offset). */
    fun serverNowMs(): Long {
        val localNow = System.currentTimeMillis()
        val offset = serverTimeOffsetMs.get()
        val safeOffset = if (localNow >= MIN_VALID_EPOCH_MS && kotlin.math.abs(offset) > 3650L * 86_400_000L) {
            0L
        } else {
            offset
        }
        return localNow + safeOffset
    }

    private fun getDatabaseOrNull(): FirebaseDatabase? {
        database?.let { return it }
        return synchronized(this) {
            database?.let { return it }
            com.example.security.AntiTamperGuard.verifyOrTerminate(appContext)
            runCatching {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    FirebaseApp.initializeApp(appContext)
                }
                FirebaseDatabase.getInstance(DATABASE_URL).also { db ->
                    runCatching { db.setPersistenceEnabled(true) }
                    database = db
                }
            }.onFailure { err ->
                Log.w(TAG, "Cloud sync init deferred: ${err.message}")
            }.getOrNull()
        }
    }

    fun start(
        deviceId: String,
        onAdminConfigChanged: (RemoteAdminConfig) -> Unit,
        onDeviceSnapshotChanged: (RemoteDeviceSnapshot) -> Unit,
        onPendingPaymentChanged: (PendingPaymentSnapshot?) -> Unit = {},
        onHandshakeResult: (Boolean, Boolean) -> Unit = { _, _ -> }
    ) {
        if (deviceId.isBlank()) return
        activeDeviceId = deviceId
        activeAdminConfigCallback = onAdminConfigChanged
        activeDeviceSnapshotCallback = onDeviceSnapshotChanged
        activePendingPaymentCallback = onPendingPaymentChanged
        activeHandshakeCallback = onHandshakeResult

        registerNetworkMonitorOnce()
        checkActiveNetwork()

        if (!started.compareAndSet(false, true)) {
            runCatching { database?.goOnline() }
            triggerImmediateSync()
            return
        }

        triggerImmediateSync()

        val db = getDatabaseOrNull()
        if (db == null) {
            started.set(false)
            return
        }

        try {
            db.goOnline()

            // 1. Live server clock offset (.info/serverTimeOffset) to defeat phone date tampering
            val oRef = db.getReference(".info/serverTimeOffset")
            val oListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offset = snapshot.readLongOrNull() ?: 0L
                    serverTimeOffsetMs.set(offset)
                    hasSyncedServerOffset.set(true)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Clock offset listener cancelled")
                }
            }
            offsetRef = oRef
            offsetListener = oListener
            oRef.addValueEventListener(oListener)

            // 2. Live online presence (.info/connected -> /devices/{deviceId}/isOnline + onDisconnect)
            val devRef = db.getReference(NODE_DEVICES).child(deviceId)
            val cRef = db.getReference(".info/connected")
            val cListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.readBooleanOrNull() ?: false
                    sdkConnected.set(connected)
                    if (connected) {
                        _hasNetwork.value = true
                        _isConnected.value = true
                        runCatching {
                            devRef.onDisconnect().updateChildren(
                                mapOf(
                                    FIELD_IS_ONLINE to false,
                                    FIELD_LAST_ACTIVE to ServerValue.TIMESTAMP,
                                    FIELD_LAST_ACTIVE_AT to ServerValue.TIMESTAMP
                                )
                            )
                            devRef.updateChildren(
                                mapOf(
                                    FIELD_IS_ONLINE to true,
                                    FIELD_LAST_ACTIVE to ServerValue.TIMESTAMP,
                                    FIELD_LAST_ACTIVE_AT to ServerValue.TIMESTAMP
                                )
                            ).addOnSuccessListener {
                                _lastSyncTimestampMs.value = System.currentTimeMillis()
                            }
                        }
                    } else {
                        updateConnectedState()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    sdkConnected.set(false)
                    updateConnectedState()
                }
            }
            connectedRef = cRef
            connectedListener = cListener
            cRef.addValueEventListener(cListener)

            // 3. Real-time /admin_config listener (trial days, globalAllFree, custom-day pricing, banners)
            val cfgRef = db.getReference(NODE_ADMIN_CONFIG)
            val cfgListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (checkActiveNetwork() && (sdkConnected.get() || restReachable.get())) {
                        _lastSyncTimestampMs.value = System.currentTimeMillis()
                    }
                    val parsed = parseAdminConfig(snapshot)
                    onAdminConfigChanged(parsed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Remote config listener cancelled")
                }
            }
            adminConfigRef = cfgRef
            adminConfigListener = cfgListener
            cfgRef.addValueEventListener(cfgListener)

            // 4. Real-time /devices/{deviceId} listener + strict one-time atomic firstInstallTimestamp / firstInstallAt transaction
            val devListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val existingFirstInstall = normalizeEpochMs(
                        snapshot.child(FIELD_FIRST_INSTALL_TS).readLongOrNull()
                            ?: snapshot.child(FIELD_FIRST_INSTALL_AT).readLongOrNull()
                            ?: snapshot.child(FIELD_INSTALLED_AT).readLongOrNull()
                    )
                    if (existingFirstInstall <= 0L) {
                        ensureFirstInstallTimestampOnce(devRef)
                    } else {
                        firstInstallTransactionAttempted.set(true)
                        val updates = hashMapOf<String, Any>()
                        if (snapshot.child(FIELD_FIRST_INSTALL_TS).readLongOrNull()?.let { normalizeEpochMs(it) } != existingFirstInstall) {
                            updates[FIELD_FIRST_INSTALL_TS] = existingFirstInstall
                        }
                        if (snapshot.child(FIELD_FIRST_INSTALL_AT).readLongOrNull()?.let { normalizeEpochMs(it) } != existingFirstInstall) {
                            updates[FIELD_FIRST_INSTALL_AT] = existingFirstInstall
                        }
                        if (snapshot.child(FIELD_INSTALLED_AT).readLongOrNull()?.let { normalizeEpochMs(it) } != existingFirstInstall) {
                            updates[FIELD_INSTALLED_AT] = existingFirstInstall
                        }
                        if (snapshot.child(FIELD_INSTALL_DATE_TEXT).readStringOrNull().isNullOrBlank()) {
                            updates[FIELD_INSTALL_DATE_TEXT] = formatInstallDateText(existingFirstInstall)
                        }
                        if (updates.isNotEmpty()) {
                            runCatching { devRef.updateChildren(updates) }
                        }
                    }

                    if (checkActiveNetwork() && (sdkConnected.get() || restReachable.get())) {
                        _lastSyncTimestampMs.value = System.currentTimeMillis()
                    }
                    val parsed = parseDeviceSnapshot(snapshot)
                    onDeviceSnapshotChanged(parsed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Device state listener cancelled")
                }
            }
            deviceRef = devRef
            deviceListener = devListener
            devRef.addValueEventListener(devListener)

            // 5. Real-time /pending_payments/{deviceId} listener for instant auto-unlock upon admin/webhook approval
            val payRef = db.getReference(NODE_PENDING_PAYMENTS).child(deviceId)
            val payListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val parsed = parsePendingPayment(snapshot)
                    onPendingPaymentChanged(parsed)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Payment status listener cancelled")
                }
            }
            pendingPaymentRef = payRef
            pendingPaymentListener = payListener
            payRef.addValueEventListener(payListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Cloud listener setup deferred")
            started.set(false)
        }
    }

    /**
     * Performs a direct HTTPS handshake against the backend database so connectivity,
     * server UTC clock, admin config, and device trial/VIP status sync immediately on launch,
     * resume, network reconnect, and retry even when persistent WebSockets lag.
     */
    fun triggerImmediateSync() {
        val deviceId = activeDeviceId
        if (deviceId.isBlank()) return
        if (!checkActiveNetwork()) {
            restReachable.set(false)
            _isConnected.value = false
            activeHandshakeCallback?.invoke(false, false)
            return
        }
        if (!restSyncInFlight.compareAndSet(false, true)) return
        ioExecutor.execute {
            try {
                com.example.security.AntiTamperGuard.verifyOrTerminate(appContext)
                if (!checkActiveNetwork()) {
                    restReachable.set(false)
                    _isConnected.value = false
                    activeHandshakeCallback?.invoke(false, false)
                    return@execute
                }
                val adminRes = httpGetJson("${DATABASE_URL}${NODE_ADMIN_CONFIG}.json")
                val devRes = httpGetJson("${DATABASE_URL}${NODE_DEVICES}/$deviceId.json")
                val payRes = httpGetJson("${DATABASE_URL}${NODE_PENDING_PAYMENTS}/$deviceId.json")

                val serverHeaderMs = maxOf(adminRes.serverDateMs, devRes.serverDateMs)
                if (serverHeaderMs >= MIN_VALID_EPOCH_MS) {
                    serverTimeOffsetMs.set(serverHeaderMs - System.currentTimeMillis())
                    hasSyncedServerOffset.set(true)
                }

                if (adminRes.success && devRes.success) {
                    restReachable.set(true)
                    _hasNetwork.value = true
                    _isConnected.value = true
                    _lastSyncTimestampMs.value = System.currentTimeMillis()

                    val parsedConfig = parseAdminConfigJson(adminRes.json)
                    val parsedDevice = parseDeviceSnapshotJson(devRes.json)
                    val parsedPayment = if (payRes.success) parsePendingPaymentJson(payRes.json) else null

                    activeAdminConfigCallback?.invoke(parsedConfig)
                    activeDeviceSnapshotCallback?.invoke(parsedDevice)
                    if (payRes.success) {
                        activePendingPaymentCallback?.invoke(parsedPayment)
                    }
                    activeHandshakeCallback?.invoke(true, true)
                } else {
                    restReachable.set(false)
                    updateConnectedState()
                    activeHandshakeCallback?.invoke(false, checkActiveNetwork())
                }
            } catch (_: Throwable) {
                restReachable.set(false)
                updateConnectedState()
                activeHandshakeCallback?.invoke(false, checkActiveNetwork())
            } finally {
                restSyncInFlight.set(false)
            }
        }
    }

    private data class RestGetResult(
        val success: Boolean,
        val json: JSONObject?,
        val serverDateMs: Long
    )

    private fun httpGetJson(urlStr: String): RestGetResult {
        var conn: HttpsURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 7_000
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val serverDate = conn.date.takeIf { it >= MIN_VALID_EPOCH_MS } ?: 0L
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
                val obj = if (body.isEmpty() || body == "null") null else runCatching { JSONObject(body) }.getOrNull()
                RestGetResult(success = true, json = obj, serverDateMs = serverDate)
            } else {
                RestGetResult(success = false, json = null, serverDateMs = serverDate)
            }
        } catch (_: Throwable) {
            RestGetResult(success = false, json = null, serverDateMs = 0L)
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPatchJsonAsync(path: String, payload: Map<String, Any>) {
        if (!checkActiveNetwork()) return
        ioExecutor.execute {
            var conn: HttpsURLConnection? = null
            try {
                val url = URL("${DATABASE_URL}$path.json")
                conn = (url.openConnection() as HttpsURLConnection).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 7_000
                    readTimeout = 7_000
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val jsonBody = mapToJsonObject(payload, serverNowMs()).toString()
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    restReachable.set(true)
                    _hasNetwork.value = true
                    _isConnected.value = true
                    _lastSyncTimestampMs.value = System.currentTimeMillis()
                }
            } catch (_: Throwable) {
                // SDK offline persistence / next retry handles transient transport failures
            } finally {
                conn?.disconnect()
            }
        }
    }

    private fun mapToJsonObject(map: Map<String, Any>, resolvedServerNowMs: Long): JSONObject {
        val root = JSONObject()
        map.forEach { (rawKey, rawVal) ->
            val value = if (rawVal == ServerValue.TIMESTAMP) resolvedServerNowMs else rawVal
            if (rawKey.contains('/')) {
                val parts = rawKey.split('/').filter { it.isNotEmpty() }
                var cursor = root
                for (i in 0 until parts.lastIndex) {
                    val segment = parts[i]
                    val next = cursor.optJSONObject(segment) ?: JSONObject().also { cursor.put(segment, it) }
                    cursor = next
                }
                cursor.put(parts.last(), value)
            } else {
                root.put(rawKey, value)
            }
        }
        return root
    }

    /**
     * Records a customer-safe discounted unique-paisa pending payment at `/pending_payments/{deviceId}`.
     * When the admin panel or payment webhook sets `status = "approved"`, the real-time listener unlocks
     * the exact purchased plan automatically without requiring manual intervention.
     */
    fun submitPendingPayment(
        deviceId: String,
        planType: String,
        baseAmount: Int,
        uniqueAmountFormatted: String,
        durationDays: Int,
        txnRef: String
    ) {
        if (deviceId.isBlank()) return
        val payload = hashMapOf<String, Any>(
            FIELD_DEVICE_ID to deviceId,
            FIELD_DEVICE_MODEL to Build.MODEL.orEmpty().trim().ifBlank { "UNKNOWN_MODEL" },
            FIELD_PLAN_TYPE to planType,
            FIELD_BASE_AMOUNT to baseAmount,
            FIELD_UNIQUE_AMOUNT to uniqueAmountFormatted,
            FIELD_UNIQUE_AMOUNT_NUM to (uniqueAmountFormatted.toDoubleOrNull() ?: baseAmount.toDouble()),
            FIELD_DURATION_DAYS to durationDays,
            FIELD_TXN_REF to txnRef,
            FIELD_STATUS to STATUS_PAYMENT_PENDING,
            FIELD_CREATED_AT to ServerValue.TIMESTAMP
        )
        val db = getDatabaseOrNull()
        if (db != null) {
            runCatching {
                val payRef = db.getReference(NODE_PENDING_PAYMENTS).child(deviceId)
                payRef.setValue(payload)
            }.onFailure {
                Log.w(TAG, "Failed to submit pending payment via SDK")
            }
        }
        if (checkActiveNetwork() && (!sdkConnected.get() || db == null)) {
            httpPatchJsonAsync("$NODE_PENDING_PAYMENTS/$deviceId", payload)
        }
    }

    /**
     * Atomically writes `firstInstallTimestamp = ServerValue.TIMESTAMP` ONLY IF the node does not
     * already exist on the server. Even if the user uninstalls and reinstalls the app on the same
     * phone, an existing server `firstInstallTimestamp` is NEVER overwritten or extended.
     */
    private fun ensureFirstInstallTimestampOnce(devRef: DatabaseReference) {
        if (!firstInstallTransactionAttempted.compareAndSet(false, true)) return
        runCatching {
            devRef.child(FIELD_FIRST_INSTALL_TS).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val existing = normalizeEpochMs(
                        when (val v = currentData.value) {
                            is Number -> v.toLong()
                            is String -> v.trim().toLongOrNull() ?: 0L
                            else -> 0L
                        }
                    )
                    if (existing > 0L) {
                        // Already recorded validly on server: preserve strictly!
                        return Transaction.abort()
                    }
                    currentData.value = ServerValue.TIMESTAMP
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        firstInstallTransactionAttempted.set(false)
                        return
                    }
                    val ts = normalizeEpochMs(currentData?.readLongOrNull())
                    if (ts > 0L) {
                        runCatching {
                            devRef.updateChildren(
                                mapOf(
                                    FIELD_FIRST_INSTALL_AT to ts,
                                    FIELD_INSTALLED_AT to ts,
                                    FIELD_INSTALL_DATE_TEXT to formatInstallDateText(ts)
                                )
                            )
                        }
                    }
                }
            })
        }.onFailure {
            firstInstallTransactionAttempted.set(false)
        }
    }

    fun syncDeviceStatus(
        deviceId: String,
        vipStatus: String,
        vipExpiryTimestampMs: Long,
        firstInstallTimestampMs: Long = 0L,
        banglishExpiryTimestampMs: Long = 0L,
        trialStartedAtMs: Long = 0L,
        trialExpiresAtMs: Long = 0L,
        trialPolicyVersion: Int = 5,
        overrideLegacyTrialTimestamps: Boolean = false,
        writeFirstInstallFields: Boolean = false,
        allowTrialAndVipStatusWrite: Boolean = true,
        trialUsed: Boolean = true,
        isLifetime: Boolean = false,
        isBlocked: Boolean = false,
        reinstallDetected: Boolean = false,
        reinstallCount: Int = 0,
        clockRollbackDetected: Boolean = false,
        clockRollbackCount: Int = 0,
        localTamperDetected: Boolean = false,
        integrityStatus: String = "verified"
    ) {
        if (deviceId.isBlank()) return
        val db = getDatabaseOrNull()
        runCatching {
            val model = Build.MODEL.orEmpty().trim().ifBlank { "UNKNOWN_MODEL" }
            val manufacturer = normalizeManufacturerName(Build.MANUFACTURER)
            val cleanDeviceName = buildCleanDeviceName(Build.MANUFACTURER, model)
            val safeFirstInstall = normalizeEpochMs(firstInstallTimestampMs)
            val safeTrialStart = normalizeEpochMs(trialStartedAtMs).takeIf { it > 0L } ?: safeFirstInstall
            val safeTrialExpiry = normalizeEpochMs(trialExpiresAtMs)
            val safeVipExpiry = normalizeEpochMs(vipExpiryTimestampMs)
            val safeBanglishExpiry = normalizeEpochMs(banglishExpiryTimestampMs)
            val certDigest = com.example.security.AntiTamperGuard.getSigningCertSha256Prefix(appContext)

            val payload = hashMapOf<String, Any>(
                FIELD_RK_ID to deviceId,
                FIELD_DEVICE_ID to deviceId,
                FIELD_DEVICE_NAME to cleanDeviceName,
                FIELD_DEVICE_MODEL to model,
                FIELD_MANUFACTURER to manufacturer,
                FIELD_LAST_ACTIVE to ServerValue.TIMESTAMP,
                FIELD_LAST_ACTIVE_AT to ServerValue.TIMESTAMP,
                FIELD_IS_ONLINE to true,
                FIELD_APP_VERSION to getAppVersionName(),
                "$FIELD_SECURITY_FLAGS/hardwareBoundId" to deviceId,
                "$FIELD_SECURITY_FLAGS/appPackage" to appContext.packageName.orEmpty(),
                "$FIELD_SECURITY_FLAGS/signingCertHash" to certDigest,
                "$FIELD_SECURITY_FLAGS/lastVerifiedAt" to ServerValue.TIMESTAMP
            )

            // Only update trial/VIP status & timestamps once server state has been reconciled (or on verified purchase),
            // preventing a reinstall launch from overwriting earlier server trial/expiry records before the listener fires.
            if (allowTrialAndVipStatusWrite) {
                payload[FIELD_VIP_STATUS] = vipStatus
                payload[FIELD_VIP_EXPIRY_TS] = safeVipExpiry
                payload[FIELD_BANGLISH_EXPIRY_TS] = safeBanglishExpiry
                payload[FIELD_TRIAL_POLICY_VERSION] = trialPolicyVersion
                payload[FIELD_TRIAL_USED] = trialUsed
                payload[FIELD_LIFETIME] = isLifetime
                payload[FIELD_BLOCKED] = isBlocked
                payload["$FIELD_SECURITY_FLAGS/reinstallDetected"] = reinstallDetected
                payload["$FIELD_SECURITY_FLAGS/reinstallCount"] = reinstallCount.coerceAtLeast(0)
                payload["$FIELD_SECURITY_FLAGS/clockRollbackDetected"] = clockRollbackDetected
                payload["$FIELD_SECURITY_FLAGS/clockRollbackCount"] = clockRollbackCount.coerceAtLeast(0)
                payload["$FIELD_SECURITY_FLAGS/localTamperDetected"] = localTamperDetected
                payload["$FIELD_SECURITY_FLAGS/integrityStatus"] = integrityStatus

                if (safeFirstInstall > 0L && (writeFirstInstallFields || overrideLegacyTrialTimestamps)) {
                    payload[FIELD_FIRST_INSTALL_TS] = safeFirstInstall
                    payload[FIELD_FIRST_INSTALL_AT] = safeFirstInstall
                    payload[FIELD_INSTALLED_AT] = safeFirstInstall
                    payload[FIELD_INSTALL_DATE_TEXT] = formatInstallDateText(safeFirstInstall)
                }
                if (safeTrialStart > 0L) {
                    payload[FIELD_TRIAL_STARTED_AT] = safeTrialStart
                }
                if (safeTrialExpiry > 0L) {
                    payload[FIELD_TRIAL_EXPIRES_AT] = safeTrialExpiry
                }
            }

            if (db != null) {
                val devRef = db.getReference(NODE_DEVICES).child(deviceId)
                devRef.updateChildren(payload)
                    .addOnSuccessListener {
                        sdkConnected.set(true)
                        _hasNetwork.value = true
                        _isConnected.value = true
                        _lastSyncTimestampMs.value = System.currentTimeMillis()
                    }
                ensureFirstInstallTimestampOnce(devRef)
            }
            if (checkActiveNetwork() && (!sdkConnected.get() || writeFirstInstallFields || db == null)) {
                httpPatchJsonAsync("$NODE_DEVICES/$deviceId", payload)
            } else if (_isConnected.value) {
                _lastSyncTimestampMs.value = System.currentTimeMillis()
            }
        }.onFailure {
            if (checkActiveNetwork() && allowTrialAndVipStatusWrite) {
                runCatching {
                    val model = Build.MODEL.orEmpty().trim().ifBlank { "UNKNOWN_MODEL" }
                    val fallback = hashMapOf<String, Any>(
                        FIELD_RK_ID to deviceId,
                        FIELD_DEVICE_ID to deviceId,
                        FIELD_DEVICE_NAME to buildCleanDeviceName(Build.MANUFACTURER, model),
                        FIELD_DEVICE_MODEL to model,
                        FIELD_IS_ONLINE to true,
                        FIELD_VIP_STATUS to vipStatus,
                        FIELD_VIP_EXPIRY_TS to normalizeEpochMs(vipExpiryTimestampMs),
                        FIELD_BANGLISH_EXPIRY_TS to normalizeEpochMs(banglishExpiryTimestampMs),
                        FIELD_LIFETIME to isLifetime,
                        FIELD_BLOCKED to isBlocked
                    )
                    httpPatchJsonAsync("$NODE_DEVICES/$deviceId", fallback)
                }
            }
        }
    }

    fun getAppVersionLabel(): String = getAppVersionName()

    private fun getAppVersionName(): String {
        return runCatching {
            val pm = appContext.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(appContext.packageName, 0)
            info.versionName?.trim()?.takeIf { it.isNotEmpty() } ?: "1.0"
        }.getOrDefault("1.0")
    }

    private fun parseAdminConfig(snapshot: DataSnapshot): RemoteAdminConfig {
        if (!snapshot.exists()) return RemoteAdminConfig()
        val rawTrialDays = snapshot.child(FIELD_FREE_TRIAL_DAYS).readIntOrNull()
            ?: snapshot.child(FIELD_DEFAULT_TRIAL_DAYS).readIntOrNull()
        val freeTrialDays = rawTrialDays
            ?.takeIf { it > 0 }
            ?.coerceIn(1, 365)
            ?: RemoteAdminConfig.DEFAULT_FREE_TRIAL_DAYS
        val globalAllFree = snapshot.child(FIELD_GLOBAL_ALL_FREE).readBooleanOrNull() ?: false
        val vipPrice = snapshot.child(FIELD_VIP_PRICE)
            .readIntOrNull()
            ?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_VIP_PRICE

        val rawVipPlanType = snapshot.child(FIELD_VIP_PLAN_TYPE)
            .readStringOrNull()
            ?.lowercase()
        val rawVipDays = snapshot.child(FIELD_VIP_DURATION_DAYS).readIntOrNull()
        val vipDurationDays = when {
            rawVipDays != null -> rawVipDays.coerceIn(0, 3650)
            rawVipPlanType != null -> RemoteAdminConfig.planTypeToDefaultDays(rawVipPlanType)
            else -> RemoteAdminConfig.DEFAULT_VIP_DURATION_DAYS
        }
        val vipPlanType = RemoteAdminConfig.durationDaysToPlanType(vipDurationDays)

        val banglishPrice = snapshot.child(FIELD_BANGLISH_PRICE)
            .readIntOrNull()
            ?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_PRICE
        val rawBanglishPlanType = snapshot.child(FIELD_BANGLISH_PLAN_TYPE)
            .readStringOrNull()
            ?.lowercase()
        val rawBanglishDays = snapshot.child(FIELD_BANGLISH_DURATION_DAYS).readIntOrNull()
        val banglishDurationDays = when {
            rawBanglishDays != null -> rawBanglishDays.coerceIn(0, 3650)
            rawBanglishPlanType != null -> RemoteAdminConfig.planTypeToDefaultDays(rawBanglishPlanType)
            else -> RemoteAdminConfig.DEFAULT_BANGLISH_DURATION_DAYS
        }
        val banglishPlanType = RemoteAdminConfig.durationDaysToPlanType(banglishDurationDays)

        val banglishDailyPrice = snapshot.child(FIELD_BANGLISH_DAILY_PRICE)
            .readIntOrNull()
            ?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_DAILY_PRICE
        val banglishWeeklyPrice = snapshot.child(FIELD_BANGLISH_WEEKLY_PRICE)
            .readIntOrNull()
            ?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_WEEKLY_PRICE
        val banglishMonthlyPrice = snapshot.child(FIELD_BANGLISH_MONTHLY_PRICE)
            .readIntOrNull()
            ?.coerceAtLeast(1)
            ?: banglishPrice

        val customBanglishPlans = parseCustomBanglishPlans(snapshot.child(FIELD_BANGLISH_PLANS))

        val freeNode = snapshot.child(FIELD_GLOBAL_FREE_FEATURES)
        val globalFree = FreeFeatureOverrides(
            rgbThemes = freeNode.child(FEAT_RGB_THEMES).readBooleanOrNull() ?: false,
            banglish = freeNode.child(FEAT_BANGLISH).readBooleanOrNull() ?: false,
            callBar = freeNode.child(FEAT_CALL_BAR).readBooleanOrNull() ?: false,
            rgbSticker = freeNode.child(FEAT_RGB_STICKER).readBooleanOrNull() ?: false,
            stylishFonts = freeNode.child(FEAT_STYLISH_FONTS).readBooleanOrNull() ?: false,
            aiHub = freeNode.child(FEAT_AI_HUB).readBooleanOrNull() ?: false
        )

        val bannersNode = snapshot.child(FIELD_BANNERS)
        val banners = PromoBannerConfig(
            banner1Url = bannersNode.child(FIELD_BANNER1_URL).readStringOrNull().orEmpty(),
            banner1Image = bannersNode.child(FIELD_BANNER1_IMAGE).readStringOrNull().orEmpty(),
            banner1Title = bannersNode.child(FIELD_BANNER1_TITLE).readStringOrNull()
                ?: PromoBannerConfig.DEFAULT_BANNER1_TITLE,
            banner1Subtitle = bannersNode.child(FIELD_BANNER1_SUBTITLE).readStringOrNull()
                ?: PromoBannerConfig.DEFAULT_BANNER1_SUBTITLE,
            banner2Url = bannersNode.child(FIELD_BANNER2_URL).readStringOrNull().orEmpty(),
            banner2Image = bannersNode.child(FIELD_BANNER2_IMAGE).readStringOrNull().orEmpty(),
            banner2Title = bannersNode.child(FIELD_BANNER2_TITLE).readStringOrNull()
                ?: PromoBannerConfig.DEFAULT_BANNER2_TITLE,
            banner2Subtitle = bannersNode.child(FIELD_BANNER2_SUBTITLE).readStringOrNull()
                ?: PromoBannerConfig.DEFAULT_BANNER2_SUBTITLE
        )

        val freeThemeIds = parseFreeThemeIds(snapshot.child(FIELD_FREE_THEME_IDS))
        val cloudThemes = parseCloudThemes(snapshot.child(FIELD_CLOUD_THEMES))
        val cloudAnimations = parseCloudAnimations(snapshot.child(FIELD_CLOUD_ANIMATIONS))
        val offerNode = snapshot.child(FIELD_OFFER)
        val offerTitle = offerNode.child("title").readStringOrNull()
            ?: snapshot.child(FIELD_OFFER_TITLE).readStringOrNull().orEmpty()
        val offerMessageBn = offerNode.child("messageBn").readStringOrNull()
            ?: snapshot.child(FIELD_OFFER_MESSAGE_BN).readStringOrNull().orEmpty()
        val offerMessageEn = offerNode.child("messageEn").readStringOrNull()
            ?: snapshot.child(FIELD_OFFER_MESSAGE_EN).readStringOrNull().orEmpty()
        val offerIntervalDays = (offerNode.child("intervalDays").readIntOrNull()
            ?: snapshot.child(FIELD_OFFER_INTERVAL_DAYS).readIntOrNull()
            ?: RemoteAdminConfig.DEFAULT_OFFER_INTERVAL_DAYS).coerceIn(4, 7)

        return RemoteAdminConfig(
            freeTrialDays = freeTrialDays,
            globalAllFree = globalAllFree,
            vipPrice = vipPrice,
            vipDurationDays = vipDurationDays,
            vipPlanType = vipPlanType,
            banglishPrice = banglishPrice,
            banglishDurationDays = banglishDurationDays,
            banglishPlanType = banglishPlanType,
            banglishDailyPrice = banglishDailyPrice,
            banglishWeeklyPrice = banglishWeeklyPrice,
            banglishMonthlyPrice = banglishMonthlyPrice,
            banglishCustomPlans = customBanglishPlans,
            globalFreeFeatures = globalFree,
            banners = banners,
            freeThemeIds = freeThemeIds,
            cloudThemes = cloudThemes,
            cloudAnimations = cloudAnimations,
            offerTitle = offerTitle,
            offerMessageBn = offerMessageBn,
            offerMessageEn = offerMessageEn,
            offerIntervalDays = offerIntervalDays
        )
    }

    private fun parseFreeThemeIds(node: DataSnapshot): Set<String> {
        if (!node.exists()) return emptySet()
        val result = linkedSetOf<String>()
        if (node.childrenCount > 0) {
            node.children.forEach { child ->
                val rawVal = child.value
                when (rawVal) {
                    is Boolean -> if (rawVal) child.key?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
                    is String -> {
                        val trimmed = rawVal.trim()
                        if (trimmed.equals("true", ignoreCase = true) || trimmed == "1") {
                            child.key?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
                        } else if (trimmed.isNotEmpty() && !trimmed.equals("false", ignoreCase = true)) {
                            result.add(trimmed)
                        }
                    }
                    is Number -> if (rawVal.toInt() != 0) child.key?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
                }
            }
        } else {
            val csv = node.readStringOrNull().orEmpty()
            csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { result.add(it) }
        }
        return result
    }

    private fun parseCloudThemes(node: DataSnapshot): List<CloudThemeConfig> {
        if (!node.exists()) return emptyList()
        val list = mutableListOf<CloudThemeConfig>()
        node.children.forEach { child ->
            val enabled = child.child("enabled").readBooleanOrNull() ?: true
            if (!enabled) return@forEach
            val id = child.child("id").readStringOrNull() ?: child.key?.trim().orEmpty()
            val title = child.child("title").readStringOrNull() ?: return@forEach
            if (id.isBlank()) return@forEach
            val colors = mutableListOf<Long>()
            child.child("colors").children.forEach { cChild ->
                val hex = cChild.readStringOrNull()?.removePrefix("#")
                val parsed = hex?.toLongOrNull(16)
                if (parsed != null) {
                    colors.add(if (hex.length <= 6) (0xFF000000L or parsed) else parsed)
                }
            }
            list += CloudThemeConfig(
                id = id,
                title = title,
                subtitle = child.child("subtitle").readStringOrNull().orEmpty(),
                category = child.child("category").readStringOrNull() ?: "cyber",
                imageUrl = child.child("imageUrl").readStringOrNull()
                    ?: child.child("image").readStringOrNull().orEmpty(),
                effectId = child.child("effectId").readStringOrNull()
                    ?: child.child("effect").readStringOrNull()
                    ?: "quantum_vortex",
                touchEffectId = child.child("touchEffectId").readStringOrNull()
                    ?: child.child("touchEffect").readStringOrNull()
                    ?: "touch_spark",
                paletteName = child.child("palette").readStringOrNull() ?: "VICE_CITY_NEON",
                previewColors = colors.ifEmpty { listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF0C1022) },
                isFree = child.child("isFree").readBooleanOrNull() ?: false
            )
        }
        return list
    }

    private fun parseCloudAnimations(node: DataSnapshot): List<CloudAnimationConfig> {
        if (!node.exists()) return emptyList()
        val list = mutableListOf<CloudAnimationConfig>()
        node.children.forEach { child ->
            val enabled = child.child("enabled").readBooleanOrNull() ?: true
            if (!enabled) return@forEach
            val id = child.child("id").readStringOrNull() ?: child.key?.trim().orEmpty()
            val title = child.child("title").readStringOrNull() ?: return@forEach
            if (id.isBlank()) return@forEach
            list += CloudAnimationConfig(
                id = id,
                title = title,
                subtitle = child.child("subtitle").readStringOrNull().orEmpty(),
                baseEffectId = child.child("baseEffectId").readStringOrNull()
                    ?: child.child("effect").readStringOrNull()
                    ?: "quantum_vortex",
                paletteName = child.child("palette").readStringOrNull() ?: "SPECTRUM_PRISM",
                isFree = child.child("isFree").readBooleanOrNull() ?: false
            )
        }
        return list
    }

    private fun parseCustomBanglishPlans(plansNode: DataSnapshot): List<BanglishPlanOption> {
        if (!plansNode.exists()) return emptyList()
        val result = mutableListOf<BanglishPlanOption>()
        plansNode.children.forEach { child ->
            val enabled = child.child("enabled").readBooleanOrNull() ?: true
            if (!enabled) return@forEach
            val price = child.child("price").readIntOrNull()?.coerceAtLeast(1) ?: return@forEach
            val days = child.child("days").readIntOrNull()?.coerceIn(0, 3650)
                ?: child.child("durationDays").readIntOrNull()?.coerceIn(0, 3650)
                ?: return@forEach
            val id = child.key.orEmpty().ifBlank { "plan_${days}d" }
            val labelBn = child.child("labelBn").readStringOrNull()
                ?: child.child("label").readStringOrNull()
                ?: when {
                    days <= 0 -> "লাইফটাইম"
                    days == 1 -> "১ দিন"
                    days == 7 -> "৭ দিন"
                    days == 30 -> "৩০ দিন"
                    else -> "${RemoteAdminConfig.toBengaliDigits(days)} দিন"
                }
            val labelEn = child.child("labelEn").readStringOrNull()
                ?: when {
                    days <= 0 -> "Lifetime"
                    days == 1 -> "1 Day"
                    else -> "$days Days"
                }
            result += BanglishPlanOption(
                id = id,
                labelBn = labelBn,
                labelEn = labelEn,
                priceInr = price,
                durationDays = days
            )
        }
        return result.sortedBy { if (it.durationDays <= 0) Int.MAX_VALUE else it.durationDays }
    }

    private fun parseDeviceSnapshot(snapshot: DataSnapshot): RemoteDeviceSnapshot {
        if (!snapshot.exists()) return RemoteDeviceSnapshot(exists = false)
        val firstInstallAtTs = normalizeEpochMs(
            snapshot.child(FIELD_FIRST_INSTALL_AT).readLongOrNull()
        ).takeIf { it > 0L }
        val firstInstallTs = normalizeEpochMs(
            snapshot.child(FIELD_FIRST_INSTALL_TS).readLongOrNull()
                ?: snapshot.child(FIELD_FIRST_INSTALL_AT).readLongOrNull()
                ?: snapshot.child(FIELD_INSTALLED_AT).readLongOrNull()
        ).takeIf { it > 0L }
        val trialStartedAtTs = normalizeEpochMs(
            snapshot.child(FIELD_TRIAL_STARTED_AT).readLongOrNull()
        ).takeIf { it > 0L }
        val trialExpiresAtTs = normalizeEpochMs(
            snapshot.child(FIELD_TRIAL_EXPIRES_AT).readLongOrNull()
        ).takeIf { it > 0L }
        val trialUsed = snapshot.child(FIELD_TRIAL_USED).readBooleanOrNull()
        val trialPolicyVersion = snapshot.child(FIELD_TRIAL_POLICY_VERSION).readIntOrNull() ?: 0
        val vipStatus = snapshot.child(FIELD_VIP_STATUS).readStringOrNull()
        val explicitLifetime = snapshot.child(FIELD_LIFETIME).readBooleanOrNull()
        val vipExpiryTs = normalizeEpochMs(snapshot.child(FIELD_VIP_EXPIRY_TS).readLongOrNull())
        val banglishExpiryTs = normalizeEpochMs(snapshot.child(FIELD_BANGLISH_EXPIRY_TS).readLongOrNull())

        val securityNode = snapshot.child(FIELD_SECURITY_FLAGS)
        val securityFlags = RemoteSecurityFlags(
            reinstallDetected = securityNode.child("reinstallDetected").readBooleanOrNull() ?: false,
            reinstallCount = (securityNode.child("reinstallCount").readIntOrNull() ?: 0).coerceAtLeast(0),
            clockRollbackDetected = securityNode.child("clockRollbackDetected").readBooleanOrNull() ?: false,
            clockRollbackCount = (securityNode.child("clockRollbackCount").readIntOrNull() ?: 0).coerceAtLeast(0),
            localTamperDetected = securityNode.child("localTamperDetected").readBooleanOrNull() ?: false,
            tamperBlocked = securityNode.child("tamperBlocked").readBooleanOrNull() ?: false
        )

        val overridesNode = snapshot.child(FIELD_OVERRIDES)
        val isBlocked = overridesNode.child(FIELD_IS_BLOCKED).readBooleanOrNull()
            ?: overridesNode.child(FIELD_BLOCKED).readBooleanOrNull()
            ?: overridesNode.child(FIELD_DEVICE_LOCKED).readBooleanOrNull()
            ?: snapshot.child(FIELD_IS_BLOCKED).readBooleanOrNull()
            ?: snapshot.child(FIELD_BLOCKED).readBooleanOrNull()
            ?: securityFlags.tamperBlocked
        val adminFreeVip = overridesNode.child(FIELD_ADMIN_FREE_VIP).readBooleanOrNull() ?: false
        val adminAllFree = overridesNode.child(FIELD_ADMIN_ALL_FREE).readBooleanOrNull() ?: false
        val customExpiryTs = normalizeEpochMs(overridesNode.child(FIELD_CUSTOM_EXPIRY_TS).readLongOrNull())
        val allowedNode = overridesNode.child(FIELD_ALLOWED_FEATURES)
        val allowed = DeviceFeatureAllowMap(
            rgbThemes = allowedNode.child(FEAT_RGB_THEMES).readBooleanOrNull(),
            banglish = allowedNode.child(FEAT_BANGLISH).readBooleanOrNull(),
            callBar = allowedNode.child(FEAT_CALL_BAR).readBooleanOrNull(),
            rgbSticker = allowedNode.child(FEAT_RGB_STICKER).readBooleanOrNull(),
            stylishFonts = allowedNode.child(FEAT_STYLISH_FONTS).readBooleanOrNull(),
            aiHub = allowedNode.child(FEAT_AI_HUB).readBooleanOrNull()
        )

        return RemoteDeviceSnapshot(
            exists = true,
            firstInstallTimestampMs = firstInstallTs,
            firstInstallAtMs = firstInstallAtTs,
            trialStartedAtMs = trialStartedAtTs,
            trialExpiresAtMs = trialExpiresAtTs,
            trialUsed = trialUsed,
            trialPolicyVersion = trialPolicyVersion,
            vipStatus = vipStatus,
            explicitLifetime = explicitLifetime,
            vipExpiryTimestampMs = vipExpiryTs,
            banglishExpiryTimestampMs = banglishExpiryTs,
            isBlocked = isBlocked,
            overrides = DeviceRemoteOverrides(
                adminFreeVip = adminFreeVip,
                adminAllFree = adminAllFree,
                customExpiryTimestampMs = customExpiryTs,
                isBlocked = isBlocked,
                allowedFeatures = allowed
            ),
            securityFlags = securityFlags
        )
    }

    private fun parsePendingPayment(snapshot: DataSnapshot): PendingPaymentSnapshot? {
        if (!snapshot.exists()) return null
        val status = snapshot.child(FIELD_STATUS).readStringOrNull().orEmpty()
        val planType = snapshot.child(FIELD_PLAN_TYPE).readStringOrNull().orEmpty()
        val uniqueAmount = snapshot.child(FIELD_UNIQUE_AMOUNT).readStringOrNull().orEmpty()
        if (status.isBlank() && uniqueAmount.isBlank()) return null
        return PendingPaymentSnapshot(
            deviceId = snapshot.child(FIELD_DEVICE_ID).readStringOrNull().orEmpty(),
            planType = planType,
            baseAmount = snapshot.child(FIELD_BASE_AMOUNT).readIntOrNull() ?: 0,
            uniqueAmount = uniqueAmount,
            durationDays = snapshot.child(FIELD_DURATION_DAYS).readIntOrNull() ?: 0,
            txnRef = snapshot.child(FIELD_TXN_REF).readStringOrNull().orEmpty(),
            status = status,
            createdAtMs = snapshot.child(FIELD_CREATED_AT).readLongOrNull() ?: 0L
        )
    }

    private fun parseAdminConfigJson(root: JSONObject?): RemoteAdminConfig {
        if (root == null) return RemoteAdminConfig()
        val rawTrialDays = root.optIntOrNull(FIELD_FREE_TRIAL_DAYS)
            ?: root.optIntOrNull(FIELD_DEFAULT_TRIAL_DAYS)
        val freeTrialDays = rawTrialDays?.takeIf { it > 0 }?.coerceIn(1, 365)
            ?: RemoteAdminConfig.DEFAULT_FREE_TRIAL_DAYS
        val globalAllFree = root.optBooleanOrNull(FIELD_GLOBAL_ALL_FREE) ?: false
        val vipPrice = root.optIntOrNull(FIELD_VIP_PRICE)?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_VIP_PRICE
        val rawVipPlanType = root.optStringOrNull(FIELD_VIP_PLAN_TYPE)?.lowercase()
        val rawVipDays = root.optIntOrNull(FIELD_VIP_DURATION_DAYS)
        val vipDurationDays = when {
            rawVipDays != null -> rawVipDays.coerceIn(0, 3650)
            rawVipPlanType != null -> RemoteAdminConfig.planTypeToDefaultDays(rawVipPlanType)
            else -> RemoteAdminConfig.DEFAULT_VIP_DURATION_DAYS
        }
        val vipPlanType = RemoteAdminConfig.durationDaysToPlanType(vipDurationDays)

        val banglishPrice = root.optIntOrNull(FIELD_BANGLISH_PRICE)?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_PRICE
        val rawBanglishPlanType = root.optStringOrNull(FIELD_BANGLISH_PLAN_TYPE)?.lowercase()
        val rawBanglishDays = root.optIntOrNull(FIELD_BANGLISH_DURATION_DAYS)
        val banglishDurationDays = when {
            rawBanglishDays != null -> rawBanglishDays.coerceIn(0, 3650)
            rawBanglishPlanType != null -> RemoteAdminConfig.planTypeToDefaultDays(rawBanglishPlanType)
            else -> RemoteAdminConfig.DEFAULT_BANGLISH_DURATION_DAYS
        }
        val banglishPlanType = RemoteAdminConfig.durationDaysToPlanType(banglishDurationDays)
        val banglishDailyPrice = root.optIntOrNull(FIELD_BANGLISH_DAILY_PRICE)?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_DAILY_PRICE
        val banglishWeeklyPrice = root.optIntOrNull(FIELD_BANGLISH_WEEKLY_PRICE)?.coerceAtLeast(1)
            ?: RemoteAdminConfig.DEFAULT_BANGLISH_WEEKLY_PRICE
        val banglishMonthlyPrice = root.optIntOrNull(FIELD_BANGLISH_MONTHLY_PRICE)?.coerceAtLeast(1)
            ?: banglishPrice

        val customBanglishPlans = parseCustomBanglishPlansJson(root.opt(FIELD_BANGLISH_PLANS))
        val freeNode = root.optJSONObject(FIELD_GLOBAL_FREE_FEATURES)
        val globalFree = FreeFeatureOverrides(
            rgbThemes = freeNode?.optBooleanOrNull(FEAT_RGB_THEMES) ?: false,
            banglish = freeNode?.optBooleanOrNull(FEAT_BANGLISH) ?: false,
            callBar = freeNode?.optBooleanOrNull(FEAT_CALL_BAR) ?: false,
            rgbSticker = freeNode?.optBooleanOrNull(FEAT_RGB_STICKER) ?: false,
            stylishFonts = freeNode?.optBooleanOrNull(FEAT_STYLISH_FONTS) ?: false,
            aiHub = freeNode?.optBooleanOrNull(FEAT_AI_HUB) ?: false
        )
        val bannersNode = root.optJSONObject(FIELD_BANNERS)
        val banners = PromoBannerConfig(
            banner1Url = bannersNode?.optStringOrNull(FIELD_BANNER1_URL).orEmpty(),
            banner1Image = bannersNode?.optStringOrNull(FIELD_BANNER1_IMAGE).orEmpty(),
            banner1Title = bannersNode?.optStringOrNull(FIELD_BANNER1_TITLE)
                ?: PromoBannerConfig.DEFAULT_BANNER1_TITLE,
            banner1Subtitle = bannersNode?.optStringOrNull(FIELD_BANNER1_SUBTITLE)
                ?: PromoBannerConfig.DEFAULT_BANNER1_SUBTITLE,
            banner2Url = bannersNode?.optStringOrNull(FIELD_BANNER2_URL).orEmpty(),
            banner2Image = bannersNode?.optStringOrNull(FIELD_BANNER2_IMAGE).orEmpty(),
            banner2Title = bannersNode?.optStringOrNull(FIELD_BANNER2_TITLE)
                ?: PromoBannerConfig.DEFAULT_BANNER2_TITLE,
            banner2Subtitle = bannersNode?.optStringOrNull(FIELD_BANNER2_SUBTITLE)
                ?: PromoBannerConfig.DEFAULT_BANNER2_SUBTITLE
        )
        val freeThemeIds = parseFreeThemeIdsJson(root.opt(FIELD_FREE_THEME_IDS))
        val cloudThemes = parseCloudThemesJson(root.opt(FIELD_CLOUD_THEMES))
        val cloudAnimations = parseCloudAnimationsJson(root.opt(FIELD_CLOUD_ANIMATIONS))
        val offerNode = root.optJSONObject(FIELD_OFFER)
        val offerTitle = offerNode?.optStringOrNull("title")
            ?: root.optStringOrNull(FIELD_OFFER_TITLE).orEmpty()
        val offerMessageBn = offerNode?.optStringOrNull("messageBn")
            ?: root.optStringOrNull(FIELD_OFFER_MESSAGE_BN).orEmpty()
        val offerMessageEn = offerNode?.optStringOrNull("messageEn")
            ?: root.optStringOrNull(FIELD_OFFER_MESSAGE_EN).orEmpty()
        val offerIntervalDays = (offerNode?.optIntOrNull("intervalDays")
            ?: root.optIntOrNull(FIELD_OFFER_INTERVAL_DAYS)
            ?: RemoteAdminConfig.DEFAULT_OFFER_INTERVAL_DAYS).coerceIn(4, 7)

        return RemoteAdminConfig(
            freeTrialDays = freeTrialDays,
            globalAllFree = globalAllFree,
            vipPrice = vipPrice,
            vipDurationDays = vipDurationDays,
            vipPlanType = vipPlanType,
            banglishPrice = banglishPrice,
            banglishDurationDays = banglishDurationDays,
            banglishPlanType = banglishPlanType,
            banglishDailyPrice = banglishDailyPrice,
            banglishWeeklyPrice = banglishWeeklyPrice,
            banglishMonthlyPrice = banglishMonthlyPrice,
            banglishCustomPlans = customBanglishPlans,
            globalFreeFeatures = globalFree,
            banners = banners,
            freeThemeIds = freeThemeIds,
            cloudThemes = cloudThemes,
            cloudAnimations = cloudAnimations,
            offerTitle = offerTitle,
            offerMessageBn = offerMessageBn,
            offerMessageEn = offerMessageEn,
            offerIntervalDays = offerIntervalDays
        )
    }

    private fun parseFreeThemeIdsJson(raw: Any?): Set<String> {
        if (raw == null || raw == JSONObject.NULL) return emptySet()
        val result = linkedSetOf<String>()
        when (raw) {
            is JSONObject -> {
                raw.keys().forEach { key ->
                    val v = raw.opt(key)
                    when (v) {
                        is Boolean -> if (v && key.isNotBlank()) result.add(key.trim())
                        is Number -> if (v.toInt() != 0 && key.isNotBlank()) result.add(key.trim())
                        is String -> {
                            val trimmed = v.trim()
                            if (trimmed.equals("true", true) || trimmed == "1") {
                                if (key.isNotBlank()) result.add(key.trim())
                            } else if (trimmed.isNotEmpty() && !trimmed.equals("false", true)) {
                                result.add(trimmed)
                            }
                        }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until raw.length()) {
                    val s = raw.optString(i)?.trim().orEmpty()
                    if (s.isNotEmpty() && s != "null") result.add(s)
                }
            }
            is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { result.add(it) }
        }
        return result
    }

    private fun parseCloudThemesJson(raw: Any?): List<CloudThemeConfig> {
        val list = mutableListOf<CloudThemeConfig>()
        val entries: List<Pair<String, JSONObject>> = when (raw) {
            is JSONObject -> raw.keys().asSequence().mapNotNull { k ->
                raw.optJSONObject(k)?.let { k to it }
            }.toList()
            is JSONArray -> (0 until raw.length()).mapNotNull { idx ->
                raw.optJSONObject(idx)?.let { idx.toString() to it }
            }
            else -> emptyList()
        }
        entries.forEach { (key, obj) ->
            if (obj.optBooleanOrNull("enabled") == false) return@forEach
            val id = obj.optStringOrNull("id") ?: key.trim()
            val title = obj.optStringOrNull("title") ?: return@forEach
            if (id.isBlank()) return@forEach
            val colors = mutableListOf<Long>()
            val cArr = obj.optJSONArray("colors")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val hex = cArr.optString(i)?.trim()?.removePrefix("#")
                    val parsed = hex?.toLongOrNull(16)
                    if (parsed != null) {
                        colors.add(if (hex.length <= 6) (0xFF000000L or parsed) else parsed)
                    }
                }
            }
            list += CloudThemeConfig(
                id = id,
                title = title,
                subtitle = obj.optStringOrNull("subtitle").orEmpty(),
                category = obj.optStringOrNull("category") ?: "cyber",
                imageUrl = obj.optStringOrNull("imageUrl") ?: obj.optStringOrNull("image").orEmpty(),
                effectId = obj.optStringOrNull("effectId") ?: obj.optStringOrNull("effect") ?: "quantum_vortex",
                touchEffectId = obj.optStringOrNull("touchEffectId") ?: obj.optStringOrNull("touchEffect") ?: "touch_spark",
                paletteName = obj.optStringOrNull("palette") ?: "VICE_CITY_NEON",
                previewColors = colors.ifEmpty { listOf(0xFF00E5FF, 0xFFFF2A85, 0xFF0C1022) },
                isFree = obj.optBooleanOrNull("isFree") ?: false
            )
        }
        return list
    }

    private fun parseCloudAnimationsJson(raw: Any?): List<CloudAnimationConfig> {
        val list = mutableListOf<CloudAnimationConfig>()
        val entries: List<Pair<String, JSONObject>> = when (raw) {
            is JSONObject -> raw.keys().asSequence().mapNotNull { k ->
                raw.optJSONObject(k)?.let { k to it }
            }.toList()
            is JSONArray -> (0 until raw.length()).mapNotNull { idx ->
                raw.optJSONObject(idx)?.let { idx.toString() to it }
            }
            else -> emptyList()
        }
        entries.forEach { (key, obj) ->
            if (obj.optBooleanOrNull("enabled") == false) return@forEach
            val id = obj.optStringOrNull("id") ?: key.trim()
            val title = obj.optStringOrNull("title") ?: return@forEach
            if (id.isBlank()) return@forEach
            list += CloudAnimationConfig(
                id = id,
                title = title,
                subtitle = obj.optStringOrNull("subtitle").orEmpty(),
                baseEffectId = obj.optStringOrNull("baseEffectId") ?: obj.optStringOrNull("effect") ?: "quantum_vortex",
                paletteName = obj.optStringOrNull("palette") ?: "SPECTRUM_PRISM",
                isFree = obj.optBooleanOrNull("isFree") ?: false
            )
        }
        return list
    }

    private fun parseCustomBanglishPlansJson(raw: Any?): List<BanglishPlanOption> {
        val result = mutableListOf<BanglishPlanOption>()
        val entries: List<Pair<String, JSONObject>> = when (raw) {
            is JSONObject -> raw.keys().asSequence().mapNotNull { k ->
                raw.optJSONObject(k)?.let { k to it }
            }.toList()
            is JSONArray -> (0 until raw.length()).mapNotNull { idx ->
                raw.optJSONObject(idx)?.let { idx.toString() to it }
            }
            else -> emptyList()
        }
        entries.forEach { (key, obj) ->
            if (obj.optBooleanOrNull("enabled") == false) return@forEach
            val price = obj.optIntOrNull("price")?.coerceAtLeast(1) ?: return@forEach
            val days = obj.optIntOrNull("days")?.coerceIn(0, 3650)
                ?: obj.optIntOrNull("durationDays")?.coerceIn(0, 3650)
                ?: return@forEach
            val id = key.ifBlank { "plan_${days}d" }
            val labelBn = obj.optStringOrNull("labelBn") ?: obj.optStringOrNull("label") ?: when {
                days <= 0 -> "লাইফটাইম"
                days == 1 -> "১ দিন"
                days == 7 -> "৭ দিন"
                days == 30 -> "৩০ দিন"
                else -> "${RemoteAdminConfig.toBengaliDigits(days)} দিন"
            }
            val labelEn = obj.optStringOrNull("labelEn") ?: when {
                days <= 0 -> "Lifetime"
                days == 1 -> "1 Day"
                else -> "$days Days"
            }
            result += BanglishPlanOption(id = id, labelBn = labelBn, labelEn = labelEn, priceInr = price, durationDays = days)
        }
        return result.sortedBy { if (it.durationDays <= 0) Int.MAX_VALUE else it.durationDays }
    }

    private fun parseDeviceSnapshotJson(root: JSONObject?): RemoteDeviceSnapshot {
        if (root == null) return RemoteDeviceSnapshot(exists = false)
        val firstInstallAtTs = normalizeEpochMs(root.optLongOrNull(FIELD_FIRST_INSTALL_AT)).takeIf { it > 0L }
        val firstInstallTs = normalizeEpochMs(
            root.optLongOrNull(FIELD_FIRST_INSTALL_TS)
                ?: root.optLongOrNull(FIELD_FIRST_INSTALL_AT)
                ?: root.optLongOrNull(FIELD_INSTALLED_AT)
        ).takeIf { it > 0L }
        val trialStartedAtTs = normalizeEpochMs(root.optLongOrNull(FIELD_TRIAL_STARTED_AT)).takeIf { it > 0L }
        val trialExpiresAtTs = normalizeEpochMs(root.optLongOrNull(FIELD_TRIAL_EXPIRES_AT)).takeIf { it > 0L }
        val trialUsed = root.optBooleanOrNull(FIELD_TRIAL_USED)
        val trialPolicyVersion = root.optIntOrNull(FIELD_TRIAL_POLICY_VERSION) ?: 0
        val vipStatus = root.optStringOrNull(FIELD_VIP_STATUS)
        val explicitLifetime = root.optBooleanOrNull(FIELD_LIFETIME)
        val vipExpiryTs = normalizeEpochMs(root.optLongOrNull(FIELD_VIP_EXPIRY_TS))
        val banglishExpiryTs = normalizeEpochMs(root.optLongOrNull(FIELD_BANGLISH_EXPIRY_TS))

        val securityNode = root.optJSONObject(FIELD_SECURITY_FLAGS)
        val securityFlags = RemoteSecurityFlags(
            reinstallDetected = securityNode?.optBooleanOrNull("reinstallDetected") ?: false,
            reinstallCount = (securityNode?.optIntOrNull("reinstallCount") ?: 0).coerceAtLeast(0),
            clockRollbackDetected = securityNode?.optBooleanOrNull("clockRollbackDetected") ?: false,
            clockRollbackCount = (securityNode?.optIntOrNull("clockRollbackCount") ?: 0).coerceAtLeast(0),
            localTamperDetected = securityNode?.optBooleanOrNull("localTamperDetected") ?: false,
            tamperBlocked = securityNode?.optBooleanOrNull("tamperBlocked") ?: false
        )

        val overridesNode = root.optJSONObject(FIELD_OVERRIDES)
        val isBlocked = overridesNode?.optBooleanOrNull(FIELD_IS_BLOCKED)
            ?: overridesNode?.optBooleanOrNull(FIELD_BLOCKED)
            ?: overridesNode?.optBooleanOrNull(FIELD_DEVICE_LOCKED)
            ?: root.optBooleanOrNull(FIELD_IS_BLOCKED)
            ?: root.optBooleanOrNull(FIELD_BLOCKED)
            ?: securityFlags.tamperBlocked
        val adminFreeVip = overridesNode?.optBooleanOrNull(FIELD_ADMIN_FREE_VIP) ?: false
        val adminAllFree = overridesNode?.optBooleanOrNull(FIELD_ADMIN_ALL_FREE) ?: false
        val customExpiryTs = normalizeEpochMs(overridesNode?.optLongOrNull(FIELD_CUSTOM_EXPIRY_TS))
        val allowedNode = overridesNode?.optJSONObject(FIELD_ALLOWED_FEATURES)
        val allowed = DeviceFeatureAllowMap(
            rgbThemes = allowedNode?.optBooleanOrNull(FEAT_RGB_THEMES),
            banglish = allowedNode?.optBooleanOrNull(FEAT_BANGLISH),
            callBar = allowedNode?.optBooleanOrNull(FEAT_CALL_BAR),
            rgbSticker = allowedNode?.optBooleanOrNull(FEAT_RGB_STICKER),
            stylishFonts = allowedNode?.optBooleanOrNull(FEAT_STYLISH_FONTS),
            aiHub = allowedNode?.optBooleanOrNull(FEAT_AI_HUB)
        )

        return RemoteDeviceSnapshot(
            exists = true,
            firstInstallTimestampMs = firstInstallTs,
            firstInstallAtMs = firstInstallAtTs,
            trialStartedAtMs = trialStartedAtTs,
            trialExpiresAtMs = trialExpiresAtTs,
            trialUsed = trialUsed,
            trialPolicyVersion = trialPolicyVersion,
            vipStatus = vipStatus,
            explicitLifetime = explicitLifetime,
            vipExpiryTimestampMs = vipExpiryTs,
            banglishExpiryTimestampMs = banglishExpiryTs,
            isBlocked = isBlocked,
            overrides = DeviceRemoteOverrides(
                adminFreeVip = adminFreeVip,
                adminAllFree = adminAllFree,
                customExpiryTimestampMs = customExpiryTs,
                isBlocked = isBlocked,
                allowedFeatures = allowed
            ),
            securityFlags = securityFlags
        )
    }

    private fun parsePendingPaymentJson(root: JSONObject?): PendingPaymentSnapshot? {
        if (root == null) return null
        val status = root.optStringOrNull(FIELD_STATUS).orEmpty()
        val planType = root.optStringOrNull(FIELD_PLAN_TYPE).orEmpty()
        val uniqueAmount = root.optStringOrNull(FIELD_UNIQUE_AMOUNT).orEmpty()
        if (status.isBlank() && uniqueAmount.isBlank()) return null
        return PendingPaymentSnapshot(
            deviceId = root.optStringOrNull(FIELD_DEVICE_ID).orEmpty(),
            planType = planType,
            baseAmount = root.optIntOrNull(FIELD_BASE_AMOUNT) ?: 0,
            uniqueAmount = uniqueAmount,
            durationDays = root.optIntOrNull(FIELD_DURATION_DAYS) ?: 0,
            txnRef = root.optStringOrNull(FIELD_TXN_REF).orEmpty(),
            status = status,
            createdAtMs = root.optLongOrNull(FIELD_CREATED_AT) ?: 0L
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toInt()
            is String -> v.trim().toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toLong()
            is String -> v.trim().toLongOrNull()
            else -> null
        }
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> when (v.trim().lowercase()) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
            else -> null
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val raw = opt(key)?.toString()?.trim()
        return raw?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun DataSnapshot.readIntOrNull(): Int? {
        return when (val v = value) {
            is Number -> v.toInt()
            is String -> v.trim().toIntOrNull()
            else -> null
        }
    }

    private fun DataSnapshot.readLongOrNull(): Long? {
        return when (val v = value) {
            is Number -> v.toLong()
            is String -> v.trim().toLongOrNull()
            else -> null
        }
    }

    private fun DataSnapshot.readBooleanOrNull(): Boolean? {
        return when (val v = value) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> when (v.trim().lowercase()) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
            else -> null
        }
    }

    private fun DataSnapshot.readStringOrNull(): String? {
        val raw = value?.toString()?.trim()
        return raw?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TAG = "CloudRemoteSync"
        const val DATABASE_URL = "https://rakib-ai-keyboard-default-rtdb.firebaseio.com/"

        private const val NODE_ADMIN_CONFIG = "admin_config"
        private const val NODE_DEVICES = "devices"
        private const val NODE_PENDING_PAYMENTS = "pending_payments"

        private const val FIELD_FREE_TRIAL_DAYS = "freeTrialDays"
        private const val FIELD_DEFAULT_TRIAL_DAYS = "defaultTrialDays"
        private const val FIELD_GLOBAL_ALL_FREE = "globalAllFree"
        private const val FIELD_OFFER = "offer"
        private const val FIELD_OFFER_TITLE = "offerTitle"
        private const val FIELD_OFFER_MESSAGE_BN = "offerMessageBn"
        private const val FIELD_OFFER_MESSAGE_EN = "offerMessageEn"
        private const val FIELD_OFFER_INTERVAL_DAYS = "offerIntervalDays"
        private const val FIELD_VIP_PRICE = "vipPrice"
        private const val FIELD_VIP_DURATION_DAYS = "vipDurationDays"
        private const val FIELD_VIP_PLAN_TYPE = "vipPlanType"
        private const val FIELD_BANGLISH_PRICE = "banglishPrice"
        private const val FIELD_BANGLISH_DURATION_DAYS = "banglishDurationDays"
        private const val FIELD_BANGLISH_PLAN_TYPE = "banglishPlanType"
        private const val FIELD_BANGLISH_DAILY_PRICE = "banglishDailyPrice"
        private const val FIELD_BANGLISH_WEEKLY_PRICE = "banglishWeeklyPrice"
        private const val FIELD_BANGLISH_MONTHLY_PRICE = "banglishMonthlyPrice"
        private const val FIELD_BANGLISH_PLANS = "banglishPlans"
        private const val FIELD_GLOBAL_FREE_FEATURES = "globalFreeFeatures"
        private const val FIELD_BANNERS = "banners"
        private const val FIELD_BANNER1_URL = "banner1Url"
        private const val FIELD_BANNER1_IMAGE = "banner1Image"
        private const val FIELD_BANNER1_TITLE = "banner1Title"
        private const val FIELD_BANNER1_SUBTITLE = "banner1Subtitle"
        private const val FIELD_BANNER2_URL = "banner2Url"
        private const val FIELD_BANNER2_IMAGE = "banner2Image"
        private const val FIELD_BANNER2_TITLE = "banner2Title"
        private const val FIELD_BANNER2_SUBTITLE = "banner2Subtitle"
        private const val FIELD_FREE_THEME_IDS = "freeThemeIds"
        private const val FIELD_CLOUD_THEMES = "cloudThemes"
        private const val FIELD_CLOUD_ANIMATIONS = "cloudAnimations"

        private const val FIELD_RK_ID = "rkId"
        private const val FIELD_DEVICE_ID = "deviceId"
        private const val FIELD_DEVICE_NAME = "deviceName"
        private const val FIELD_DEVICE_MODEL = "deviceModel"
        private const val FIELD_MANUFACTURER = "manufacturer"
        private const val FIELD_FIRST_INSTALL_TS = "firstInstallTimestamp"
        private const val FIELD_FIRST_INSTALL_AT = "firstInstallAt"
        private const val FIELD_INSTALLED_AT = "installedAt"
        private const val FIELD_INSTALL_DATE_TEXT = "installDateText"
        private const val FIELD_LAST_ACTIVE = "lastActive"
        private const val FIELD_LAST_ACTIVE_AT = "lastActiveAt"
        private const val FIELD_IS_ONLINE = "isOnline"
        private const val FIELD_TRIAL_STARTED_AT = "trialStartedAt"
        private const val FIELD_TRIAL_EXPIRES_AT = "trialExpiresAt"
        private const val FIELD_TRIAL_USED = "trialUsed"
        private const val FIELD_TRIAL_POLICY_VERSION = "trialPolicyVersion"
        private const val FIELD_SECURITY_FLAGS = "securityFlags"
        private const val FIELD_VIP_STATUS = "vipStatus"
        private const val FIELD_LIFETIME = "lifetime"
        private const val FIELD_VIP_EXPIRY_TS = "vipExpiryTimestamp"
        private const val FIELD_BANGLISH_EXPIRY_TS = "banglishExpiryTimestamp"
        private const val FIELD_APP_VERSION = "appVersion"
        private const val FIELD_OVERRIDES = "overrides"
        private const val FIELD_IS_BLOCKED = "isBlocked"
        private const val FIELD_BLOCKED = "blocked"
        private const val FIELD_DEVICE_LOCKED = "deviceLocked"
        private const val FIELD_ADMIN_FREE_VIP = "adminFreeVip"
        private const val FIELD_ADMIN_ALL_FREE = "adminAllFree"
        private const val FIELD_CUSTOM_EXPIRY_TS = "customExpiryTimestamp"
        private const val FIELD_ALLOWED_FEATURES = "allowedFeatures"

        private const val FIELD_PLAN_TYPE = "planType"
        private const val FIELD_BASE_AMOUNT = "baseAmount"
        private const val FIELD_UNIQUE_AMOUNT = "uniqueAmount"
        private const val FIELD_UNIQUE_AMOUNT_NUM = "uniqueAmountNumber"
        private const val FIELD_DURATION_DAYS = "durationDays"
        private const val FIELD_TXN_REF = "txnRef"
        private const val FIELD_STATUS = "status"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val STATUS_PAYMENT_PENDING = "pending"

        private const val FEAT_RGB_THEMES = "rgbThemes"
        private const val FEAT_BANGLISH = "banglish"
        private const val FEAT_CALL_BAR = "callBar"
        private const val FEAT_RGB_STICKER = "rgbSticker"
        private const val FEAT_STYLISH_FONTS = "stylishFonts"
        private const val FEAT_AI_HUB = "aiHub"

        const val MIN_VALID_EPOCH_MS = 1_577_836_800_000L // 2020-01-01T00:00:00Z
        const val MAX_VALID_EPOCH_MS = 4_102_444_800_000L // 2100-01-01T00:00:00Z
        private const val MIN_VALID_EPOCH_SEC = 1_577_836_800L
        private const val MAX_VALID_EPOCH_SEC = 4_102_444_800L

        /**
         * Normalizes an epoch timestamp so `0`, `null`, negative, or corrupted values return `0L`
         * (never epoch `0` -> `20697d`), and 10-digit seconds timestamps scale cleanly to millis.
         */
        fun normalizeEpochMs(raw: Long?): Long {
            if (raw == null || raw <= 0L) return 0L
            val scaled = if (raw in MIN_VALID_EPOCH_SEC..MAX_VALID_EPOCH_SEC) {
                raw * 1000L
            } else {
                raw
            }
            return if (scaled in MIN_VALID_EPOCH_MS..MAX_VALID_EPOCH_MS) scaled else 0L
        }

        fun normalizeManufacturerName(manufacturer: String?): String {
            val clean = manufacturer.orEmpty().trim()
            if (clean.isEmpty()) return "Android"
            return clean.split(Regex("\\s+")).joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                }
            }
        }

        fun buildCleanDeviceName(
            manufacturer: String? = Build.MANUFACTURER,
            model: String? = Build.MODEL
        ): String {
            val cleanModel = model.orEmpty().trim()
            val cleanMfr = normalizeManufacturerName(manufacturer)
            if (cleanModel.isEmpty()) return "$cleanMfr Device"
            return if (cleanModel.startsWith(cleanMfr, ignoreCase = true)) {
                cleanModel
            } else {
                "$cleanMfr $cleanModel"
            }
        }

        fun formatInstallDateText(timestampMs: Long): String {
            val safeTs = normalizeEpochMs(timestampMs).takeIf { it > 0L } ?: return "Unknown"
            return runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm 'IST'", Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                fmt.format(Date(safeTs))
            }.getOrDefault(safeTs.toString())
        }

        @Volatile
        private var instance: FirebaseRemoteManager? = null

        fun getInstance(context: Context): FirebaseRemoteManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseRemoteManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
