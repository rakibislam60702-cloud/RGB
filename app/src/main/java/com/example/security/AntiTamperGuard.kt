package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.data.FirebaseRemoteManager
import com.google.firebase.FirebaseApp
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

/**
 * Runtime Anti-Tamper & Firebase Integrity Guard for AI Keyboard.
 *
 * Enforces:
 * 1. Exact application package identity (`com.aistudio.prorgbkeyboard.vkxmzq`).
 * 2. Exact Firebase Realtime Database URL & project ID lock (`rakib-ai-keyboard` /
 *    `https://rakib-ai-keyboard-default-rtdb.firebaseio.com`).
 * 3. Signing certificate presence + hook/patcher framework detection (LuckyPatcher, Xposed/LSPosed, Frida).
 * 4. HMAC-SHA256 verification on local VIP / Trial / Lifetime SharedPreferences flags so any
 *    unauthorized local XML/SharedPreferences edit immediately revokes privileges and exits cleanly.
 *
 * Note: Legitimate Firebase Admin remote switches (`globalAllFree`, `adminAllFree`, `adminFreeVip`,
 * `customExpiryTimestamp`, verified device VIP/expiry) are applied inside [TrialLicenseManager] and
 * sealed with a device-bound HMAC, so admin overrides and debug builds continue to operate normally.
 */
object AntiTamperGuard {

    private const val TAG = "AntiTamperGuard"

    const val EXPECTED_PACKAGE_NAME = "com.aistudio.prorgbkeyboard.vkxmzq"
    const val EXPECTED_FIREBASE_PROJECT_ID = "rakib-ai-keyboard"
    const val EXPECTED_FIREBASE_DATABASE_URL = "https://rakib-ai-keyboard-default-rtdb.firebaseio.com"

    private const val PREFS_NAME = "rakib_trial_license_prefs"
    private const val HMAC_DOMAIN_SALT = "AIKB_INTEGRITY_HMAC_V1_2026_RAKIB"

    private val PATCHER_PACKAGES = listOf(
        "com.chelpus.lackypatch",
        "com.dimonvideo.luckypatcher",
        "com.forpda.lp",
        "com.android.vending.billing.InAppBillingService.LUCK",
        "org.lsposed.manager",
        "de.robv.android.xposed.installer",
        "io.va.exposed",
        "me.weishu.exp"
    )

