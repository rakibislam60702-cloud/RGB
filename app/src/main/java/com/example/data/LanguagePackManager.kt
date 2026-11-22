package com.example.data

import android.content.Context
import com.example.model.DynamicKeyboardLayout
import com.example.model.LanguageCatalog
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages lightweight dynamic JSON language layouts saved locally in `filesDir/layouts/`.
 * Keeps APK and RgbKeyboardView clean by loading downloaded language packs on demand.
 */
class LanguagePackManager private constructor(
    private val context: Context
) {
    private val layoutsDir: File
        get() = File(context.filesDir, "layouts").apply {
            if (!exists()) mkdirs()
        }

    private data class CachedLayout(
        val lastModified: Long,
        val layout: DynamicKeyboardLayout
    )

    private val layoutCache = ConcurrentHashMap<String, CachedLayout>()

    private val _downloadedFiles = MutableStateFlow<Map<String, Long>>(emptyMap())
    val downloadedFiles: StateFlow<Map<String, Long>> = _downloadedFiles.asStateFlow()

    init {
        ensureDefaultTriLanguagePacks()
        refreshDownloadedPacks()
    }

    private fun ensureDefaultTriLanguagePacks() {
        val prefs = context.getSharedPreferences("rakib_lang_pack_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("preseeded_tri_lang_v1", false)) return
        listOf("bn", "hi").forEach { langId ->
            runCatching {
                val target = layoutFileFor(langId)
                if (!target.exists() || target.length() <= 0L) {
                    val rawJson = context.assets.open("language_packs/$langId.json")
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    target.writeText(rawJson, Charsets.UTF_8)
                }
            }
        }
        prefs.edit().putBoolean("preseeded_tri_lang_v1", true).apply()
    }

    fun refreshDownloadedPacks() {
        val map = linkedMapOf<String, Long>()
        layoutsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json") && file.length() > 0L) {
                val id = file.name.removeSuffix(".json")
                map[id] = file.length()
            }
        }
        _downloadedFiles.value = map
    }

    fun isDownloaded(langId: String): Boolean {
        if (langId == LanguageCatalog.ENGLISH_ID) return true
        val file = layoutFileFor(langId)
        return file.exists() && file.length() > 0L
    }

    fun layoutFileFor(langId: String): File = File(layoutsDir, "$langId.json")

    /**
     * Fetches/saves the compact JSON layout pack into `filesDir/layouts/<langId>.json`,
     * validates it via JSON parser, caches it, and updates the reactive state flow.
     */
    suspend fun downloadLanguagePack(langId: String): Result<DynamicKeyboardLayout> = withContext(Dispatchers.IO) {
        runCatching {
            // Brief natural pacing so users see the download feedback clearly
            delay(280L)
            val assetPath = "language_packs/$langId.json"
            val rawJson = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = parseLayoutJson(rawJson)

            val targetFile = layoutFileFor(langId)
            val tempFile = File(layoutsDir, "$langId.json.tmp")
            tempFile.writeText(rawJson, Charsets.UTF_8)
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                targetFile.writeText(rawJson, Charsets.UTF_8)
                tempFile.delete()
            }

            layoutCache[langId] = CachedLayout(targetFile.lastModified(), parsed)
            refreshDownloadedPacks()
            parsed
        }
    }

    /**
     * Removes a downloaded JSON layout pack from `filesDir/layouts/`.
     */
    suspend fun deleteLanguagePack(langId: String) = withContext(Dispatchers.IO) {
        if (langId == LanguageCatalog.ENGLISH_ID) return@withContext
        val file = layoutFileFor(langId)
        if (file.exists()) {
            file.delete()
        }
        layoutCache.remove(langId)
        refreshDownloadedPacks()
    }

    /**
     * Dynamically loads a downloaded JSON layout from `filesDir/layouts/<langId>.json`.
     * Returns null for built-in English or if the pack has not been downloaded yet.
     */
    fun loadLayoutFromDisk(langId: String): DynamicKeyboardLayout? {
        if (langId == LanguageCatalog.ENGLISH_ID) return null
        val file = layoutFileFor(langId)
        if (!file.exists() || file.length() <= 0L) {
            layoutCache.remove(langId)
            return null
        }

        val modTime = file.lastModified()
        layoutCache[langId]?.let { cached ->
            if (cached.lastModified == modTime) {
                return cached.layout
            }
        }

        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            val parsed = parseLayoutJson(raw)
            layoutCache[langId] = CachedLayout(modTime, parsed)
            parsed
        }.getOrNull()
    }

    fun getDownloadedLanguageIds(): Set<String> {
        refreshDownloadedPacks()
        return _downloadedFiles.value.keys
    }

    private fun parseLayoutJson(rawJson: String): DynamicKeyboardLayout {
        val obj = JSONObject(rawJson)
        val id = obj.getString("id")
        val locale = obj.optString("locale", id)
        val name = obj.optString("name", id.uppercase())
        val nativeName = obj.optString("nativeName", name)
        val spacebarLabel = obj.optString("spacebarLabel", nativeName)
        val shortBadge = obj.optString("shortBadge", id.uppercase())
        val isRtl = obj.optBoolean("isRtl", false)
        val symbolsKeyLabel = obj.optString("symbolsKeyLabel", "?123")
        val backToAlphaLabel = obj.optString("backToAlphaLabel", "ABC")
        val sentenceEndPunct = obj.optString("sentenceEndPunct", ".")

        val karBar = obj.optJSONArray("karBar").toStringList()
        val consonantRange = obj.optJSONArray("consonantUnicodeRange")
        val startCp = if (consonantRange != null && consonantRange.length() >= 2) consonantRange.optInt(0) else null
        val endCp = if (consonantRange != null && consonantRange.length() >= 2) consonantRange.optInt(1) else null
        val extraConsonants = obj.optJSONArray("extraConsonants").toStringList().toSet()

        val rows = obj.optJSONArray("rows").toRowsList()
        val shiftRows = obj.optJSONArray("shiftRows").toRowsList().ifEmpty { rows }
        val topRowHints = obj.optJSONArray("topRowHints").toStringList()

        val longPressMap = mutableMapOf<String, String>()
        val lpObj = obj.optJSONObject("longPressMap")
        if (lpObj != null) {
            val keys = lpObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                longPressMap[k] = lpObj.optString(k, "")
            }
        }

        require(rows.isNotEmpty()) { "Dynamic layout '$id' must contain at least one key row" }

        return DynamicKeyboardLayout(
            id = id,
            locale = locale,
            name = name,
            nativeName = nativeName,
            spacebarLabel = spacebarLabel,
            shortBadge = shortBadge,
            isRtl = isRtl,
            symbolsKeyLabel = symbolsKeyLabel,
            backToAlphaLabel = backToAlphaLabel,
            sentenceEndPunct = sentenceEndPunct,
            karBar = karBar,
            consonantStartCodePoint = startCp,
            consonantEndCodePoint = endCp,
            extraConsonants = extraConsonants,
            rows = rows,
            shiftRows = shiftRows,
            topRowHints = topRowHints,
            longPressMap = longPressMap
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { idx -> optString(idx, "") }.filter { it.isNotEmpty() }
    }

    private fun JSONArray?.toRowsList(): List<List<String>> {
        if (this == null) return emptyList()
        val result = mutableListOf<List<String>>()
        for (i in 0 until length()) {
            val rowArr = optJSONArray(i) ?: continue
            val row = rowArr.toStringList()
            if (row.isNotEmpty()) {
                result.add(row)
            }
        }
        return result
    }

    companion object {
        @Volatile
        private var instance: LanguagePackManager? = null

        fun getInstance(context: Context): LanguagePackManager {
            return instance ?: synchronized(this) {
                instance ?: LanguagePackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
