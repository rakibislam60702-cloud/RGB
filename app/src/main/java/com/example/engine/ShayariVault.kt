package com.example.engine

import kotlin.math.abs

enum class ShayariCategory(
    val id: String,
    val emoji: String,
    val tabTitle: String,
    val accentHex: Int
) {
    ROMANTIC("romantic", "❤️", "রোমান্টিক", 0xFFFF2A85.toInt()),
    EMOTIONAL("emotional", "💔", "ইমোশনাল", 0xFFA855F7.toInt()),
    MOTIVATION("motivation", "🚀", "মোটিভেশন", 0xFF00F5D4.toInt()),
    ATTITUDE("attitude", "🔥", "অ্যাটিটিউড", 0xFFFF9E00.toInt()),
    QUICK_REPLY("quick_reply", "💬", "কুইক রিপ্লাই", 0xFF38BDF8.toInt())
}

data class ShayariVariation(
    val chipLabel: String,
    val text: String
)

data class GeneratedShayariResult(
    val resolvedTopic: String,
    val usedFallbackTheme: Boolean,
    val lines: List<String>
)

/**
 * Strictly curated, proofread 170-line Bengali & English Shayari and Smart Reply Vault
 * plus an input-validated, error-proof poetic Bengali auto-shayari generator.
 */
object ShayariVault {

    val quickTopicSuggestions: List<String> = listOf(
        "বৃষ্টি",
        "ভালোবাসা",
        "বন্ধু",
        "চাঁদ",
        "রাত",
        "স্বপ্ন"
    )

    private val fallbackPoeticThemes: List<String> = listOf(
        "ভালোবাসা",
        "বৃষ্টি",
        "চাঁদনী রাত",
        "বন্ধুত্ব",
        "মিষ্টি সন্ধ্যা",
        "নতুন স্বপ্ন"
    )

    // Common English/Banglish topic words cleanly mapped to natural Bengali poetic nouns
    private val topicNormalizationMap: Map<String, String> = mapOf(
        "love" to "ভালোবাসা",
        "valobasha" to "ভালোবাসা",
        "bhalobasha" to "ভালোবাসা",
        "rain" to "বৃষ্টি",
        "bristi" to "বৃষ্টি",
        "brishti" to "বৃষ্টি",
        "friend" to "বন্ধু",
        "friends" to "বন্ধু",
        "friendship" to "বন্ধুত্ব",
        "bondhu" to "বন্ধু",
        "moon" to "চাঁদ",
        "chand" to "চাঁদ",
        "night" to "রাত",
        "rat" to "রাত",
        "dream" to "স্বপ্ন",
        "sopno" to "স্বপ্ন",
        "swapno" to "স্বপ্ন",
        "smile" to "হাসি",
        "hasi" to "হাসি",
        "heart" to "মন",
        "mon" to "মন",
        "life" to "জীবন",
        "jibon" to "জীবন",
        "sky" to "আকাশ",
        "akash" to "আকাশ",
        "flower" to "ফুল",
        "phul" to "ফুল",
        "ful" to "ফুল",
        "evening" to "সন্ধ্যা",
        "sondha" to "সন্ধ্যা",
        "morning" to "সকাল",
        "sokal" to "সকাল",
        "tea" to "চা",
        "cha" to "চা",
        "coffee" to "কফি",
        "time" to "সময়",
        "somoy" to "সময়",
        "memory" to "স্মৃতি",
        "smriti" to "স্মৃতি",
        "alone" to "একাকীত্ব",
        "miss" to "অপেক্ষা",
        "wait" to "অপেক্ষা",
        "mother" to "মা",
        "maa" to "মা",
        "mom" to "মা",
        "music" to "গান",
        "song" to "গান",
        "river" to "নদী",
        "nodi" to "নদী",
        "star" to "তারা",
        "stars" to "তারা"
    )