    private val HOOK_CLASSES = listOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers",
        "com.saurik.substrate.MS"
    )

    sealed interface TamperCheckResult {
        data object Valid : TamperCheckResult
        data class Tampered(val reason: String) : TamperCheckResult
    }

    /**
     * Checks package identity, Firebase configuration lock, signing certificate presence,
     * and common runtime hooking/repacking indicators.
     */
    fun verifyAppIntegrity(
        context: Context,
        firebaseDatabaseUrl: String = FirebaseRemoteManager.DATABASE_URL
    ): TamperCheckResult {
        val pkg = context.packageName.orEmpty()
        if (pkg != EXPECTED_PACKAGE_NAME) {
            return TamperCheckResult.Tampered("Invalid package identity")
        }

        val normalizedExpectedUrl = normalizeUrl(EXPECTED_FIREBASE_DATABASE_URL)
        val normalizedActualUrl = normalizeUrl(firebaseDatabaseUrl)
        if (normalizedActualUrl != normalizedExpectedUrl) {
            return TamperCheckResult.Tampered("Unauthorized Firebase database endpoint")
        }

        val firebaseApps = runCatching { FirebaseApp.getApps(context) }.getOrNull().orEmpty()
        for (app in firebaseApps) {
            val options = runCatching { app.options }.getOrNull() ?: continue
            val projectId = options.projectId.orEmpty().trim()
            if (projectId.isNotEmpty() && projectId != EXPECTED_FIREBASE_PROJECT_ID) {
                return TamperCheckResult.Tampered("Unauthorized Firebase project configuration")
            }
            val dbUrl = options.databaseUrl.orEmpty().trim()
            if (dbUrl.isNotEmpty() && normalizeUrl(dbUrl) != normalizedExpectedUrl) {
                return TamperCheckResult.Tampered("Modified Firebase Realtime Database URL")
            }
        }

        if (!isRunningInRobolectricTest()) {
            if (!hasValidSigningCertificate(context, pkg)) {
                return TamperCheckResult.Tampered("Missing or invalid APK signing certificate")
            }
            detectRuntimeTamperTooling(context)?.let { reason ->
                return TamperCheckResult.Tampered(reason)
            }
        }

        return TamperCheckResult.Valid
    }

    /**
     * Verifies integrity and immediately revokes privileges + terminates the process if tampered.
     */
    fun verifyOrTerminate(context: Context) {
        when (val result = verifyAppIntegrity(context)) {
            is TamperCheckResult.Valid -> Unit
            is TamperCheckResult.Tampered -> enforceTamperTermination(context, result.reason)
        }
    }

    /**
     * Revokes local VIP/trial privilege flags and terminates cleanly on tampering.
     */
    fun enforceTamperTermination(context: Context, reason: String): Nothing {
        Log.e(TAG, "Integrity verification failed: $reason")
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_lifetime_unlocked", false)
                .putBoolean("is_banglish_lifetime_unlocked", false)
                .putBoolean("admin_granted_vip", false)
                .putBoolean("admin_all_free_device", false)
                .putBoolean("admin_global_all_free", false)
                .putLong("vip_expiry_timestamp_ms", 0L)
                .putLong("banglish_expiry_timestamp_ms", 0L)
                .putLong("device_custom_expiry_ms", 0L)
                .remove("prefs_integrity_hmac")
                .commit()
        }

        if (isRunningInRobolectricTest()) {
            throw SecurityException("Integrity verification failed: $reason")
        }

        Process.killProcess(Process.myPid())
        exitProcess(1)
    }

    /**
     * Computes a device-bound HMAC-SHA256 tag over sensitive subscription & VIP SharedPreferences
     * flags so manual XML tampering outside [TrialLicenseManager] is detected immediately.
     */
    fun computePrefsIntegrityHmac(
        deviceId: String,
        rawAndroidId: String,
        packageName: String,
        isLifetimeUnlocked: Boolean,
        isBanglishLifetimeUnlocked: Boolean,
        banglishExpiryMs: Long,
        vipExpiryMs: Long,
        adminGrantedVip: Boolean,
        adminAllFreeDevice: Boolean,
        adminGlobalAllFree: Boolean,
        deviceCustomExpiryMs: Long,
        unlockMethod: String?,
        unlockTxnId: String?,
        isDeviceBlocked: Boolean = false
    ): String {
        val secretKeyMaterial = "$deviceId|$rawAndroidId|$packageName|$HMAC_DOMAIN_SALT"
        val keyHash = MessageDigest.getInstance("SHA-256")
            .digest(secretKeyMaterial.toByteArray(Charsets.UTF_8))

        val payload = buildString {
            append("v1|")
            append(deviceId).append('|')
            append(packageName).append('|')
            append(isLifetimeUnlocked).append('|')
            append(isBanglishLifetimeUnlocked).append('|')
            append(banglishExpiryMs).append('|')
            append(vipExpiryMs).append('|')
            append(adminGrantedVip).append('|')
            append(adminAllFreeDevice).append('|')
            append(adminGlobalAllFree).append('|')
            append(deviceCustomExpiryMs).append('|')
            append(unlockMethod.orEmpty()).append('|')
            append(unlockTxnId.orEmpty()).append('|')
            append(isDeviceBlocked)
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyHash, "HmacSHA256"))
        val raw = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hardened v2 HMAC covering BOTH VIP/Admin privileges AND local trial timestamps,
     * sticky expiry, and security anti-tamper flags so XML edits to trial start/expiry
     * or rollback counters are caught immediately.
     */
    fun computePrefsIntegrityHmacV2(
        deviceId: String,
        rawAndroidId: String,
        packageName: String,
        isLifetimeUnlocked: Boolean,
        isBanglishLifetimeUnlocked: Boolean,
        banglishExpiryMs: Long,
        vipExpiryMs: Long,
        adminGrantedVip: Boolean,
        adminAllFreeDevice: Boolean,
        adminGlobalAllFree: Boolean,
        deviceCustomExpiryMs: Long,
        unlockMethod: String?,
        unlockTxnId: String?,
        isDeviceBlocked: Boolean,
        firstInstallTimestampMs: Long,
        maxSeenElapsedMs: Long,
        trialExpiredSticky: Boolean,
        reinstallDetected: Boolean,
        clockRollbackDetected: Boolean
    ): String {
        val secretKeyMaterial = "$deviceId|$rawAndroidId|$packageName|$HMAC_DOMAIN_SALT"
        val keyHash = MessageDigest.getInstance("SHA-256")
            .digest(secretKeyMaterial.toByteArray(Charsets.UTF_8))

        val payload = buildString {
            append("v2|")
            append(deviceId).append('|')
            append(packageName).append('|')
            append(isLifetimeUnlocked).append('|')
            append(isBanglishLifetimeUnlocked).append('|')
            append(banglishExpiryMs).append('|')
            append(vipExpiryMs).append('|')
            append(adminGrantedVip).append('|')
            append(adminAllFreeDevice).append('|')
            append(adminGlobalAllFree).append('|')
            append(deviceCustomExpiryMs).append('|')
            append(unlockMethod.orEmpty()).append('|')
            append(unlockTxnId.orEmpty()).append('|')
            append(isDeviceBlocked).append('|')
            append(firstInstallTimestampMs).append('|')
            append(maxSeenElapsedMs).append('|')
            append(trialExpiredSticky).append('|')
            append(reinstallDetected).append('|')
            append(clockRollbackDetected)
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyHash, "HmacSHA256"))
        val raw = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    fun verifyPrefsIntegrityHmac(
        providedHmac: String?,
        deviceId: String,
        rawAndroidId: String,
        packageName: String,
        isLifetimeUnlocked: Boolean,
        isBanglishLifetimeUnlocked: Boolean,
        banglishExpiryMs: Long,
        vipExpiryMs: Long,
        adminGrantedVip: Boolean,
        adminAllFreeDevice: Boolean,
        adminGlobalAllFree: Boolean,
        deviceCustomExpiryMs: Long,
        unlockMethod: String?,
        unlockTxnId: String?,
        isDeviceBlocked: Boolean = false
    ): Boolean {
        val actual = providedHmac?.trim().orEmpty()
        if (actual.isEmpty()) return false
        val expected = computePrefsIntegrityHmac(
            deviceId = deviceId,
            rawAndroidId = rawAndroidId,
            packageName = packageName,
            isLifetimeUnlocked = isLifetimeUnlocked,
            isBanglishLifetimeUnlocked = isBanglishLifetimeUnlocked,
            banglishExpiryMs = banglishExpiryMs,
            vipExpiryMs = vipExpiryMs,
            adminGrantedVip = adminGrantedVip,
            adminAllFreeDevice = adminAllFreeDevice,
            adminGlobalAllFree = adminGlobalAllFree,
            deviceCustomExpiryMs = deviceCustomExpiryMs,
            unlockMethod = unlockMethod,
            unlockTxnId = unlockTxnId,
            isDeviceBlocked = isDeviceBlocked
        )
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expected.toByteArray(Charsets.UTF_8)
        )
    }

    fun verifyPrefsIntegrityHmacV2(
        providedHmac: String?,
        deviceId: String,
        rawAndroidId: String,
        packageName: String,
        isLifetimeUnlocked: Boolean,
        isBanglishLifetimeUnlocked: Boolean,
        banglishExpiryMs: Long,
        vipExpiryMs: Long,
        adminGrantedVip: Boolean,
        adminAllFreeDevice: Boolean,
        adminGlobalAllFree: Boolean,
        deviceCustomExpiryMs: Long,
        unlockMethod: String?,
        unlockTxnId: String?,
        isDeviceBlocked: Boolean,
        firstInstallTimestampMs: Long,
        maxSeenElapsedMs: Long,
        trialExpiredSticky: Boolean,
        reinstallDetected: Boolean,
        clockRollbackDetected: Boolean
    ): Boolean {
        val actual = providedHmac?.trim().orEmpty()
        if (actual.isEmpty()) return false
        val expected = computePrefsIntegrityHmacV2(
            deviceId = deviceId,
            rawAndroidId = rawAndroidId,
            packageName = packageName,
            isLifetimeUnlocked = isLifetimeUnlocked,
            isBanglishLifetimeUnlocked = isBanglishLifetimeUnlocked,
            banglishExpiryMs = banglishExpiryMs,
            vipExpiryMs = vipExpiryMs,
            adminGrantedVip = adminGrantedVip,
            adminAllFreeDevice = adminAllFreeDevice,
            adminGlobalAllFree = adminGlobalAllFree,
            deviceCustomExpiryMs = deviceCustomExpiryMs,
            unlockMethod = unlockMethod,
            unlockTxnId = unlockTxnId,
            isDeviceBlocked = isDeviceBlocked,
            firstInstallTimestampMs = firstInstallTimestampMs,
            maxSeenElapsedMs = maxSeenElapsedMs,
            trialExpiredSticky = trialExpiredSticky,
            reinstallDetected = reinstallDetected,
            clockRollbackDetected = clockRollbackDetected
        )
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expected.toByteArray(Charsets.UTF_8)
        )
    }

    @Suppress("DEPRECATION")
    fun getSigningCertSha256Prefix(context: Context): String {
        return runCatching {
            val pm = context.packageManager
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners
                    ?: info.signingInfo?.signingCertificateHistory
                signers?.firstOrNull()?.toByteArray()
            } else {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return "unknown"
            MessageDigest.getInstance("SHA-256")
                .digest(certBytes)
                .joinToString("") { "%02x".format(it) }
                .take(16)
        }.getOrDefault("unknown")
    }

    @Suppress("DEPRECATION")
    private fun hasValidSigningCertificate(context: Context, packageName: String): Boolean {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners
                    ?: info.signingInfo?.signingCertificateHistory
                !signers.isNullOrEmpty() && signers.any { it.toByteArray().isNotEmpty() }
            } else {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                val sigs = info.signatures
                !sigs.isNullOrEmpty() && sigs.any { it.toByteArray().isNotEmpty() }
            }
        }.getOrDefault(false)
    }

    private fun detectRuntimeTamperTooling(context: Context): String? {
        for (className in HOOK_CLASSES) {
            if (runCatching { Class.forName(className, false, context.classLoader) }.isSuccess) {
                return "Runtime hook framework detected"
            }
        }

        val pm = context.packageManager
        for (pkg in PATCHER_PACKAGES) {
            val installed = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
            }.isSuccess
            if (installed) {
                return "Unauthorized patching tool installed"
            }
        }

        val mapsFile = File("/proc/self/maps")
        if (mapsFile.canRead()) {
            val suspiciousMap = runCatching {
                mapsFile.bufferedReader().useLines { lines ->
                    lines.any { line ->
                        val lower = line.lowercase(Locale.US)
                        lower.contains("frida-agent") ||
                            lower.contains("frida-gadget") ||
                            lower.contains("libxposed")
                    }
                }
            }.getOrDefault(false)
            if (suspiciousMap) {
                return "Unauthorized memory instrumentation detected"
            }
        }

        return null
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().trimEnd('/').lowercase(Locale.US)
    }

    private fun isRunningInRobolectricTest(): Boolean {
        return Build.FINGERPRINT.equals("robolectric", ignoreCase = true) &&
            runCatching { Class.forName("org.robolectric.Robolectric") }.isSuccess
    }
}
