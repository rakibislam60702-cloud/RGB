package com.example.engine

/**
 * Fast, zero-latency offline word prediction & next-word suggestion engine for
 * Bengali (`bn`), English (`en`), and Hindi (`hi`) with smart phrase extraction
 * for the glowing "✨ RGB Sticker" chip.
 */
object SmartWordPredictor {

    data class PredictionResult(
        val suggestions: List<String>,
        val currentTokenLength: Int,
        val stickerPhrase: String
    )

    fun predict(textBeforeCursor: String, languageId: String): PredictionResult {
        val stickerPhrase = extractStickerPhrase(textBeforeCursor)
        val endedWithSpace = textBeforeCursor.isNotEmpty() && textBeforeCursor.last().isWhitespace()

        if (textBeforeCursor.isBlank()) {
            return PredictionResult(
                suggestions = defaultStarters(languageId),
                currentTokenLength = 0,
                stickerPhrase = ""
            )
        }

        if (endedWithSpace) {
            val prevWord = extractLastCompletedWord(textBeforeCursor)
            val nextWords = nextWordPredictions(prevWord, languageId)
            return PredictionResult(
                suggestions = nextWords.ifEmpty { defaultStarters(languageId) },
                currentTokenLength = 0,
                stickerPhrase = stickerPhrase
            )
        }

        val currentWord = extractCurrentTypingWord(textBeforeCursor)
        if (currentWord.isEmpty()) {
            return PredictionResult(
                suggestions = defaultStarters(languageId),
                currentTokenLength = 0,
                stickerPhrase = stickerPhrase
            )
        }

        val prefixMatches = prefixPredictions(currentWord, languageId)
        return PredictionResult(
            suggestions = prefixMatches,
            currentTokenLength = currentWord.length,
            stickerPhrase = stickerPhrase
        )
    }

