package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.concurrent.Executors

data class ScreenshotClip(
    val uri: Uri,
    val timestampMs: Long,
    val mimeType: String = "image/png"
)

/**
 * Manages real system clipboard history for both RgbKeyboardService and the Companion Studio.
 * Listens to Android's ClipboardManager (text + screenshot/image URIs) and also observes
 * MediaStore screenshots safely without requesting broad storage permissions.
 */
class ClipboardHistoryManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val clipboardManager =
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val _clips = MutableStateFlow<List<String>>(loadPersistedClips())
    val clips: StateFlow<List<String>> = _clips.asStateFlow()

    private val _hasFreshTextClip = MutableStateFlow(false)
    val hasFreshTextClip: StateFlow<Boolean> = _hasFreshTextClip.asStateFlow()

    private val _latestScreenshotClip = MutableStateFlow<ScreenshotClip?>(null)
    val latestScreenshotClip: StateFlow<ScreenshotClip?> = _latestScreenshotClip.asStateFlow()

    private var lastPastedTextSignature: String? = null
    private var lastObservedTextSignature: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        lastPastedTextSignature = null
        captureCurrentPrimaryClip()
    }

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri != null) {
                inspectPotentialScreenshotUri(uri)
            }
        }
    }

    init {
        runCatching {
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
        }
        runCatching {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
            )
        }
        captureCurrentPrimaryClip()
    }

    fun captureCurrentPrimaryClip() {
        runCatching {
            val clipData = clipboardManager?.primaryClip ?: return
            if (clipData.itemCount <= 0) return
            val item = clipData.getItemAt(0) ?: return

            // 1. Detect screenshot/image copied directly to system clipboard (Android 11+ & rich copy)
            val clipUri = item.uri
            if (clipUri != null) {
                val resolvedMime = runCatching {
                    appContext.contentResolver.getType(clipUri)
                }.getOrNull() ?: run {
                    val desc = clipData.description
                    var found: String? = null
                    for (i in 0 until desc.mimeTypeCount) {
                        val m = desc.getMimeType(i)
                        if (m.startsWith("image/")) {
                            found = m
                            break
                        }
                    }
                    found
                }
                if (resolvedMime != null && resolvedMime.startsWith("image/")) {
                    updateScreenshotClip(clipUri, resolvedMime)
                }
            }

            // 2. Capture text clip and expose 1-tap auto-paste chip while fresh
            val text = item.coerceToText(appContext)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                val signature = text.take(MAX_CLIP_CHARS)
                val changed = signature != lastObservedTextSignature
                lastObservedTextSignature = signature
                addSnippetInternal(signature)
                if (signature != lastPastedTextSignature || changed) {
                    _hasFreshTextClip.value = signature != lastPastedTextSignature
                }
            }
        }
    }

    fun markClipboardPasted(pastedText: String) {
        lastPastedTextSignature = pastedText.trim().take(MAX_CLIP_CHARS)
        _hasFreshTextClip.value = false
    }

    fun dismissScreenshotClip() {
        _latestScreenshotClip.value = null
    }

    fun setScreenshotClipForTesting(uri: Uri, mimeType: String = "image/png") {
        updateScreenshotClip(uri, mimeType)
    }

    private fun updateScreenshotClip(uri: Uri, mimeType: String) {
        val current = _latestScreenshotClip.value
        if (current != null && current.uri == uri &&
            (System.currentTimeMillis() - current.timestampMs) < 5_000L
        ) {
            return
        }
        _latestScreenshotClip.value = ScreenshotClip(
            uri = uri,
            timestampMs = System.currentTimeMillis(),
            mimeType = mimeType
        )
    }

    private fun inspectPotentialScreenshotUri(uri: Uri) {
        ioExecutor.execute {
            runCatching {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_ADDED
                )
                appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use
                    val idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val bucketIdx = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val dateIdx = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val bucketName = if (bucketIdx >= 0) cursor.getString(bucketIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else "image/png"
                    val dateAddedSec = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val nowSec = System.currentTimeMillis() / 1000L

                    val isRecent = dateAddedSec == 0L || (nowSec - dateAddedSec) in -5..120
                    val looksLikeScreenshot =
                        displayName.contains("screenshot", ignoreCase = true) ||
                            bucketName.contains("screenshot", ignoreCase = true)
                    if (isRecent && looksLikeScreenshot) {
                        val contentUri = if (idIdx >= 0) {
                            val id = cursor.getLong(idIdx)
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        } else {
                            uri
                        }
                        updateScreenshotClip(
                            uri = contentUri,
                            mimeType = mime.ifBlank { "image/png" }
                        )
                    }
                }
            }
        }
    }

    fun copyAndSave(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        lastPastedTextSignature = null
        runCatching {
            clipboardManager?.setPrimaryClip(ClipData.newPlainText("Pro RGB Keyboard", cleaned))
        }
        addSnippetInternal(cleaned)
        _hasFreshTextClip.value = true
    }

    fun removeClip(text: String) {
        val updated = _clips.value.filterNot { it == text }
        _clips.value = updated
        if (updated.isEmpty()) _hasFreshTextClip.value = false
        persistClips(updated)
    }

    fun clearAll() {
        _clips.value = emptyList()
        _hasFreshTextClip.value = false
        _latestScreenshotClip.value = null
        persistClips(emptyList())
    }

    private fun addSnippetInternal(snippet: String) {
        val truncated = if (snippet.length > MAX_CLIP_CHARS) {
            snippet.take(MAX_CLIP_CHARS)
        } else {
            snippet
        }
        val current = _clips.value.toMutableList()
        current.removeAll { it == truncated }
        current.add(0, truncated)
        val trimmed = current.take(MAX_CLIPS)
        _clips.value = trimmed
        persistClips(trimmed)
    }

    private fun loadPersistedClips(): List<String> {
        val raw = prefs.getString(KEY_CLIPS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i)
                    if (value.isNotEmpty()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistClips(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(KEY_CLIPS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "pro_rgb_clipboard_history"
        private const val KEY_CLIPS = "clips_json"
        private const val MAX_CLIPS = 20
        private const val MAX_CLIP_CHARS = 500
        const val SCREENSHOT_FRESH_WINDOW_MS = 180_000L

        @Volatile
        private var instance: ClipboardHistoryManager? = null

        fun getInstance(context: Context): ClipboardHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: ClipboardHistoryManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