    // ❤️ রোমান্টিক (42 curated, proofread Bengali romantic & flirt lines)
    private val romanticLines: List<String> = listOf(
        "তোমার হাসিটা দেখলে পুরো দিনটাই সুন্দর হয়ে যায় ✨",
        "চাঁদ তো আকাশে থাকে, কিন্তু আমার চাঁদটা এখন এই মেসেজটা পড়ছে ❤️",
        "কথা কম বলো, কিন্তু যখন বলো মনটা কেড়ে নাও 💫",
        "তোমাকে ছাড়া সবকিছু যেন একটু অসম্পূর্ণ লাগে 🌸",
        "হাজার মানুষের ভিড়েও আমার চোখ শুধু তোমাকেই খোঁজে ❤️",
        "তোমার একটা ছোট্ট মেসেজেই আমার মন ভালো হয়ে যায় 😊",
        "ভালোবাসা মানে বড় কথা নয়, প্রতিদিন তোমার খোঁজ নেওয়া 🌹",
        "তুমি পাশে থাকলে সাধারণ একটা সন্ধ্যাও অসাধারণ হয়ে ওঠে ✨",
        "তোমার চোখে তাকালেই পৃথিবীর সব ক্লান্তি দূর হয়ে যায় 💫",
        "আমার প্রতিটি সুন্দর প্রার্থনায় তোমার নামটা চুপচাপ থাকে 🤲❤️",
        "বৃষ্টির দিনে এক কাপ চা আর তোমার সঙ্গে গল্প—এইটুকুই যথেষ্ট ☕🌧️",
        "তুমি আমার জীবনের সবচেয়ে মিষ্টি অভ্যাস হয়ে গেছ 💖",
        "তোমার কণ্ঠস্বর শুনলে মনের সব অস্থিরতা শান্ত হয়ে যায় 🎶",
        "সময় যতই বদলাক, তোমার প্রতি আমার অনুভূতিটা একই থাকবে ❤️",
        "তুমি যখন হাসো, তখন মনে হয় চারপাশে ফুল ফুটেছে 🌷",
        "আমার ফোনের নোটিফিকেশনে তোমার নামটাই সবচেয়ে প্রিয় 💌",
        "হাতটা ধরে সারাজীবন একসাথে হাঁটার স্বপ্ন দেখি শুধু তোমাকে নিয়ে 🤝✨",
        "তুমি শুধু আমার পছন্দ নও, তুমি আমার মনের শান্তি 🌙",
        "দিনশেষে তোমার সাথে দুটো কথা বললেই সব ক্লান্তি ভুলে যাই 💛",
        "ভালোবাসি কথাটা হয়তো রোজ বলা হয় না, কিন্তু প্রতিটি মুহূর্তে অনুভব করি ❤️",
        "তোমার অভিমানটাও আমার কাছে ভীষণ আদরের লাগে 🥺✨",
        "পৃথিবীর সবচেয়ে সুন্দর জায়গা হলো তোমার হাসিমুখ 🌸",
        "তোমার সাথে কথা বলতে বসলে ঘড়ির কাঁটা কখন যে ছুটে যায় টেরই পাই না ⏳💫",
        "তুমি আমার কল্পনার চেয়েও অনেক বেশি সুন্দর 💖",
        "আকাশে যত তারা আছে, তার চেয়েও বেশি তোমাকে মনে পড়ে 🌟",
        "তোমার একটুখানি যত্ন আমাকে পুরো পৃথিবী জয়ের সাহস দেয় ❤️",
        "রাত যত গভীর হয়, তোমার কথা তত বেশি মনে পড়ে 🌙✨",
        "তুমি থাকলে জীবনটাকে সত্যিই একটা সুন্দর গল্প মনে হয় 📖💕",
        "তোমাকে দেখার পর থেকে অন্য কোনো সৌন্দর্য আর চোখে পড়ে না 🌹",
        "আমার প্রতিদিনের ভালো থাকার সবচেয়ে বড় কারণ তুমি 😊❤️",
        "একটা জীবন কম পড়ে যাবে তোমাকে পুরোপুরি ভালোবাসতে 💫",
        "তুমি শুধু একবার হাসো, আমার পুরো মনটাই আলোয় ভরে যাবে ✨",
        "তোমার নামের সাথেই আমার মনের সব সুন্দর স্বপ্ন জড়িয়ে আছে 💭💖",
        "দূরে থাকলেও তুমি সবসময় আমার হৃদয়ের সবচেয়ে কাছে থাকো ❤️",
        "তোমার প্রতিটি ছোট্ট কথা আমি খুব যত্ন করে মনে রাখি 🌷",
        "ভালোবাসা মানেই তোমার হাতে হাত রেখে নীরবে বসে থাকা 🤝🌙",
        "তুমি আমার সেই প্রিয় কবিতা, যা আমি প্রতিদিন নতুন করে পড়ি 📜✨",
        "সকালের রোদের মতোই তোমার উপস্থিতি আমার দিনটাকে উজ্জ্বল করে ☀️💛",
        "তোমার একটুখানি রাগ ভাঙাতেও আমার ভীষণ ভালো লাগে 😊❤️",
        "সব ব্যস্ততার মাঝেও আমার মনটা শুধু তোমার কাছেই ফিরে আসে 💫",
        "তুমি আমার জীবনের সেই উপহার, যা আমি কখনো হারাতে চাই না 🎁💖",
        "একটু সময় দিও, তোমার সাথে আরও অনেক সুন্দর স্মৃতি গড়তে চাই 🌸✨"
    )

