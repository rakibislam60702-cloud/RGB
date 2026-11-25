package com.example.engine

/**
 * Natural Indian / West Bengal Romanized Bengali (বাংলিশ / BN-EN) Transliteration Engine.
 *
 * Strictly one-way Bengali -> Romanized English conversion designed for everyday Kolkata /
 * West Bengal conversational chat spelling (never stiff academic transliteration, and NEVER
 * converts English letters back into Bengali Unicode script):
 * - 260+ curated everyday Indian Banglish word dictionary (`তুমি` -> `tumi`, `ভালো` -> `valo`, `করছো` -> `korcho`, etc.)
 * - Phonetic Bengali-to-Latin Conjunct & Matra Engine for any out-of-dictionary word
 * - Automatic sentence-start capitalization & Bengali punctuation normalization (`।` -> `.`)
 * - Strictly Romanized English suggestions for both typed Latin prefixes and pasted/spoken Bengali words
 */
object BengaliTransliterator {

    /**
     * 260+ everyday Indian / West Bengal Bengali -> natural Romanized Banglish dictionary.
     */
    val bengaliToBanglishDict: Map<String, String> = linkedMapOf(
        // Pronouns & People
        "আমি" to "ami",
        "আমার" to "amar",
        "আমাকে" to "amake",
        "আমায়" to "amay",
        "আমায়" to "amay",
        "আমরা" to "amra",
        "আমাদের" to "amader",
        "তুমি" to "tumi",
        "তোমার" to "tomar",
        "তোমাকে" to "tomake",
        "তোমায়" to "tomay",
        "তোমায়" to "tomay",
        "তোমরা" to "tomra",
        "তোমাদের" to "tomader",
        "তুই" to "tui",
        "তোর" to "tor",
        "তোকে" to "toke",
        "তোরা" to "tora",
        "আপনি" to "apni",
        "আপনার" to "apnar",
        "আপনাকে" to "apnake",
        "আপনারা" to "apnara",
        "সে" to "se",
        "তার" to "tar",
        "তাকে" to "take",
        "তারা" to "tara",
        "তাদের" to "tader",
        "ও" to "o",
        "ওর" to "or",
        "ওকে" to "oke",
        "ওরা" to "ora",
        "ওদের" to "oder",
        "এটা" to "eta",
        "ওটা" to "ota",
        "এগুলো" to "egulo",
        "ওগুলো" to "ogulo",
        "নিজে" to "nije",
        "নিজের" to "nijer",
        "সবাই" to "sobai",
        "কেউ" to "keu",
        "কারো" to "karo",
        "বন্ধু" to "bondhu",
        "ভাই" to "bhai",
        "দাদা" to "dada",
        "দিদি" to "didi",
        "বোন" to "bon",
        "মা" to "ma",
        "বাবা" to "baba",
        "ছেলে" to "chele",
        "মেয়ে" to "meye",
        "মেয়ে" to "meye",
        "মানুষ" to "manush",
        "লোক" to "lok",
        "পরিবার" to "poribar",

        // Core Verbs & Daily Actions (West Bengal colloquial)
        "আছি" to "achi",
        "আছো" to "acho",
        "আছ" to "acho",
        "আছে" to "ache",
        "আছেন" to "achen",
        "আছিস" to "achis",
        "ছিল" to "chilo",
        "ছিলাম" to "chilam",
        "ছিলে" to "chile",
        "থাকি" to "thaki",
        "থাকো" to "thako",
        "থেকো" to "theko",
        "থাকব" to "thakbo",
        "থাকবো" to "thakbo",
        "থাকবে" to "thakbe",
        "থাকতে" to "thakte",
        "থেকে" to "theke",
        "করি" to "kori",
        "করো" to "koro",
        "কর" to "koro",
        "করে" to "kore",
        "করছি" to "korchi",
        "করছো" to "korcho",
        "করছ" to "korcho",
        "করছে" to "korche",
        "করেছি" to "korechi",
        "করেছো" to "korecho",
        "করেছ" to "korecho",
        "করেছে" to "koreche",
        "করব" to "korbo",
        "করবো" to "korbo",
        "করবে" to "korbe",
        "করতে" to "korte",
        "করলাম" to "korlam",
        "করলে" to "korle",
        "যাই" to "jai",
        "যাও" to "jao",
        "যায়" to "jay",
        "যায়" to "jay",
        "যাচ্ছি" to "jacchi",
        "যাচ্ছো" to "jaccho",
        "যাচ্ছ" to "jaccho",
        "যাচ্ছে" to "jacche",
        "যাব" to "jabo",
        "যাবো" to "jabo",
        "যাবে" to "jabe",
        "যেতে" to "jete",
        "গেছি" to "gechi",
        "গেছো" to "gecho",
        "গেছে" to "geche",
        "গেলাম" to "gelam",
        "আসি" to "asi",
        "এসো" to "eso",
        "আসো" to "aso",
        "আসছি" to "aschi",
        "আসছো" to "ascho",
        "আসছে" to "asche",
        "আসব" to "asbo",
        "আসবো" to "asbo",
        "আসবে" to "asbe",
        "আসতে" to "aste",
        "এসেছি" to "esechi",
        "এসেছো" to "esecho",
        "এসেছে" to "eseche",
        "এলাম" to "elam",
        "বলি" to "boli",
        "বলো" to "bolo",
        "বল" to "bolo",
        "বলে" to "bole",
        "বলছি" to "bolchi",
        "বলছো" to "bolcho",
        "বলছে" to "bolche",
        "বলেছি" to "bolechi",
        "বলেছো" to "bolecho",
        "বলেছে" to "boleche",
        "বলব" to "bolbo",
        "বলবো" to "bolbo",
        "বলবে" to "bolbe",
        "বলতে" to "bolte",
        "বললাম" to "bollam",
        "খাই" to "khai",
        "খাও" to "khao",
        "খাচ্ছি" to "khacchi",
        "খাচ্ছো" to "khaccho",
        "খাব" to "khabo",
        "খাবো" to "khabo",
        "খাবে" to "khabe",
        "খেতে" to "khete",
        "খেয়েছি" to "kheyechi",
        "খেয়েছি" to "kheyechi",
        "খেয়েছো" to "kheyecho",
        "খেয়েছো" to "kheyecho",
        "খেয়েছ" to "kheyecho",
        "খেলাম" to "khelam",
        "দেখি" to "dekhi",
        "দেখো" to "dekho",
        "দেখ" to "dekho",
        "দেখছি" to "dekhchi",
        "দেখছো" to "dekhcho",
        "দেখেছি" to "dekhechi",
        "দেখেছো" to "dekhecho",
        "দেখব" to "dekhbo",
        "দেখবো" to "dekhbo",
        "দেখতে" to "dekhte",
        "দেখা" to "dekha",
        "শুনি" to "shuni",
        "শোনো" to "shono",
        "শুনছি" to "shunchi",
        "শুনেছি" to "shunechi",
        "শুনতে" to "shunte",
        "জানি" to "jani",
        "জানো" to "jano",
        "জানে" to "jane",
        "জানাচ্ছি" to "janacchi",
        "জানিও" to "janio",
        "জানতে" to "jante",
        "বুঝি" to "bujhi",
        "বুঝেছি" to "bujhechi",
        "বুঝেছো" to "bujhecho",
        "বুঝতে" to "bujhte",
        "পারি" to "pari",
        "পারো" to "paro",
        "পারে" to "pare",
        "পারব" to "parbo",
        "পারবো" to "parbo",
        "পারবে" to "parbe",
        "পারছি" to "parchi",
        "চাই" to "chai",
        "চাও" to "chao",
        "চায়" to "chay",
        "চায়" to "chay",
        "চলো" to "cholo",
        "চল" to "cholo",
        "চলে" to "chole",
        "চলছে" to "cholche",
        "দাও" to "dao",
        "দিচ্ছি" to "dicchi",
        "দিলাম" to "dilam",
        "দিয়েছি" to "diyechi",
        "দিয়েছি" to "diyechi",
        "দিতে" to "dite",
        "দেবো" to "debo",
        "দেব" to "debo",
        "দেবে" to "debe",
        "নাও" to "nao",
        "নিচ্ছি" to "nicchi",
        "নিলাম" to "nilam",
        "নিয়ে" to "niye",
        "নিয়ে" to "niye",
        "নিতে" to "nite",
        "নেবো" to "nebo",
        "নেব" to "nebo",
        "পাঠাও" to "pathao",
        "পাঠিয়েছি" to "pathiyechi",
        "পাঠিয়েছি" to "pathiyechi",
        "পাঠাচ্ছি" to "pathacchi",
        "পেয়েছি" to "peyechi",
        "পেয়েছি" to "peyechi",
        "পাবো" to "pabo",
        "পাব" to "pabo",
        "রাখো" to "rakho",
        "রাখছি" to "rakhchi",
        "লিখছি" to "likhchi",
        "লিখেছি" to "likhechi",
        "পড়ছি" to "porchi",
        "পড়ছি" to "porchi",
        "পড়েছি" to "porechi",
        "ভাবছি" to "vabchi",
        "ভেবেছি" to "vebechi",
        "দাঁড়াও" to "darao",
        "দাঁড়াও" to "darao",
        "বসো" to "boso",
        "ঘুম" to "ghum",
        "ঘুমাচ্ছি" to "ghumacchi",
        "ঘুমাবো" to "ghumabo",
        "হয়" to "hoy",
        "হয়" to "hoy",
        "হবে" to "hobe",
        "হচ্ছে" to "hocche",
        "হয়েছে" to "hoyeche",
        "হয়েছে" to "hoyeche",
        "হয়েছিল" to "hoyechilo",
        "হয়েছিল" to "hoyechilo",
        "হলো" to "holo",
        "হল" to "holo",
        "হোক" to "hok",
        "লাগে" to "lage",
        "লাগছে" to "lagche",
        "লাগবে" to "lagbe",
        "ভালোবাসি" to "valobashi",
        "ভালোবাসা" to "valobasha",
        "ভালবাসি" to "valobashi",
        "ভালবাসা" to "valobasha",

        // Questions & Connectors
        "কি" to "ki",
        "কী" to "ki",
        "কেমন" to "kemon",
        "কোথায়" to "kothay",
        "কোথায়" to "kothay",
        "কখন" to "kokhon",
        "কেন" to "keno",
        "কে" to "ke",
        "কার" to "kar",
        "কাকে" to "kake",
        "কোন" to "kon",
        "কোনো" to "kono",
        "কত" to "koto",
        "কিভাবে" to "kivabe",
        "কীভাবে" to "kivabe",
        "এবং" to "ebong",
        "আর" to "ar",
        "আরো" to "aro",
        "কিন্তু" to "kintu",
        "তবে" to "tobe",
        "তাহলে" to "tahole",
        "তাই" to "tai",
        "যদি" to "jodi",
        "যেন" to "jeno",
        "কারণ" to "karon",
        "নাকি" to "naki",
        "বা" to "ba",
        "অথবা" to "othoba",
        "তারপর" to "tarpor",
        "জন্য" to "jonno",
        "সাথে" to "sathe",
        "সঙ্গে" to "songe",
        "কাছে" to "kache",
        "মধ্যে" to "moddhe",
        "ভেতরে" to "vetore",
        "বাইরে" to "baire",
        "উপরে" to "upore",
        "নিচে" to "niche",
        "সামনে" to "samne",
        "পিছনে" to "pichone",
        "পাশে" to "pashe",

        // Time & Place
        "এখন" to "ekhon",
        "এখনো" to "ekhono",
        "এখনি" to "ekhoni",
        "পরে" to "pore",
        "আগে" to "age",
        "তখন" to "tokhon",
        "যখন" to "jokhon",
        "আজ" to "aj",
        "আজকে" to "ajke",
        "কাল" to "kal",
        "কালকে" to "kalke",
        "পরশু" to "porshu",
        "সকাল" to "sokal",
        "সকালে" to "sokale",
        "দুপুর" to "dupur",
        "দুপুরে" to "dupure",
        "বিকেল" to "bikel",
        "বিকেলে" to "bikele",
        "সন্ধ্যা" to "sondha",
        "রাত" to "rat",
        "রাতে" to "rate",
        "দিন" to "din",
        "সময়" to "somoy",
        "সময়" to "somoy",
        "ঘণ্টা" to "ghonta",
        "মিনিট" to "minit",
        "সপ্তাহ" to "soptaho",
        "মাস" to "mas",
        "বছর" to "bochor",
        "বাড়ি" to "bari",
        "বাড়ি" to "bari",
        "বাড়িতে" to "barite",
        "বাড়িতে" to "barite",
        "বাসায়" to "basay",
        "বাসায়" to "basay",
        "ঘরে" to "ghore",
        "এখানে" to "ekhane",
        "ওখানে" to "okhane",
        "সেখানে" to "sekhane",
        "রাস্তায়" to "rastay",
        "রাস্তায়" to "rastay",
        "বাজার" to "bajar",
        "দোকান" to "dokan",
        "অফিস" to "office",
        "কলেজ" to "college",
        "স্কুল" to "school",
        "শহর" to "sohor",
        "গ্রাম" to "gram",
        "কলকাতা" to "Kolkata",
        "বাংলা" to "Bangla",

        // Adjectives, Adverbs & Chat Essentials
        "ভালো" to "valo",
        "ভাল" to "valo",
        "খারাপ" to "kharap",
        "সুন্দর" to "sundor",
        "দারুণ" to "darun",
        "দারুন" to "darun",
        "অসাধারণ" to "osadharon",
        "খুব" to "khub",
        "অনেক" to "onek",
        "একটু" to "ektu",
        "একদম" to "ekdom",
        "বেশি" to "beshi",
        "কম" to "kom",
        "সব" to "sob",
        "কিছু" to "kichu",
        "কিছুই" to "kichui",
        "নতুন" to "notun",
        "পুরনো" to "purono",
        "বড়" to "boro",
        "বড়" to "boro",
        "ছোট" to "choto",
        "ঠান্ডা" to "thanda",
        "গরম" to "gorom",
        "তাড়াতাড়ি" to "taratari",
        "তাড়াতাড়ি" to "taratari",
        "ধীরে" to "dhire",
        "সত্যি" to "sotti",
        "মিথ্যে" to "mitthe",
        "ঠিক" to "thik",
        "ভুল" to "vul",
        "ব্যস্ত" to "byasto",
        "ফ্রি" to "free",
        "খুশি" to "khushi",
        "দুঃখ" to "dukkho",
        "দুঃখিত" to "dukkhito",
        "কষ্ট" to "koshto",
        "রাগ" to "rag",
        "ভয়" to "voy",
        "ভয়" to "voy",
        "মজা" to "moja",
        "দরকার" to "dorkar",
        "জরুরি" to "joruri",
        "সমস্যা" to "somossa",
        "ব্যাপার" to "byapar",
        "অপেক্ষা" to "opekkha",
        "চেষ্টা" to "chesta",
        "চিন্তা" to "chinta",
        "মন" to "mon",
        "মনে" to "mone",
        "কথা" to "kotha",
        "কাজ" to "kaj",
        "খবর" to "khobor",
        "খাবার" to "khabar",
        "ভাত" to "vat",
        "চা" to "cha",
        "জল" to "jol",
        "পানি" to "pani",
        "টাকা" to "taka",
        "দাম" to "dam",
        "নাম" to "nam",
        "ছবি" to "chobi",
        "গান" to "gan",
        "গল্প" to "golpo",
        "বই" to "boi",
        "ফোন" to "phone",
        "কল" to "call",
        "মেসেজ" to "message",
        "মোবাইল" to "mobile",
        "গাড়ি" to "gari",
        "গায়ে" to "gaye",
        "বৃষ্টি" to "brishti",
        "চাঁদ" to "chand",
        "আকাশ" to "akash",
        "স্বপ্ন" to "sopno",
        "জীবন" to "jibon",
        "হাত" to "hat",
        "চোখ" to "chokh",
        "হাসি" to "hashi",
        "শান্তি" to "shanti",
        "প্রিয়" to "priyo",
        "প্রিয়" to "priyo",
        "ধন্যবাদ" to "dhonnobad",
        "নমস্কার" to "nomoskar",
        "শুভ" to "shuvo",
        "রাত্রি" to "ratri",
        "জন্মদিন" to "jonmodin",
        "হ্যালো" to "hello",
        "হাই" to "hi",
        "বাই" to "bye",
        "হ্যাঁ" to "hya",
        "হাঁ" to "ha",
        "না" to "na",
        "নেই" to "nei",
        "নাই" to "nai",
        "আচ্ছা" to "accha",
        "ওকে" to "ok",
        "আরে" to "are",
        "বাহ" to "bah",
        "আবার" to "abar",
        "শুধু" to "shudhu",
        "হয়তো" to "hoyto",
        "হয়তো" to "hoyto",
        "অবশ্যই" to "obosshoi",
        "নিশ্চয়ই" to "nishchoi",
        "নিশ্চয়ই" to "nishchoi",
        "একটা" to "ekta",
        "দুটো" to "duto",
        "তিনটে" to "tinte",
        "সরি" to "sorry",
        "প্লিজ" to "please",
        "থ্যাংকস" to "thanks",
        "রাকিব" to "Rakib"
    )

