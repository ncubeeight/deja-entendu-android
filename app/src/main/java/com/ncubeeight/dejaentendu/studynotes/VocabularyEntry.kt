package com.ncubeeight.dejaentendu.studynotes

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single vocabulary term, mirroring iOS's VocabularyEntry.swift. Added
 * via a tapped transcript word, manual entry, or (later) a share-in.
 */
@Serializable
data class VocabularyEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val addedAtEpochMillis: Long,
    // Known when the term came from a tapped transcript word (the
    // recording it came from is already tagged with a language); null for
    // manual entry, which asks the flashcard generator to infer it instead.
    val language: SupportedLanguage? = null,
    // Generated on-device the first time the flashcard is opened, then
    // cached here so it isn't regenerated on every visit.
    val pronunciation: String? = null,
    val translation: String? = null,
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null,
) {
    val hasFlashcardDetails: Boolean
        get() = pronunciation != null && exampleSentence != null
}