    // 💔 ইমোশনাল (32 curated, deep & touching Bengali emotional lines)
    private val emotionalLines: List<String> = listOf(
        "কিছু মানুষ পাশে না থাকলেও মনের ভেতর সারাজীবন থেকে যায় 💔",
        "সব কষ্ট মুখে বলা যায় না, কিছু ব্যথা চুপচাপ বুকে পুষে রাখতে হয় 🌧️",
        "যাকে সবচেয়ে বেশি বিশ্বাস করেছিলাম, সেই মানুষটাই আজ সবচেয়ে অচেনা 😔",
        "বাইরে থেকে হাসিখুশি থাকলেও মনের ভেতরটা আজ ভীষণ ক্লান্ত 🌙",
        "সময় অনেক কিছু বদলে দেয়, কিন্তু প্রিয় মানুষের স্মৃতি মুছতে পারে না 💭",
        "আজকাল একা থাকতে থাকতে একাকীত্বটাই সবচেয়ে আপন হয়ে গেছে 🖤",
        "কিছু বিদায় খুব নীরবে হয়, কোনো শব্দ ছাড়াই মনটা ভেঙে দিয়ে যায় 💔",
        "মানুষ বদলে যায় না, শুধু তাদের প্রয়োজন ফুরিয়ে গেলে গুরুত্ব কমে যায় 😔",
        "রাতের নীরবতায় সবচেয়ে বেশি মনে পড়ে সেই হারিয়ে যাওয়া দিনগুলো 🌌",
        "অনেক কথা বলার ছিল, কিন্তু শোনার মতো মানুষটাই আর পাশে নেই 🥀",
        "কাউকে খুব বেশি গুরুত্ব দিলে একটা সময় নিজেকেই অবহেলিত হতে হয় 💔",
        "চোখের জল লুকানো সহজ, কিন্তু মনের হাহাকার লুকানো খুব কঠিন 😢",
        "পুরোনো মেসেজগুলো আজও পড়ি, আর ভাবি মানুষটা কতটা বদলে গেছে 📱🥀",
        "সবাই বলে ভালো থাকো, কিন্তু কেউ জিজ্ঞেস করে না কীভাবে ভালো আছি 😔",
        "যার জন্য পুরো পৃথিবী ছেড়ে দিতে চেয়েছিলাম, সে-ই আজ আমার খোঁজ নেয় না 💔",
        "মাঝে মাঝে মনে হয় একটু দূরে গিয়ে নিজেকে নতুন করে খুঁজে নিই 🌧️",
        "ভালোবাসা কম ছিল না, শুধু ভাগ্যটাই আমাদের একসাথে রাখেনি 🌙",
        "কিছু মানুষ জীবনে আসে শুধু শেখানোর জন্য, সারাজীবন থাকার জন্য নয় 🖤",
        "মন ভেঙে গেলে আর চিৎকার করতে ইচ্ছে করে না, শুধু চুপ হয়ে যেতে ইচ্ছে করে 😔",
        "অপেক্ষাটা আজও শেষ হয়নি, যদিও জানি তুমি আর আগের মতো ফিরবে না ⏳💔",
        "সবচেয়ে বড় কষ্ট হলো প্রিয় মানুষটাকে অনলাইনে দেখেও কথা বলতে না পারা 🥀",
        "হাসির আড়ালে কতটা ক্লান্তি লুকিয়ে আছে, তা কেউ দেখতে পায় না 🌙",
        "যে মানুষটা একদিন আমার সবটুকু ছিল, আজ তার সাথেই কোনো কথা নেই 💔",
        "অতিরিক্ত মায়া মানুষকে শেষ পর্যন্ত ভীষণ একা করে দেয় 🌧️",
        "নিজের কষ্ট নিজেই সামলে নিতে শিখে গেছি, এখন আর কারও কাছে অভিযোগ করি না 🖤",
        "স্মৃতিগুলো বড় অদ্ভুত—কখনো হাসায়, আবার কখনো হঠাৎ চোখ ভিজিয়ে দেয় 😢",
        "সব সম্পর্ক ভেঙে যাওয়ার জন্য ঝগড়া লাগে না, নীরবতাই যথেষ্ট 🥀",
        "তোমাকে ভুলে যাওয়া সহজ নয়, তাই স্মৃতিগুলো নিয়েই চুপচাপ বেঁচে আছি 💭💔",
        "মাঝে মাঝে খুব ইচ্ছে করে পুরোনো সময়টায় ফিরে গিয়ে সব ঠিক করে নিই ⏳",
        "যারা মন থেকে ভালোবাসে, দিনশেষে তারাই সবচেয়ে বেশি কষ্ট পায় 😔",
        "কথা কমে গেলে দূরত্ব বাড়ে, আর দূরত্ব বাড়লে মানুষটা ধীরে ধীরে হারিয়ে যায় 🌙",
        "আজও তোমার ভালো থাকাটাই চাই, শুধু আর আমার গল্পে তোমাকে খুঁজি না 🥀"
    )

