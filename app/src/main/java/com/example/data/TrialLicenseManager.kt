package com.example.data

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.Immutable
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class LicenseUiState(
    val isLifetimeUnlocked: Boolean = false,
    val isTrialExpired: Boolean = false,
    val remainingSeconds: Long = TrialLicenseManager.TRIAL_DURATION_SECONDS,
    val deviceId: String = "000000",
    val unlockMethod: String? = null,
    val unlockedTxnId: String? = null
) {
    /** True during the initial 24-hour VIP trial OR permanently after ₹20 Lifetime VIP unlock. */
    val isVipActive: Boolean
        get() = isLifetimeUnlocked || !isTrialExpired

    /** True after 24-hour VIP trial ends without purchase: system keyboard stays free in standard matte-dark mode. */
    val isFreeStandardMode: Boolean
        get() = !isLifetimeUnlocked && isTrialExpired

    val formattedRemainingBangla: String
        get() {
            val clamped = remainingSeconds.coerceAtLeast(0L)
            val hours = clamped / 3600L
            val mins = (clamped % 3600L) / 60L
            val secs = clamped % 60L
            return when {
                hours > 0L -> "${toBengaliDigits(hours)} ঘণ্টা ${toBengaliDigits(mins)} মিনিট"
                mins > 0L -> "${toBengaliDigits(mins)} মিনিট ${toBengaliDigits(secs)} সেকেন্ড"
                else -> "${toBengaliDigits(secs)} সেকেন্ড"
            }
        }

    companion object {
        private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

        fun toBengaliDigits(value: Long): String {
            return value.toString().map { ch ->
                if (ch in '0'..'9') BENGALI_DIGITS[ch - '0'] else ch
            }.joinToString("")
        }
    }
}

sealed interface UpiVerificationResult {
    data class Success(val txnId: String) : UpiVerificationResult
    data object Cancelled : UpiVerificationResult
    data object PendingOrSubmitted : UpiVerificationResult
    data class Failed(val reason: String) : UpiVerificationResult
}

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object InvalidPin : PinVerificationResult
    data class Cooldown(val secondsRemaining: Long) : PinVerificationResult
}

/**
 * Manages the strict 5-minute (300 seconds) free trial timer, official merchant UPI ₹20
 * lifetime unlock verification, and owner-controlled salted SHA-256 PIN fallback.
 *
 * Syncs live across both MainActivity and RgbKeyboardService via SharedPreferences.
 */
