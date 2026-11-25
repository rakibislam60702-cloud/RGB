package com.example.engine

import android.content.Context
import android.content.SharedPreferences

/**
 * Dynamic, real-time Prefix + Next-Word Prediction & Personal Learning Engine for
 * Bengali (`bn`), English (`en`), and Hindi (`hi`).
 *
 * Features:
 * - 650+ curated everyday conversational words across Bengali, English, and Hindi
 * - Contextual next-word bigram predictions (e.g., after "আমি" -> "যাচ্ছি", "এখন", "ভালো আছি", "পারব না")
 * - Romanized Banglish shortcut support (e.g., typing "ki" or "কি" suggests "করছ", "হয়েছে", "খবর", "ব্যাপার")
 * - Automatic personal user-dictionary learning persisted in SharedPreferences so words and
 *   phrases the user frequently types rise naturally to the top.
 */
object SmartWordPredictor {

    private const val PREFS_NAME = "ai_keyboard_user_dictionary"
    private const val MAX_USER_WORDS_PER_LANG = 250
    private const val MAX_USER_BIGRAMS_PER_LANG = 180

    @Volatile
    private var appPrefs: SharedPreferences? = null

    private val userWordFreqs = mutableMapOf<String, MutableMap<String, Int>>()
    private val userBigrams = mutableMapOf<String, MutableMap<String, MutableMap<String, Int>>>()
    private val loadedLangs = mutableSetOf<String>()

    data class PredictionResult(
        val suggestions: List<String>,
        val currentTokenLength: Int,
        val stickerPhrase: String,
        val banglaConvertSource: String = "",
        val banglaConvertPreview: String = ""
    )