    /**
     * Extracts the latest active typed phrase (up to 48 chars on the current line)
     * so tapping "✨ RGB Sticker" turns the user's message into a glowing sticker.
     */
    fun extractStickerPhrase(textBeforeCursor: String): String {
        val lastLine = textBeforeCursor.substringAfterLast('\n').trim()
        if (lastLine.isEmpty()) return ""
        return if (lastLine.length <= 48) lastLine else lastLine.takeLast(48).trimStart()
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

    private fun defaultStarters(languageId: String): List<String> = when (languageId) {
        "bn" -> listOf("আমি", "হ্যাঁ", "ঠিক আছে", "কেমন আছো?", "ভালো আছি", "ধন্যবাদ")
        "hi" -> listOf("मैं", "हाँ", "ठीक है", "कैसे हो?", "धन्यवाद", "अभी")
        else -> listOf("I'm", "Hello", "Okay", "Good", "Thanks", "On my way")
    }

    private fun nextWordPredictions(prevWord: String, languageId: String): List<String> {
        if (prevWord.isEmpty()) return emptyList()
        return when (languageId) {
            "bn" -> bengaliBigrams[prevWord] ?: bengaliBigrams.entries
                .firstOrNull { it.key.equals(prevWord, ignoreCase = true) }?.value.orEmpty()
            "hi" -> hindiBigrams[prevWord].orEmpty()
            else -> englishBigrams[prevWord.lowercase()].orEmpty()
        }
    }

    private fun prefixPredictions(prefix: String, languageId: String): List<String> {
        return when (languageId) {
            "bn" -> {
                val exactPrefix = bengaliDictionary.filter { it.startsWith(prefix) && it != prefix }
                val contains = if (exactPrefix.size < 5) {
                    bengaliDictionary.filter { it.contains(prefix) && !it.startsWith(prefix) }
                } else emptyList()
                (exactPrefix + contains).distinct().take(6)
            }
            "hi" -> {
                val exactPrefix = hindiDictionary.filter { it.startsWith(prefix) && it != prefix }
                val contains = if (exactPrefix.size < 5) {
                    hindiDictionary.filter { it.contains(prefix) && !it.startsWith(prefix) }
                } else emptyList()
                (exactPrefix + contains).distinct().take(6)
            }
            else -> {
                val lower = prefix.lowercase()
                val shouldCap = prefix.firstOrNull()?.isUpperCase() == true
                val isAllCaps = prefix.length > 1 && prefix.all { it.isUpperCase() }
                val matches = englishDictionary
                    .filter { it.startsWith(lower) && !it.equals(lower, ignoreCase = true) }
                    .distinct()
                    .take(6)
                matches.map { word ->
                    when {
                        isAllCaps -> word.uppercase()
                        shouldCap -> word.replaceFirstChar { it.uppercase() }
                        else -> word
                    }
                }
            }
        }
    }

    // ── Bengali Next-Word Bigrams ───────────────────────────────────────────────
    private val bengaliBigrams: Map<String, List<String>> = mapOf(
        "আমি" to listOf("যাচ্ছি", "যাব না", "ভালো আছি", "এখন", "আসছি", "কাজ করছি"),
        "তুমি" to listOf("কেমন আছো?", "কোথায়?", "কি করছো?", "আসবে?", "ভালো থেকো"),
        "আপনি" to listOf("কেমন আছেন?", "কোথায়?", "কখন আসবেন?", "ভালো থাকবেন"),
        "কেমন" to listOf("আছো?", "আছেন?", "লাগছে?", "হয়েছে?", "আছে?"),
        "ভালো" to listOf("আছি", "লাগছে", "থেকো", "হয়েছে", "খবর", "লাগে"),
        "ঠিক" to listOf("আছে", "বলেছো", "হয়েছে", "আছে ভাই", "করেছি"),
        "শুভ" to listOf("রাত্রি", "সকাল", "সন্ধ্যা", "জন্মদিন", "কামনা", "অভিনন্দন"),
        "কি" to listOf("করছো?", "খবর?", "হয়েছে?", "ব্যাপার?", "বলো?"),
        "কী" to listOf("করছো?", "খবর?", "হয়েছে?", "ব্যাপার?", "বলো?"),
        "এখন" to listOf("ব্যস্ত আছি", "যাচ্ছি", "আসছি", "কথা বলব", "কোথায়?", "ফ্রি আছি"),
        "একটু" to listOf("পরে", "অপেক্ষা করো", "ব্যস্ত আছি", "দাঁড়াও", "আসছি"),
        "খুব" to listOf("ভালো", "সুন্দর", "মজা", "দরকার", "খুশি", "কষ্ট"),
        "অনেক" to listOf("ধন্যবাদ", "ভালো", "সুন্দর", "ভালোবাসা", "সময়"),
        "পরে" to listOf("কথা বলব", "কল দিচ্ছি", "দেখা হবে", "জানাচ্ছি", "আসব"),
        "হ্যাঁ" to listOf("ঠিক আছে", "যাচ্ছি", "ভাই", "অবশ্যই", "আমি আছি"),
        "না" to listOf("ভাই", "এখন না", "ঠিক আছে", "সমস্যা নেই", "আমি যাব না"),
        "আসসালামু" to listOf("আলাইকুম"),
        "ওয়ালাইকুম" to listOf("আসসালাম"),
        "ইনশাআল্লাহ" to listOf("হবে", "দেখা হবে", "আসব", "ভালো হবে"),
        "ধন্যবাদ" to listOf("ভাই", "বন্ধু", "অনেক", "আপনাকে")
    )

    // ── Bengali High-Frequency Chat Dictionary ──────────────────────────────────
    private val bengaliDictionary: List<String> = listOf(
        // হ
        "হ্যাঁ", "হবে", "হয়েছে", "হ্যালো", "হয়", "হয়তো", "হাত", "হাসি", "হোয়াটসঅ্যাপ",
        // আ
        "আমি", "আপনি", "আছি", "আছেন", "আসছি", "আসব", "আচ্ছা", "আজ", "আমার", "আমাদের", "আসসালামু", "আলাইকুম", "আবার",
        // ত
        "তুমি", "তোমার", "তোমাকে", "তাই", "তবে", "তাড়াতাড়ি", "তাহলে", "তারপর", "তারা",
        // ক
        "কেমন", "কি", "কী", "করছো", "করেছি", "করব", "কথা", "কোথায়", "কাজ", "কখন", "কেন", "কিছু", "কল",
        // ভ
        "ভালো", "ভালো আছি", "ভালোবাসি", "ভাই", "ভাত", "ভুল", "ভয়",
        // য
        "যাচ্ছি", "যাব", "যাব না", "যদি", "যেন", "যাই", "যাবে",
        // ঠ
        "ঠিক", "ঠিক আছে", "ঠান্ডা",
        // ন
        "না", "নাকি", "নিয়ে", "নাম", "নতুন", "নেই", "নম্বর",
        // ধ
        "ধন্যবাদ", "ধরে", "ধীরে",
        // এ
        "এখন", "একটু", "এখানে", "এসেছি", "একটা", "এটা", "এবং", "একসাথে",
        // স
        "সব", "সবাই", "সকাল", "সময়", "সুন্দর", "সত্যি", "সরি", "সমস্যা", "সাথে", "সন্ধ্যা",
        // ব
        "বলো", "বলছি", "বলেছি", "বাড়ি", "বাংলা", "বাংলাদেশ", "বন্ধু", "বেশি", "ব্যস্ত", "ব্যাপার", "বুঝেছি", "বাই",
        // ম
        "মনে", "মা", "মানুষ", "মোবাইল", "মজা", "মাশাআল্লাহ", "মেসেজ", "মিনিট",
        // খ
        "খুব", "খাবার", "খেয়েছো", "খুশি", "খবর", "খুঁজছি",
        // চ
        "চলো", "চলে", "চাই", "চিন্তা", "ছবি",
        // প
        "পরে", "পারব", "প্লিজ", "পাঠিয়েছি", "প্রিয়", "পড়েছি", "পানি", "পাশে",
        // শ
        "শুভ", "শুভ রাত্রি", "শুভ সকাল", "শুরু", "শুধু", "শুনছি", "শেষ",
        // ও
        "ওকে", "ওখানে", "ওরা", "ওয়ালাইকুম",
        // গ
        "গেছি", "গল্প", "গাড়ি", "গান", "ঘুম", "ঘরে",
        // দ
        "দাও", "দরকার", "দিন", "দেখা", "দোয়া", "দুঃখিত", "দাঁড়াও",
        // র
        "রাত", "রাখো", "রাস্তায়", "রাকিব", "রেডি",
        // ল
        "লাগবে", "লিখেছি", "লাইক", "লাগছে",
        // ই / উ / জ / ট / ফ / স
        "ইনশাআল্লাহ", "ইংরেজি", "উত্তর", "জানি", "জানাচ্ছি", "জন্য", "জন্মদিন", "টাকা", "ফোন", "ফ্রি"
    )

    // ── English Next-Word Bigrams ───────────────────────────────────────────────
    private val englishBigrams: Map<String, List<String>> = mapOf(
        "i" to listOf("am", "will", "have", "think", "need", "love"),
        "i'm" to listOf("going", "good", "busy", "coming", "here", "ready"),
        "good" to listOf("morning", "night", "luck", "job", "evening", "idea"),
        "how" to listOf("are", "is", "much", "about", "was"),
        "are" to listOf("you", "we", "they", "you ready?"),
        "you" to listOf("are", "too", "can", "know", "want"),
        "thank" to listOf("you", "you so much", "god"),
        "thanks" to listOf("a lot", "bro", "so much", "for"),
        "see" to listOf("you", "you soon", "you later"),
        "on" to listOf("my way", "the way", "time"),
        "ok" to listOf("bro", "sure", "done", "got it", "thanks"),
        "okay" to listOf("sure", "bro", "done", "great"),
        "where" to listOf("are you?", "is it?", "now?"),
        "what" to listOf("are you doing?", "happened?", "time?", "is"),
        "please" to listOf("send", "wait", "call me", "check"),
        "call" to listOf("me", "you later", "now"),
        "happy" to listOf("birthday", "new year", "to help"),
        "let's" to listOf("go", "meet", "talk", "do it")
    )

    // ── English High-Frequency Chat Dictionary ──────────────────────────────────
    private val englishDictionary: List<String> = listOf(
        "about", "actually", "after", "again", "agree", "almost", "already", "also", "always",
        "amazing", "and", "another", "answer", "any", "anything", "app", "are", "around", "ask", "awesome",
        "back", "bad", "beautiful", "because", "been", "before", "believe", "best", "better", "birthday",
        "brother", "bro", "busy", "but", "buy",
        "call", "calling", "came", "can", "can't", "care", "check", "class", "clean", "close",
        "come", "coming", "congrats", "congratulations", "cool", "could", "crazy",
        "dark", "data", "day", "deal", "definitely", "did", "didn't", "different", "dinner", "do",
        "does", "doing", "done", "don't", "download", "drive",
        "early", "easy", "eat", "eating", "email", "end", "enjoy", "enough", "evening", "ever",
        "every", "everyone", "everything", "exact", "exactly", "excited",
        "family", "fast", "feel", "feeling", "few", "find", "fine", "finish", "finished", "fire",
        "first", "five", "follow", "food", "for", "forget", "free", "friend", "friends", "from", "fun", "funny",
        "game", "gaming", "get", "getting", "give", "glad", "go", "going", "good", "goodbye",
        "got", "great", "group", "guess", "guys",
        "had", "haha", "hand", "happen", "happened", "happy", "hard", "has", "have", "having",
        "he", "hear", "heart", "hello", "help", "her", "here", "hey", "hi", "high", "him", "his",
        "home", "honestly", "hope", "hot", "hour", "house", "how", "hundred", "hurry",
        "idea", "if", "important", "in", "info", "inside", "instagram", "instead", "interesting", "into", "is", "it", "it's",
        "job", "join", "joke", "just",
        "keep", "keyboard", "kind", "king", "know",
        "late", "later", "laugh", "learn", "leave", "leaving", "left", "let", "let's", "life",
        "light", "like", "link", "listen", "little", "live", "load", "location", "long", "look",
        "looking", "looks", "lot", "love", "luck", "lunch",
        "made", "make", "making", "man", "many", "maybe", "me", "mean", "meet", "meeting",
        "message", "might", "mind", "minute", "minutes", "miss", "missed", "mobile", "moment",
        "money", "month", "more", "morning", "most", "move", "movie", "much", "music", "must", "my",
        "name", "near", "need", "neon", "never", "new", "news", "next", "nice", "night", "no",
        "nobody", "normal", "not", "nothing", "now", "number",
        "of", "off", "office", "often", "oh", "ok", "okay", "old", "on", "once", "one", "online",
        "only", "open", "or", "order", "other", "our", "out", "outside", "over",
        "page", "party", "pass", "pay", "payment", "peace", "people", "perfect", "person",
        "phone", "photo", "pic", "pick", "picture", "place", "plan", "play", "playing", "please",
        "pls", "plus", "point", "possible", "post", "power", "pretty", "price", "problem", "pro", "promise",
        "quick", "quickly", "quiet", "quite",
        "rain", "rakib", "reach", "reached", "read", "ready", "real", "really", "reason",
        "receive", "remember", "reply", "rest", "result", "rgb", "right", "ring", "road", "room", "run",
        "sad", "safe", "said", "same", "save", "say", "saying", "school", "screen", "search",
        "second", "see", "seeing", "seem", "send", "sending", "sent", "serious", "set", "settings",
        "share", "she", "shop", "short", "should", "show", "shut", "sick", "side", "simple",
        "since", "sir", "sister", "sit", "sleep", "sleeping", "slow", "small", "smart", "smile",
        "so", "some", "someone", "something", "sometimes", "soon", "sorry", "sound", "space",
        "speak", "special", "speed", "spend", "start", "started", "status", "stay", "sticker",
        "still", "stop", "story", "strong", "study", "stuff", "style", "super", "support", "sure", "sweet",
        "take", "taking", "talk", "talking", "team", "telegram", "tell", "text", "than", "thank",
        "thanks", "that", "that's", "the", "their", "them", "then", "there", "these", "they",
        "thing", "things", "think", "thinking", "this", "those", "though", "thought", "three",
        "through", "time", "tired", "to", "today", "together", "told", "tomorrow", "tonight",
        "too", "took", "top", "touch", "travel", "true", "trust", "try", "trying", "turn", "two", "type", "typing",
        "under", "understand", "unlock", "until", "up", "update", "us", "use", "useful", "using", "usually",
        "very", "video", "view", "vip", "visit", "voice",
        "wait", "waiting", "wake", "walk", "want", "warm", "was", "watch", "watching", "water",
        "way", "we", "wear", "weather", "week", "weekend", "welcome", "well", "went", "were",
        "what", "what's", "whatsapp", "when", "where", "which", "while", "who", "whole", "why",
        "wifi", "will", "win", "wish", "with", "without", "wonderful", "won't", "word", "work",
        "working", "world", "worry", "worth", "would", "wow", "write", "wrong",
        "yeah", "year", "yes", "yesterday", "yet", "you", "you're", "your", "yourself", "youtube"
    )

    // ── Hindi Next-Word Bigrams & Dictionary ────────────────────────────────────
    private val hindiBigrams: Map<String, List<String>> = mapOf(
        "मैं" to listOf("जा रहा हूँ", "ठीक हूँ", "आ रहा हूँ", "अभी", "घर पर हूँ"),
        "आप" to listOf("कैसे हैं?", "कहाँ हैं?", "कब आएंगे?", "ठीक हैं?"),
        "तुम" to listOf("कैसे हो?", "कहाँ हो?", "क्या कर रहे हो?"),
        "क्या" to listOf("कर रहे हो?", "हुआ?", "हाल है?", "बात है?"),
        "ठीक" to listOf("है", "हूँ", "कहा", "है भाई"),
        "शुभ" to listOf("रात्रि", "प्रभात", "दिन", "जन्मदिन"),
        "बहुत" to listOf("बढ़िया", "धन्यवाद", "अच्छा", "सुंदर")
    )

    private val hindiDictionary: List<String> = listOf(
        "हाँ", "है", "हूँ", "होगा", "हेलो", "हम", "हमारा", "हाल",
        "मैं", "मेरा", "मुझे", "मिलते", "मैसेज", "मोबाइल",
        "आप", "आपका", "आ रहा हूँ", "अभी", "अच्छा", "आज",
        "तुम", "तुम्हारा", "ठीक", "ठीक है", "थोड़ा",
        "क्या", "कैसे", "कहाँ", "कब", "कर", "कल", "काम", "कॉल", "कोई",
        "नहीं", "नमस्ते", "नाम", "नया", "नंबर",
        "जा रहा हूँ", "जल्दी", "जरूर", "जी", "जन्मदिन",
        "धन्यवाद", "दोस्त", "दिन", "देखो",
        "बहुत", "बढ़िया", "भाई", "बात", "बाद", "बोलो",
        "शुभ", "शुभ रात्रि", "सुबह", "समय", "सुंदर", "सही", "सॉरी",
        "घर", "चलो", "प्यार", "पानी", "फोन", "फोटो", "रात", "यार", "लव"
    )
}
