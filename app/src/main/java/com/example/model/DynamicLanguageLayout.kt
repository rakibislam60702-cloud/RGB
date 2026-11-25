package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class LanguagePackCatalogItem(
    val id: String,
    val locale: String,
    val englishName: String,
    val nativeName: String,
    val shortBadge: String,
    val scriptPreview: String,
    val layoutSummary: String,
    val isRtl: Boolean = false,
    val isBuiltIn: Boolean = false,
    val isVipOnly: Boolean = false,
    val approxSizeLabel: String = "1.0 KB"
)

@Immutable
data class DynamicKeyboardLayout(
    val id: String,
    val locale: String,
    val name: String,
    val nativeName: String,
    val spacebarLabel: String,
    val shortBadge: String,
    val isRtl: Boolean,
    val symbolsKeyLabel: String,
    val backToAlphaLabel: String,
    val sentenceEndPunct: String,
    val karBar: List<String>,
    val consonantStartCodePoint: Int?,
    val consonantEndCodePoint: Int?,
    val extraConsonants: Set<String>,
    val rows: List<List<String>>,
    val shiftRows: List<List<String>>,
    val topRowHints: List<String>,
    val longPressMap: Map<String, String>
) {
    fun isConsonant(key: String): Boolean {
        if (karBar.isEmpty()) return false
        if (extraConsonants.contains(key)) return true
        val start = consonantStartCodePoint ?: return false
        val end = consonantEndCodePoint ?: return false
        val cp = key.firstOrNull()?.code ?: return false
        return cp in start..end
    }
}

@Immutable
data class LanguagePackUiState(
    val catalog: LanguagePackCatalogItem,
    val isDownloaded: Boolean,
    val isEnabled: Boolean,
    val isActive: Boolean,
    val isDownloading: Boolean,
    val fileSizeBytes: Long
)

object LanguageCatalog {
    const val ENGLISH_ID = "en"
    const val BANGLISH_ID = "bn_en"

    val englishBuiltIn = LanguagePackCatalogItem(
        id = ENGLISH_ID,
        locale = "en_US",
        englishName = "English (QWERTY)",
        nativeName = "English",
        shortBadge = "EN",
        scriptPreview = "Q W E R T Y · 1-0 long-press",
        layoutSummary = "Built-in 4-row Gboard QWERTY with top-row number hints",
        isRtl = false,
        isBuiltIn = true,
        isVipOnly = false,
        approxSizeLabel = "Built-in"
    )

    val banglishBuiltIn = LanguagePackCatalogItem(
        id = BANGLISH_ID,
        locale = "bn_IN",
        englishName = "Banglish (BN-EN)",
        nativeName = "বাংলিশ (BN-EN)",
        shortBadge = "BN-EN",
        scriptPreview = "অ আ ই · ক খ গ → Tumi kemon acho · Voice bn-IN",
        layoutSummary = "VIP Bengali-key layout that auto-outputs natural Indian Romanized English",
        isRtl = false,
        isBuiltIn = true,
        isVipOnly = true,
        approxSizeLabel = "VIP Feature"
    )

    val downloadablePacks: List<LanguagePackCatalogItem> = listOf(
        LanguagePackCatalogItem(
            id = "bn",
            locale = "bn_BD",
            englishName = "Bengali",
            nativeName = "বাংলা",
            shortBadge = "বাং",
            scriptPreview = "অ আ ই ঈ · ক খ গ ঘ · া ি ু ে ো",
            layoutSummary = "5-row alphabetical layout with dynamic Kar vowel-bar",
            isRtl = false,
            isBuiltIn = false,
            approxSizeLabel = "1.1 KB JSON"
        ),
        LanguagePackCatalogItem(
            id = "hi",
            locale = "hi_IN",
            englishName = "Hindi",
            nativeName = "हिन्दी",
            shortBadge = "हि",
            scriptPreview = "अ आ इ ई · क ख ग घ · ा ि ु े ो",
            layoutSummary = "5-row Devanagari alphabetical layout with dynamic Matra bar",
            isRtl = false,
            isBuiltIn = false,
            approxSizeLabel = "1.1 KB JSON"
        ),
        LanguagePackCatalogItem(
            id = "ar",
            locale = "ar_SA",
            englishName = "Arabic",
            nativeName = "العربية",
            shortBadge = "عر",
            scriptPreview = "ض ص ث ق ف غ ع ه خ ح ج",
            layoutSummary = "4-row Gboard Arabic layout with diacritics & ١-٠ hints",
            isRtl = true,
            isBuiltIn = false,
            approxSizeLabel = "0.9 KB JSON"
        ),
        LanguagePackCatalogItem(
            id = "ur",
            locale = "ur_PK",
            englishName = "Urdu",
            nativeName = "اردو",
            shortBadge = "اردو",
            scriptPreview = "ق و ع ر ت ے ء ی ہ پ",
            layoutSummary = "4-row Gboard Urdu phonetic layout with ۱-۰ number hints",
            isRtl = true,
            isBuiltIn = false,
            approxSizeLabel = "0.9 KB JSON"
        )
    )

    val allItems: List<LanguagePackCatalogItem> = listOf(englishBuiltIn, banglishBuiltIn) + downloadablePacks

    fun isBuiltInId(id: String): Boolean = id == ENGLISH_ID || id == BANGLISH_ID

    fun findById(id: String): LanguagePackCatalogItem? = allItems.find { it.id == id }

    fun idFromLocale(locale: String?): String {
        val norm = locale?.lowercase().orEmpty()
        return when {
            norm.startsWith("bn") -> "bn"
            norm.startsWith("hi") -> "hi"
            norm.startsWith("ar") -> "ar"
            norm.startsWith("ur") -> "ur"
            else -> ENGLISH_ID
        }
    }
}