    // 🚀 মোটিভেশন (32 inspiring Bengali & English success quotes)
    private val motivationLines: List<String> = listOf(
        "স্বপ্ন যত বড় হবে, পরিশ্রমও ততটাই নীরব আর দৃঢ় হওয়া উচিত 🚀",
        "আজকের ছোট ছোট প্রচেষ্টাই আগামী দিনের বড় সাফল্য গড়ে তোলে ✨",
        "হাল ছেড়ো না, কারণ সবচেয়ে কঠিন রাস্তার শেষেই সবচেয়ে সুন্দর গন্তব্য থাকে 🌟",
        "অন্যের সাথে তুলনা নয়, প্রতিদিন গতকালের নিজের চেয়ে একটু এগিয়ে যাও 💪",
        "সাফল্য একদিনে আসে না, কিন্তু প্রতিদিন চেষ্টা করলে একদিন ঠিকই আসে 🏆",
        "কঠিন সময় মানুষকে ভাঙতে নয়, আরও শক্তিশালী করে গড়ে তুলতে আসে 🔥",
        "নিজের ওপর বিশ্বাস রাখো—তুমি যতটা ভাবো, তার চেয়ে অনেক বেশি পারবে 🚀",
        "সময় নষ্ট কোরো না, কারণ আজকের পরিশ্রমই তোমার আগামীকালের পরিচয় হবে ⏳",
        "ব্যর্থতা মানে শেষ নয়, এটা নতুন করে আরও বুদ্ধিমানভাবে শুরু করার সুযোগ 💡",
        "যারা নীরবে কাজ করে যায়, তাদের সাফল্যই একদিন সবচেয়ে বেশি শব্দ করে 🎯",
        "ভয়কে জয় করতে পারলেই নতুন সম্ভাবনার দরজা খুলে যায় 🚪✨",
        "বড় কিছু অর্জন করতে হলে আরামদায়ক জায়গা ছেড়ে বেরোতেই হবে 💪",
        "ধৈর্য আর পরিশ্রম—এই দুটি থাকলে কোনো লক্ষ্যই অসম্ভব নয় 🌟",
        "নিজের স্বপ্নকে কখনো ছোট কোরো না, বরং নিজের দক্ষতাকে বড় করো 🚀",
        "প্রতিটি নতুন সকাল মানে নতুন আশা আর নতুন করে এগিয়ে চলার সুযোগ ☀️",
        "তুমি যদি নিজের জন্য না লড়ো, তবে অন্য কেউ তোমার হয়ে লড়বে না 🏆",
        "কাজ ছোট হলেও মন দিয়ে করলে সেখান থেকেই বড় সাফল্যের পথ তৈরি হয় ✨",
        "Dream big, work quietly, and let your results speak for you 🚀",
        "Small daily progress creates extraordinary long-term results ✨",
        "Discipline will take you to places motivation alone cannot reach 💪",
        "Every expert was once a beginner who refused to quit 🎯",
        "Stay patient and trust your grind; your time is coming 🌟",
        "Focus on growth, not noise, and watch your life transform 🔥",
        "Hard work beats talent when talent stops showing up 🏆",
        "Turn every setback into fuel for your strongest comeback 🚀",
        "Consistency is the secret superpower behind every great achievement 💡",
        "Believe in your vision even before anyone else can see it ✨",
        "Do not wait for the perfect moment—make this moment count ⏳",
        "Your future self will thank you for the effort you put in today 💪",
        "Great things never come from staying inside your comfort zone 🌟",
        "Keep moving forward; even slow progress is still real progress 🚀",
        "Build habits that make success inevitable, one day at a time 🏆"
    )

