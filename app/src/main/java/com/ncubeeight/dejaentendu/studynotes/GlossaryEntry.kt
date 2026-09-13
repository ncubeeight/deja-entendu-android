package com.ncubeeight.dejaentendu.studynotes

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A term/definition pair the user has entered themselves — for vocabulary
 * an on-device model wouldn't reliably know (specialized industry jargon,
 * a personal glossary) or simply to have an authoritative answer instead
 * of a generated one. Mirrors iOS's GlossaryEntry.swift. Looked up before/
 * alongside FlashcardGenerator's output on the flashcard screen.
 */
@Serializable
data class GlossaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val term: String,
    val definition: String,
    val language: SupportedLanguage,
    val addedAtEpochMillis: Long,
)
