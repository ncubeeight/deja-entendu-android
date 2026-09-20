package com.ncubeeight.dejaentendu.studynotes

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A term/definition pair in the user's glossary — either typed in by hand
 * or bulk-imported from a dictionary file the user connected from Files
 * (see ConnectLocalDictionaryScreen). Specialized vocabulary an on-device
 * model wouldn't reliably know, or just an authoritative answer instead of
 * a generated one. Mirrors iOS's GlossaryEntry.swift. Looked up before/
 * alongside FlashcardGenerator's output on the flashcard screen.
 *
 * [source] defaults to MANUAL so glossary.json files saved before it
 * existed still decode fine — kotlinx.serialization fills in a missing
 * key from the property default automatically, no custom decoder needed.
 */
@Serializable
data class GlossaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val term: String,
    val definition: String,
    val language: SupportedLanguage,
    val addedAtEpochMillis: Long,
    val source: GlossarySource = GlossarySource.MANUAL,
    /**
     * Which ConnectedDictionary contributed this term; null for hand-typed
     * terms. Entries saved when only one dictionary could be connected have
     * no ID and are stamped with that one dictionary's (see
     * ConnectedDictionaryStore.load).
     */
    val dictionaryId: String? = null,
)

/**
 * Where a GlossaryEntry came from — lets a reconnect/disconnect of a local
 * dictionary replace just the terms it contributed without touching
 * anything the user typed in themselves.
 */
@Serializable
enum class GlossarySource { MANUAL, CONNECTED_DICTIONARY }