    /**
     * Strictly Romanized English Banglish vocabulary list derived from [bengaliToBanglishDict]
     * plus everyday West Bengal chat spelling variants (NEVER emits Bengali Unicode script).
     */
    val romanizedBanglishVocabulary: List<String> by lazy {
        val unique = linkedSetOf<String>()
        bengaliToBanglishDict.values.forEach { latin ->
            if (!containsBengaliScript(latin) && latin.isNotBlank()) {
                unique.add(latin)
            }
        }
        listOf(
            "tumi", "valo", "korcho", "kothay", "ekhon", "bari", "jacchi", "aschi",
            "kemon", "acho", "ache", "bolchi", "bhalo", "bhalobashi", "bhalobasha",
            "kothai", "achho", "korchho", "bolchhi", "jacchhi", "aschhi", "barite",
            "shuvo", "nomoshkar", "kheyecho", "kheyechi", "khub", "sundor", "darun",
            "accha", "thik", "bondhu", "bhai", "dada", "didi", "pore", "kotha", "kal",
            "ajke", "kalke", "somoy", "khobor", "valobashi", "valobasha", "obosshoi"
        ).forEach { unique.add(it) }
        unique.toList()
    }

    private const val VIRAMA = '\u09CD' // ্

    private val independentVowels = mapOf(
        'অ' to "o",
        'আ' to "a",
        'ই' to "i",
        'ঈ' to "i",
        'উ' to "u",
        'ঊ' to "u",
        'ঋ' to "ri",
        'এ' to "e",
        'ঐ' to "oi",
        'ও' to "o",
        'ঔ' to "ou"
    )

