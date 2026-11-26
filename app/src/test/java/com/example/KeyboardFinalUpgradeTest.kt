package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClipboardHistoryManager
import com.example.data.CustomBackgroundManager
import com.example.data.PromoBannerConfig
import com.example.data.TrialLicenseManager
import com.example.engine.SmartWordPredictor
import com.example.model.EmojiCatalog
import com.example.model.KeyboardSettings
import com.example.model.RgbEffect
import com.example.model.SwitchSoundProfile
import com.example.model.TouchTypingEffect
import com.example.security.AntiTamperGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyboardFinalUpgradeTest {

    @Test
    fun `anti tamper guard validates official config and rejects tampered Firebase url`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val valid = AntiTamperGuard.verifyAppIntegrity(context)
        assertTrue(valid is AntiTamperGuard.TamperCheckResult.Valid)

        val tampered = AntiTamperGuard.verifyAppIntegrity(
            context = context,
            firebaseDatabaseUrl = "https://attacker-db.firebaseio.com"
        )
        assertTrue(tampered is AntiTamperGuard.TamperCheckResult.Tampered)
    }

    @Test
    fun `anti tamper guard HMAC catches forged local VIP flags`() {
        val legitHmac = AntiTamperGuard.computePrefsIntegrityHmac(
            deviceId = "DEV123",
            rawAndroidId = "android-id",
            packageName = AntiTamperGuard.EXPECTED_PACKAGE_NAME,
            isLifetimeUnlocked = false,
            isBanglishLifetimeUnlocked = false,
            banglishExpiryMs = 0L,
            vipExpiryMs = 0L,
            adminGrantedVip = false,
            adminAllFreeDevice = false,
            adminGlobalAllFree = false,
            deviceCustomExpiryMs = 0L,
            unlockMethod = "none",
            unlockTxnId = ""
        )

        assertTrue(
            AntiTamperGuard.verifyPrefsIntegrityHmac(
                providedHmac = legitHmac,
                deviceId = "DEV123",
                rawAndroidId = "android-id",
                packageName = AntiTamperGuard.EXPECTED_PACKAGE_NAME,
                isLifetimeUnlocked = false,
                isBanglishLifetimeUnlocked = false,
                banglishExpiryMs = 0L,
                vipExpiryMs = 0L,
                adminGrantedVip = false,
                adminAllFreeDevice = false,
                adminGlobalAllFree = false,
                deviceCustomExpiryMs = 0L,
                unlockMethod = "none",
                unlockTxnId = ""
            )
        )

        // Flip isLifetimeUnlocked = true without re-signing -> must fail verification
        assertFalse(
            AntiTamperGuard.verifyPrefsIntegrityHmac(
                providedHmac = legitHmac,
                deviceId = "DEV123",
                rawAndroidId = "android-id",
                packageName = AntiTamperGuard.EXPECTED_PACKAGE_NAME,
                isLifetimeUnlocked = true,
                isBanglishLifetimeUnlocked = false,
                banglishExpiryMs = 0L,
                vipExpiryMs = 0L,
                adminGrantedVip = false,
                adminAllFreeDevice = false,
                adminGlobalAllFree = false,
                deviceCustomExpiryMs = 0L,
                unlockMethod = "none",
                unlockTxnId = ""
            )
        )
    }

    @Test
    fun `sound catalog has 53 plus gaming and mechanical fx with silent default`() {
        assertEquals(SwitchSoundProfile.SILENT, KeyboardSettings().switchSound)
        assertTrue(
            "Expected at least 53 playable sound profiles, got ${SwitchSoundProfile.playableEntries.size}",
            SwitchSoundProfile.playableEntries.size >= 53
        )
    }

    @Test
    fun `clean UPI URI excludes mc tr tn and encodes merchant cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TrialLicenseManager.getInstance(context)
        val uri = manager.buildUpiQrPayload(
            planScope = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
            uniqueAmountFormatted = "19.83"
        )
        assertTrue(uri.startsWith("upi://pay?pa="))
        assertTrue(uri.contains("pn=QUALITY%20FOODS"))
        assertTrue(uri.contains("&am=19.83&cu=INR"))
        assertFalse(uri.contains("mc="))
        assertFalse(uri.contains("tr="))
        assertFalse(uri.contains("tn="))
    }

    @Test
    fun `top promo banner 2 defaults to Special Features`() {
        assertEquals("✨ Special Features", PromoBannerConfig.DEFAULT_BANNER2_TITLE)
    }

    @Test
    fun `unique AI emojis tab has 80 plus items and contextual emoji predictor responds`() {
        val aiCategory = EmojiCatalog.categories.firstOrNull { it.id == "ai_unique" }
        assertNotNull(aiCategory)
        assertTrue("Expected >= 80 AI emojis", aiCategory!!.emojis.size >= 80)

        val enLove = SmartWordPredictor.predict("I love ", "en")
        assertTrue(enLove.suggestedEmojis.contains("❤️"))

        val bnBangla = SmartWordPredictor.predict("bhalobasha ", "bn_en")
        assertTrue(bnBangla.suggestedEmojis.isNotEmpty())
    }

    @Test
    fun `brightness mapping maps 0 to 100 percent smoothly onto dimAlpha bounds`() {
        assertEquals(CustomBackgroundManager.MIN_DIM_ALPHA, CustomBackgroundManager.brightnessToDimAlpha(1f), 0.001f)
        assertEquals(CustomBackgroundManager.MAX_DIM_ALPHA, CustomBackgroundManager.brightnessToDimAlpha(0f), 0.001f)
    }

    @Test
    fun `clipboard history tracks fresh text and screenshot quick paste safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = ClipboardHistoryManager.getInstance(context)
        clipboard.copyAndSave("Hello AI Keyboard")
        assertTrue(clipboard.hasFreshTextClip.value)
        clipboard.markClipboardPasted("Hello AI Keyboard")
        assertFalse(clipboard.hasFreshTextClip.value)

        val fakeUri = Uri.parse("content://media/external/images/media/999")
        clipboard.setScreenshotClipForTesting(fakeUri)
        assertEquals(fakeUri, clipboard.latestScreenshotClip.value?.uri)
        clipboard.dismissScreenshotClip()
        assertEquals(null, clipboard.latestScreenshotClip.value)
    }

    @Test
    fun `fresh device install grants 7 day free VIP trial and defaults to Classic Black plus Touch Response and Key borders`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TrialLicenseManager.getInstance(context)
        val state = manager.refreshState()

        assertTrue("Expected VIP active during 7-day trial", state.isVipActive)
        assertFalse("Fresh install should not be expired", state.isTrialExpired)
        assertTrue("Fresh trial should grant > 6 days", state.remainingSeconds > 6L * 86_400L)
        assertTrue("RGB themes should be unlocked during trial", state.isRgbThemesUnlocked)

        val defaultSettings = KeyboardSettings()
        assertEquals(RgbEffect.NORMAL_DEFAULT, defaultSettings.effect)
        assertEquals(TouchTypingEffect.AI_NEURAL_PULSE, defaultSettings.touchEffect)
        assertTrue(defaultSettings.showKeyBorders)
        assertEquals(SwitchSoundProfile.SILENT, defaultSettings.switchSound)
    }

    @Test
    fun `keyboard strip chip uses Special Features instead of Bangla or Romanized Bengali`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val label = context.getString(R.string.chip_bangla_to_banglish)
        val preview = context.getString(R.string.chip_bangla_to_banglish_preview, "kemon")
        val paste = context.getString(R.string.chip_bangla_to_banglish_paste)

        assertTrue(label.contains("Special Features"))
        assertTrue(preview.contains("Special Features"))
        assertTrue(paste.contains("Special Features"))
        assertFalse(label.contains("বাংলা"))
        assertFalse(label.contains("Romanized Bengali", ignoreCase = true))
    }

    @Test
    fun `legacy pre-HMAC privilege flags migrate safely on app update without false lockout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resetSingleton = {
            TrialLicenseManager::class.java.getDeclaredField("instance").apply {
                isAccessible = true
                set(null, null)
            }
        }
        resetSingleton()
        try {
            val prefs = context.getSharedPreferences("rakib_trial_license_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .clear()
                .putBoolean("is_lifetime_unlocked", true)
                .putBoolean("admin_global_all_free", true)
                .commit()

            val manager = TrialLicenseManager.getInstance(context)
            val state = manager.refreshState()

            assertFalse("Unverified legacy lifetime flag should reset cleanly", state.isLifetimeUnlocked)
            assertFalse("Unverified legacy global flag should reset cleanly", state.isGlobalAllFree)
            assertTrue("7-day free trial should remain active after migration", state.isVipActive)
            assertFalse(prefs.getString("prefs_integrity_hmac", null).isNullOrBlank())
        } finally {
            resetSingleton()
        }
    }

    @Test
    fun `post-seal manual XML privilege tampering triggers integrity violation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resetSingleton = {
            TrialLicenseManager::class.java.getDeclaredField("instance").apply {
                isAccessible = true
                set(null, null)
            }
        }
        resetSingleton()
        try {
            val manager = TrialLicenseManager.getInstance(context)
            val prefs = context.getSharedPreferences("rakib_trial_license_prefs", Context.MODE_PRIVATE)
            assertFalse(prefs.getString("prefs_integrity_hmac", null).isNullOrBlank())

            var caughtSecurityException = false
            try {
                // Simulate unauthorized local XML edit without updating HMAC
                prefs.edit().putBoolean("is_lifetime_unlocked", true).commit()
                manager.refreshState()
            } catch (_: SecurityException) {
                caughtSecurityException = true
            }
            assertTrue("Expected SecurityException on post-seal privilege tampering", caughtSecurityException)
            assertFalse(prefs.getBoolean("is_lifetime_unlocked", false))
        } finally {
            resetSingleton()
        }
    }
}