    // 🔥 অ্যাটিটিউড (32 confident, classy Bengali & English captions)
    private val attitudeLines: List<String> = listOf(
        "আমি কারও মতো হতে চাই না, নিজের মতো হয়েই সেরা থাকতে ভালোবাসি 🔥",
        "নীরবতা আমার দুর্বলতা নয়, ওটা আমার শান্ত আত্মবিশ্বাস 😎",
        "সবাইকে খুশি করার দায়িত্ব আমার নয়, নিজের সম্মান বজায় রাখাই আমার কাজ 👑",
        "যারা আমাকে চেনে তারা জানে, আমি কথায় নয় কাজে উত্তর দিই ⚡",
        "আমার স্টাইল আর মানসিকতা—দুটোই নিজের মতো, কারও কপি নয় 🔥",
        "সম্মান দিলে সম্মান পাবে, আর অবহেলা করলে খুব সহজেই ভুলে যাব 💯",
        "আমি প্রতিযোগিতায় বিশ্বাস করি না, নিজের মান নিজেই তৈরি করি 👑",
        "ভিড়ের মাঝে মিশে যাওয়ার জন্য নয়, আলাদা হয়ে জ্বলার জন্যই এসেছি ✨",
        "আমার শান্ত স্বভাব দেখে ভুল বুঝো না, প্রয়োজনে আমি একাই যথেষ্ট 🔥",
        "যারা পেছনে কথা বলে, তারা সবসময় পেছনেই থেকে যায় 😎",
        "আমি কম কথা বলি, কারণ নিজের কাজের মূল্য আমি ভালোই জানি ⚡",
        "জীবনটা নিজের শর্তেই সুন্দর, অন্যের মতামত নিয়ে নয় 💯",
        "সবার পছন্দ হতে হবে এমন কোনো কথা নেই, নিজের কাছে সৎ থাকাই যথেষ্ট 👑",
        "সময় যখন আমার হবে, তখন নীরবতাই সবচেয়ে বড় উত্তর দেবে 🔥",
        "আত্মসম্মান যেখানে নেই, সেই জায়গা থেকে হাসিমুখে সরে আসি 😎",
        "আমি কখনো হারি না—হয় জিতি, না হয় আরও শক্ত হয়ে শিখি ⚡",
        "নকল প্রশংসার চেয়ে সত্যিকারের সমালোচনা আমার বেশি পছন্দ 💯",
        "Classy mind, calm energy, and unstoppable confidence 👑",
        "I do not chase attention; my presence speaks on its own 🔥",
        "Silence is my favorite reply when words are not worth my time 😎",
        "Built with patience, powered by self-respect, focused on winning ⚡",
        "Stay humble in victory and unshakeable in every storm 💯",
        "My standards are high because I know the value I bring 👑",
        "No drama, no fake vibes—just pure focus and real goals 🔥",
        "Respect is earned with character, not demanded with noise 😎",
        "I compete only with who I was yesterday, and I always level up ⚡",
        "Calm outside, storm of ambition inside 🚀🔥",
        "Keep your circle small, your vision sharp, and your moves private 👑",
        "Confidence looks quiet when you truly know your worth 💯",
        "Do not mistake my kindness for weakness; I know my boundaries 😎",
        "Original by nature, rare by choice, focused by habit ✨🔥",
        "Walk alone if needed, but never lower your standards for company 👑"
    )

