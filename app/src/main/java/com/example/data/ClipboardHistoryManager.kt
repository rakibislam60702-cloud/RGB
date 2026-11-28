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
import org.json.JSONObject
import java.util.concurrent.Executors

data class ScreenshotClip(
    val uri: Uri,
    val timestampMs: Long,
    val mimeType: String = "image/png"
)

private data class StoredClipRecord(
    val text: String,
    val timestampMs: Long
)

/**
 * Manages real system clipboard history for both RgbKeyboardService and the Companion Studio.
 * - Filters out internal app URLs / RTDB endpoints / UPI payment intents.
 * - Auto-deletes saved clipboard items older than 1 hour ([ONE_HOUR_MS]).
 * - Surfaces copy / screenshot quick-paste chips once until pasted or dismissed when typing begins.
 */
class ClipboardHistoryManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val clipboardManager =
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var clipRecords: MutableList<StoredClipRecord> = loadPersistedRecords().toMutableList()
    private val _clips = MutableStateFlow(clipRecords.map { it.text })
    val clips: StateFlow<List<String>> = _clips.asStateFlow()

    private val _hasFreshTextClip = MutableStateFlow(false)
    val hasFreshTextClip: StateFlow<Boolean> = _hasFreshTextClip.asStateFlow()

    private val _latestScreenshotClip = MutableStateFlow<ScreenshotClip?>(null)
    val latestScreenshotClip: StateFlow<ScreenshotClip?> = _latestScreenshotClip.asStateFlow()

    private var lastPastedTextSignature: String? = null
    private var lastDismissedTextSignature: String? = null
    private var lastObservedTextSignature: String? = null
    private var lastDismissedScreenshotUri: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        lastPastedTextSignature = null
        lastDismissedTextSignature = null
        captureCurrentPrimaryClip(fromClipChangedEvent = true)
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
        pruneExpiredClips()
        captureCurrentPrimaryClip(fromClipChangedEvent = false)
    }

    fun pruneExpiredClips(nowMs: Long = System.currentTimeMillis()) {
        val fresh = clipRecords.filter { rec ->
            rec.text.isNotBlank() &&
                !isBlockedInternalClip(rec.text) &&
                (nowMs - rec.timestampMs) in 0L..ONE_HOUR_MS
        }
        if (fresh.size != clipRecords.size) {
            clipRecords = fresh.toMutableList()
            _clips.value = clipRecords.map { it.text }
            persistRecords(clipRecords)
            if (clipRecords.isEmpty()) {
                _hasFreshTextClip.value = false
            }
        }
        val activeShot = _latestScreenshotClip.value
        if (activeShot != null && (nowMs - activeShot.timestampMs) > SCREENSHOT_FRESH_WINDOW_MS) {
            _latestScreenshotClip.value = null
        }
    }

    fun captureCurrentPrimaryClip(fromClipChangedEvent: Boolean = false) {
        val nowMs = System.currentTimeMillis()
        pruneExpiredClips(nowMs)
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
                    updateScreenshotClip(clipUri, resolvedMime, forceNew = fromClipChangedEvent)
                }
            }

            // 2. Capture text clip and expose 1-tap auto-paste chip once while fresh
            val text = item.coerceToText(appContext)?.toString()?.trim()
            if (!text.isNullOrEmpty() && !isBlockedInternalClip(text)) {
                val signature = text.take(MAX_CLIP_CHARS)
                val isBrandNewObservation = fromClipChangedEvent ||
                    (lastObservedTextSignature == null && signature != lastPastedTextSignature && signature != lastDismissedTextSignature) ||
                    (lastObservedTextSignature != null && signature != lastObservedTextSignature)
                lastObservedTextSignature = signature
                addSnippetInternal(signature, nowMs)
                if (isBrandNewObservation &&
                    signature != lastPastedTextSignature &&
                    signature != lastDismissedTextSignature
                ) {
                    _hasFreshTextClip.value = true
                }
            }
        }
    }

    fun markClipboardPasted(pastedText: String) {
        val sig = pastedText.trim().take(MAX_CLIP_CHARS)
        lastPastedTextSignature = sig
        lastDismissedTextSignature = sig
        _hasFreshTextClip.value = false
    }

    /** Dismisses the one-time clipboard quick-paste suggestion chip as soon as user types. */
    fun dismissFreshTextClip() {
        val sig = lastObservedTextSignature ?: _clips.value.firstOrNull()
        if (!sig.isNullOrBlank()) {
            lastDismissedTextSignature = sig
        }
        _hasFreshTextClip.value = false
    }

    fun dismissScreenshotClip() {
        _latestScreenshotClip.value?.uri?.toString()?.let { uriStr ->
            lastDismissedScreenshotUri = uriStr
        }
        _latestScreenshotClip.value = null
    }

    fun setScreenshotClipForTesting(uri: Uri, mimeType: String = "image/png") {
        lastDismissedScreenshotUri = null
        updateScreenshotClip(uri, mimeType, forceNew = true)
    }

    private fun updateScreenshotClip(uri: Uri, mimeType: String, forceNew: Boolean = false) {
        val uriStr = uri.toString()
        if (!forceNew && uriStr == lastDismissedScreenshotUri) {
            return
        }
        val current = _latestScreenshotClip.value
        if (current != null && current.uri == uri &&
            (System.currentTimeMillis() - current.timestampMs) < 5_000L
        ) {
            return
        }
        lastDismissedScreenshotUri = null
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
        if (cleaned.isEmpty() || isBlockedInternalClip(cleaned)) return
        lastPastedTextSignature = null
        lastDismissedTextSignature = null
        lastObservedTextSignature = cleaned.take(MAX_CLIP_CHARS)
        runCatching {
            clipboardManager?.setPrimaryClip(ClipData.newPlainText("Pro RGB Keyboard", cleaned))
        }
        addSnippetInternal(cleaned, System.currentTimeMillis())
        _hasFreshTextClip.value = true
    }

    fun removeClip(text: String) {
        clipRecords = clipRecords.filterNot { it.text == text }.toMutableList()
        _clips.value = clipRecords.map { it.text }
        if (clipRecords.isEmpty()) _hasFreshTextClip.value = false
        persistRecords(clipRecords)
    }

    fun clearAll() {
        clipRecords.clear()
        _clips.value = emptyList()
        _hasFreshTextClip.value = false
        _latestScreenshotClip.value = null
        persistRecords(emptyList())
    }

    private fun addSnippetInternal(snippet: String, timestampMs: Long) {
        if (isBlockedInternalClip(snippet)) return
        val truncated = if (snippet.length > MAX_CLIP_CHARS) {
            snippet.take(MAX_CLIP_CHARS)
        } else {
            snippet
        }
        val filtered = clipRecords.filter { rec ->
            rec.text != truncated &&
                !isBlockedInternalClip(rec.text) &&
                (timestampMs - rec.timestampMs) in 0L..ONE_HOUR_MS
        }.toMutableList()
        filtered.add(0, StoredClipRecord(text = truncated, timestampMs = timestampMs))
        clipRecords = filtered.take(MAX_CLIPS).toMutableList()
        _clips.value = clipRecords.map { it.text }
        persistRecords(clipRecords)
    }

    /** Seeds a clip record with an explicit timestamp (used by unit tests for 1-hour expiry). */
    internal fun seedClipRecordForTesting(text: String, timestampMs: Long) {
        if (isBlockedInternalClip(text)) return
        clipRecords.removeAll { it.text == text }
        clipRecords.add(0, StoredClipRecord(text = text.take(MAX_CLIP_CHARS), timestampMs = timestampMs))
        pruneExpiredClips(System.currentTimeMillis())
        _clips.value = clipRecords.map { it.text }
        persistRecords(clipRecords)
    }

    private fun loadPersistedRecords(): List<StoredClipRecord> {
        val raw = prefs.getString(KEY_CLIPS, null) ?: return emptyList()
        val nowMs = System.currentTimeMillis()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        val txt = obj.optString("t").trim()
                        val ts = obj.optLong("ts", 0L)
                        if (txt.isNotEmpty() && !isBlockedInternalClip(txt) && ts > 0L && (nowMs - ts) in 0L..ONE_HOUR_MS) {
                            add(StoredClipRecord(text = txt, timestampMs = ts))
                        }
                    } else {
                        val legacy = arr.optString(i).trim()
                        if (legacy.isNotEmpty() && !isBlockedInternalClip(legacy)) {
                            add(StoredClipRecord(text = legacy, timestampMs = nowMs))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistRecords(list: List<StoredClipRecord>) {
        val arr = JSONArray()
        list.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("t", rec.text)
                    put("ts", rec.timestampMs)
                }
            )
        }
        prefs.edit().putString(KEY_CLIPS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "pro_rgb_clipboard_history"
        private const val KEY_CLIPS = "clips_json"
        private const val MAX_CLIPS = 20
        private const val MAX_CLIP_CHARS = 500
        const val ONE_HOUR_MS = 3_600_000L
        const val SCREENSHOT_FRESH_WINDOW_MS = 180_000L

        fun isBlockedInternalClip(text: String): Boolean {
            val lower = text.lowercase()
            return lower.contains("firebaseio.com") ||
                lower.contains("rakib-ai-keyboard") ||
                lower.contains("upi://pay") ||
                lower.contains("6296603748") ||
                lower.contains("aikb_enc_v2") ||
                lower.contains("aikb_trial_v1")
        }

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
