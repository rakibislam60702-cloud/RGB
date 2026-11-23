package com.example.engine

/**
 * Clean Unicode Stylish Font transformer (`Aa`) for English characters.
 * Bengali, Hindi, numbers, punctuation, and emojis pass through untouched so native scripts stay pure.
 */
enum class StylishFontStyle(
    val id: String,
    val label: String,
    val preview: String
) {
    NORMAL("normal", "Normal", "Aa"),
    BOLD("bold", "𝐁𝐨𝐥𝐝", "𝐀𝐚"),
    ITALIC("italic", "𝐼𝑡𝑎𝑙𝑖𝑐", "𝐴𝑎"),
    BOLD_ITALIC("bold_italic", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄", "𝑨𝒂"),
    SCRIPT("script", "𝓢𝓬𝓻𝓲𝓹𝓽", "𝓐𝓪"),
    DOUBLE_STRUCK("double_struck", "𝔻𝕠𝕦𝕓𝕝𝕖", "𝔸𝕒"),
    MONOSPACE("monospace", "𝙼𝚘𝚗𝚘", "𝙰𝚊"),
    CIRCLED("circled", "Ⓒⓘⓡⓒⓛⓔ", "Ⓐⓐ")
}

object StylishFontEngine {

    private val scriptUpper = arrayOf(
        "𝓐", "𝓑", "𝓒", "𝓓", "𝓔", "𝓕", "𝓖", "𝓗", "𝓘", "𝓙", "𝓚", "𝓛", "𝓜",
        "𝓝", "𝓞", "𝓟", "𝓠", "𝓡", "𝓢", "𝓣", "𝓤", "𝓥", "𝓦", "𝓧", "𝓨", "𝓩"
    )
    private val scriptLower = arrayOf(
        "𝓪", "𝓫", "𝓬", "𝓭", "𝓮", "𝓯", "𝓰", "𝓱", "𝓲", "𝓳", "𝓴", "𝓵", "𝓶",
        "𝓷", "𝓸", "𝓹", "𝓺", "𝓻", "𝓼", "𝓽", "𝓾", "𝓿", "𝔀", "𝔁", "𝔂", "𝔃"
    )
    private val doubleUpper = arrayOf(
        "𝔸", "𝔹", "ℂ", "𝔻", "𝔼", "𝔽", "𝔾", "ℍ", "𝕀", "𝕁", "𝕂", "𝕃", "𝕄",
        "ℕ", "𝕆", "ℙ", "ℚ", "ℝ", "𝕊", "𝕋", "𝕌", "𝕍", "𝕎", "𝕏", "𝕐", "ℤ"
    )
    private val doubleLower = arrayOf(
        "𝕒", "𝕓", "𝕔", "𝕕", "𝕖", "𝕗", "𝕘", "𝕙", "𝕚", "𝕛", "𝕜", "𝕝", "𝕞",
        "𝕟", "𝕠", "𝕡", "𝕢", "𝕣", "𝕤", "𝕥", "𝕦", "𝕧", "𝕨", "𝕩", "𝕪", "𝕫"
    )

    fun transform(input: String, style: StylishFontStyle): String {
        if (style == StylishFontStyle.NORMAL || input.isEmpty()) return input
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            sb.append(transformChar(ch, style))
        }
        return sb.toString()
    }

    private fun transformChar(ch: Char, style: StylishFontStyle): String {
        val isUpper = ch in 'A'..'Z'
        val isLower = ch in 'a'..'z'
        if (!isUpper && !isLower) return ch.toString()
        val idx = if (isUpper) ch - 'A' else ch - 'a'

        return when (style) {
            StylishFontStyle.NORMAL -> ch.toString()
            StylishFontStyle.BOLD -> {
                val base = if (isUpper) 0x1D400 else 0x1D41A
                String(Character.toChars(base + idx))
            }
            StylishFontStyle.ITALIC -> {
                if (!isUpper && ch == 'h') return "ℎ"
                val base = if (isUpper) 0x1D434 else 0x1D44E
                String(Character.toChars(base + idx))
            }
            StylishFontStyle.BOLD_ITALIC -> {
                val base = if (isUpper) 0x1D468 else 0x1D482
                String(Character.toChars(base + idx))
            }
            StylishFontStyle.SCRIPT -> if (isUpper) scriptUpper[idx] else scriptLower[idx]
            StylishFontStyle.DOUBLE_STRUCK -> if (isUpper) doubleUpper[idx] else doubleLower[idx]
            StylishFontStyle.MONOSPACE -> {
                val base = if (isUpper) 0x1D670 else 0x1D68A
                String(Character.toChars(base + idx))
            }
            StylishFontStyle.CIRCLED -> {
                val base = if (isUpper) 0x24B6 else 0x24D0
                String(Character.toChars(base + idx))
            }
        }
    }
}