    private val matras = mapOf(
        '\u09BE' to "a",  // া
        '\u09BF' to "i",  // ি
        '\u09C0' to "i",  // ী
        '\u09C1' to "u",  // ু
        '\u09C2' to "u",  // ূ
        '\u09C3' to "ri", // ৃ
        '\u09C7' to "e",  // ে
        '\u09C8' to "oi", // ৈ
        '\u09CB' to "o",  // ো
        '\u09CC' to "ou"  // ৌ
    )

    private val consonants = mapOf(
        'ক' to "k",
        'খ' to "kh",
        'গ' to "g",
        'ঘ' to "gh",
        'ঙ' to "ng",
        'চ' to "ch",
        'ছ' to "ch",
        'জ' to "j",
        'ঝ' to "jh",
        'ঞ' to "n",
        'ট' to "t",
        'ঠ' to "th",
        'ড' to "d",
        'ঢ' to "dh",
        'ণ' to "n",
        'ত' to "t",
        'থ' to "th",
        'দ' to "d",
        'ধ' to "dh",
        'ন' to "n",
        'প' to "p",
        'ফ' to "f",
        'ব' to "b",
        'ভ' to "v",
        'ম' to "m",
        'য' to "j",
        'র' to "r",
        'ল' to "l",
        'শ' to "sh",
        'ষ' to "sh",
        'স' to "s",
        'হ' to "h",
        'ড়' to "r",
        'ঢ়' to "r",
        'য়' to "y"
    )

