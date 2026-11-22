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
import androidx.compose.runtime.Immutable
import java.io.File
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
    val registeredMobileNumber: String = "",
    val savedVipKey: String = "",
    val unlockMethod: String? = null,
    val unlockedTxnId: String? = null
) {
    /** True during the initial 7-day VIP trial OR permanently after ₹20 Lifetime VIP unlock. */
    val isVipActive: Boolean
        get() = isLifetimeUnlocked || !isTrialExpired

    /** True after 7-day VIP trial ends without purchase: real keyboard switches to Normal / Classic Black (No RGB). */
    val isFreeStandardMode: Boolean
        get() = !isLifetimeUnlocked && isTrialExpired

    val formattedRemainingBangla: String
        get() {
            val clamped = remainingSeconds.coerceAtLeast(0L)
            val days = clamped / 86_400L
            val hours = (clamped % 86_400L) / 3_600L
            val mins = (clamped % 3_600L) / 60L
            val secs = clamped % 60L
            return when {
                days > 0L -> "${toBengaliDigits(days)} দিন ${toBengaliDigits(hours)} ঘণ্টা"
                hours > 0L -> "${toBengaliDigits(hours)} ঘণ্টা ${toBengaliDigits(mins)} মিনিট"
                mins > 0L -> "${toBengaliDigits(mins)} মিনিট ${toBengaliDigits(secs)} সেকেন্ড"
                else -> "${toBengaliDigits(secs)} সেকেন্ড"
            }
        }

    /** Ultra-compact Bengali label for the minimalist top hero pill (e.g., "৬ দিন ২৩ ঘণ্টা"). */
    val formattedCompactPillBangla: String
        get() {
            val clamped = remainingSeconds.coerceAtLeast(0L)
            val days = clamped / 86_400L
            val hours = (clamped % 86_400L) / 3_600L
            val mins = (clamped % 3_600L) / 60L
            val secs = clamped % 60L
            return when {
                days > 0L -> "${toBengaliDigits(days)} দিন ${toBengaliDigits(hours)} ঘণ্টা"
                hours > 0L -> "${toBengaliDigits(hours)} ঘণ্টা ${toBengaliDigits(mins)} মি."
                mins > 0L -> "${toBengaliDigits(mins)} মি. বাকি"
                else -> "${toBengaliDigits(secs)} সে. বাকি"
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
    data class Success(val txnId: String, val generatedVipKey: String) : UpiVerificationResult
    data object Cancelled : UpiVerificationResult
    data object PendingOrSubmitted : UpiVerificationResult
    data class Failed(val reason: String) : UpiVerificationResult
}

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object InvalidPin : PinVerificationResult
    data class Cooldown(val secondsRemaining: Long) : PinVerificationResult
}

sealed interface RestoreVipResult {
    data class Restored(val mobileNumber: String, val vipKey: String) : RestoreVipResult
    data object WrongDevice : RestoreVipResult
    data object NotFoundForDevice : RestoreVipResult
    data object InvalidMobile : RestoreVipResult
}

/**
 * Manages:
 * - Strict 7-day (604,800s) clock-rollback-resistant Full VIP Trial
 * - Official Merchant UPI ₹20 Lifetime VIP activation
 * - Automatic Device-Bound 6-Digit VIP Key generation:
 *   `SHA256(mobileNumber + ":" + deviceId + ":RAKIB_QUALITY_FOODS_2026_SECRET")` (first 6 numeric digits)
 * - Automatic dual persistence to SharedPreferences + hidden `Documents/.rakib_official_vip.lic`
 * - Silent startup auto-restore on reinstall (strictly bound to same phone's `ANDROID_ID`)
 * - 1-tap automatic restore via SIM Phone Number Hint picker
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
        tryAutoRestoreFromLicenseFile()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        _licenseState.value = computeCurrentState()
    }

    private fun ensureFirstLaunchTimestamp() {
        val policyVersion = prefs.getInt(KEY_TRIAL_POLICY_VERSION, 1)
        val needsMigration = policyVersion < CURRENT_TRIAL_POLICY_VERSION &&
            !prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)

        if (!prefs.contains(KEY_FIRST_LAUNCH_WALL_MS) || needsMigration) {
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

    fun savePendingMobileNumber(rawMobile: String): String {
        val normalized = normalizeMobileNumber(rawMobile)
        if (normalized.isNotEmpty()) {
            prefs.edit().putString(KEY_SAVED_MOBILE_NUMBER, normalized).apply()
            _licenseState.value = computeCurrentState()
        }
        return normalized
    }

    fun getSavedMobileNumber(): String {
        return prefs.getString(KEY_SAVED_MOBILE_NUMBER, "").orEmpty()
    }

    fun refreshState(): LicenseUiState {
        if (!prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            tryAutoRestoreFromLicenseFile()
        }
        val state = computeCurrentState()
        _licenseState.value = state
        return state
    }

    /**
     * Reads `Documents/.rakib_official_vip.lic` on app startup after reinstall and validates it
     * strictly against the current phone's `ANDROID_ID` (`deviceId`). If valid, silently and
     * immediately restores Lifetime VIP without requiring any user interaction.
     */
    fun tryAutoRestoreFromLicenseFile(): Boolean {
        if (prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            val storedMobile = prefs.getString(KEY_SAVED_MOBILE_NUMBER, "").orEmpty()
            val storedKey = prefs.getString(KEY_SAVED_VIP_KEY, "").orEmpty()
            // Ensure license file exists on disk for future reinstalls
            if (storedKey.length == 6 && readHiddenLicenseRecord() == null) {
                writeHiddenLicenseFile(
                    mobileNumber = storedMobile,
                    vipKey = storedKey,
                    txnId = prefs.getString(KEY_UNLOCK_TXN_ID, "RESTORED").orEmpty()
                )
            }
            return true
        }

        val record = readHiddenLicenseRecord() ?: return false
        if (!record.deviceId.equals(deviceId, ignoreCase = true)) {
            return false
        }

        val expectedKey = if (record.mobile.isNotEmpty()) {
            computeDeviceMobileVipKey(record.mobile, deviceId)
        } else {
            computeExpectedOwnerPin(deviceId)
        }
        val expectedSig = computeLicenseSignature(record.deviceId, record.mobile, record.vipKey)

        val keyMatches = MessageDigest.isEqual(
            record.vipKey.toByteArray(Charsets.UTF_8),
            expectedKey.toByteArray(Charsets.UTF_8)
        )
        val sigMatches = MessageDigest.isEqual(
            record.signature.toByteArray(Charsets.UTF_8),
            expectedSig.toByteArray(Charsets.UTF_8)
        )

        if (!keyMatches || !sigMatches) {
            return false
        }

        markLifetimeUnlocked(
            method = "AUTO_RESTORE_LICENSE_FILE",
            txnId = record.txnId.ifBlank { "LIC_$deviceId" },
            mobileNumber = record.mobile,
            generatedVipKey = record.vipKey,
            writeLicenseFile = false
        )
        return true
    }

    /**
     * 1-tap automatic VIP restore triggered via the native SIM Phone Number Hint picker.
     * Validates that the picked mobile number was genuinely registered on THIS phone's `deviceId`
     * (via `Documents/.rakib_official_vip.lic` or Android Auto Backup's device-bound registry).
     */
    fun restoreVipByMobileNumber(rawMobile: String): RestoreVipResult {
        val normalized = normalizeMobileNumber(rawMobile)
        if (normalized.length < 10) {
            return RestoreVipResult.InvalidMobile
        }

        savePendingMobileNumber(normalized)
        val expectedKey = computeDeviceMobileVipKey(normalized, deviceId)

        // 1. Check hidden Documents/.rakib_official_vip.lic first
        val fileRecord = readHiddenLicenseRecord()
        if (fileRecord != null) {
            if (!fileRecord.deviceId.equals(deviceId, ignoreCase = true)) {
                return RestoreVipResult.WrongDevice
            }
            val expectedSig = computeLicenseSignature(fileRecord.deviceId, fileRecord.mobile, fileRecord.vipKey)
            val fileAuthentic = MessageDigest.isEqual(
                fileRecord.signature.toByteArray(Charsets.UTF_8),
                expectedSig.toByteArray(Charsets.UTF_8)
            )
            if (fileAuthentic &&
                fileRecord.mobile == normalized &&
                MessageDigest.isEqual(
                    fileRecord.vipKey.toByteArray(Charsets.UTF_8),
                    expectedKey.toByteArray(Charsets.UTF_8)
                )
            ) {
                markLifetimeUnlocked(
                    method = "AUTO_RESTORE_PHONE_HINT",
                    txnId = fileRecord.txnId.ifBlank { "RESTORE_$deviceId" },
                    mobileNumber = normalized,
                    generatedVipKey = expectedKey,
                    writeLicenseFile = true
                )
                return RestoreVipResult.Restored(normalized, expectedKey)
            }
        }

        // 2. If license file was cleared, validate against device-bound registration stored in SharedPreferences backup
        val registryToken = computeRegistrationToken(normalized, deviceId)
        val registeredTokens = prefs.getStringSet(KEY_REGISTERED_MOBILE_TOKENS, emptySet()).orEmpty()
        val storedMobile = prefs.getString(KEY_SAVED_MOBILE_NUMBER, "").orEmpty()
        val storedKey = prefs.getString(KEY_SAVED_VIP_KEY, "").orEmpty()

        val matchesBackupPrefs = (storedMobile == normalized &&
            storedKey.length == 6 &&
            MessageDigest.isEqual(
                storedKey.toByteArray(Charsets.UTF_8),
                expectedKey.toByteArray(Charsets.UTF_8)
            )) || registryToken in registeredTokens

        if (matchesBackupPrefs) {
            markLifetimeUnlocked(
                method = "AUTO_RESTORE_PHONE_REGISTRY",
                txnId = prefs.getString(KEY_UNLOCK_TXN_ID, null).orEmpty().ifBlank { "RESTORE_$deviceId" },
                mobileNumber = normalized,
                generatedVipKey = expectedKey,
                writeLicenseFile = true
            )
            return RestoreVipResult.Restored(normalized, expectedKey)
        }

        return RestoreVipResult.NotFoundForDevice
    }

    private fun computeCurrentState(): LicenseUiState {
        ensureFirstLaunchTimestamp()

        val savedMobile = prefs.getString(KEY_SAVED_MOBILE_NUMBER, "").orEmpty()
        val savedKey = prefs.getString(KEY_SAVED_VIP_KEY, "").orEmpty()
        val unlocked = prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)
        val unlockMethod = prefs.getString(KEY_UNLOCK_METHOD, null)
        val unlockedTxnId = prefs.getString(KEY_UNLOCK_TXN_ID, null)
        if (unlocked) {
            return LicenseUiState(
                isLifetimeUnlocked = true,
                isTrialExpired = false,
                remainingSeconds = 0L,
                deviceId = deviceId,
                registeredMobileNumber = savedMobile,
                savedVipKey = savedKey,
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
                deviceId = deviceId,
                registeredMobileNumber = savedMobile,
                savedVipKey = savedKey
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
                deviceId = deviceId,
                registeredMobileNumber = savedMobile,
                savedVipKey = savedKey
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
            deviceId = deviceId,
            registeredMobileNumber = savedMobile,
            savedVipKey = savedKey
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
     * Builds the scannable official UPI QR code URI for `6296603748@okbizaxis` (₹20 QUALITY FOODS).
     */
    fun buildUpiQrPayload(): String {
        return "upi://pay" +
            "?pa=${Uri.encode(MERCHANT_UPI_ID)}" +
            "&pn=${Uri.encode(MERCHANT_NAME)}" +
            "&mc=$MERCHANT_MCC" +
            "&am=$AMOUNT_INR" +
            "&cu=INR" +
            "&tn=${Uri.encode(TRANSACTION_NOTE)}"
    }

    /**
     * Strictly verifies the authentic intent response returned by the UPI app.
     * On `Status=SUCCESS` with valid `txnId`/`ApprovalRefNo`, automatically generates the
     * device-bound 6-digit VIP key `SHA256(mobileNumber + ":" + deviceId + ":RAKIB_QUALITY_FOODS_2026_SECRET")`,
     * saves both `mobileNumber` and the key to SharedPreferences + `Documents/.rakib_official_vip.lic`,
     * and unlocks Lifetime VIP immediately!
     */
    fun verifyUpiIntentResult(
        resultCode: Int,
        data: Intent?,
        expectedTxnRef: String?,
        rawMobileNumber: String = getSavedMobileNumber()
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

        val normalizedMobile = normalizeMobileNumber(rawMobileNumber)
        val generatedKey = if (normalizedMobile.isNotEmpty()) {
            computeDeviceMobileVipKey(normalizedMobile, deviceId)
        } else {
            computeExpectedOwnerPin(deviceId)
        }

        markLifetimeUnlocked(
            method = "UPI_OFFICIAL",
            txnId = verifiedId,
            mobileNumber = normalizedMobile,
            generatedVipKey = generatedKey,
            writeLicenseFile = true
        )
        return UpiVerificationResult.Success(verifiedId, generatedKey)
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
     * Verifies either the new device+mobile 6-digit VIP key
     * (`SHA256(mobileNumber + ":" + deviceId + ":RAKIB_QUALITY_FOODS_2026_SECRET")`)
     * or the legacy device-bound 6-digit Owner PIN (`SHA256(deviceId + "RAKIB_QUALITY_FOODS_2026_SECRET")`).
     */
    fun verifyOwnerPin(enteredPin: String, rawMobileNumber: String = getSavedMobileNumber()): PinVerificationResult {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed < pinCooldownUntilElapsedMs) {
            val secs = ((pinCooldownUntilElapsedMs - nowElapsed) + 999L) / 1000L
            return PinVerificationResult.Cooldown(secs.coerceAtLeast(1L))
        }

        val sanitized = enteredPin.trim()
        if (sanitized.length != 6 || !sanitized.all { it.isDigit() }) {
            return PinVerificationResult.InvalidPin
        }

        val normalizedMobile = normalizeMobileNumber(rawMobileNumber)
        val expectedLegacyPin = computeExpectedOwnerPin(deviceId)
        val expectedMobileKey = if (normalizedMobile.isNotEmpty()) {
            computeDeviceMobileVipKey(normalizedMobile, deviceId)
        } else {
            ""
        }

        val matchedLegacy = MessageDigest.isEqual(
            sanitized.toByteArray(Charsets.UTF_8),
            expectedLegacyPin.toByteArray(Charsets.UTF_8)
        )
        val matchedMobile = expectedMobileKey.isNotEmpty() && MessageDigest.isEqual(
            sanitized.toByteArray(Charsets.UTF_8),
            expectedMobileKey.toByteArray(Charsets.UTF_8)
        )

        return if (matchedMobile || matchedLegacy) {
            failedPinAttempts = 0
            pinCooldownUntilElapsedMs = 0L
            val keyToStore = if (normalizedMobile.isNotEmpty()) {
                computeDeviceMobileVipKey(normalizedMobile, deviceId)
            } else {
                expectedLegacyPin
            }
            markLifetimeUnlocked(
                method = "OWNER_PIN",
                txnId = "PIN_$deviceId",
                mobileNumber = normalizedMobile,
                generatedVipKey = keyToStore,
                writeLicenseFile = true
            )
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

    fun buildWhatsAppReceiptUri(rawMobileNumber: String = getSavedMobileNumber()): Uri {
        val normalized = normalizeMobileNumber(rawMobileNumber)
        val mobileLine = if (normalized.isNotEmpty()) "\nআমার Mobile: $normalized" else ""
        val message = "আসসালামু আলাইকুম, আমি Rakib official Keyboard-এর জন্য ₹20 পেমেন্ট করেছি।\n" +
            "আমার Device ID: $deviceId$mobileLine\n" +
            "অনুগ্রহ করে আমাকে ৬ সংখ্যার Lifetime Unlock PIN দিন।"
        return Uri.parse("https://wa.me/$WHATSAPP_OWNER_PHONE?text=${Uri.encode(message)}")
    }

    private fun markLifetimeUnlocked(
        method: String,
        txnId: String,
        mobileNumber: String,
        generatedVipKey: String,
        writeLicenseFile: Boolean
    ) {
        val normalizedMobile = normalizeMobileNumber(mobileNumber)
        val updatedTokens = prefs.getStringSet(KEY_REGISTERED_MOBILE_TOKENS, emptySet())
            .orEmpty()
            .toMutableSet()
        if (normalizedMobile.isNotEmpty()) {
            updatedTokens.add(computeRegistrationToken(normalizedMobile, deviceId))
        }

        prefs.edit()
            .putBoolean(KEY_IS_LIFETIME_UNLOCKED, true)
            .putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
            .putString(KEY_UNLOCK_METHOD, method)
            .putString(KEY_UNLOCK_TXN_ID, txnId)
            .putString(KEY_SAVED_MOBILE_NUMBER, normalizedMobile)
            .putString(KEY_SAVED_VIP_KEY, generatedVipKey)
            .putStringSet(KEY_REGISTERED_MOBILE_TOKENS, updatedTokens)
            .putLong(KEY_UNLOCKED_AT_MS, System.currentTimeMillis())
            .apply()

        if (writeLicenseFile) {
            writeHiddenLicenseFile(
                mobileNumber = normalizedMobile,
                vipKey = generatedVipKey,
                txnId = txnId
            )
        }
        _licenseState.value = computeCurrentState()
    }

    private data class LicenseFileRecord(
        val deviceId: String,
        val mobile: String,
        val vipKey: String,
        val txnId: String,
        val signature: String
    )

    private fun buildLicenseFilePayload(mobileNumber: String, vipKey: String, txnId: String): String {
        val sig = computeLicenseSignature(deviceId, mobileNumber, vipKey)
        return buildString {
            appendLine("deviceId=$deviceId")
            appendLine("mobile=$mobileNumber")
            appendLine("vipKey=$vipKey")
            appendLine("txnId=$txnId")
            appendLine("sig=$sig")
        }
    }

    private fun parseLicenseFilePayload(raw: String): LicenseFileRecord? {
        val map = mutableMapOf<String, String>()
        raw.lineSequence().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1).trim()
                map[k] = v
            }
        }
        val dev = map["deviceId"].orEmpty()
        val key = map["vipKey"].orEmpty()
        val sig = map["sig"].orEmpty()
        if (dev.isEmpty() || key.length != 6 || sig.isEmpty()) return null
        return LicenseFileRecord(
            deviceId = dev,
            mobile = normalizeMobileNumber(map["mobile"].orEmpty()),
            vipKey = key,
            txnId = map["txnId"].orEmpty(),
            signature = sig
        )
    }

    /**
     * Persists the hidden license file `Documents/.rakib_official_vip.lic` using zero-permission
     * Scoped Storage `MediaStore.Files` on Android 10+ (API 29+) and public `Documents/` on older devices,
     * plus a local mirror in app storage.
     */
    private fun writeHiddenLicenseFile(mobileNumber: String, vipKey: String, txnId: String) {
        val payloadBytes = buildLicenseFilePayload(mobileNumber, vipKey, txnId).toByteArray(Charsets.UTF_8)

        // 1. Scoped Storage MediaStore (Android 10+ / API 29+): survives uninstall without storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(HIDDEN_LICENSE_FILENAME, "${Environment.DIRECTORY_DOCUMENTS}%")

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
                        put(MediaStore.MediaColumns.DISPLAY_NAME, HIDDEN_LICENSE_FILENAME)
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
            }
        }

        // 2. Direct public Documents/.rakib_official_vip.lic fallback when writable
        runCatching {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (publicDocs != null) {
                if (!publicDocs.exists()) publicDocs.mkdirs()
                val licFile = File(publicDocs, HIDDEN_LICENSE_FILENAME)
                licFile.writeBytes(payloadBytes)
            }
        }

        // 3. App-private Documents mirror
        runCatching {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!appDocs.exists()) appDocs.mkdirs()
            File(appDocs, HIDDEN_LICENSE_FILENAME).writeBytes(payloadBytes)
        }
    }

    private fun readHiddenLicenseRecord(): LicenseFileRecord? {
        // 1. Query MediaStore Documents/.rakib_official_vip.lic on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fromMediaStore = runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(HIDDEN_LICENSE_FILENAME, "${Environment.DIRECTORY_DOCUMENTS}%")
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
            }.getOrNull()

            if (!fromMediaStore.isNullOrBlank()) {
                parseLicenseFilePayload(fromMediaStore)?.let { return it }
            }
        }

        // 2. Direct public Documents/.rakib_official_vip.lic
        val fromPublicFile = runCatching {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = if (publicDocs != null) File(publicDocs, HIDDEN_LICENSE_FILENAME) else null
            if (file != null && file.exists() && file.canRead()) {
                file.readText(Charsets.UTF_8)
            } else null
        }.getOrNull()

        if (!fromPublicFile.isNullOrBlank()) {
            parseLicenseFilePayload(fromPublicFile)?.let { return it }
        }

        // 3. App-private mirror fallback
        val fromAppMirror = runCatching {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(appDocs, HIDDEN_LICENSE_FILENAME)
            if (file.exists() && file.canRead()) file.readText(Charsets.UTF_8) else null
        }.getOrNull()

        if (!fromAppMirror.isNullOrBlank()) {
            parseLicenseFilePayload(fromAppMirror)?.let { return it }
        }

        return null
    }

    companion object {
        const val MERCHANT_UPI_ID = "6296603748@okbizaxis"
        const val MERCHANT_NAME = "QUALITY FOODS"
        const val MERCHANT_MCC = "5411"
        const val AMOUNT_INR = "20.00"
        const val TRANSACTION_NOTE = "Rakib Keyboard Lifetime"
        const val WHATSAPP_OWNER_PHONE = "916296603748"

        const val TRIAL_DURATION_SECONDS = 604_800L // Strict 7-day (168 hours) Full VIP trial
        const val TRIAL_DURATION_MS = TRIAL_DURATION_SECONDS * 1000L

        private const val HIDDEN_LICENSE_FILENAME = ".rakib_official_vip.lic"
        private const val OWNER_PIN_SALT = "RAKIB_QUALITY_FOODS_2026_SECRET"
        private const val MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN = 5
        private const val PIN_COOLDOWN_MS = 15_000L
        private const val CURRENT_TRIAL_POLICY_VERSION = 3

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
        private const val KEY_SAVED_MOBILE_NUMBER = "saved_mobile_number"
        private const val KEY_SAVED_VIP_KEY = "saved_vip_key"
        private const val KEY_REGISTERED_MOBILE_TOKENS = "registered_mobile_tokens"

        @Volatile
        private var instance: TrialLicenseManager? = null

        fun getInstance(context: Context): TrialLicenseManager {
            return instance ?: synchronized(this) {
                instance ?: TrialLicenseManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Normalizes SIM hint or manually typed mobile numbers to a canonical 10-digit mobile
         * string (strips `+91`, `+880`, leading `0`, spaces, and punctuation) so keys match consistently.
         */
        fun normalizeMobileNumber(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            return when {
                digits.length >= 10 -> digits.takeLast(10)
                else -> digits
            }
        }

        /**
         * Generates the exact device-bound 6-digit VIP key:
         * `SHA256(mobileNumber + ":" + deviceId + ":RAKIB_QUALITY_FOODS_2026_SECRET")` (first 6 numeric digits).
         */
        fun computeDeviceMobileVipKey(mobileNumber: String, deviceId: String): String {
            val normalized = normalizeMobileNumber(mobileNumber)
            val payload = "$normalized:$deviceId:$OWNER_PIN_SALT"
            return extractFirstSixNumericDigits(sha256Bytes(payload))
        }

        /**
         * Legacy device-only 6-digit owner unlock PIN:
         * `SHA256(deviceId + "RAKIB_QUALITY_FOODS_2026_SECRET")` -> first 6 numeric digits.
         */
        fun computeExpectedOwnerPin(deviceId: String): String {
            return extractFirstSixNumericDigits(sha256Bytes(deviceId + OWNER_PIN_SALT))
        }

        private fun computeLicenseSignature(deviceId: String, mobileNumber: String, vipKey: String): String {
            val payload = "LIC_SIG:$deviceId:$mobileNumber:$vipKey:$OWNER_PIN_SALT"
            return sha256Bytes(payload).joinToString("") { "%02x".format(it) }.take(24)
        }

        private fun computeRegistrationToken(mobileNumber: String, deviceId: String): String {
            val payload = "REG_TOK:$mobileNumber:$deviceId:$OWNER_PIN_SALT"
            return sha256Bytes(payload).joinToString("") { "%02x".format(it) }.take(32)
        }

        private fun sha256Bytes(input: String): ByteArray {
            return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        }

        private fun extractFirstSixNumericDigits(digest: ByteArray): String {
            val hex = digest.joinToString("") { "%02x".format(it) }
            val digitsOnly = buildString {
                for (ch in hex) {
                    if (ch in '0'..'9') append(ch)
                    if (length == 6) break
                }
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

            val digest = sha256Bytes("RKBD_DEV_$androidId")
            return digest.joinToString("") { "%02X".format(it) }.take(6)
        }
    }
}
