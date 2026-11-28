package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.BannerPresetCatalog
import com.example.data.ClipboardHistoryManager
import com.example.data.ComboCategory
import com.example.data.ComboThemeCatalog
import com.example.data.CustomBackgroundManager
import com.example.data.FirebaseRemoteManager
import com.example.data.KeyboardPreferencesRepository
import com.example.data.PromoBannerConfig
import com.example.data.TrialLicenseManager
import com.example.data.UpiVerificationResult
import com.example.engine.SmartWordPredictor
import com.example.model.AppUiLanguage
import com.example.model.EmojiCatalog
import com.example.model.appStringsFor
import com.example.model.KeyboardSettings
import com.example.model.RgbEffect
import com.example.model.RgbPalette
import com.example.model.SwitchSoundProfile
import com.example.model.TouchTypingEffect
import com.example.security.AntiTamperGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.File
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
        assertEquals(TouchTypingEffect.TOUCH_SHOCKWAVE_RING, defaultSettings.touchEffect)
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

    @Test
    fun `catalogs expose 106 touch fx 105 themes and 104 hd photos`() {
        assertTrue(
            "Expected >= 106 TouchTypingEffect entries, got ${TouchTypingEffect.activeEffects.size}",
            TouchTypingEffect.activeEffects.size >= 106
        )
        val combos = ComboThemeCatalog.byCategory(ComboCategory.ALL)
        assertTrue(
            "Expected >= 105 ComboThemeCatalog entries, got ${combos.size}",
            combos.size >= 105
        )
        assertTrue(
            "Expected >= 104 static HD photo presets, got ${BannerPresetCatalog.staticBannerPresets.size}",
            BannerPresetCatalog.staticBannerPresets.size >= 104
        )
    }

    @Test
    fun `trial seeds Cute Cartoon Sky once and expiry reverts preset while preserving custom gallery photo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bgPrefs = context.getSharedPreferences("rakib_custom_theme_prefs", Context.MODE_PRIVATE)
        bgPrefs.edit().clear().commit()
        val manager = CustomBackgroundManager.getInstance(context)

        // 1. Trial seeds Cute Cartoon Sky
        manager.seedTrialDefaultPresetIfUnset()
        assertEquals(BannerPresetCatalog.PRESET_CUTE_CARTOON, manager.getCurrentTheme().presetId)

        // 2. Expiry without custom gallery photo clears preset back to Classic Black
        manager.resetPostTrialVisualRevertFlag()
        manager.revertPaidVisualsOnTrialExpiryPreservingGalleryPhoto()
        assertNull(manager.getCurrentTheme().presetId)
        assertFalse(manager.getCurrentTheme().hasPhoto)

        // 3. With a custom gallery photo saved, expiry preserves the user's photo permanently free
        val customPhotoFile = File(context.filesDir, "user_custom_gallery_test.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        try {
            bgPrefs.edit()
                .remove("custom_bg_preset_id")
                .remove("downloaded_theme_id")
                .putString("custom_bg_path", customPhotoFile.absolutePath)
                .commit()
            manager.resetPostTrialVisualRevertFlag()
            manager.revertPaidVisualsOnTrialExpiryPreservingGalleryPhoto()
            assertTrue(manager.getCurrentTheme().hasCustomGalleryPhoto)
            assertEquals(customPhotoFile.absolutePath, manager.getCurrentTheme().photoPath)
        } finally {
            customPhotoFile.delete()
        }
    }

    @Test
    fun `clipboard prunes clips older than 1 hour blocks sensitive internal text and dismisses one-time chip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = ClipboardHistoryManager.getInstance(context)
        clipboard.clearAll()

        // Sensitive internal URL/UPI/merchant phone must never be stored
        clipboard.copyAndSave("https://rakib-ai-keyboard-default-rtdb.firebaseio.com/secret")
        clipboard.copyAndSave("upi://pay?pa=6296603748@ybl&pn=QUALITY%20FOODS")
        assertTrue(clipboard.clips.value.isEmpty())

        // Old clip (> 1 hour) gets pruned immediately while recent clip stays
        val now = System.currentTimeMillis()
        clipboard.seedClipRecordForTesting("Recent note", now - 5 * 60_000L)
        clipboard.seedClipRecordForTesting("Expired old note", now - 3_700_000L)
        clipboard.pruneExpiredClips(now)
        assertTrue(clipboard.clips.value.contains("Recent note"))
        assertFalse(clipboard.clips.value.contains("Expired old note"))

        // One-time dismissal hides the fresh text chip without deleting history
        clipboard.copyAndSave("One time paste chip")
        assertTrue(clipboard.hasFreshTextClip.value)
        clipboard.dismissFreshTextClip()
        assertFalse(clipboard.hasFreshTextClip.value)
        assertTrue(clipboard.clips.value.contains("One time paste chip"))
    }

    @Test
    fun `bottom navigation uses crisp English labels across all app languages`() {
        AppUiLanguage.entries.forEach { lang ->
            val strings = appStringsFor(lang)
            assertEquals("Studio", strings.navStudio)
            assertEquals("Language", strings.navLanguages)
            assertEquals("Effects", strings.navEffects)
            assertEquals("Feel", strings.navFeel)
            assertEquals("Settings", strings.navSettings)
            assertEquals("Device Info", strings.navDeviceInfo)
        }
    }

    @Test
    fun `contextual emoji predictor responds to expanded English Banglish Bengali and Hindi cues`() {
        val cricket = SmartWordPredictor.predict("Let's play cricket ", "en")
        assertTrue(cricket.suggestedEmojis.contains("🏏"))

        val banglishCoffee = SmartWordPredictor.predict("ek cup cha ", "bn_en")
        assertTrue(banglishCoffee.suggestedEmojis.any { it == "☕" || it == "🍵" })

        val hindiNamaste = SmartWordPredictor.predict("namaste ", "hi")
        assertTrue(hindiNamaste.suggestedEmojis.contains("🙏"))
    }

    @Test
    fun `anti tamper guard HMAC includes isDeviceBlocked state`() {
        val unblocked = AntiTamperGuard.computePrefsIntegrityHmac(
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
            unlockTxnId = "",
            isDeviceBlocked = false
        )
        val blocked = AntiTamperGuard.computePrefsIntegrityHmac(
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
            unlockTxnId = "",
            isDeviceBlocked = true
        )
        assertNotEquals(unblocked, blocked)
    }

    @Test
    fun `normalizeEpochMs and buildCleanDeviceName reject zero or corrupt epochs and format clean model names`() {
        assertEquals(0L, FirebaseRemoteManager.normalizeEpochMs(null))
        assertEquals(0L, FirebaseRemoteManager.normalizeEpochMs(0L))
        assertEquals(0L, FirebaseRemoteManager.normalizeEpochMs(-5000L))
        assertEquals(0L, FirebaseRemoteManager.normalizeEpochMs(Long.MAX_VALUE))
        assertEquals(1_750_000_000_000L, FirebaseRemoteManager.normalizeEpochMs(1_750_000_000L))
        assertEquals(1_750_000_123_000L, FirebaseRemoteManager.normalizeEpochMs(1_750_000_123_000L))

        assertEquals("Samsung SM-M215F", FirebaseRemoteManager.buildCleanDeviceName("samsung", "SM-M215F"))
        assertEquals("Google Pixel 8", FirebaseRemoteManager.buildCleanDeviceName("Google", "Google Pixel 8"))
        assertEquals("Xiaomi Device", FirebaseRemoteManager.buildCleanDeviceName("xiaomi", ""))
    }

    @Test
    fun `corrupt zero trial timestamps self heal to valid 7 day trial and never display 20697d or LIFETIME VIP`() {
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
                .putLong("trial_install_wall_ms", 0L)
                .putLong("first_install_timestamp_ms", 0L)
                .putLong("max_seen_elapsed_ms", Long.MAX_VALUE / 2L)
                .commit()

            val manager = TrialLicenseManager.getInstance(context)
            val state = manager.refreshState()

            assertFalse("Trial must NOT report LIFETIME VIP", state.isTrueLifetimeDisplay)
            assertFalse("Trial must NOT set isLifetimeUnlocked", state.isLifetimeUnlocked)
            assertTrue("Healed trial must be active", state.isVipActive)
            assertTrue(
                "Healed trial days must be <= 7 (never 20697d), got ${state.remainingDays}",
                state.remainingDays in 6L..7L
            )
            assertFalse(state.formattedVipPillText.contains("LIFETIME VIP"))
            assertTrue(state.trialStartedAtMs > 1_577_836_800_000L)
            assertTrue(state.trialExpiresAtMs > state.trialStartedAtMs)
        } finally {
            resetSingleton()
        }
    }

    @Test
    fun `upi intent uri keeps literal merchant vpa and verifies PhonePe GPay success without txnid`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = TrialLicenseManager.getInstance(context)
        val uri = manager.buildUpiQrPayload(
            planScope = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
            uniqueAmountFormatted = "19.83"
        )
        assertTrue(uri.contains("pa=6296603748@okbizaxis"))
        assertFalse(uri.contains("%40"))

        val phonePeSuccessIntent = Intent().apply {
            putExtra("response", "Status=SUCCESS&responseCode=00&ApprovalRefNo=412345678901")
        }
        val result = manager.verifyUpiIntentResult(
            resultCode = Activity.RESULT_OK,
            data = phonePeSuccessIntent,
            expectedTxnRef = "RKVIP12345",
            planScope = TrialLicenseManager.PLAN_SCOPE_VIP_FEATURES,
            purchasedDurationDays = 0
        )
        assertTrue("Expected Success even when txnid extra is omitted, got $result", result is UpiVerificationResult.Success)
    }

    @Test
    fun `keyboard preferences synchronous cache survives singleton reload without default flash`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resetRepo = {
            KeyboardPreferencesRepository::class.java.getDeclaredField("instance").apply {
                isAccessible = true
                set(null, null)
            }
        }
        resetRepo()
        try {
            val repo1 = KeyboardPreferencesRepository.getInstance(context)
            repo1.updateActiveLanguage("bn")
            repo1.updateEffect(RgbEffect.LIGHTNING_ARC)
            repo1.updateTouchEffect(TouchTypingEffect.TOUCH_MAGMA_EMBER)
            repo1.updatePalette(RgbPalette.SPECTRUM_PRISM)
            repo1.updateShowKeyBorders(false)

            // Simulate IME service restart creating a fresh repository instance
            resetRepo()
            val repo2 = KeyboardPreferencesRepository.getInstance(context)
            val cached = repo2.getCachedSettingsSnapshot()

            assertEquals("bn", cached.activeLanguageId)
            assertEquals(RgbEffect.LIGHTNING_ARC, cached.effect)
            assertEquals(TouchTypingEffect.TOUCH_MAGMA_EMBER, cached.touchEffect)
            assertEquals(RgbPalette.SPECTRUM_PRISM, cached.palette)
            assertFalse(cached.showKeyBorders)
        } finally {
            resetRepo()
        }
    }

    @Test
    fun `LicenseUiState strictly clamps remainingMillis to 0 through trialDays when not lifetime or all-free`() {
        val hugeState = com.example.data.LicenseUiState(
            isLifetimeUnlocked = false,
            isGlobalAllFree = false,
            remainingMillis = 26_697L * 86_400_000L,
            remainingSeconds = 26_697L * 86_400L,
            adminConfig = com.example.data.RemoteAdminConfig(freeTrialDays = 7),
            deviceOverrides = com.example.data.DeviceRemoteOverrides(adminAllFree = false)
        )
        assertEquals(7L * 86_400_000L, hugeState.clampedRemainingMillis)
        assertEquals(7L, hugeState.remainingDays)
        assertFalse(hugeState.formattedVipPillText.contains("26697"))
        assertTrue(hugeState.formattedVipPillText.contains("7d"))

        val customTrialState = com.example.data.LicenseUiState(
            isLifetimeUnlocked = false,
            isGlobalAllFree = false,
            remainingMillis = 26_697L * 86_400_000L,
            remainingSeconds = 26_697L * 86_400L,
            adminConfig = com.example.data.RemoteAdminConfig(freeTrialDays = 14),
            deviceOverrides = com.example.data.DeviceRemoteOverrides(adminAllFree = false)
        )
        assertEquals(14L * 86_400_000L, customTrialState.clampedRemainingMillis)
        assertEquals(14L, customTrialState.remainingDays)

        val lifetimeState = hugeState.copy(isLifetimeUnlocked = true)
        assertEquals("👑 LIFETIME VIP", lifetimeState.formattedVipPillText)
    }

    @Test
    fun `far future year-2100 expiry timestamps sanitize cleanly and never display 26697d`() {
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
            val year2100Ms = 4_102_444_800_000L
            prefs.edit()
                .clear()
                .putLong("vip_expiry_timestamp_ms", year2100Ms)
                .putLong("device_custom_expiry_ms", year2100Ms)
                .putLong("banglish_expiry_timestamp_ms", year2100Ms)
                .commit()

            val manager = TrialLicenseManager.getInstance(context)
            val state = manager.refreshState()

            assertFalse(state.isLifetimeUnlocked)
            assertFalse(state.isGlobalAllFree)
            assertFalse(state.deviceOverrides.adminAllFree)
            assertTrue(
                "Expected remainingMillis in 0..7d, got ${state.remainingMillis}",
                state.remainingMillis in 0L..(7L * 86_400_000L)
            )
            assertTrue(
                "Expected remainingDays <= 7, got ${state.remainingDays}",
                state.remainingDays in 0L..7L
            )
            assertFalse(state.formattedVipPillText.contains("26697"))
            assertFalse(state.formattedVipPillText.contains("LIFETIME VIP"))
            assertEquals(0L, prefs.getLong("vip_expiry_timestamp_ms", -1L))
            assertEquals(0L, prefs.getLong("device_custom_expiry_ms", -1L))
            assertEquals(0L, prefs.getLong("banglish_expiry_timestamp_ms", -1L))
        } finally {
            resetSingleton()
        }
    }

    @Test
    fun `legacy pre-v5 leftover ~5 day trial resets cleanly to exact 7 day trial while v5 trial persists`() {
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
            val twoDaysAgo = System.currentTimeMillis() - (2L * 86_400_000L)
            prefs.edit()
                .clear()
                .putInt("trial_policy_version", 4)
                .putLong("first_install_timestamp", twoDaysAgo)
                .putLong("first_launch_timestamp_ms", twoDaysAgo)
                .commit()

            val manager1 = TrialLicenseManager.getInstance(context)
            val state1 = manager1.refreshState()

            assertTrue("Upgraded v5 trial should restore full 7-day window", state1.remainingSeconds >= 6L * 86_400L + 86_300L)
            assertEquals(5, prefs.getInt("trial_policy_version", 0))
            assertFalse(manager1.getAppVersionLabel().isBlank())

            // Now simulate an existing v5 install with 2 days elapsed -> must NOT reset back to 7 days
            resetSingleton()
            prefs.edit()
                .putInt("trial_policy_version", 5)
                .putLong("first_install_timestamp", twoDaysAgo)
                .putLong("first_launch_timestamp_ms", twoDaysAgo)
                .remove("prefs_integrity_hmac")
                .commit()

            val manager2 = TrialLicenseManager.getInstance(context)
            val state2 = manager2.refreshState()
            assertTrue("Existing v5 trial must preserve elapsed 2 days (~5d left)", state2.remainingDays in 4L..5L)
        } finally {
            resetSingleton()
        }
    }

    @Test
    fun `touch response auto-attaches matching banner, allows banner switching, and clears when RGB or Theme is selected`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.StudioViewModel(app)
        val idleMain = {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(60)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }

        // 1. Selecting a Touch Response automatically attaches its matching default banner
        vm.setTouchEffect(TouchTypingEffect.TOUCH_MAGMA_EMBER)
        idleMain()

        val repo = KeyboardPreferencesRepository.getInstance(app)
        val bg = CustomBackgroundManager.getInstance(app)
        assertEquals(
            BannerPresetCatalog.defaultBannerForTouchEffect(TouchTypingEffect.TOUCH_MAGMA_EMBER),
            bg.getCurrentTheme().presetId
        )
        assertEquals(TouchTypingEffect.TOUCH_MAGMA_EMBER, repo.getCachedSettingsSnapshot().touchEffect)
        assertEquals(RgbEffect.NORMAL_DEFAULT, repo.getCachedSettingsSnapshot().effect)

        // 2. User can freely switch to any other Banner afterward while keeping Touch Response active
        vm.applyBannerPhotoPreservingTouch(BannerPresetCatalog.PRESET_CUTE_CARTOON)
        idleMain()
        assertEquals(BannerPresetCatalog.PRESET_CUTE_CARTOON, bg.getCurrentTheme().presetId)
        assertEquals(TouchTypingEffect.TOUCH_MAGMA_EMBER, repo.getCachedSettingsSnapshot().touchEffect)

        // 3. Selecting RGB Design resets Touch Response and clears Photo/Banner
        vm.setEffect(RgbEffect.LIGHTNING_ARC)
        idleMain()
        assertEquals(RgbEffect.LIGHTNING_ARC, repo.getCachedSettingsSnapshot().effect)
        assertEquals(TouchTypingEffect.NONE, repo.getCachedSettingsSnapshot().touchEffect)
        assertFalse(bg.getCurrentTheme().hasPhoto)

        // 4. Switching back to Touch Response turns off RGB Design and pairs its default banner
        vm.setTouchEffect(TouchTypingEffect.TOUCH_SHOCKWAVE_RING)
        idleMain()
        assertEquals(TouchTypingEffect.TOUCH_SHOCKWAVE_RING, repo.getCachedSettingsSnapshot().touchEffect)
        assertEquals(
            BannerPresetCatalog.defaultBannerForTouchEffect(TouchTypingEffect.TOUCH_SHOCKWAVE_RING),
            bg.getCurrentTheme().presetId
        )
        assertEquals(RgbEffect.NORMAL_DEFAULT, repo.getCachedSettingsSnapshot().effect)

        // 5. Selecting a Theme turns off Touch Response
        val combo = ComboThemeCatalog.byCategory(ComboCategory.ALL).first()
        vm.applyComboTheme(combo)
        idleMain()
        assertEquals(TouchTypingEffect.NONE, repo.getCachedSettingsSnapshot().touchEffect)
    }

    @Test
    fun `every touch response maps to a valid built-in banner preset`() {
        TouchTypingEffect.activeEffects.forEach { fx ->
            val presetId = BannerPresetCatalog.defaultBannerForTouchEffect(fx)
            assertNotNull(
                "Missing BannerPresetCatalog entry '$presetId' for touch effect ${fx.name}",
                BannerPresetCatalog.findById(presetId)
            )
        }
    }

    @Test
    fun `anti tamper guard v2 HMAC detects trial timestamp and sticky expiry tampering`() {
        val now = 1_770_000_000_000L
        val hmac = AntiTamperGuard.computePrefsIntegrityHmacV2(
            deviceId = "RK-1234ABCD",
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
            unlockMethod = null,
            unlockTxnId = null,
            isDeviceBlocked = false,
            firstInstallTimestampMs = now,
            maxSeenElapsedMs = 86_400_000L,
            trialExpiredSticky = true,
            reinstallDetected = true,
            clockRollbackDetected = false
        )

        assertTrue(
            AntiTamperGuard.verifyPrefsIntegrityHmacV2(
                providedHmac = hmac,
                deviceId = "RK-1234ABCD",
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
                unlockMethod = null,
                unlockTxnId = null,
                isDeviceBlocked = false,
                firstInstallTimestampMs = now,
                maxSeenElapsedMs = 86_400_000L,
                trialExpiredSticky = true,
                reinstallDetected = true,
                clockRollbackDetected = false
            )
        )

        // Clearing trialExpiredSticky without re-signing must fail
        assertFalse(
            AntiTamperGuard.verifyPrefsIntegrityHmacV2(
                providedHmac = hmac,
                deviceId = "RK-1234ABCD",
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
                unlockMethod = null,
                unlockTxnId = null,
                isDeviceBlocked = false,
                firstInstallTimestampMs = now,
                maxSeenElapsedMs = 86_400_000L,
                trialExpiredSticky = false,
                reinstallDetected = true,
                clockRollbackDetected = false
            )
        )
    }

    @Test
    fun `touch response catalog exceeds 200 effects with fire flame styles and UI strings hide Firebase`() {
        val allTouch = TouchTypingEffect.byCategory(com.example.model.TouchEffectCategory.ALL)
        assertTrue("Expected >= 200 touch responses, got ${allTouch.size}", allTouch.size >= 200)
        assertEquals(allTouch.size, allTouch.map { it.id }.distinct().size)
        assertTrue(
            "Expected multiple RISING_EMBER fire/flame effects",
            allTouch.count { it.visualStyle == com.example.model.TouchEffectVisualStyle.RISING_EMBER } >= 15
        )

        AppUiLanguage.entries.forEach { lang ->
            val strings = appStringsFor(lang)
            assertEquals("Device Info", strings.navDeviceInfo)
            assertFalse(strings.settingsAutoRestoreNote.contains("Firebase", ignoreCase = true))
        }
    }

    @Test
    fun `lifetime VIP keeps Special Feature gated separately and expired VIP hides badge and locks paid items while keeping gallery photo free`() {
        // 1. Lifetime VIP (Card 1) does NOT unlock Special Feature (Rumi / Nice Bengali / Banglish) after trial expiry
        val lifetimeOnlyState = com.example.data.LicenseUiState(
            isLifetimeUnlocked = true,
            isTrialExpired = true,
            remainingMillis = 0L,
            remainingSeconds = 0L
        )
        assertTrue("Lifetime VIP must keep VIP active", lifetimeOnlyState.isVipActive)
        assertTrue("Lifetime VIP unlocks RGB/Themes", lifetimeOnlyState.isRgbThemesUnlocked)
        assertFalse(
            "Lifetime VIP must NOT unlock Special Feature (Banglish) without separate payment",
            lifetimeOnlyState.isBanglishUnlocked
        )

        // Separate payment unlocks Special Feature independently
        val bothUnlockedState = lifetimeOnlyState.copy(isBanglishLifetimeUnlocked = true)
        assertTrue(bothUnlockedState.isBanglishUnlocked)

        // 2. Expired VIP (isVipActive == false) drops VIP badge state, locks paid themes/banners/RGB/TouchFX,
        // while keeping Normal Black & custom gallery photo permanently free
        val expiredState = com.example.data.LicenseUiState(
            isLifetimeUnlocked = false,
            isTrialExpired = true,
            remainingMillis = 0L,
            remainingSeconds = 0L
        )
        assertFalse("Expired state must mark isVipActive false (hides VIP blue badge)", expiredState.isVipActive)
        assertTrue(expiredState.isFreeStandardMode)
        assertTrue(expiredState.isThemeItemUnlocked(RgbEffect.NORMAL_DEFAULT.id, isPermanentlyFree = true))
        assertTrue(expiredState.isThemeItemUnlocked("custom_gallery_photo", isPermanentlyFree = true))
        assertFalse(expiredState.isThemeItemUnlocked(RgbEffect.LIGHTNING_ARC.id, isPermanentlyFree = false))
        assertFalse(expiredState.isThemeItemUnlocked(TouchTypingEffect.TOUCH_SHOCKWAVE_RING.id, isPermanentlyFree = false))
        assertFalse(expiredState.isThemeItemUnlocked(BannerPresetCatalog.PRESET_CUTE_CARTOON, isPermanentlyFree = false))

        // 3. ViewModel blocks paid visual setters when VIP is expired while preserving user custom gallery photo
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val resetSingleton = {
            TrialLicenseManager::class.java.getDeclaredField("instance").apply {
                isAccessible = true
                set(null, null)
            }
        }
        resetSingleton()
        try {
            val prefs = app.getSharedPreferences("rakib_trial_license_prefs", Context.MODE_PRIVATE)
            val expiredInstallMs = System.currentTimeMillis() - (10L * 86_400_000L)
            prefs.edit()
                .clear()
                .putInt("trial_policy_version", 5)
                .putLong("first_install_timestamp", expiredInstallMs)
                .putLong("first_launch_timestamp_ms", expiredInstallMs)
                .putLong("max_seen_elapsed_ms", 10L * 86_400_000L)
                .putBoolean("trial_expired_sticky", true)
                .commit()

            val vm = com.example.viewmodel.StudioViewModel(app)
            vm.refreshLicenseState()
            val bg = CustomBackgroundManager.getInstance(app)
            val repo = KeyboardPreferencesRepository.getInstance(app)

            // Save a custom user gallery bitmap (must stay allowed after expiry!)
            val customBmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
            vm.saveCroppedPhotoBitmap(customBmp, 0.4f, true)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(60)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue("User custom gallery photo must remain free after VIP expiry", bg.getCurrentTheme().hasCustomGalleryPhoto)

            // Attempting to apply locked RGB, Touch FX, or built-in preset must be blocked cleanly
            vm.setEffect(RgbEffect.LIGHTNING_ARC)
            vm.setTouchEffect(TouchTypingEffect.TOUCH_MAGMA_EMBER)
            vm.selectBannerPreset(BannerPresetCatalog.PRESET_SUNSET_GLOW)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(60)
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

            assertEquals(RgbEffect.NORMAL_DEFAULT, repo.getCachedSettingsSnapshot().effect)
            assertNotEquals(TouchTypingEffect.TOUCH_MAGMA_EMBER, repo.getCachedSettingsSnapshot().touchEffect)
            assertTrue("Built-in banner must not overwrite user custom gallery photo when expired", bg.getCurrentTheme().hasCustomGalleryPhoto)
        } finally {
            resetSingleton()
        }
    }
}