    private val specialSigns = mapOf(
        'ং' to "ng",
        'ঃ' to "h",
        'ঁ' to "n",
        'ৎ' to "t"
    )

    private val bengaliDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    /** Common multi-codepoint Bengali conjuncts tuned for Indian colloquial chat spelling. */
    private val conjunctMap = mapOf(
        "ক্ষ্ম" to "kkh",
        "ক্ষ" to "kkh",
        "জ্ঞ" to "gg",
        "ত্র" to "tr",
        "ঞ্জ" to "nj",
        "ঙ্গ" to "ng",
        "ঞ্চ" to "nch",
        "ঙ্ক" to "nk",
        "ন্ত" to "nt",
        "ন্দ" to "nd",
        "ন্ধ" to "ndh",
        "স্ত" to "st",
        "ষ্ট" to "sht",
        "ষ্ঠ" to "shth",
        "চ্ছ" to "cch",
        "জ্জ" to "jj",
        "ত্ত" to "tt",
        "দ্দ" to "dd",
        "ন্ন" to "nn",
        "ম্ম" to "mm",
        "ল্ল" to "ll",
        "ক্ত" to "kt",
        "ল্প" to "lp",
        "স্প" to "sp",
        "স্ক" to "sk",
        "স্ট" to "st",
        "স্ব" to "sw",
        "শ্ব" to "shw",
        "হ্ম" to "hm",
        "হ্ন" to "hn"
    )

