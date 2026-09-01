package com.ncubeeight.dejaentendu.studynotes

import com.atilika.kuromoji.ipadic.Tokenizer

/**
 * A real, dictionary-backed Japanese reading for kanji — the Android analog
 * of iOS's JapaneseReading (FlashcardGenerator.swift), which uses Apple's
 * CFStringTokenizer Latin-transcription attribute. Android has no OS-level
 * equivalent, so this uses Kuromoji (a pure-JVM Japanese morphological
 * analyzer + IPADIC dictionary, no native code) to get each word's real
 * katakana pronunciation, then converts that to Hepburn romaji by hand.
 *
 * This is real dictionary data, not a language-model guess — Gemini Nano's
 * own pronunciation guesses for kanji were inconsistent and sometimes
 * Chinese-pinyin-flavored (課題, correctly "kadai", came back romanized
 * with 'zh'/'q'/'x'-style consonants), the same failure mode iOS's commit
 * history describes hitting with FoundationModels.
 */
object JapaneseReading {
    private val tokenizer by lazy { Tokenizer() }

    /** Hepburn romaji for [text], hyphenated per mora, or null if nothing could be read. */
    fun romaji(text: String): String? {
        val tokens = tokenizer.tokenize(text)
        val syllables = tokens.flatMap { token ->
            val katakana = token.pronunciation.takeIf { it != "*" } ?: token.reading.takeIf { it != "*" } ?: return@flatMap emptyList()
            katakanaToRomajiSyllables(katakana)
        }
        return syllables.joinToString("-").ifEmpty { null }
    }

    /** Splits a katakana reading into romaji morae, handling sokuon (ッ) and chōonpu (ー). */
    private fun katakanaToRomajiSyllables(katakana: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        var pendingGemination = false

        while (i < katakana.length) {
            val ch = katakana[i]

            if (ch == 'ッ') {
                pendingGemination = true
                i++
                continue
            }
            if (ch == 'ー') {
                // Chōonpu — repeat the last mora's vowel.
                val lastVowel = result.lastOrNull()?.lastOrNull { it in "aiueo" }
                if (lastVowel != null) result.add(lastVowel.toString())
                i++
                continue
            }

            // Try a 2-character combo (consonant + small y/vowel) before a single character.
            val twoChar = if (i + 1 < katakana.length) katakana.substring(i, i + 2) else null
            val (romaji, consumed) = when {
                twoChar != null && KANA_COMBOS.containsKey(twoChar) -> KANA_COMBOS.getValue(twoChar) to 2
                KANA_SINGLE.containsKey(ch) -> KANA_SINGLE.getValue(ch) to 1
                else -> null to 1 // Unknown character (rare/loanword edge case) — skip it.
            }

            if (romaji != null) {
                val syllable = if (pendingGemination) {
                    // Sokuon doubles the following consonant — "tch" for a
                    // following "ch" is the standard Hepburn exception.
                    val firstConsonant = romaji.firstOrNull { it !in "aiueo" }
                    if (firstConsonant != null) {
                        (if (romaji.startsWith("ch")) "t" else firstConsonant.toString()) + romaji
                    } else {
                        romaji
                    }
                } else {
                    romaji
                }
                result.add(syllable)
                pendingGemination = false
            }
            i += consumed
        }

        return result
    }

    private val KANA_SINGLE: Map<Char, String> = mapOf(
        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu", 'ゲ' to "ge", 'ゴ' to "go",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ダ' to "da", 'ヂ' to "ji", 'ヅ' to "zu", 'デ' to "de", 'ド' to "do",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo",
        'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヲ' to "o", 'ン' to "n",
        'ヴ' to "vu",
    )

    private val KANA_COMBOS: Map<String, String> = mapOf(
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho", "シェ" to "she",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo", "ジェ" to "je",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho", "チェ" to "che",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ビャ" to "bya", "ビュ" to "byu", "ビョ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ティ" to "ti", "ディ" to "di", "デュ" to "dyu",
        "ファ" to "fa", "フィ" to "fi", "フェ" to "fe", "フォ" to "fo",
        "ウィ" to "wi", "ウェ" to "we", "ウォ" to "wo",
        "ツァ" to "tsa", "ツェ" to "tse", "ツォ" to "tso",
    )
}