class TrialLicenseManager private constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val deviceId: String = deriveStableSixCharDeviceId(context)

    private var failedPinAttempts: Int = 0
    private var pinCooldownUntilElapsedMs: Long = 0L

    private val _licenseState = MutableStateFlow(computeCurrentState())
    val licenseState: StateFlow<LicenseUiState> = _licenseState.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _licenseState.value = computeCurrentState()
    }

    init {
        ensureFirstLaunchTimestamp()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        _licenseState.value = computeCurrentState()
    }

    private fun ensureFirstLaunchTimestamp() {
        val policyVersion = prefs.getInt(KEY_TRIAL_POLICY_VERSION, 1)
        val needsV2Migration = policyVersion < CURRENT_TRIAL_POLICY_VERSION &&
            !prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)

        if (!prefs.contains(KEY_FIRST_LAUNCH_WALL_MS) || needsV2Migration) {
            val nowWall = System.currentTimeMillis()
            val nowElapsed = SystemClock.elapsedRealtime()
            prefs.edit()
                .putInt(KEY_TRIAL_POLICY_VERSION, CURRENT_TRIAL_POLICY_VERSION)
                .putLong(KEY_FIRST_LAUNCH_WALL_MS, nowWall)
                .putLong(KEY_FIRST_LAUNCH_ELAPSED_MS, nowElapsed)
                .putLong(KEY_LAST_SEEN_WALL_MS, nowWall)
                .putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
                .apply()
        } else if (policyVersion < CURRENT_TRIAL_POLICY_VERSION) {
            prefs.edit().putInt(KEY_TRIAL_POLICY_VERSION, CURRENT_TRIAL_POLICY_VERSION).apply()
        }
    }

    fun refreshState(): LicenseUiState {
        val state = computeCurrentState()
        _licenseState.value = state
        return state
    }

    private fun computeCurrentState(): LicenseUiState {
        ensureFirstLaunchTimestamp()

        val unlocked = prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)
        val unlockMethod = prefs.getString(KEY_UNLOCK_METHOD, null)
        val unlockedTxnId = prefs.getString(KEY_UNLOCK_TXN_ID, null)
        if (unlocked) {
            return LicenseUiState(
                isLifetimeUnlocked = true,
                isTrialExpired = false,
                remainingSeconds = 0L,
                deviceId = deviceId,
                unlockMethod = unlockMethod,
                unlockedTxnId = unlockedTxnId
            )
        }

        // Sticky expiry prevents rolling the system clock backward to reset an expired trial
        if (prefs.getBoolean(KEY_TRIAL_EXPIRED_STICKY, false)) {
            return LicenseUiState(
                isLifetimeUnlocked = false,
                isTrialExpired = true,
                remainingSeconds = 0L,
                deviceId = deviceId
            )
        }

        val firstWall = prefs.getLong(KEY_FIRST_LAUNCH_WALL_MS, System.currentTimeMillis())
        val firstElapsed = prefs.getLong(KEY_FIRST_LAUNCH_ELAPSED_MS, SystemClock.elapsedRealtime())
        val lastSeenWall = prefs.getLong(KEY_LAST_SEEN_WALL_MS, firstWall)

        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // Detect backward wall-clock rollback
        if (nowWall + 10_000L < lastSeenWall) {
            prefs.edit().putBoolean(KEY_TRIAL_EXPIRED_STICKY, true).apply()
            return LicenseUiState(
                isLifetimeUnlocked = false,
                isTrialExpired = true,
                remainingSeconds = 0L,
                deviceId = deviceId
            )
        } else if (nowWall > lastSeenWall) {
            prefs.edit().putLong(KEY_LAST_SEEN_WALL_MS, nowWall).apply()
        }

        val wallDeltaMs = (nowWall - firstWall).coerceAtLeast(0L)
        val monoDeltaMs = if (nowElapsed >= firstElapsed) (nowElapsed - firstElapsed) else 0L
        val elapsedMs = maxOf(wallDeltaMs, monoDeltaMs)
        val remainingMs = (TRIAL_DURATION_MS - elapsedMs).coerceAtLeast(0L)
        val expired = remainingMs <= 0L

        if (expired) {
            prefs.edit().putBoolean(KEY_TRIAL_EXPIRED_STICKY, true).apply()
        }

        return LicenseUiState(
            isLifetimeUnlocked = false,
            isTrialExpired = expired,
            remainingSeconds = (remainingMs + 999L) / 1000L,
            deviceId = deviceId
        )
    }

    /**
     * Builds the official RBI-compliant Merchant UPI Intent with a unique transaction ref (`tr`).
     */
    fun createOfficialUpiIntent(): Pair<String, Intent> {
        val txnRef = "RKBD_${System.currentTimeMillis()}"
        val uriString = "upi://pay" +
            "?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
            "&pn=${Uri.encode(MERCHANT_NAME)}" +
            "&mc=$MERCHANT_MCC" +
            "&tr=${Uri.encode(txnRef)}" +
            "&am=$AMOUNT_INR" +
            "&cu=INR" +
            "&tn=${Uri.encode(TRANSACTION_NOTE)}"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        return txnRef to intent
    }

    /**
     * Strictly verifies the authentic intent response returned by the UPI app.
     * Unlocks ONLY when resultCode == RESULT_OK, Status == SUCCESS (case-insensitive),
     * AND a valid non-placeholder txnId or ApprovalRefNo is returned (and txnRef matches if present).
     */
    fun verifyUpiIntentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?
    ): UpiVerificationResult {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return UpiVerificationResult.Cancelled
        }

        val params = linkedMapOf<String, String>()

        // Parse standard UPI "response" query string (e.g., "txnId=...&responseCode=00&Status=SUCCESS&txnRef=...")
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

        // Also inspect direct extras returned by some UPI apps
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

        // Verify returned transaction reference matches our initiated request if the UPI app returned one
        if (!expectedTxnRef.isNullOrBlank() && returnedTxnRef.isNotBlank() &&
            !returnedTxnRef.equals(expectedTxnRef, ignoreCase = true)
        ) {
            return UpiVerificationResult.Failed("Transaction reference mismatch")
        }

        // Reject explicit non-zero failure response codes when provided
        if (responseCode.isNotBlank() && responseCode != "00" && responseCode != "0") {
            return UpiVerificationResult.Failed("ResponseCode=$responseCode")
        }

        val verifiedId = listOf(txnId, approvalRef).firstOrNull { isAuthenticTxnIdentifier(it) }
            ?: return UpiVerificationResult.Failed("Missing valid txnId / ApprovalRefNo")

        markLifetimeUnlocked(method = "UPI_OFFICIAL", txnId = verifiedId)
        return UpiVerificationResult.Success(verifiedId)
    }

    private fun isAuthenticTxnIdentifier(candidate: String): Boolean {
        if (candidate.length < 6) return false
        val upper = candidate.uppercase(Locale.US)
        if (upper in setOf("NULL", "NONE", "NA", "N/A", "FAILED", "CANCELLED", "000000", "123456")) {
            return false
        }
        return candidate.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    }

    /**
     * Verifies the 6-digit Owner Unlock PIN derived via salted SHA-256 of the user's Device ID.
     * Formula: SHA256(deviceId + "RAKIB_QUALITY_FOODS_2026_SECRET"), first 6 numeric digits.
     */
    fun verifyOwnerPin(enteredPin: String): PinVerificationResult {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed < pinCooldownUntilElapsedMs) {
            val secs = ((pinCooldownUntilElapsedMs - nowElapsed) + 999L) / 1000L
            return PinVerificationResult.Cooldown(secs.coerceAtLeast(1L))
        }

        val sanitized = enteredPin.trim()
        if (sanitized.length != 6 || !sanitized.all { it.isDigit() }) {
            return PinVerificationResult.InvalidPin
        }

        val expectedPin = computeExpectedOwnerPin(deviceId)
        val matched = MessageDigest.isEqual(
            sanitized.toByteArray(Charsets.UTF_8),
            expectedPin.toByteArray(Charsets.UTF_8)
        )

        return if (matched) {
            failedPinAttempts = 0
            pinCooldownUntilElapsedMs = 0L
            markLifetimeUnlocked(method = "OWNER_PIN", txnId = "PIN_$deviceId")
            PinVerificationResult.Success
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN) {
                failedPinAttempts = 0
                pinCooldownUntilElapsedMs = nowElapsed + PIN_COOLDOWN_MS
                PinVerificationResult.Cooldown(PIN_COOLDOWN_MS / 1000L)
            } else {
                PinVerificationResult.InvalidPin
            }
        }
    }

    fun buildWhatsAppReceiptUri(): Uri {
        val message = "আসসালামু আলাইকুম, আমি Rakib official Keyboard-এর জন্য ₹২০ পেমেন্ট করেছি (QUALITY FOODS)।\n" +
            "আমার Device ID: $deviceId\n" +
            "অনুগ্রহ করে আমাকে ৬ সংখ্যার Lifetime Unlock PIN দিন।"
        return Uri.parse("https://wa.me/$WHATSAPP_OWNER_PHONE?text=${Uri.encode(message)}")
    }

    private fun markLifetimeUnlocked(method: String, txnId: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LIFETIME_UNLOCKED, true)
            .putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
            .putString(KEY_UNLOCK_METHOD, method)
            .putString(KEY_UNLOCK_TXN_ID, txnId)
            .putLong(KEY_UNLOCKED_AT_MS, System.currentTimeMillis())
            .apply()
        _licenseState.value = computeCurrentState()
    }

    companion object {
        const val MERCHANT_UPI_ID = "6296603748@okbizaxis"
        const val MERCHANT_NAME = "QUALITY FOODS"
        const val MERCHANT_MCC = "5411"
        const val AMOUNT_INR = "20.00"
        const val TRANSACTION_NOTE = "Rakib Keyboard Lifetime"
        const val WHATSAPP_OWNER_PHONE = "916296603748"

        const val TRIAL_DURATION_SECONDS = 86_400L // Strict 24-hour (1-day) VIP trial
        const val TRIAL_DURATION_MS = TRIAL_DURATION_SECONDS * 1000L

        private const val OWNER_PIN_SALT = "RAKIB_QUALITY_FOODS_2026_SECRET"
        private const val MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN = 5
        private const val PIN_COOLDOWN_MS = 15_000L
        private const val CURRENT_TRIAL_POLICY_VERSION = 2

        private const val PREFS_NAME = "rakib_trial_license_prefs"
        private const val KEY_TRIAL_POLICY_VERSION = "trial_policy_version"
        private const val KEY_FIRST_LAUNCH_WALL_MS = "first_launch_timestamp_ms"
        private const val KEY_FIRST_LAUNCH_ELAPSED_MS = "first_launch_elapsed_ms"
        private const val KEY_LAST_SEEN_WALL_MS = "last_seen_wall_ms"
        private const val KEY_TRIAL_EXPIRED_STICKY = "trial_expired_sticky"
        private const val KEY_IS_LIFETIME_UNLOCKED = "is_lifetime_unlocked"
        private const val KEY_UNLOCK_METHOD = "unlock_method"
        private const val KEY_UNLOCK_TXN_ID = "unlock_txn_id"
        private const val KEY_UNLOCKED_AT_MS = "unlocked_at_ms"

        @Volatile
        private var instance: TrialLicenseManager? = null

        fun getInstance(context: Context): TrialLicenseManager {
            return instance ?: synchronized(this) {
                instance ?: TrialLicenseManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Generates the exact 6-digit owner unlock PIN:
         * SHA256(deviceId + "RAKIB_QUALITY_FOODS_2026_SECRET") -> first 6 numeric digits.
         */
        fun computeExpectedOwnerPin(deviceId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest((deviceId + OWNER_PIN_SALT).toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            val digitsOnly = buildString {
                for (ch in hex) {
                    if (ch in '0'..'9') append(ch)
                    if (length == 6) break
                }
                // Deterministic fallback if hex ever had fewer than 6 decimal chars
                if (length < 6) {
                    for (b in digest) {
                        append(((b.toInt() and 0xFF) % 10).toString())
                        if (length == 6) break
                    }
                }
            }
            return digitsOnly.take(6)
        }

        @SuppressLint("HardwareIds")
        private fun deriveStableSixCharDeviceId(context: Context): String {
            val androidId = runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull().orEmpty().ifBlank { "FALLBACK_RAKIB_DEVICE" }

            val digest = MessageDigest.getInstance("SHA-256")
                .digest("RKBD_DEV_$androidId".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02X".format(it) }.take(6)
        }
    }
}
