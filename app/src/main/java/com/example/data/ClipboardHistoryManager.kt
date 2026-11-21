package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Manages real system clipboard history for both RgbKeyboardService and the Companion Studio.
 * Listens to Android's ClipboardManager and persists recent text snippets locally.
 */
class ClipboardHistoryManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val clipboardManager =
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _clips = MutableStateFlow<List<String>>(loadPersistedClips())
    val clips: StateFlow<List<String>> = _clips.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureCurrentPrimaryClip()
    }

    init {
        runCatching {
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
        }
        captureCurrentPrimaryClip()
    }

    fun captureCurrentPrimaryClip() {
        runCatching {
            val clipData = clipboardManager?.primaryClip ?: return
            if (clipData.itemCount <= 0) return
            val text = clipData.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                addSnippetInternal(text)
            }
        }
    }

    fun copyAndSave(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        runCatching {
            clipboardManager?.setPrimaryClip(ClipData.newPlainText("Pro RGB Keyboard", cleaned))
        }
        addSnippetInternal(cleaned)
    }

    fun removeClip(text: String) {
        val updated = _clips.value.filterNot { it == text }
        _clips.value = updated
        persistClips(updated)
    }

    fun clearAll() {
        _clips.value = emptyList()
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
