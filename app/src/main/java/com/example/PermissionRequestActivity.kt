package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Ultra-lightweight transparent Activity used ONLY when `RECORD_AUDIO` has not yet been
 * granted while the user taps the Mic (`🎙️`) key inside `RgbKeyboardService`.
 * Immediately displays the native Android microphone permission dialog and calls `finish()`
 * without ever opening `MainActivity`.
 */
class PermissionRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val permPrefs = getSharedPreferences(PREFS_PERMISSIONS, MODE_PRIVATE)
        if (alreadyGranted || permPrefs.getBoolean(KEY_MIC_ACTIVITY_PROMPTED_ONCE, false)) {
            finishQuietly()
            return
        }

        permPrefs.edit()
            .putBoolean(KEY_MIC_PROMPTED_ONCE, true)
            .putBoolean(KEY_MIC_ACTIVITY_PROMPTED_ONCE, true)
            .apply()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQ_RECORD_AUDIO
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        finishQuietly()
    }

    @Suppress("DEPRECATION")
    private fun finishQuietly() {
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val REQ_RECORD_AUDIO = 4091
        private const val PREFS_PERMISSIONS = "rakib_permissions_prefs"
        private const val KEY_MIC_PROMPTED_ONCE = "mic_permission_prompted_once"
        private const val KEY_MIC_ACTIVITY_PROMPTED_ONCE = "mic_activity_prompted_once"
    }
}