    // 💬 কুইক রিপ্লাই (32 natural everyday Bengali & English chat replies)
    private val quickReplyLines: List<String> = listOf(
        "হ্যালো বলো",
        "ভালো আছি, তুমি?",
        "বাড়িতে আছি",
        "এখনই আসছি",
        "ব্যস্ত আছি, পরে কথা বলছি",
        "ঠিক আছে",
        "হ্যাঁ, একদম ঠিক বলেছ",
        "একটু অপেক্ষা করো, আসছি",
        "পৌঁছে গেছি",
        "রাস্তায় আছি, ১০ মিনিট লাগবে",
        "খাওয়া হয়েছে তোমার?",
        "হ্যাঁ, মাত্র খেলাম",
        "অনেক ধন্যবাদ",
        "কোনো ব্যাপার না, সবসময় পাশে আছি",
        "দুঃখিত, একটু দেরি হয়ে গেল",
        "শুভ সকাল! দিনটা ভালো কাটুক ☀️",
        "শুভ রাত্রি, ভালো করে ঘুমাও 🌙",
        "পরে কল করছি তোমাকে",
        "এখন কল ধরতে পারছি না, মেসেজ করো",
        "কাজ শেষ করে জানাচ্ছি",
        "একদম দারুণ হয়েছে!",
        "খুব ভালো লাগল শুনে 😊",
        "হ্যাঁ, নিশ্চয়ই করব",
        "না, এখন আর লাগবে না",
        "কোথায় আছো এখন?",
        "কী করছো এখন?",
        "সব খবর ভালো তো?",
        "দেখা হবে খুব তাড়াতাড়ি ইনশাআল্লাহ",
        "সাবধানে যেও, পৌঁছে জানিও",
        "Sure, I will be right there!",
        "Sounds good, let's do it!",
        "I am a little busy right now, will text you soon."
    )

    fun linesForCategory(category: ShayariCategory): List<String> = when (category) {
        ShayariCategory.ROMANTIC -> romanticLines
        ShayariCategory.EMOTIONAL -> emotionalLines
        ShayariCategory.MOTIVATION -> motivationLines
        ShayariCategory.ATTITUDE -> attitudeLines
        ShayariCategory.QUICK_REPLY -> quickReplyLines
    }

