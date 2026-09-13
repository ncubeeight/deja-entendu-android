package com.ncubeeight.dejaentendu.studynotes

import android.content.Context
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the user's custom glossary the same way VocabularyStore
 * persists vocabulary — a JSON file in the app's own storage. Mirrors
 * iOS's GlossaryStore.swift.
 */
object GlossaryStore {
    private const val FILE_NAME = "glossary.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<GlossaryEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, entries: List<GlossaryEntry>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.encodeToString(entries))
    }

    /**
     * Case-insensitive exact match on term, preferring an entry tagged with
     * [language] when there's ambiguity (the same spelled word could exist
     * in more than one language's glossary). Falls back to any match if
     * the term's language isn't known.
     */
    fun definition(context: Context, term: String, language: SupportedLanguage?): String? {
        val matches = load(context).filter { it.term.equals(term, ignoreCase = true) }
        if (language != null) {
            matches.firstOrNull { it.language == language }?.let { return it.definition }
        }
        return matches.firstOrNull()?.definition
    }
}
