package com.example.keyboard

import androidx.compose.runtime.Immutable
import com.example.model.KeyboardLanguage

enum class KeyboardPage {
    LETTERS,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

enum class ImeActionType(val label: String) {
    ENTER("↵"),
    DONE("Done"),
    SEARCH("Search"),
    SEND("Send"),
    GO("Go"),
    NEXT("Next")
}

sealed interface KeyAction {
    data class Character(val text: String) : KeyAction
    data object Shift : KeyAction
    data object Backspace : KeyAction
    data object Space : KeyAction
    data object Enter : KeyAction
    data class SwitchPage(val page: KeyboardPage) : KeyAction
    data object ToggleEmoji : KeyAction
    data object ToggleLanguage : KeyAction
    data object SwitchIme : KeyAction
}

@Immutable
data class KeySpec(
    val id: String,
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val secondaryHint: String? = null,
    val isBengaliGlyph: Boolean = false
)

object KeyboardLayoutData {

    private val englishNumberRow: List<KeySpec> =
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { d ->
            KeySpec(id = "num_$d", label = d, action = KeyAction.Character(d))
        }

    private val bengaliNumberRow: List<KeySpec> =
        listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০").mapIndexed { idx, d ->
            KeySpec(
                id = "bn_num_$idx",
                label = d,
                action = KeyAction.Character(d),
                isBengaliGlyph = true
            )
        }

    private val letterRow1 = listOf(
        "q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5",
        "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0"
    )
    private val letterRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    private val letterRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    // Bengali unshifted primary keys matching RgbKeyboardView bnRows
    private val bnRow1Normal = listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়")
    private val bnRow2Normal = listOf("া", "ি", "ু", "ে", "ো", "ক", "ত", "দ", "ম", "ন")
    private val bnRow3Normal = listOf("ব", "স", "ল", "র", "শ", "ভ", "ধ")