    fun containsBengaliScript(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    /**
     * Converts any Bengali or mixed Bengali-English sentence into natural Indian / West Bengal
     * Romanized Bengali with automatic sentence-start capitalization.
     *
     * Example: "তুমি কেমন আছো, এখন কি করছো?" -> "Tumi kemon acho, ekhon ki korcho?"
     */
    fun transliterate(input: String, capitalizeSentences: Boolean = true): String {
        if (input.isBlank()) return input

        val out = StringBuilder(input.length + 12)
        val token = StringBuilder()

        fun flushToken() {
            if (token.isEmpty()) return
            val word = token.toString()
            token.setLength(0)
            if (containsBengaliScript(word)) {
                out.append(transliterateWord(word))
            } else {
                out.append(word)
            }
        }

        for (ch in input) {
            when {
                isBengaliWordChar(ch) -> token.append(ch)
                ch == '।' -> {
                    flushToken()
                    out.append('.')
                }
                ch in bengaliDigits -> {
                    flushToken()
                    out.append(bengaliDigits[ch])
                }
                else -> {
                    flushToken()
                    out.append(ch)
                }
            }
        }
        flushToken()

        val rawResult = out.toString()
        return if (capitalizeSentences) applySentenceCapitalization(rawResult) else rawResult
    }

    /**
     * Transliterates a single Bengali word using the 260+ colloquial Indian Banglish dictionary
     * first, falling back to the Phonetic Conjunct & Matra Engine for any unseen word.
     */
    fun transliterateWord(word: String): String {
        if (word.isEmpty()) return word
        bengaliToBanglishDict[word]?.let { return it }

        // Normalize decomposed nukta forms (ড + ় -> ড়, ঢ + ় -> ঢ়, য + ় -> য়)
        val normalized = word
            .replace("ড\u09BC", "ড়")
            .replace("ঢ\u09BC", "ঢ়")
            .replace("য\u09BC", "য়")
        bengaliToBanglishDict[normalized]?.let { return it }

        val sb = StringBuilder(normalized.length * 2)
        var i = 0
        val n = normalized.length

        while (i < n) {
            val ch = normalized[i]

            // 1. Try 3-char or 2-char conjunct match first (e.g., ক্ষ, জ্ঞ, ত্র, চ্ছ)
            val matchedConjunct = matchConjunctAt(normalized, i)
            if (matchedConjunct != null) {
                val (key, latinBase) = matchedConjunct
                val latin = if (key == "ক্ষ" && i == 0) "kh" else latinBase
                sb.append(latin)
                i += key.length
                i = consumeTrailingVowelOrInherent(normalized, i, sb, wasConjunct = true)
                continue
            }

            // 2. Independent vowels
            independentVowels[ch]?.let { v ->
                sb.append(v)
                i++
                continue
            }

            // 3. Consonants + phalas + matras + smart inherent vowel
            if (consonants.containsKey(ch)) {
                val base = when {
                    // Word-internal 'ভ' often reads smoothly as 'bh' or 'v'; keep natural 'v' for everyday flow
                    ch == 'ভ' -> if (i == 0) "v" else "bh"
                    // Word-initial 'স' before 'া/ো/ে' is natural 's', otherwise 's'
                    else -> consonants.getValue(ch)
                }
                sb.append(base)
                i++

                // Handle Bengali phalas (Virama + র / য / ব)
                while (i + 1 < n && normalized[i] == VIRAMA) {
                    val nextC = normalized[i + 1]
                    when (nextC) {
                        'র' -> { // র-ফলা (ra-phala)
                            sb.append("r")
                            i += 2
                        }
                        'য' -> { // য-ফলা (ja-phala)
                            // At start with আ-কার (e.g., হ্যালো / ব্যস্ত), let matra or 'y' handle smoothly
                            if (i + 2 < n && normalized[i + 2] == '\u09BE') {
                                sb.append("y")
                            } else if (i > 1) {
                                // Medial ja-phala often doubles/softens softly
                                sb.append("y")
                            } else {
                                sb.append("y")
                            }
                            i += 2
                        }
                        'ব' -> { // ব-ফলা (ba-phala)
                            if (i == 1) {
                                // Word-initial ba-phala is silent in colloquial West Bengal Bengali (স্বপ্ন -> sopno)
                                i += 2
                            } else {
                                sb.append("w")
                                i += 2
                            }
                        }
                        else -> break
                    }
                }

                // Explicit Virama suppresses inherent vowel
                if (i < n && normalized[i] == VIRAMA) {
                    i++
                    continue
                }

                i = consumeTrailingVowelOrInherent(normalized, i, sb, wasConjunct = false)
                continue
            }

            // 4. Special signs (ং, ঃ, ঁ, ৎ)
            specialSigns[ch]?.let { s ->
                // Suppress trailing chandrabindu 'ঁ' nasal if already ends in 'n'/'m'
                if (ch == 'ঁ') {
                    if (sb.isNotEmpty() && sb.last() != 'n' && sb.last() != 'm') {
                        sb.append(s)
                    }
                } else {
                    sb.append(s)
                }
                i++
                continue
            }

            // 5. Stray matra or digit or unknown char
            matras[ch]?.let { m ->
                sb.append(m)
                i++
                continue
            }
            bengaliDigits[ch]?.let { d ->
                sb.append(d)
                i++
                continue
            }
            if (ch != VIRAMA && ch != '\u09BC') {
                sb.append(ch)
            }
            i++
        }

        return sb.toString()
    }

    private fun matchConjunctAt(s: String, index: Int): Pair<String, String>? {
        if (index + 5 <= s.length) {
            val sub5 = s.substring(index, index + 5)
            conjunctMap[sub5]?.let { return sub5 to it }
        }
        if (index + 3 <= s.length && s[index + 1] == VIRAMA) {
            val sub3 = s.substring(index, index + 3)
            conjunctMap[sub3]?.let { return sub3 to it }
        }
        return null
    }

    private fun consumeTrailingVowelOrInherent(
        word: String,
        index: Int,
        sb: StringBuilder,
        wasConjunct: Boolean
    ): Int {
        if (index >= word.length) {
            // Word-final consonant: colloquial Bengali drops final inherent 'o' except after conjuncts
            if (wasConjunct && sb.isNotEmpty() && sb.last() !in "aeiou") {
                sb.append('o')
            }
            return index
        }

        val next = word[index]
        matras[next]?.let { m ->
            sb.append(m)
            return index + 1
        }

        if (next == VIRAMA) {
            return index + 1
        }

        // Insert natural inherent 'o' before another consonant / sign in the middle of the word
        if (consonants.containsKey(next) || specialSigns.containsKey(next)) {
            if (sb.isNotEmpty() && sb.last() !in "aeiou") {
                sb.append('o')
            }
        }
        return index
    }

    private fun isBengaliWordChar(ch: Char): Boolean {
        return (ch in '\u0980'..'\u09FF') && ch != '।' && ch != '॥' && ch !in bengaliDigits
    }

    /**
     * Capitalizes the first Latin letter of the text and after sentence terminators (`.`, `!`, `?`, `\n`).
     */
    fun applySentenceCapitalization(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        var capNext = true
        for (i in chars.indices) {
            val c = chars[i]
            if (capNext && c.isLetter()) {
                chars[i] = c.uppercaseChar()
                capNext = false
            } else if (c == '.' || c == '!' || c == '?' || c == '\n') {
                capNext = true
            } else if (!c.isWhitespace() && c != '"' && c != '\'' && c != '(') {
                capNext = false
            }
        }
        return String(chars)
    }

    /**
     * Returns true if [textBeforeWord] ends at a sentence boundary (start of input, newline, or `.`/`!`/`?`/`।`),
     * so the next Banglish word gets natural sentence-start capitalization (`Tumi`, while mid-sentence stays `kemon acho`).
     */
    fun isSentenceStartContext(textBeforeWord: String): Boolean {
        val trimmed = textBeforeWord.trimEnd { it == ' ' || it == '\t' }
        if (trimmed.isEmpty()) return true
        val last = trimmed.last()
        return last == '.' || last == '!' || last == '?' || last == '।' || last == '\n'
    }

    /**
     * Extracts the contiguous Bengali word right before the cursor (letters, matras, virama, signs)
     * so pressing Space, punctuation, or Enter converts that Bengali word cleanly to Romanized English.
     */
    fun extractTrailingBengaliWord(textBeforeCursor: String): String {
        if (textBeforeCursor.isEmpty()) return ""
        var start = textBeforeCursor.length
        while (start > 0 && isBengaliWordChar(textBeforeCursor[start - 1])) {
            start--
        }
        return if (start < textBeforeCursor.length) textBeforeCursor.substring(start) else ""
    }

    /**
     * Transliterates a single typed Bengali word into natural Indian Romanized English,
     * applying sentence-start capitalization ONLY when preceded by start-of-line or sentence terminator.
     */
    fun transliterateWordWithContext(bengaliWord: String, textBeforeWord: String): String {
        if (bengaliWord.isBlank()) return ""
        val latin = transliterate(bengaliWord, capitalizeSentences = false).trim()
        if (latin.isEmpty()) return ""
        return if (isSentenceStartContext(textBeforeWord)) {
            latin.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
        } else {
            latin
        }
    }

    /**
     * Returns true for punctuation keys that should commit & auto-transliterate a preceding Bengali word
     * when typing with Bengali keys in `বাংলিশ (BN-EN)` mode.
     */
    fun isAutoTransliterateTriggerPunctuation(committed: String): Boolean {
        if (committed.length != 1) return false
        return committed[0] in charArrayOf('।', '.', ',', '?', '!', ';', ':', ')', '"', '\'', '-', '—')
    }

    /**
     * Returns true if [committed] is a Bengali alphabet / vowel / matra / virama keystroke
     * that belongs to a live composing Banglish word.
     */
    fun isBengaliWordText(committed: String): Boolean {
        if (committed.isEmpty()) return false
        return committed.all { isBengaliWordChar(it) }
    }

    /**
     * Returns true if [committed] consists of Bengali numerals (`০`..`৯`).
     */
    fun isBengaliDigitText(committed: String): Boolean {
        if (committed.isEmpty()) return false
        return committed.all { it in bengaliDigits }
    }

    /**
     * Converts Bengali numerals (`০`..`৯`) to standard ASCII numerals (`0`..`9`).
     */
    fun convertBengaliDigits(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(bengaliDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Normalizes Bengali sentence danda `।` -> `.` when outputting Romanized English in Banglish mode.
     */
    fun normalizeBanglishPunctuation(committed: String): String {
        return when (committed) {
            "।" -> "."
            "॥" -> "."
            else -> committed
        }
    }

    /**
     * Extracts the active current-line segment containing Bengali Unicode script before the cursor
     * so the "বাংলা → বাংলিশ" converter chip can convert spoken/pasted/typed Bengali into
     * natural Indian Romanized English in one tap.
     */
    fun extractConvertibleBengaliSegment(textBeforeCursor: String): String {
        if (textBeforeCursor.isEmpty()) return ""
        val lastLine = textBeforeCursor.substringAfterLast('\n')
        if (!containsBengaliScript(lastLine)) return ""
        val firstBengaliIdx = lastLine.indexOfFirst { it in '\u0980'..'\u09FF' }
        if (firstBengaliIdx < 0) return ""
        val segment = lastLine.substring(firstBengaliIdx)
        return if (segment.length <= 140) segment else segment.takeLast(140)
    }

    /**
     * Strictly one-way Bengali -> Romanized English suggestions for `বাংলিশ (BN-EN)` mode.
     * - If the user has Bengali script (pasted/spoken/typed), returns ONLY natural Romanized English
     *   equivalents (`তুমি` -> `tumi`, `ভাল` -> `valo`, `valobashi`, `valobasha`).
     * - If the user types English/Latin letters, returns ONLY natural Romanized Indian Banglish
     *   completions (`val` -> `valo`, `valobashi`, `valobasha`; `kor` -> `korcho`, `korchi`, `korechi`).
     * - NEVER converts English letters back into Bengali Unicode script.
     */
    fun suggestRomanizedBanglish(prefix: String, maxItems: Int = 6): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        val shouldCap = prefix.firstOrNull()?.isUpperCase() == true
        val results = linkedSetOf<String>()

        if (containsBengaliScript(prefix)) {
            val directLatin = transliterateWord(prefix).trim()
            if (directLatin.isNotBlank() && !containsBengaliScript(directLatin)) {
                results.add(directLatin)
            }
            bengaliToBanglishDict.entries
                .asSequence()
                .filter { it.key.startsWith(prefix) }
                .map { it.value }
                .filter { !containsBengaliScript(it) }
                .take(maxItems)
                .forEach { results.add(it) }
        } else {
            // Exact lowercase match first (pure Romanized English only)
            romanizedBanglishVocabulary
                .firstOrNull { it.equals(lower, ignoreCase = true) }
                ?.let { exact ->
                    results.add(if (shouldCap) exact.replaceFirstChar { it.uppercase() } else exact)
                }
            // Prefix completions in natural Indian Banglish
            romanizedBanglishVocabulary
                .asSequence()
                .filter { it.startsWith(lower, ignoreCase = true) && !it.equals(lower, ignoreCase = true) }
                .take(maxItems)
                .forEach { latin ->
                    val formatted = if (shouldCap) latin.replaceFirstChar { it.uppercase() } else latin
                    results.add(formatted)
                }
        }
        return results.filter { !containsBengaliScript(it) }.take(maxItems)
    }
}