    fun totalLineCount(): Int =
        romanticLines.size + emotionalLines.size + motivationLines.size + attitudeLines.size + quickReplyLines.size

    /**
     * Validates and sanitizes raw user topic input. Rejects blank text, symbol-only strings,
     * repeated-character spam, and random consonant gibberish; falls back to curated poetic themes
     * and weaves the clean keyword into 4 grammatically natural Bengali poetic templates.
     */
    fun generatePoeticShayaris(rawInput: String, seedOffset: Int = 0): GeneratedShayariResult {
        val cleaned = sanitizeTopicInput(rawInput)
        val isValid = isMeaningfulTopic(cleaned)
        val fallbackIndex = abs(rawInput.hashCode() + seedOffset) % fallbackPoeticThemes.size
        val resolvedTopic = if (isValid) {
            topicNormalizationMap[cleaned.lowercase()] ?: cleaned
        } else {
            fallbackPoeticThemes[fallbackIndex]
        }

        val templates = listOf(
            "$resolvedTopic নিয়ে যখনই ভাবি, মনের ভেতর একটা মিষ্টি শান্তি ছড়িয়ে পড়ে ✨",
            "কিছু অনুভূতি মুখে বলা যায় না, ঠিক যেমন $resolvedTopic ছুঁয়ে যায় নীরব হৃদয় ❤️",
            "সময়ের ভিড়ে অনেক কিছু বদলে গেলেও $resolvedTopic-এর মায়াটা আজও একই রকম সুন্দর 🌙",
            "একটুখানি $resolvedTopic আর প্রিয় মানুষের সঙ্গ—জীবনটা সুন্দর হতে আর কিছুই লাগে না 💫"
        )

        return GeneratedShayariResult(
            resolvedTopic = resolvedTopic,
            usedFallbackTheme = !isValid,
            lines = templates
        )
    }

    /**
     * Produces 4 clean, grammatically pure variation chips for any selected shayari or reply:
     * 1. সংক্ষিপ্ত রূপ (Short form, clipped at natural clause boundaries)
     * 2. গভীর রোমান্টিক (Deep poetic enhancement)
     * 3. ইমোজি স্টাইল (Aesthetic emoji framing)
     * 4. সরাসরি পাঠান (Exact curated line)
     */
    fun buildVariations(line: String, category: ShayariCategory): List<ShayariVariation> {
        val exact = line.trim()
        val stripped = stripTrailingEmojis(exact)
        val shortForm = buildCleanShortForm(stripped)
        val deepPoetic = buildDeepPoeticForm(stripped, category)
        val emojiStyled = buildEmojiStyledForm(stripped, category)

        return listOf(
            ShayariVariation("সংক্ষিপ্ত রূপ", shortForm),
            ShayariVariation("গভীর রোমান্টিক", deepPoetic),
            ShayariVariation("ইমোজি স্টাইল", emojiStyled),
            ShayariVariation("সরাসরি পাঠান", exact)
        )
    }