    fun init(context: Context) {
        if (appPrefs == null) {
            synchronized(this) {
                if (appPrefs == null) {
                    appPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    /**
     * Learns the most recently completed word (and previous->current bigram) into the
     * user's personal SharedPreferences dictionary.
     */
    fun learnFromCompletedInput(context: Context, textBeforeCursor: String, languageId: String) {
        init(context)
        val lang = normalizeLang(languageId)
        ensureLoaded(lang)

        // In Banglish (BN-EN), always store strictly Romanized English words (never Bengali Unicode script)
        val effectiveInput = if (lang == "bn_en" && BengaliTransliterator.containsBengaliScript(textBeforeCursor)) {
            BengaliTransliterator.transliterate(textBeforeCursor, capitalizeSentences = false)
        } else {
            textBeforeCursor
        }

        val tokens = effectiveInput
            .trim()
            .split(Regex("[\\s.,!?।;:\\n]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it.length <= 28 }

        if (tokens.isEmpty()) return
        val lastWord = tokens.last()
        val prevWord = if (tokens.size >= 2) tokens[tokens.size - 2] else null
        if (lang == "bn_en" && BengaliTransliterator.containsBengaliScript(lastWord)) return

        synchronized(this) {
            val wordsMap = userWordFreqs.getOrPut(lang) { linkedMapOf() }
            val isLatinLang = lang == "en" || lang == "bn_en"
            val canonicalLast = if (isLatinLang) lastWord.lowercase() else lastWord
            wordsMap[canonicalLast] = (wordsMap[canonicalLast] ?: 0) + 1
            trimWordMapIfNeeded(wordsMap)

            if (!prevWord.isNullOrBlank() && !(lang == "bn_en" && BengaliTransliterator.containsBengaliScript(prevWord))) {
                val canonicalPrev = if (isLatinLang) prevWord.lowercase() else prevWord
                val bigramMap = userBigrams.getOrPut(lang) { linkedMapOf() }
                val nexts = bigramMap.getOrPut(canonicalPrev) { linkedMapOf() }
                nexts[lastWord] = (nexts[lastWord] ?: 0) + 1
                trimBigramMapIfNeeded(bigramMap)
            }
            persistUserDictionaryLocked(lang)
        }
    }

    fun predict(
        textBeforeCursor: String,
        languageId: String,
        context: Context? = null
    ): PredictionResult {
        if (context != null) init(context)
        val lang = normalizeLang(languageId)
        ensureLoaded(lang)

        val stickerPhrase = extractStickerPhrase(textBeforeCursor)
        val endedWithSpace = textBeforeCursor.isNotEmpty() && textBeforeCursor.last().isWhitespace()

        // In Banglish (BN-EN) mode, detect any Bengali Unicode text before cursor for 1-tap বাংলা -> বাংলিশ conversion
        val banglaSource = if (lang == "bn_en") {
            BengaliTransliterator.extractConvertibleBengaliSegment(textBeforeCursor)
        } else {
            ""
        }
        val banglaPreview = if (banglaSource.isNotEmpty()) {
            BengaliTransliterator.transliterate(banglaSource, capitalizeSentences = true)
        } else {
            ""
        }

        if (textBeforeCursor.isBlank()) {
            return PredictionResult(
                suggestions = mergedStarters(lang),
                currentTokenLength = 0,
                stickerPhrase = "",
                banglaConvertSource = banglaSource,
                banglaConvertPreview = banglaPreview
            )
        }

        if (endedWithSpace) {
            val prevWord = extractLastCompletedWord(textBeforeCursor)
            val nextWords = nextWordPredictions(prevWord, lang)
            return PredictionResult(
                suggestions = nextWords.ifEmpty { mergedStarters(lang) },
                currentTokenLength = 0,
                stickerPhrase = stickerPhrase,
                banglaConvertSource = banglaSource,
                banglaConvertPreview = banglaPreview
            )
        }

        val currentWord = extractCurrentTypingWord(textBeforeCursor)
        if (currentWord.isEmpty()) {
            return PredictionResult(
                suggestions = mergedStarters(lang),
                currentTokenLength = 0,
                stickerPhrase = stickerPhrase,
                banglaConvertSource = banglaSource,
                banglaConvertPreview = banglaPreview
            )
        }

        val prefixMatches = prefixPredictions(currentWord, lang).let { matches ->
            if (lang == "bn_en" && BengaliTransliterator.containsBengaliScript(currentWord)) {
                val beforeToken = textBeforeCursor.dropLast(currentWord.length)
                if (BengaliTransliterator.isSentenceStartContext(beforeToken)) {
                    matches.map { w -> w.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it } }
                } else {
                    matches
                }
            } else {
                matches
            }
        }
        return PredictionResult(
            suggestions = prefixMatches.ifEmpty { mergedStarters(lang).take(4) },
            currentTokenLength = currentWord.length,
            stickerPhrase = stickerPhrase,
            banglaConvertSource = banglaSource,
            banglaConvertPreview = banglaPreview
        )
    }

    /**
     * Extracts the latest active typed phrase (up to 48 chars on the current line)
     * so tapping "✨ RGB Sticker" turns the user's message into a glowing neon sticker.
     */
    fun extractStickerPhrase(textBeforeCursor: String): String {
        val lastLine = textBeforeCursor.substringAfterLast('\n').trim()
        if (lastLine.isEmpty()) return ""
        return if (lastLine.length <= 48) lastLine else lastLine.takeLast(48).trimStart()
    }

    private fun normalizeLang(languageId: String): String = when (languageId) {
        "bn", "bn_en", "hi", "en" -> languageId
        else -> "en"
    }

    private fun ensureLoaded(lang: String) {
        val prefs = appPrefs ?: return
        synchronized(this) {
            if (lang in loadedLangs) return
            loadedLangs.add(lang)

            val wordsRaw = prefs.getString("words_$lang", "").orEmpty()
            val wordsMap = linkedMapOf<String, Int>()
            if (wordsRaw.isNotBlank()) {
                wordsRaw.split('|').forEach { entry ->
                    val idx = entry.lastIndexOf(':')
                    if (idx > 0) {
                        val word = entry.substring(0, idx)
                        val count = entry.substring(idx + 1).toIntOrNull() ?: 1
                        if (word.isNotBlank()) wordsMap[word] = count
                    }
                }
            }
            userWordFreqs[lang] = wordsMap

            val bigramsRaw = prefs.getString("bigrams_$lang", "").orEmpty()
            val bigramMap = linkedMapOf<String, MutableMap<String, Int>>()
            if (bigramsRaw.isNotBlank()) {
                bigramsRaw.split('|').forEach { entry ->
                    val arrow = entry.indexOf("->")
                    val colon = entry.lastIndexOf(':')
                    if (arrow > 0 && colon > arrow + 2) {
                        val prev = entry.substring(0, arrow)
                        val next = entry.substring(arrow + 2, colon)
                        val count = entry.substring(colon + 1).toIntOrNull() ?: 1
                        if (prev.isNotBlank() && next.isNotBlank()) {
                            bigramMap.getOrPut(prev) { linkedMapOf() }[next] = count
                        }
                    }
                }
            }
            userBigrams[lang] = bigramMap
        }
    }

    private fun persistUserDictionaryLocked(lang: String) {
        val prefs = appPrefs ?: return
        val wordsSerialized = userWordFreqs[lang].orEmpty()
            .entries
            .sortedByDescending { it.value }
            .take(MAX_USER_WORDS_PER_LANG)
            .joinToString("|") { "${it.key}:${it.value}" }

        val bigramsSerialized = userBigrams[lang].orEmpty()
            .entries
            .flatMap { (prev, nexts) ->
                nexts.entries.map { (next, count) -> Triple(prev, next, count) }
            }
            .sortedByDescending { it.third }
            .take(MAX_USER_BIGRAMS_PER_LANG)
            .joinToString("|") { "${it.first}->${it.second}:${it.third}" }

        prefs.edit()
            .putString("words_$lang", wordsSerialized)
            .putString("bigrams_$lang", bigramsSerialized)
            .apply()
    }

    private fun trimWordMapIfNeeded(map: MutableMap<String, Int>) {
        if (map.size > MAX_USER_WORDS_PER_LANG + 30) {
            val top = map.entries.sortedByDescending { it.value }.take(MAX_USER_WORDS_PER_LANG)
            map.clear()
            top.forEach { map[it.key] = it.value }
        }
    }

    private fun trimBigramMapIfNeeded(map: MutableMap<String, MutableMap<String, Int>>) {
        if (map.size > MAX_USER_BIGRAMS_PER_LANG) {
            val keepKeys = map.entries
                .sortedByDescending { entry -> entry.value.values.sum() }
                .take(MAX_USER_BIGRAMS_PER_LANG - 20)
                .map { it.key }
                .toSet()
            map.keys.retainAll(keepKeys)
        }
    }

    private fun extractCurrentTypingWord(text: String): String {
        var idx = text.length - 1
        while (idx >= 0 && !text[idx].isWhitespace() && !isPunctuationBoundary(text[idx])) {
            idx--
        }
        return text.substring(idx + 1)
    }

    private fun extractLastCompletedWord(text: String): String {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return ""
        var idx = trimmed.length - 1
        while (idx >= 0 && !trimmed[idx].isWhitespace() && !isPunctuationBoundary(trimmed[idx])) {
            idx--
        }
        return trimmed.substring(idx + 1)
    }

    private fun isPunctuationBoundary(ch: Char): Boolean {
        return ch == '.' || ch == ',' || ch == '!' || ch == '?' || ch == '।' ||
            ch == ';' || ch == ':' || ch == '\n'
    }

    private fun mergedStarters(lang: String): List<String> {
        val learnedTop = synchronized(this) {
            userWordFreqs[lang].orEmpty()
                .entries
                .filter { it.value >= 2 }
                .sortedByDescending { it.value }
                .map { it.key }
                .filter { word ->
                    // Never surface Bengali Unicode script inside Banglish (BN-EN) or pure English modes
                    if (lang == "bn_en" || lang == "en") !BengaliTransliterator.containsBengaliScript(word) else true
                }
                .take(2)
        }
        val base = when (lang) {
            "bn_en" -> listOf("Tumi", "Ami", "Valo", "Kemon", "Ekhon", "Accha")
            "bn" -> listOf("আমি", "হ্যাঁ", "ঠিক আছে", "কেমন আছো?", "ভালো আছি", "আসছি")
            "hi" -> listOf("मैं", "हाँ", "ठीक है", "कैसे हो?", "अभी", "धन्यवाद")
            else -> listOf("I'm", "Hello", "Okay", "Good", "Thanks", "On my way")
        }
        return (learnedTop + base).distinct().take(6)
    }

    private fun nextWordPredictions(prevWord: String, lang: String): List<String> {
        if (prevWord.isEmpty()) return emptyList()
        val lookupPrev = when (lang) {
            "bn_en" -> if (BengaliTransliterator.containsBengaliScript(prevWord)) {
                BengaliTransliterator.transliterateWord(prevWord).lowercase()
            } else {
                prevWord.lowercase()
            }
            "en" -> prevWord.lowercase()
            else -> prevWord
        }

        val learnedNext = synchronized(this) {
            userBigrams[lang]?.get(lookupPrev)
                ?.entries
                ?.sortedByDescending { it.value }
                ?.map { it.key }
                ?.filter { word ->
                    if (lang == "bn_en" || lang == "en") !BengaliTransliterator.containsBengaliScript(word) else true
                }
                .orEmpty()
        }

        val builtInNext = when (lang) {
            "bn_en" -> banglishRomanizedBigrams[lookupPrev]
                ?: listOf("kemon", "valo", "ekhon", "kothay", "korcho", "thik")
            "bn" -> bengaliBigrams[prevWord] ?: emptyList()
            "hi" -> hindiBigrams[prevWord].orEmpty()
            else -> englishBigrams[lookupPrev].orEmpty()
        }

        return (learnedNext + builtInNext).distinct().take(6)
    }

    private fun prefixPredictions(prefix: String, lang: String): List<String> {
        val lower = prefix.lowercase()

        val learnedMatches = synchronized(this) {
            userWordFreqs[lang].orEmpty()
                .entries
                .filter { (word, _) ->
                    when (lang) {
                        "bn_en", "en" -> !BengaliTransliterator.containsBengaliScript(word) &&
                            word.startsWith(lower) && !word.equals(lower, ignoreCase = true)
                        else -> word.startsWith(prefix) && word != prefix
                    }
                }
                .sortedByDescending { it.value }
                .map { it.key }
        }

        return when (lang) {
            "bn_en" -> {
                // Strictly one-way Bengali -> Romanized English (NEVER English -> Bengali Unicode!)
                val shouldCap = prefix.firstOrNull()?.isUpperCase() == true
                val romanizedSuggestions = BengaliTransliterator.suggestRomanizedBanglish(prefix, maxItems = 6)
                val formattedLearned = learnedMatches.map { w ->
                    if (shouldCap && w.firstOrNull()?.isLetter() == true && w.first().code < 128) {
                        w.replaceFirstChar { it.uppercase() }
                    } else w
                }
                val englishFallback = if (!BengaliTransliterator.containsBengaliScript(prefix) && romanizedSuggestions.size < 4) {
                    englishDictionary
                        .filter { it.startsWith(lower) && !it.equals(lower, ignoreCase = true) }
                        .take(3)
                        .map { word -> if (shouldCap) word.replaceFirstChar { it.uppercase() } else word }
                } else {
                    emptyList()
                }
                (romanizedSuggestions + formattedLearned + englishFallback)
                    .filter { !BengaliTransliterator.containsBengaliScript(it) }
                    .distinct()
                    .take(6)
            }
            "bn" -> {
                // Pure Bengali Unicode script stays strictly inside the 'বাংলা' tab
                val inlineFollowups = if (prefix == "কি" || prefix == "কী") {
                    listOf("করছ", "হয়েছে", "খবর", "ব্যাপার", "কিছু", "কিন্তু")
                } else emptyList()
                val exactPrefix = bengaliDictionary.filter { it.startsWith(prefix) && it != prefix }
                val contains = if (exactPrefix.size + learnedMatches.size < 6) {
                    bengaliDictionary.filter { it.contains(prefix) && !it.startsWith(prefix) }
                } else emptyList()
                (learnedMatches + inlineFollowups + exactPrefix + contains)
                    .distinct()
                    .take(6)
            }
            "hi" -> {
                val exactPrefix = hindiDictionary.filter { it.startsWith(prefix) && it != prefix }
                val contains = if (exactPrefix.size + learnedMatches.size < 6) {
                    hindiDictionary.filter { it.contains(prefix) && !it.startsWith(prefix) }
                } else emptyList()
                (learnedMatches + exactPrefix + contains).distinct().take(6)
            }
            else -> {
                val shouldCap = prefix.firstOrNull()?.isUpperCase() == true
                val isAllCaps = prefix.length > 1 && prefix.all { it.isUpperCase() }
                val dictMatches = englishDictionary
                    .filter { it.startsWith(lower) && !it.equals(lower, ignoreCase = true) }
                (learnedMatches + dictMatches)
                    .distinct()
                    .map { word ->
                        when {
                            isAllCaps -> word.uppercase()
                            shouldCap -> word.replaceFirstChar { it.uppercase() }
                            else -> word
                        }
                    }
                    .take(6)
            }
        }
    }

    // ── Indian Banglish (BN-EN) Strictly Romanized Next-Word Bigrams (Zero Bengali Unicode) ──
    private val banglishRomanizedBigrams: Map<String, List<String>> = mapOf(
        "tumi" to listOf("kemon", "kothay", "ki", "ekhon", "valo", "asbe"),
        "ami" to listOf("valo", "jacchi", "aschi", "ekhon", "bari", "bolchi"),
        "kemon" to listOf("acho", "achen", "lagche", "cholche", "achis", "hoyeche"),
        "valo" to listOf("achi", "theko", "lagche", "hoyeche", "kotha", "khobor"),
        "ekhon" to listOf("ki", "kothay", "jacchi", "bari", "free", "busy"),
        "ki" to listOf("korcho", "khobor", "hoyeche", "byapar", "bolo", "lagbe"),
        "thik" to listOf("ache", "bolecho", "hoyeche", "korecho", "somoye", "bhai"),
        "kothay" to listOf("acho", "jaccho", "ekhon", "thako", "asbe", "gele"),
        "pore" to listOf("kotha", "asbo", "call", "janacchi", "dekha", "bolbo"),
        "bari" to listOf("jacchi", "achi", "ferbo", "theke", "aschi", "phirchi"),
        "khub" to listOf("valo", "sundor", "khushi", "dorkar", "moja", "darun"),
        "accha" to listOf("thik", "pore", "bolchi", "tahole", "ok", "dekha")
    )

    // ── Bengali Next-Word Bigrams ───────────────────────────────────────────────
    private val bengaliBigrams: Map<String, List<String>> = mapOf(
        "আমি" to listOf("যাচ্ছি", "এখন", "ভালো আছি", "পারব না", "আসছি", "কাজ করছি"),
        "তুমি" to listOf("কেমন আছো?", "কোথায়?", "কি করছ?", "আসবে?", "ভালো থেকো", "খেয়েছো?"),
        "আপনি" to listOf("কেমন আছেন?", "কোথায়?", "কখন আসবেন?", "ভালো থাকবেন", "ব্যস্ত?"),
        "কেমন" to listOf("আছো?", "আছেন?", "লাগছে?", "হয়েছে?", "চলছে?", "আছে?"),
        "ভালো" to listOf("আছি", "লাগছে", "থেকো", "হয়েছে", "খবর", "লাগে"),
        "ঠিক" to listOf("আছে", "বলেছ", "হয়েছে", "আছে ভাই", "করেছ", "সময়ে"),
        "শুভ" to listOf("রাত্রি", "সকাল", "সন্ধ্যা", "জন্মদিন", "কামনা", "অভিনন্দন"),
        "কি" to listOf("করছ", "হয়েছে", "খবর", "ব্যাপার", "বলো", "করবে?"),
        "কী" to listOf("করছ", "হয়েছে", "খবর", "ব্যাপার", "বলো", "করবে?"),
        "এখন" to listOf("যাচ্ছি", "ব্যস্ত আছি", "আসছি", "কোথায়?", "ফ্রি আছি", "কথা বলব"),
        "একটু" to listOf("পরে", "অপেক্ষা করো", "ব্যস্ত আছি", "দাঁড়াও", "আসছি", "সময় দাও"),
        "খুব" to listOf("ভালো", "সুন্দর", "মজা", "দরকার", "খুশি", "কষ্ট"),
        "অনেক" to listOf("ধন্যবাদ", "ভালো", "সুন্দর", "ভালোবাসা", "সময়", "দিন পর"),
        "পরে" to listOf("কথা বলব", "কল দিচ্ছি", "দেখা হবে", "জানাচ্ছি", "আসব", "মেসেজ দিও"),
        "হ্যাঁ" to listOf("ঠিক আছে", "যাচ্ছি", "ভাই", "অবশ্যই", "আমি আছি", "হবে"),
        "না" to listOf("ভাই", "এখন না", "ঠিক আছে", "সমস্যা নেই", "আমি যাব না", "পারব না"),
        "আসসালামু" to listOf("আলাইকুম", "আলাইকুম ভাই"),
        "ওয়ালাইকুম" to listOf("আসসালাম", "আসসালাম ভাই"),
        "ইনশাআল্লাহ" to listOf("হবে", "দেখা হবে", "আসব", "ভালো হবে", "পারব"),
        "ধন্যবাদ" to listOf("ভাই", "বন্ধু", "অনেক", "আপনাকে"),
        "আমার" to listOf("নাম", "ফোন", "বাড়ি", "কাজ", "সাথে", "কাছে"),
        "তোমার" to listOf("খবর কি?", "বাসায়", "নাম", "ফোন", "সাথে", "জন্য"),
        "আজ" to listOf("সকালে", "রাতে", "বিকেলে", "দেখা হবে", "ব্যস্ত আছি", "ছুটি"),
        "কাল" to listOf("সকালে", "দেখা হবে", "আসব", "যাব", "কথা হবে", "ছুটি"),
        "কোথায়" to listOf("আছো?", "যাচ্ছো?", "গিয়েছিলে?", "দেখা করবে?"),
        "কখন" to listOf("আসবে?", "যাবে?", "ফ্রি হবে?", "দেখা হবে?", "কল করব?"),
        "কেন" to listOf("কি হয়েছে?", "বলো", "আসলে না?", "রাগ করেছ?"),
        "আচ্ছা" to listOf("ঠিক আছে", "পরে কথা হবে", "তাহলে", "দেখা হবে", "বলো"),
        "ভাই" to listOf("কেমন আছো?", "কোথায়?", "একটু", "শোনো", "কি খবর?", "ধন্যবাদ"),
        "চলো" to listOf("যাই", "দেখা করি", "ঘুরে আসি", "খাই", "শুরু করি"),
        "কোনো" to listOf("সমস্যা নেই", "ব্যাপার না", "খবর আছে?", "কাজ আছে?"),
        "সমস্যা" to listOf("নেই", "হয়েছে", "হলে জানিও", "সমাধান"),
        "দেখা" to listOf("হবে", "করব", "যাবে", "হোক", "দাও"),
        "কথা" to listOf("বলব", "আছে", "হবে", "শোনো", "দিলাম"),
        "ফোন" to listOf("করো", "দাও", "ধরো", "করেছিলাম", "বন্ধ")
    )

    // ── Bengali 250+ Everyday Conversational Dictionary ─────────────────────────
    private val bengaliDictionary: List<String> = listOf(
        // আ
        "আমি", "আজ", "আসছি", "আচ্ছা", "আমার", "আপনি", "আপনার", "আছি", "আছেন", "আসব",
        "আসবে", "আমাদের", "আমাকে", "আসসালামু", "আলাইকুম", "আবার", "আগে", "আরে", "আলহামদুলিল্লাহ",
        "আশা", "আকাশ", "আলো", "আনন্দ", "আসল", "আর", "আরো", "আছে",
        // অ
        "অনেক", "অপেক্ষা", "অবশ্যই", "অফিস", "অবস্থা", "অল্প", "অন্য", "অসুবিধা", "অভিনন্দন", "অসাধারণ",
        // ই / ঈ / উ / এ / ঐ / ও
        "ইনশাআল্লাহ", "ইংরেজি", "ইচ্ছা", "ইতিমধ্যে", "ইমোজি", "ঈদ", "ঈদ মোবারক",
        "উত্তর", "উচিত", "উপরে", "উৎসব", "উপহার", "উঠেছি",
        "এখন", "একটু", "এখানে", "এসেছি", "একটা", "এটা", "এবং", "একসাথে", "একদম", "এত", "এবার", "একজন", "এখনো",
        "ওকে", "ওখানে", "ওরা", "ওয়ালাইকুম", "ওটা", "ওপর", "ওষুধ",
        // ক
        "কি", "কী", "করছ", "করছো", "করেছি", "করব", "করো", "কথা", "কেমন", "কোথায়", "কাজ",
        "কখন", "কেন", "কিছু", "কিন্তু", "কল", "কাল", "কষ্ট", "কোনো", "কাছে", "কারণ", "কত",
        "কিবোর্ড", "ক্লাস", "কলেজ", "কফি", "কাপড়", "কিনব", "কিনেছি", "ক্যাম্পাস",
        // খ / গ / ঘ
        "খুব", "খবর", "খাবার", "খেয়েছো", "খেয়েছি", "খুশি", "খুঁজছি", "খেলা", "খারাপ", "খোলো", "খালি",
        "গেছি", "গল্প", "গাড়ি", "গান", "গ্রাম", "গুরুত্বপূর্ণ", "গরম", "ঘুম", "ঘরে", "ঘুরতে", "ঘণ্টা",
        // চ / ছ / জ / ঝ
        "চলো", "চলে", "চাই", "চিন্তা", "চা", "চাকরি", "চোখ", "চাঁদ", "চলছে", "চেষ্টা",
        "ছবি", "ছোট", "ছুটি", "ছেলে", "ছাত্র",
        "জানি", "জানাচ্ছি", "জন্য", "জন্মদিন", "জরুরি", "জীবন", "জায়গা", "জামা", "জিজ্ঞেস", "জিতব", "জবাব",
        "ঝামেলা", "ঝড়",
        // ট / ঠ / ড / ঢ
        "টাকা", "টাইম", "টিভি", "টেনশন", "টেবিল", "ট্রেন", "ঠিক", "ঠিক আছে", "ঠান্ডা", "ঠিকানা",
        "ডাক্তার", "ডাকছে", "ঢাকা",
        // ত / থ / দ / ধ / ন
        "তুমি", "তোমার", "তোমাকে", "তাই", "তবে", "তাড়াতাড়ি", "তাহলে", "তারপর", "তারা", "তখন", "তারিখ", "তৈরি",
        "থাকো", "থেকো", "থাকব", "থেকে", "থামো",
        "দাও", "দরকার", "দিন", "দেখা", "দেখছি", "দেখেছি", "দোয়া", "দুঃখিত", "দাঁড়াও", "দোকান", "দাম", "দূরে", "দুপুর", "দেশ",
        "ধন্যবাদ", "ধরে", "ধীরে", "ধরো",
        "না", "নাকি", "নিয়ে", "নাম", "নতুন", "নেই", "নম্বর", "নিশ্চয়ই", "নিজে", "নেট", "নাস্তা", "নদী",
        // প / ফ / ব / ভ / ম
        "পরে", "পারব", "পারব না", "পারি", "প্লিজ", "পাঠিয়েছি", "পাঠাও", "প্রিয়", "পড়েছি", "পানি",
        "পাশে", "পছন্দ", "পরীক্ষা", "পরিবার", "পেয়েছি", "প্রস্তুত", "প্রয়োজন", "প্রতিদিন", "পৃথিবী",
        "ফোন", "ফ্রি", "ফটো", "ফিরব", "ফিরেছি", "ফেসবুক",
        "বলো", "বলছি", "বলেছি", "বাড়ি", "বাসায়", "বাংলা", "বাংলাদেশ", "বন্ধু", "বেশি", "ব্যস্ত",
        "ব্যাপার", "বুঝেছি", "বুঝতে", "বাই", "বাইরে", "বাজার", "বৃষ্টি", "বিকেল", "বই", "বড়", "বাবা", "বোন", "বস",
        "ভালো", "ভালো আছি", "ভালোবাসি", "ভালোবাসা", "ভাই", "ভাত", "ভুল", "ভয়", "ভাবছি", "ভেতরে",
        "মনে", "মা", "মানুষ", "মোবাইল", "মজা", "মাশাআল্লাহ", "মেসেজ", "মিনিট", "মাস", "মিটিং", "মন", "মুখ", "মেয়ে",
        // য / র / ল / শ / স / হ
        "যাচ্ছি", "যাব", "যাব না", "যদি", "যেন", "যাই", "যাবে", "যখন", "যেতে", "যথেষ্ট",
        "রাত", "রাখো", "রাস্তায়", "রাকিব", "রেডি", "রাগ", "রং", "রুম", "রবিবার",
        "লাগবে", "লিখেছি", "লাইক", "লাগছে", "লোক", "লম্বা",
        "শুভ", "শুভ রাত্রি", "শুভ সকাল", "শুরু", "শুধু", "শুনছি", "শোনো", "শেষ", "শহর", "শীত", "শান্তি",
        "সব", "সবাই", "সকাল", "সময়", "সুন্দর", "সত্যি", "সরি", "সমস্যা", "সাথে", "সন্ধ্যা",
        "সাহায্য", "সফল", "সুস্থ", "স্বপ্ন", "স্কুল", "স্টিকার", "সোমবার", "সপ্তাহ", "সামনে",
        "হ্যাঁ", "হবে", "হয়েছে", "হ্যালো", "হয়", "হয়তো", "হাত", "হাসি", "হোয়াটসঅ্যাপ", "হঠাৎ", "হিসাব", "হোটেল"
    )

    // ── English Next-Word Bigrams ───────────────────────────────────────────────
    private val englishBigrams: Map<String, List<String>> = mapOf(
        "i" to listOf("am", "will", "have", "think", "need", "love"),
        "i'm" to listOf("going", "good", "busy", "coming", "here", "ready"),
        "good" to listOf("morning", "night", "luck", "job", "evening", "idea"),
        "how" to listOf("are", "is", "much", "about", "was", "long"),
        "are" to listOf("you", "we", "they", "you ready?", "you free?"),
        "you" to listOf("are", "too", "can", "know", "want", "ready?"),
        "thank" to listOf("you", "you so much", "god"),
        "thanks" to listOf("a lot", "bro", "so much", "for", "man"),
        "see" to listOf("you", "you soon", "you later", "you tomorrow"),
        "on" to listOf("my way", "the way", "time", "call"),
        "ok" to listOf("bro", "sure", "done", "got it", "thanks"),
        "okay" to listOf("sure", "bro", "done", "great", "fine"),
        "where" to listOf("are you?", "is it?", "now?", "did you go?"),
        "what" to listOf("are you doing?", "happened?", "time?", "is", "about"),
        "please" to listOf("send", "wait", "call me", "check", "help"),
        "call" to listOf("me", "you later", "now", "back"),
        "happy" to listOf("birthday", "new year", "to help", "anniversary"),
        "let's" to listOf("go", "meet", "talk", "do it", "play"),
        "can" to listOf("you", "we", "i", "wait"),
        "will" to listOf("be", "come", "call", "send", "do"),
        "don't" to listOf("worry", "know", "forget", "mind"),
        "no" to listOf("problem", "worries", "way", "thanks"),
        "send" to listOf("me", "it", "the photo", "location"),
        "just" to listOf("reached", "woke up", "checking", "now", "a minute"),
        "have" to listOf("a great day", "fun", "you", "to go"),
        "talk" to listOf("to you later", "soon", "now"),
        "going" to listOf("home", "sleep", "out", "office", "college")
    )

    // ── English 310+ Everyday Conversational Dictionary ─────────────────────────
    private val englishDictionary: List<String> = listOf(
        "about", "actually", "address", "after", "afternoon", "again", "agree", "ahead", "almost", "alone",
        "already", "also", "always", "amazing", "amount", "and", "angry", "anniversary", "another", "answer",
        "any", "anybody", "anymore", "anyone", "anything", "anyway", "anywhere", "app", "apply", "appreciate",
        "are", "area", "around", "arrive", "arrived", "ask", "asking", "asleep", "available", "away", "awesome",
        "baby", "back", "bad", "balance", "bank", "battery", "beautiful", "because", "become", "bed", "been",
        "before", "begin", "behind", "believe", "best", "better", "between", "big", "bill", "birthday", "bit",
        "black", "bless", "blue", "book", "bored", "boss", "both", "bought", "boy", "break", "breakfast",
        "bring", "bro", "brother", "brought", "budget", "build", "bus", "business", "busy", "but", "buy", "bye",
        "cafe", "cake", "call", "called", "calling", "calm", "came", "camera", "campus", "can", "can't",
        "cancel", "car", "card", "care", "careful", "carry", "case", "cash", "catch", "celebrate", "center",
        "chance", "change", "charge", "charger", "chat", "cheap", "check", "checking", "cheers", "chicken",
        "chill", "choice", "choose", "city", "class", "clean", "clear", "click", "close", "closed", "clothes",
        "cloud", "club", "coffee", "cold", "college", "color", "come", "coming", "comment", "company",
        "complete", "computer", "confirm", "congrats", "congratulations", "connect", "contact", "continue",
        "cool", "copy", "correct", "cost", "could", "count", "country", "couple", "course", "cover", "crazy",
        "create", "cricket", "cry", "cup", "current", "custom", "customer", "cute",
        "dad", "daily", "dance", "dark", "data", "date", "day", "days", "deal", "dear", "decide", "decided",
        "deep", "definitely", "delay", "delete", "deliver", "delivery", "depends", "design", "detail", "details",
        "device", "did", "didn't", "different", "difficult", "dinner", "direct", "discount", "discuss", "distance",
        "do", "doctor", "document", "does", "doesn't", "doing", "done", "don't", "door", "double", "doubt",
        "down", "download", "dream", "dress", "drink", "drive", "driving", "drop", "due", "during",
        "each", "early", "earn", "earth", "easy", "eat", "eating", "edit", "effect", "either", "else", "email",
        "emoji", "empty", "end", "energy", "english", "enjoy", "enough", "enter", "entire", "error", "especially",
        "evening", "event", "ever", "every", "everybody", "everyday", "everyone", "everything", "everywhere",
        "exact", "exactly", "exam", "example", "excellent", "excited", "exciting", "excuse", "exercise", "exist",
        "expect", "expensive", "experience", "explain", "extra", "eyes",
        "face", "facebook", "fact", "fail", "fair", "fall", "family", "famous", "fan", "fantastic", "far", "fast",
        "father", "favorite", "fear", "feature", "feel", "feeling", "feet", "festival", "fever", "few", "field",
        "fight", "file", "fill", "film", "final", "finally", "find", "fine", "finger", "finish", "finished",
        "fire", "first", "fit", "five", "fix", "fixed", "flight", "floor", "flower", "fly", "focus", "follow",
        "food", "football", "for", "forever", "forget", "forgive", "forgot", "form", "forward", "found", "four",
        "free", "fresh", "friday", "friend", "friends", "from", "front", "fruit", "full", "fun", "funny", "future",
        "gallery", "game", "gaming", "garden", "gate", "gave", "general", "get", "getting", "gift", "girl",
        "give", "given", "giving", "glad", "glass", "glow", "go", "goal", "god", "goes", "going", "gold", "gone",
        "good", "goodbye", "goodnight", "google", "got", "gotta", "grade", "grand", "great", "green", "group",
        "grow", "guess", "guest", "guide", "guy", "guys", "gym",
        "habit", "had", "haha", "hair", "half", "hall", "hand", "handle", "hang", "happen", "happened", "happy",
        "hard", "has", "hate", "have", "haven't", "having", "he", "head", "headache", "health", "healthy", "hear",
        "heard", "heart", "heat", "heavy", "hello", "help", "helpful", "her", "here", "hey", "hi", "hide", "high",
        "him", "his", "history", "hit", "hold", "holiday", "home", "homework", "honest", "honestly", "hope",
        "hospital", "hot", "hotel", "hour", "hours", "house", "how", "however", "huge", "human", "hundred",
        "hungry", "hurry", "hurt",
        "ice", "idea", "if", "ignore", "image", "imagine", "immediately", "important", "improve", "in", "include",
        "india", "info", "information", "inside", "instagram", "install", "instant", "instead", "interest",
        "interesting", "internet", "interview", "into", "invite", "is", "isn't", "issue", "it", "it's", "item",
        "job", "join", "joining", "joke", "journey", "joy", "juice", "jump", "just",
        "keep", "keeping", "kept", "key", "keyboard", "kick", "kid", "kids", "kind", "king", "kitchen", "knew", "know",
        "language", "laptop", "large", "last", "late", "later", "latest", "laugh", "launch", "lazy", "lead",
        "learn", "learning", "least", "leave", "leaving", "left", "less", "lesson", "let", "let's", "letter",
        "level", "library", "life", "lifetime", "light", "like", "likely", "limit", "line", "link", "list",
        "listen", "listening", "little", "live", "living", "load", "loading", "local", "location", "lock",
        "login", "lol", "long", "look", "looking", "looks", "lose", "lost", "lot", "lots", "loud", "love",
        "lovely", "low", "luck", "lucky", "lunch",
        "mad", "made", "magic", "mail", "main", "make", "making", "mall", "man", "manage", "many", "map",
        "market", "marry", "match", "matter", "may", "maybe", "me", "meal", "mean", "meaning", "means", "media",
        "medicine", "meet", "meeting", "member", "memory", "mention", "menu", "message", "messages", "met",
        "metro", "middle", "midnight", "might", "milk", "million", "mind", "mine", "minute", "minutes", "mirror",
        "miss", "missed", "missing", "mistake", "mobile", "mode", "model", "modern", "mom", "moment", "monday",
        "money", "month", "months", "mood", "moon", "more", "morning", "most", "mother", "mountain", "move",
        "movie", "movies", "moving", "much", "music", "must", "my", "myself",
        "name", "nap", "natural", "nature", "near", "nearby", "nearly", "necessary", "need", "needed", "needs",
        "neon", "net", "network", "never", "new", "news", "next", "nice", "night", "nine", "no", "nobody",
        "noise", "none", "noon", "normal", "not", "note", "notes", "nothing", "notice", "now", "number",
        "obviously", "of", "off", "offer", "office", "official", "often", "oh", "ok", "okay", "old", "omg",
        "on", "once", "one", "online", "only", "onto", "open", "opening", "opinion", "option", "or", "orange",
        "order", "ordered", "original", "other", "others", "our", "out", "outside", "over", "own", "owner",
        "pack", "package", "page", "paid", "pain", "paint", "pair", "paper", "parcel", "parents", "park",
        "parking", "part", "party", "pass", "password", "past", "path", "patient", "pay", "paying", "payment",
        "paytm", "peace", "pen", "pending", "people", "perfect", "perhaps", "permission", "person", "personal",
        "phone", "phonepe", "photo", "photos", "pic", "pick", "pickup", "pics", "picture", "piece", "pink",
        "pizza", "place", "plan", "plane", "planning", "plans", "play", "player", "playing", "please", "pleasure",
        "plenty", "pls", "plus", "pocket", "point", "police", "polite", "pool", "poor", "popular", "possible",
        "post", "power", "practice", "pray", "prefer", "premium", "prepare", "present", "press", "pretty",
        "price", "pride", "print", "private", "prize", "pro", "probably", "problem", "problems", "process",
        "product", "profile", "program", "progress", "project", "promise", "proper", "protect", "proud", "prove",
        "public", "pull", "purple", "purpose", "push", "put",
        "quality", "question", "quick", "quickly", "quiet", "quite",
        "rain", "raining", "rakib", "random", "rate", "rather", "reach", "reached", "reaching", "read", "reading",
        "ready", "real", "realize", "really", "reason", "receipt", "receive", "received", "recent", "recently",
        "recharge", "recommend", "record", "red", "refresh", "regular", "relax", "release", "remember", "remind",
        "reminder", "remove", "rent", "repair", "repeat", "reply", "report", "request", "required", "reset",
        "respect", "response", "rest", "restaurant", "restore", "result", "results", "return", "review", "reward",
        "rgb", "rich", "ride", "right", "ring", "rise", "risk", "river", "road", "rock", "role", "room", "round",
        "route", "routine", "royal", "rule", "rules", "run", "running", "rush",
        "sad", "safe", "safely", "safety", "said", "salary", "sale", "same", "sample", "saturday", "save",
        "saved", "saving", "say", "saying", "says", "scan", "scared", "scene", "schedule", "school", "science",
        "score", "screen", "screenshot", "sea", "search", "season", "seat", "second", "seconds", "secret",
        "section", "secure", "see", "seeing", "seem", "seems", "seen", "select", "self", "selfie", "sell",
        "send", "sending", "senior", "sense", "sent", "separate", "serious", "seriously", "serve", "service",
        "session", "set", "setting", "settings", "seven", "several", "shadow", "shake", "shall", "shame",
        "shape", "share", "shared", "sharing", "sharp", "she", "sheet", "shift", "shine", "shirt", "shock",
        "shoes", "shoot", "shop", "shopping", "short", "shot", "should", "shouldn't", "show", "shower", "showing",
        "shut", "shy", "sick", "side", "sign", "signal", "signature", "silent", "silly", "silver", "similar",
        "simple", "simply", "since", "sing", "single", "sir", "sister", "sit", "site", "sitting", "situation",
        "six", "size", "skill", "skin", "sky", "sleep", "sleeping", "sleepy", "slide", "slow", "slowly", "small",
        "smart", "smell", "smile", "smiling", "smoke", "smooth", "snack", "snap", "snow", "so", "social", "soft",
        "software", "sold", "solid", "solution", "solve", "some", "somebody", "someday", "somehow", "someone",
        "something", "sometimes", "somewhere", "son", "song", "songs", "soon", "sorry", "sort", "soul", "sound",
        "soup", "source", "south", "space", "spark", "speak", "speaking", "special", "speed", "spend", "spent",
        "spirit", "sport", "sports", "spot", "spring", "square", "stage", "stand", "standard", "standing", "star",
        "start", "started", "starting", "state", "station", "status", "stay", "staying", "steady", "step",
        "steps", "sticker", "stickers", "still", "stock", "stomach", "stone", "stop", "stopped", "store", "storm",
        "story", "straight", "strange", "street", "strength", "stress", "strict", "strike", "strong", "student",
        "studio", "study", "studying", "stuff", "style", "stylish", "subject", "submit", "success", "successful",
        "such", "sudden", "suddenly", "sugar", "suggest", "suggestion", "suit", "summer", "sun", "sunday",
        "sunrise", "sunset", "super", "superb", "support", "suppose", "sure", "surely", "surprise", "sweet",
        "swim", "switch", "system",
        "table", "take", "taken", "taking", "talent", "talk", "talked", "talking", "tall", "tap", "target",
        "task", "taste", "tasty", "tax", "taxi", "tea", "teach", "teacher", "team", "tech", "telegram", "tell",
        "telling", "temperature", "ten", "tension", "term", "terrible", "test", "testing", "text", "texting",
        "than", "thank", "thanks", "that", "that's", "the", "their", "them", "theme", "themselves", "then",
        "there", "these", "they", "they're", "thick", "thin", "thing", "things", "think", "thinking", "third",
        "thirsty", "this", "those", "though", "thought", "thousand", "thread", "three", "through", "throw",
        "thursday", "ticket", "tie", "tight", "till", "time", "timer", "times", "tiny", "tip", "tired", "title",
        "to", "today", "together", "toilet", "told", "tomorrow", "tone", "tonight", "too", "took", "tool",
        "tooth", "top", "topic", "total", "totally", "touch", "tough", "tour", "toward", "tower", "town",
        "track", "trade", "traffic", "train", "training", "transfer", "translate", "travel", "traveling",
        "treat", "tree", "trend", "trial", "trick", "tried", "trip", "trouble", "truck", "true", "truly",
        "trust", "truth", "try", "trying", "tuesday", "tune", "turn", "turned", "turning", "tv", "twelve",
        "twenty", "twice", "two", "type", "typing",
        "ugly", "ultimate", "umbrella", "unable", "uncle", "under", "understand", "understood", "unfortunately",
        "uniform", "unique", "unit", "united", "university", "unknown", "unless", "unlike", "unlimited", "unlock",
        "unlocked", "unlucky", "until", "unusual", "up", "update", "updated", "upgrade", "upload", "upon", "upper",
        "upset", "upstairs", "upi", "urgent", "us", "use", "used", "useful", "user", "using", "usual", "usually",
        "vacation", "valid", "valley", "value", "van", "variety", "various", "vast", "vegetable", "vehicle",
        "verify", "version", "very", "vice", "victory", "video", "videos", "view", "village", "vip", "viral",
        "virtual", "virus", "visa", "vision", "visit", "visiting", "visitor", "visual", "voice", "volume", "vote",
        "wait", "waited", "waiting", "wake", "waking", "walk", "walking", "wall", "wallet", "want", "wanted",
        "wanting", "wants", "war", "warm", "warn", "warning", "was", "wash", "wasn't", "waste", "watch",
        "watching", "water", "wave", "way", "ways", "we", "we're", "weak", "wealth", "wear", "wearing", "weather",
        "web", "website", "wedding", "wednesday", "week", "weekend", "weekly", "weeks", "weight", "weird",
        "welcome", "well", "went", "were", "west", "wet", "what", "what's", "whatever", "whatsapp", "wheel",
        "when", "whenever", "where", "wherever", "whether", "which", "while", "white", "who", "who's", "whole",
        "whom", "whose", "why", "wide", "wife", "wifi", "wild", "will", "willing", "win", "wind", "window",
        "winner", "winning", "winter", "wire", "wise", "wish", "wishes", "wishing", "with", "within", "without",
        "woke", "woman", "women", "won", "won't", "wonder", "wonderful", "wood", "word", "words", "work",
        "worked", "worker", "working", "workout", "works", "world", "worried", "worry", "worse", "worst",
        "worth", "would", "wouldn't", "wow", "wrap", "write", "writer", "writing", "written", "wrong", "wrote",
        "ya", "yard", "yeah", "year", "years", "yellow", "yep", "yes", "yesterday", "yet", "yoga", "you",
        "you'd", "you'll", "you're", "you've", "young", "your", "yours", "yourself", "youth", "youtube", "zero", "zone"
    )

    // ── Hindi Next-Word Bigrams & 130+ Everyday Dictionary ──────────────────────
    private val hindiBigrams: Map<String, List<String>> = mapOf(
        "मैं" to listOf("जा रहा हूँ", "ठीक हूँ", "आ रहा हूँ", "अभी", "घर पर हूँ", "व्यस्त हूँ"),
        "आप" to listOf("कैसे हैं?", "कहाँ हैं?", "कब आएंगे?", "ठीक हैं?", "क्या कर रहे हैं?"),
        "तुम" to listOf("कैसे हो?", "कहाँ हो?", "क्या कर रहे हो?", "कब आओगे?", "ठीक हो?"),
        "क्या" to listOf("कर रहे हो?", "हुआ?", "हाल है?", "बात है?", "खबर है?"),
        "ठीक" to listOf("है", "हूँ", "कहा", "है भाई", "हो जाएगा"),
        "शुभ" to listOf("रात्रि", "प्रभात", "दिन", "जन्मदिन", "संध्या"),
        "बहुत" to listOf("बढ़िया", "धन्यवाद", "अच्छा", "सुंदर", "खुशी"),
        "अभी" to listOf("आ रहा हूँ", "व्यस्त हूँ", "घर पर हूँ", "बात करता हूँ", "फ्री हूँ"),
        "कहाँ" to listOf("हो?", "हैं?", "जा रहे हो?", "मिलेंगे?"),
        "हाँ" to listOf("ठीक है", "भाई", "जरूर", "आ रहा हूँ", "मैं हूँ"),
        "नहीं" to listOf("भाई", "अभी नहीं", "कोई बात नहीं", "पता"),
        "कोई" to listOf("बात नहीं", "दिक्कत नहीं", "काम है?"),
        "कब" to listOf("आओगे?", "मिलोगे?", "फ्री हो?", "कॉल करूँ?"),
        "चलो" to listOf("चलते हैं", "मिलते हैं", "खाते हैं", "ठीक है")
    )

    private val hindiDictionary: List<String> = listOf(
        "अच्छा", "अभी", "अपना", "अपने", "अलग", "अरे", "अंदर", "आ रहा हूँ", "आज", "आप", "आपका", "आपको",
        "आराम", "आना", "आओ", "आया", "आशा", "इंतज़ार", "इतना", "इधर", "इस", "इसलिए", "उधर", "उनका", "ऊपर",
        "एक", "ऐसा", "और", "कब", "कम", "कर", "करना", "करो", "कल", "कहाँ", "कहना", "काम", "कॉल", "कितना",
        "किधर", "किसी", "की", "कुछ", "कैसे", "कैसा", "कोई", "कौन", "क्या", "क्यों", "खबर", "खत्म", "खाना",
        "खा लिया", "खुश", "खुशी", "खेल", "गया", "गर्मी", "गाड़ी", "गाना", "गुड", "घर", "घंटा", "चलो", "चाय",
        "चाहिए", "चिंता", "छुट्टी", "छोटा", "जगह", "जन्मदिन", "जरूर", "जरूरी", "जल्दी", "जवाब", "जा रहा हूँ",
        "जाना", "जानता", "जी", "जीवन", "ज्यादा", "ठीक", "ठीक है", "ठंड", "टाइम", "तैयार", "तो", "तुम",
        "तुम्हारा", "तुम्हें", "थोड़ा", "दिक्कत", "दिन", "दिल", "दुनिया", "देख", "देखो", "देना", "देर", "दो",
        "दोपहर", "दोस्त", "धन्यवाद", "नया", "नमस्ते", "नहीं", "नाम", "नंबर", "नींद", "पक्का", "पता", "पढ़ाई",
        "पसंद", "पानी", "पास", "पैसे", "प्यार", "प्रणाम", "फोटो", "फोन", "फ्री", "बढ़िया", "बड़ा", "बताओ",
        "बात", "बाद", "बारिश", "बाहर", "बिल्कुल", "बोलो", "भाई", "भेज", "भेजो", "मदद", "मज़ा", "मत", "मन",
        "माफ़", "माँ", "मिलते", "मिलना", "मैसेज", "मैं", "मेरा", "मेरी", "मेरे", "मौसम", "मोबाइल", "याद",
        "यार", "यह", "यहाँ", "रखना", "रहा", "रात", "रास्ता", "रुक", "रुको", "रोज़", "लगता", "लव", "लेकिन",
        "लेना", "लोग", "वक्त", "वहाँ", "वापस", "वीडियो", "व्यस्त", "शाम", "शादी", "शुक्रिया", "शुभ", "शुभ रात्रि",
        "शुरू", "सकते", "सच", "सब", "समझ", "समय", "सही", "साथ", "साल", "सुबह", "सुंदर", "सुनो", "सॉरी",
        "स्कूल", "स्टेटस", "हम", "हमारा", "हमेशा", "हाँ", "हाल", "हिन्दी", "हूँ", "है", "हैं", "हेलो", "हो", "होगा", "हो गया"
    )
}
