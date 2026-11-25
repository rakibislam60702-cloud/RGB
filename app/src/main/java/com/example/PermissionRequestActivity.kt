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

        if (alreadyGranted) {
            finishQuietly()
            return
        }

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
    }
}