    private fun sanitizeTopicInput(raw: String): String {
        val lettersOnly = raw
            .trim()
            .replace(Regex("[^\\p{L}\\p{M}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (lettersOnly.isEmpty()) return ""
        // Keep at most 2 clean words so the keyword fits naturally inside Bengali templates
        return lettersOnly.split(' ').take(2).joinToString(" ").take(24).trim()
    }

    private fun isMeaningfulTopic(cleaned: String): Boolean {
        if (cleaned.length < 2) return false
        val compact = cleaned.replace(" ", "")
        if (compact.length < 2) return false

        // Reject repeated single-character spam like "aaaa" or "কককক"
        if (compact.toSet().size == 1) return false

        val hasBengaliOrHindi = compact.any { ch ->
            ch in '\u0980'..'\u09FF' || ch in '\u0900'..'\u097F'
        }
        if (hasBengaliOrHindi) {
            // Valid Bengali/Hindi word must contain at least 2 characters and not just dependent matras
            val hasBaseLetter = compact.any { ch ->
                (ch in '\u0985'..'\u09B9' || ch in '\u09DC'..'\u09DF' || ch in '\u0905'..'\u0939')
            }
            return hasBaseLetter
        }

        // For Latin/English input, reject keyboard-mash gibberish
        val lower = compact.lowercase()
        if (topicNormalizationMap.containsKey(lower)) return true
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y')
        if (lower.none { it in vowels }) return false
        val keyboardMashes = listOf("asdf", "qwer", "zxcv", "hjkl", "uiop", "jkl", "fgh")
        if (keyboardMashes.any { lower.contains(it) }) return false
        // Reject excessive consecutive consonants (5+)
        if (Regex("[bcdfghjklmnpqrstvwxz]{5,}").containsMatchIn(lower)) return false

        return true
    }

    private fun stripTrailingEmojis(text: String): String {
        var result = text.trim()
        // Remove leading/trailing non-letter, non-punctuation symbols (emojis / variation selectors)
        while (result.isNotEmpty()) {
            val lastCodePoint = result.codePointBefore(result.length)
            if (isEmojiOrSymbolCodePoint(lastCodePoint) || Character.isWhitespace(lastCodePoint)) {
                result = result.substring(0, result.length - Character.charCount(lastCodePoint))
            } else {
                break
            }
        }
        return result.trim()
    }

    private fun isEmojiOrSymbolCodePoint(cp: Int): Boolean {
        return cp in 0x1F000..0x1FAFF ||
            cp in 0x2600..0x27BF ||
            cp == 0xFE0F ||
            cp == 0x200D ||
            cp == 0x23F3
    }

    private fun buildCleanShortForm(stripped: String): String {
        // Split cleanly at natural clause separators (comma, em-dash, semicolon) if substantial
        val separators = listOf("—", ",", ";")
        for (sep in separators) {
            val idx = stripped.indexOf(sep)
            if (idx in 8..34) {
                return stripped.substring(0, idx).trim() + " ✨"
            }
        }
        val words = stripped.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size > 6) {
            words.take(5).joinToString(" ") + " ✨"
        } else {
            "$stripped ✨"
        }
    }

    private fun buildDeepPoeticForm(stripped: String, category: ShayariCategory): String {
        val isEnglishOnly = stripped.none { it in '\u0980'..'\u09FF' }
        if (isEnglishOnly) {
            return when (category) {
                ShayariCategory.MOTIVATION -> "From the heart: $stripped — keep shining every day ✨"
                ShayariCategory.ATTITUDE -> "$stripped — quiet confidence, timeless class 👑"
                ShayariCategory.QUICK_REPLY -> "$stripped — hope everything is going wonderfully 😊"
                else -> "Truly, $stripped — felt deeply in every moment 💫"
            }
        }
        return when (category) {
            ShayariCategory.ROMANTIC -> "সত্যি বলছি, $stripped — মন থেকে বললাম ❤️"
            ShayariCategory.EMOTIONAL -> "নীরবে অনুভব করি, $stripped 🌙💔"
            ShayariCategory.MOTIVATION -> "মনে রেখো, $stripped — সাফল্য তোমারই হবে 🚀"
            ShayariCategory.ATTITUDE -> "একটাই কথা, $stripped 👑🔥"
            ShayariCategory.QUICK_REPLY -> "$stripped, তুমি ভালো থেকো 😊❤️"
        }
    }

    private fun buildEmojiStyledForm(stripped: String, category: ShayariCategory): String {
        return when (category) {
            ShayariCategory.ROMANTIC -> "✨❤️ $stripped 💫🌹"
            ShayariCategory.EMOTIONAL -> "🌙💔 $stripped 🥀🌧️"
            ShayariCategory.MOTIVATION -> "🚀✨ $stripped 💪🏆"
            ShayariCategory.ATTITUDE -> "🔥👑 $stripped 😎⚡"
            ShayariCategory.QUICK_REPLY -> "💬😊 $stripped ✨"
        }
    }
}
