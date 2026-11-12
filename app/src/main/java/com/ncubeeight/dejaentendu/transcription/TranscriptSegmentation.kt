package com.ncubeeight.dejaentendu.transcription

import java.text.BreakIterator
import java.util.Locale

/**
 * Splits transcript text into sentences and words, mirroring iOS's
 * TextSegmentation.swift (which uses Apple's NLTokenizer). Android's
 * java.text.BreakIterator is ICU-backed at the platform level — no extra
 * dependency needed — and handles CJK correctly despite no whitespace,
 * same reasoning as the iOS side.
 */
object TranscriptSegmentation {

    fun sentences(text: String, locale: Locale): List<String> {
        val iterator = BreakIterator.getSentenceInstance(locale)
        iterator.setText(text)
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val sentence = text.substring(start, end).trim()
            if (sentence.isNotEmpty()) result.add(sentence)
            start = end
            end = iterator.next()
        }
        return result.ifEmpty { listOf(text) }
    }

    /** Word tokens only — punctuation-only tokens are dropped. */
    fun words(text: String, locale: Locale): List<String> {
        val iterator = BreakIterator.getWordInstance(locale)
        iterator.setText(text)
        val ranges = mutableListOf<IntRange>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            if (text.substring(start, end).any { it.isLetterOrDigit() }) ranges.add(start until end)
            start = end
            end = iterator.next()
        }
        return rejoinHebrewWords(ranges, text).map { text.substring(it.first, it.last + 1) }
    }

    // Hebrew

    /**
     * Word breaking can split Hebrew words at marks that belong inside them:
     * gershayim in acronyms (צה״ל → צה + ל), geresh in abbreviations and
     * loan sounds (צ׳יפס → צ + יפס, ח׳ → ח) and maqaf in compounds
     * (תל־אביב → תל + אביב). News text is full of these, often typed with
     * plain " ' - instead, and a split word loses its ending (and its sound)
     * on the page and on its flashcard. This glues the pieces back so each
     * word stays exactly as written. Mirrors iOS e17b62a; a no-op where ICU
     * already kept the word whole. Other languages are unaffected.
     */
    private val hebrewJoiners = setOf(
        '\u05F4', '"', '\u201D', // gershayim ״, " and ”
        '\u05F3', '\'', '\u2019', // geresh ׳, ' and ’
        '\u05BE', '-', '\u2010', '\u2011', // maqaf ־ and hyphens
    )
    private val hebrewGeresh = setOf('\u05F3', '\'', '\u2019')

    private fun isHebrewLetter(c: Char) = c in '\u05D0'..'\u05EA'

    /** Last base letter of a token, skipping vowel points and other marks. */
    private fun lastLetter(token: CharSequence): Char? = token.lastOrNull { it.isLetter() }

    internal fun rejoinHebrewWords(ranges: List<IntRange>, text: String): List<IntRange> {
        val result = mutableListOf<IntRange>()
        for (range in ranges) {
            val previous = result.lastOrNull()
            if (previous == null) {
                result.add(range)
                continue
            }
            val gap = text.substring(previous.last + 1, range.first)
            val before = lastLetter(text.subSequence(previous.first, previous.last + 1))
            if (gap.isNotEmpty() && gap.length <= 2 && gap.all { it in hebrewJoiners } &&
                before != null && isHebrewLetter(before) && isHebrewLetter(text[range.first])
            ) {
                result[result.lastIndex] = previous.first..range.last
            } else {
                result.add(range)
            }
        }
        // A geresh right after a Hebrew word (ח׳, ג׳) is part of it.
        return result.map { range ->
            val next = range.last + 1
            val last = lastLetter(text.subSequence(range.first, next))
            if (next < text.length && text[next] in hebrewGeresh && last != null && isHebrewLetter(last)) {
                range.first..next
            } else {
                range
            }
        }
    }

    /**
     * Whether a sentence should be laid out right to left: from its own
     * strong-direction characters when they lean clearly one way (so an
     * English line inside an Arabic Sample stays left to right), otherwise
     * from the Sample's language. Mirrors iOS e3c531c, which uses the
     * sentence's detected language the same way.
     */
    fun isRightToLeft(sentence: String, language: SupportedLanguage): Boolean {
        var rtl = 0
        var ltr = 0
        for (c in sentence) {
            when (Character.getDirectionality(c)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> rtl++
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> ltr++
            }
        }
        if (rtl != ltr) return rtl > ltr
        return language.isRightToLeft
    }
}
