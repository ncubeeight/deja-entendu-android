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
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val word = text.substring(start, end)
            if (word.any { it.isLetterOrDigit() }) result.add(word)
            start = end
            end = iterator.next()
        }
        return result
    }
}
