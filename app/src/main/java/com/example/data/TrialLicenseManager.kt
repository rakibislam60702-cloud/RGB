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
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
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

    /** Crisp flagship English countdown format for the Royal Amber-Gold VIP Trial pill (e.g., "6d 23h"). */
    val formattedCompactEnglish: String
        get() {
            val clamped = remainingSeconds.coerceAtLeast(0L)
            val days = clamped / 86_400L
            val hours = (clamped % 86_400L) / 3_600L
            val mins = (clamped % 3_600L) / 60L
            val secs = clamped % 60L
            return when {
                days > 0L -> "${days}d ${hours}h"
                hours > 0L -> "${hours}h ${mins}m"
                mins > 0L -> "${mins}m"
                else -> "${secs}s"
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

/**
 * Manages:
 * - Strict 7-day (604,800s) clock-rollback-resistant Full VIP Trial
 * - Official Merchant UPI ₹20 Lifetime VIP activation (`6296603748@okbizaxis`, `QUALITY FOODS`)
 * - AES-256 encrypted, device-bound persistent license stored in `Documents/.ai_keyboard_vip.lic`
 *   (strictly bound to this phone's `ANDROID_ID` so uninstalling and reinstalling on the same phone
 *   silently restores Lifetime VIP automatically, while copied files are rejected on other phones).
 */
class TrialLicenseManager private constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val rawHardwareSeed: String = readHardwareAndroidId(context)
    val deviceId: String = deriveStableSixCharDeviceId(rawHardwareSeed)

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

    fun refreshState(): LicenseUiState {
        if (!prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            tryAutoRestoreFromLicenseFile()
        }
        val state = computeCurrentState()
        _licenseState.value = state
        return state
    }

    /**
     * Silently reads and decrypts `Documents/.ai_keyboard_vip.lic` (with fallback to legacy
     * `.rakib_official_vip.lic`) on app startup after reinstall and validates it strictly
     * against this phone's `ANDROID_ID`. If authentic, unlocks Lifetime VIP silently.
     */
    fun tryAutoRestoreFromLicenseFile(): Boolean {
        if (prefs.getBoolean(KEY_IS_LIFETIME_UNLOCKED, false)) {
            // Ensure encrypted Documents/.ai_keyboard_vip.lic exists for future reinstalls
            if (readEncryptedLicenseRecord(HIDDEN_LICENSE_FILENAME) == null) {
                writeEncryptedLicenseFile(
                    txnId = prefs.getString(KEY_UNLOCK_TXN_ID, "LIC_$deviceId").orEmpty()
                )
            }
            return true
        }

        // 1. Primary encrypted device-bound license file: Documents/.ai_keyboard_vip.lic
        val encryptedRecord = readEncryptedLicenseRecord(HIDDEN_LICENSE_FILENAME)
        if (encryptedRecord != null &&
            encryptedRecord.deviceId.equals(deviceId, ignoreCase = true) &&
            verifyLicenseSignature(encryptedRecord.deviceId, encryptedRecord.txnId, encryptedRecord.signature)
        ) {
            markLifetimeUnlocked(
                method = "AUTO_RESTORE_ENCRYPTED_LIC",
                txnId = encryptedRecord.txnId.ifBlank { "LIC_$deviceId" },
                writeLicenseFile = false
            )
            return true
        }

        // 2. Seamless backward-compatibility for previously issued .rakib_official_vip.lic on same device
        val legacyRaw = readRawLicenseText(LEGACY_LICENSE_FILENAME)
        if (!legacyRaw.isNullOrBlank()) {
            val legacyDevice = extractPlainField(legacyRaw, "deviceId")
            val legacyKey = extractPlainField(legacyRaw, "vipKey")
            val legacyTxn = extractPlainField(legacyRaw, "txnId")
            if (legacyDevice.equals(deviceId, ignoreCase = true) && legacyKey.length == 6) {
                markLifetimeUnlocked(
                    method = "AUTO_RESTORE_LEGACY_LIC",
                    txnId = legacyTxn.ifBlank { "LIC_$deviceId" },
                    writeLicenseFile = true // Upgrade to encrypted .ai_keyboard_vip.lic
                )
                return true
            }
        }

        return false
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
        val txnRef = "AIKB_${System.currentTimeMillis()}"
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
     * Unlocks immediately on `Status=SUCCESS` with valid `txnId`/`ApprovalRefNo` and saves
     * an AES-256 encrypted device-bound license to `Documents/.ai_keyboard_vip.lic`.
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

        markLifetimeUnlocked(
            method = "UPI_OFFICIAL",
            txnId = verifiedId,
            writeLicenseFile = true
        )
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

    private fun markLifetimeUnlocked(
        method: String,
        txnId: String,
        writeLicenseFile: Boolean
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LIFETIME_UNLOCKED, true)
            .putBoolean(KEY_TRIAL_EXPIRED_STICKY, false)
            .putString(KEY_UNLOCK_METHOD, method)
            .putString(KEY_UNLOCK_TXN_ID, txnId)
            .putLong(KEY_UNLOCKED_AT_MS, System.currentTimeMillis())
            .apply()

        if (writeLicenseFile) {
            writeEncryptedLicenseFile(txnId)
        }
        _licenseState.value = computeCurrentState()
    }

    private data class EncryptedLicenseRecord(
        val deviceId: String,
        val txnId: String,
        val signature: String
    )

    private fun deriveDeviceAesKey(): SecretKeySpec {
        val keyBytes = sha256Bytes("AIKB_AES256:$rawHardwareSeed:$deviceId:$LICENSE_SECRET_SALT")
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encryptLicensePayload(txnId: String): String {
        val sig = computeLicenseSignature(deviceId, txnId)
        val plain = "v=2\ndeviceId=$deviceId\ntxnId=$txnId\nsig=$sig\n"
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveDeviceAesKey(), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return "AIKB_ENC_V2:" + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptLicensePayload(encoded: String): EncryptedLicenseRecord? {
        val trimmed = encoded.trim()
        if (!trimmed.startsWith("AIKB_ENC_V2:")) return null
        val b64 = trimmed.removePrefix("AIKB_ENC_V2:")
        val plain = runCatching {
            val combined = Base64.decode(b64, Base64.DEFAULT)
            if (combined.size <= 16) return null
            val iv = combined.copyOfRange(0, 16)
            val cipherText = combined.copyOfRange(16, combined.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, deriveDeviceAesKey(), IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull() ?: return null

        val dev = extractPlainField(plain, "deviceId")
        val txn = extractPlainField(plain, "txnId")
        val sig = extractPlainField(plain, "sig")
        if (dev.isEmpty() || sig.isEmpty()) return null
        return EncryptedLicenseRecord(deviceId = dev, txnId = txn, signature = sig)
    }

    private fun extractPlainField(raw: String, key: String): String {
        return raw.lineSequence()
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }

    /**
     * Persists the AES-256 encrypted, device-bound license file `Documents/.ai_keyboard_vip.lic`
     * using zero-permission Scoped Storage `MediaStore.Files` on Android 10+ (API 29+) and
     * public `Documents/` fallback on older devices, plus an app-private mirror.
     */
    private fun writeEncryptedLicenseFile(txnId: String) {
        val payloadBytes = runCatching {
            encryptLicensePayload(txnId).toByteArray(Charsets.UTF_8)
        }.getOrNull() ?: return

        // 1. Scoped Storage MediaStore (Android 10+ / API 29+): survives uninstall with zero permissions
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

        // 2. Direct public Documents/.ai_keyboard_vip.lic fallback when writable
        runCatching {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (publicDocs != null) {
                if (!publicDocs.exists()) publicDocs.mkdirs()
                File(publicDocs, HIDDEN_LICENSE_FILENAME).writeBytes(payloadBytes)
            }
        }

        // 3. App-private Documents mirror
        runCatching {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!appDocs.exists()) appDocs.mkdirs()
            File(appDocs, HIDDEN_LICENSE_FILENAME).writeBytes(payloadBytes)
        }
    }

    private fun readEncryptedLicenseRecord(fileName: String): EncryptedLicenseRecord? {
        val raw = readRawLicenseText(fileName) ?: return null
        return decryptLicensePayload(raw)
    }

    private fun readRawLicenseText(fileName: String): String? {
        // 1. Query MediaStore Documents/<fileName> on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fromMediaStore = runCatching {
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
            }.getOrNull()

            if (!fromMediaStore.isNullOrBlank()) return fromMediaStore
        }

        // 2. Direct public Documents/<fileName>
        val fromPublicFile = runCatching {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val file = if (publicDocs != null) File(publicDocs, fileName) else null
            if (file != null && file.exists() && file.canRead()) {
                file.readText(Charsets.UTF_8)
            } else null
        }.getOrNull()

        if (!fromPublicFile.isNullOrBlank()) return fromPublicFile

        // 3. App-private mirror fallback
        val fromAppMirror = runCatching {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(appDocs, fileName)
            if (file.exists() && file.canRead()) file.readText(Charsets.UTF_8) else null
        }.getOrNull()

        return fromAppMirror?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val MERCHANT_UPI_ID = "6296603748@okbizaxis"
        const val MERCHANT_NAME = "QUALITY FOODS"
        const val MERCHANT_MCC = "5411"
        const val AMOUNT_INR = "20.00"
        const val TRANSACTION_NOTE = "AI Keyboard Lifetime VIP"

        const val TRIAL_DURATION_SECONDS = 604_800L // Strict 7-day (168 hours) Full VIP trial
        const val TRIAL_DURATION_MS = TRIAL_DURATION_SECONDS * 1000L

        private const val HIDDEN_LICENSE_FILENAME = ".ai_keyboard_vip.lic"
        private const val LEGACY_LICENSE_FILENAME = ".rakib_official_vip.lic"
        private const val LICENSE_SECRET_SALT = "RAKIB_QUALITY_FOODS_2026_SECRET"
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
            }.getOrNull().orEmpty().ifBlank { "FALLBACK_AI_KEYBOARD_DEVICE" }
        }

        private fun deriveStableSixCharDeviceId(androidId: String): String {
            val digest = sha256Bytes("RKBD_DEV_$androidId")
            return digest.joinToString("") { "%02X".format(it) }.take(6)
        }
    }
}