    // Bengali shifted keys (independent vowels, aspirated consonants, and Hasant '্')
    private val bnRow1Shift = listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "এ", "ঐ", "ও", "ঔ")
    private val bnRow2Shift = listOf("ী", "ূ", "ৈ", "ৌ", "ৃ", "খ", "থ", "ছ", "ঝ", "্")
    private val bnRow3Shift = listOf("ফ", "ষ", "ঘ", "ঠ", "ঢ", "ণ", "ঁ")

    // Bengali independent vowels + Taka + Dari on Symbols 1
    private val bnVowelsRow = listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও")
    private val bnSymbolsRow3 = listOf("ঔ", "৳", "।", "\"", "'", "!", "?")

    fun buildRows(
        language: KeyboardLanguage,
        page: KeyboardPage,
        shiftState: ShiftState,
        showNumberRow: Boolean,
        imeAction: ImeActionType
    ): List<List<KeySpec>> {
        return when (language) {
            KeyboardLanguage.ENGLISH -> buildEnglishRows(page, shiftState, showNumberRow, imeAction)
            KeyboardLanguage.BENGALI -> buildBengaliRows(page, shiftState, showNumberRow, imeAction)
        }
    }

    private fun buildEnglishRows(
        page: KeyboardPage,
        shiftState: ShiftState,
        showNumberRow: Boolean,
        imeAction: ImeActionType
    ): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (showNumberRow && page == KeyboardPage.LETTERS) {
            rows.add(englishNumberRow)
        }

        when (page) {
            KeyboardPage.LETTERS -> {
                val upper = shiftState != ShiftState.OFF
                rows.add(
                    letterRow1.map { (ch, hint) ->
                        val disp = if (upper) ch.uppercase() else ch
                        KeySpec(
                            id = "l1_$ch",
                            label = disp,
                            action = KeyAction.Character(disp),
                            secondaryHint = if (!showNumberRow) hint else null
                        )
                    }
                )
                rows.add(
                    letterRow2.map { ch ->
                        val disp = if (upper) ch.uppercase() else ch
                        KeySpec(id = "l2_$ch", label = disp, action = KeyAction.Character(disp))
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_shift",
                                label = if (shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧",
                                action = KeyAction.Shift,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                        letterRow3.forEach { ch ->
                            val disp = if (upper) ch.uppercase() else ch
                            add(KeySpec(id = "l3_$ch", label = disp, action = KeyAction.Character(disp)))
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.ENGLISH,
                        modeSwitchLabel = "?123",
                        modeSwitchTarget = KeyboardPage.SYMBOLS_1,
                        punctuationRight = ".",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.SYMBOLS_1 -> {
                rows.add(
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { s ->
                        KeySpec(id = "s1_$s", label = s, action = KeyAction.Character(s))
                    }
                )
                rows.add(
                    listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { s ->
                        KeySpec(id = "s2_$s", label = s, action = KeyAction.Character(s))
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_sym2",
                                label = "=\\<",
                                action = KeyAction.SwitchPage(KeyboardPage.SYMBOLS_2),
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                        listOf("*", "\"", "'", ":", ";", "!", "?").forEach { s ->
                            add(KeySpec(id = "s3_$s", label = s, action = KeyAction.Character(s)))
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.ENGLISH,
                        modeSwitchLabel = "ABC",
                        modeSwitchTarget = KeyboardPage.LETTERS,
                        punctuationRight = ".",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.SYMBOLS_2 -> {
                rows.add(
                    listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ").map { s ->
                        KeySpec(id = "s21_$s", label = s, action = KeyAction.Character(s))
                    }
                )
                rows.add(
                    listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").map { s ->
                        KeySpec(id = "s22_$s", label = s, action = KeyAction.Character(s))
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_sym1",
                                label = "?123",
                                action = KeyAction.SwitchPage(KeyboardPage.SYMBOLS_1),
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                        listOf("%", "©", "®", "™", "✓", "[", "]").forEach { s ->
                            add(KeySpec(id = "s23_$s", label = s, action = KeyAction.Character(s)))
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.ENGLISH,
                        modeSwitchLabel = "ABC",
                        modeSwitchTarget = KeyboardPage.LETTERS,
                        punctuationRight = ".",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.EMOJI -> Unit
        }
        return rows
    }

    private fun buildBengaliRows(
        page: KeyboardPage,
        shiftState: ShiftState,
        showNumberRow: Boolean,
        imeAction: ImeActionType
    ): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (showNumberRow && page == KeyboardPage.LETTERS) {
            rows.add(bengaliNumberRow)
        }

        when (page) {
            KeyboardPage.LETTERS -> {
                val shifted = shiftState != ShiftState.OFF
                val r1 = if (shifted) bnRow1Shift else bnRow1Normal
                val r2 = if (shifted) bnRow2Shift else bnRow2Normal
                val r3 = if (shifted) bnRow3Shift else bnRow3Normal

                rows.add(
                    r1.mapIndexed { i, ch ->
                        KeySpec(
                            id = "bn_r1_$i",
                            label = ch,
                            action = KeyAction.Character(ch),
                            isBengaliGlyph = true
                        )
                    }
                )
                rows.add(
                    r2.mapIndexed { i, ch ->
                        KeySpec(
                            id = "bn_r2_$i",
                            label = ch,
                            action = KeyAction.Character(ch),
                            isBengaliGlyph = true
                        )
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_shift",
                                label = if (shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧",
                                action = KeyAction.Shift,
                                weight = 1.25f,
                                isModifier = true
                            )
                        )
                        r3.forEachIndexed { i, ch ->
                            add(
                                KeySpec(
                                    id = "bn_r3_$i",
                                    label = ch,
                                    action = KeyAction.Character(ch),
                                    isBengaliGlyph = true
                                )
                            )
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.25f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.BENGALI,
                        modeSwitchLabel = "?১২৩",
                        modeSwitchTarget = KeyboardPage.SYMBOLS_1,
                        punctuationRight = "।",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.SYMBOLS_1 -> {
                rows.add(bengaliNumberRow)
                rows.add(
                    bnVowelsRow.mapIndexed { i, ch ->
                        KeySpec(
                            id = "bn_vow_$i",
                            label = ch,
                            action = KeyAction.Character(ch),
                            isBengaliGlyph = true
                        )
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_sym2",
                                label = "=\\<",
                                action = KeyAction.SwitchPage(KeyboardPage.SYMBOLS_2),
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                        bnSymbolsRow3.forEachIndexed { i, s ->
                            add(
                                KeySpec(
                                    id = "bn_s3_$i",
                                    label = s,
                                    action = KeyAction.Character(s),
                                    isBengaliGlyph = true
                                )
                            )
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.BENGALI,
                        modeSwitchLabel = "কখগ",
                        modeSwitchTarget = KeyboardPage.LETTERS,
                        punctuationRight = ".",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.SYMBOLS_2 -> {
                rows.add(
                    listOf("@", "#", "৳", "_", "&", "-", "+", "(", ")", "/").mapIndexed { i, s ->
                        KeySpec(
                            id = "bn_s21_$i",
                            label = s,
                            action = KeyAction.Character(s),
                            isBengaliGlyph = s == "৳"
                        )
                    }
                )
                rows.add(
                    listOf("~", "`", "|", "•", "=", "{", "}", "[", "]", "\\").mapIndexed { i, s ->
                        KeySpec(id = "bn_s22_$i", label = s, action = KeyAction.Character(s))
                    }
                )
                rows.add(
                    buildList {
                        add(
                            KeySpec(
                                id = "key_sym1",
                                label = "?১২৩",
                                action = KeyAction.SwitchPage(KeyboardPage.SYMBOLS_1),
                                weight = 1.48f,
                                isModifier = true,
                                isBengaliGlyph = true
                            )
                        )
                        listOf("%", ":", ";", "*", "$", "€", "£").forEachIndexed { i, s ->
                            add(KeySpec(id = "bn_s23_$i", label = s, action = KeyAction.Character(s)))
                        }
                        add(
                            KeySpec(
                                id = "key_backspace",
                                label = "⌫",
                                action = KeyAction.Backspace,
                                weight = 1.48f,
                                isModifier = true
                            )
                        )
                    }
                )
                rows.add(
                    buildBottomRow(
                        language = KeyboardLanguage.BENGALI,
                        modeSwitchLabel = "কখগ",
                        modeSwitchTarget = KeyboardPage.LETTERS,
                        punctuationRight = "।",
                        imeAction = imeAction
                    )
                )
            }

            KeyboardPage.EMOJI -> Unit
        }
        return rows
    }

    private fun buildBottomRow(
        language: KeyboardLanguage,
        modeSwitchLabel: String,
        modeSwitchTarget: KeyboardPage,
        punctuationRight: String,
        imeAction: ImeActionType
    ): List<KeySpec> = listOf(
        KeySpec(
            id = "key_mode",
            label = modeSwitchLabel,
            action = KeyAction.SwitchPage(modeSwitchTarget),
            weight = 1.4f,
            isModifier = true,
            isBengaliGlyph = language == KeyboardLanguage.BENGALI
        ),
        KeySpec(
            id = "key_comma",
            label = ",",
            action = KeyAction.Character(","),
            weight = 0.8f,
            isModifier = true
        ),
        KeySpec(
            id = "key_lang",
            label = language.toggle().shortBadge,
            action = KeyAction.ToggleLanguage,
            weight = 1.05f,
            isModifier = true,
            isBengaliGlyph = language == KeyboardLanguage.ENGLISH
        ),
        KeySpec(
            id = "key_space",
            label = language.spacebarLabel,
            action = KeyAction.Space,
            weight = 4.05f,
            isModifier = false,
            isBengaliGlyph = language == KeyboardLanguage.BENGALI
        ),
        KeySpec(
            id = "key_period",
            label = punctuationRight,
            action = KeyAction.Character(punctuationRight),
            weight = 0.85f,
            isModifier = true,
            isBengaliGlyph = punctuationRight == "।"
        ),
        KeySpec(
            id = "key_enter",
            label = imeAction.label,
            action = KeyAction.Enter,
            weight = 1.65f,
            isModifier = true
        )
    )

    val curatedEmojis: List<String> = listOf(
        "🔥", "⚡", "🌈", "🎮", "💻", "⌨️", "🚀", "✨",
        "😎", "😂", "❤️", "💜", "🩵", "🙌", "👏", "💯",
        "👾", "🤖", "🎧", "🎵", "🌟", "💥", "💎", "🏆",
        "👍", "👌", "✌️", "🤙", "😁", "🥳", "🤩", "🙏",
        "🌙", "☀️", "🌊", "🍀", "🎯", "⚙️", "🛡️", "🎉"
    )
}
