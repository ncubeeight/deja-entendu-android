package com.ncubeeight.dejaentendu.studynotes

import android.content.Context
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
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

    /** Just what a lookup needs — no ids or dates — so a connected
     *  dictionary with 100k+ terms stays small in memory. */
    private class IndexedDefinition(
        val definition: String,
        val language: SupportedLanguage,
        val isManual: Boolean,
    )

    /**
     * Lowercased term → matches in file order. Built lazily on the first
     * lookup and dropped on every [save], so it can't go stale. Before this,
     * every lookup decoded all of glossary.json (mirrors the iOS fix,
     * measured there at ~0.3s and +110 MB peak for a 120k-entry dictionary).
     * [generation] bumps on every save so an index built from data a save
     * has since replaced is never stored.
     */
    private val lock = Any()
    private var generation = 0
    private var index: Map<String, List<IndexedDefinition>>? = null

    /** Same folding equals(ignoreCase = true) matched before. */
    private fun indexKey(term: String): String = term.lowercase(java.util.Locale.ROOT)

    private fun buildIndex(context: Context): Map<String, List<IndexedDefinition>> {
        val built = HashMap<String, MutableList<IndexedDefinition>>()
        for (entry in load(context)) {
            built.getOrPut(indexKey(entry.term)) { ArrayList(1) }.add(
                IndexedDefinition(entry.definition, entry.language, entry.source == GlossarySource.MANUAL)
            )
        }
        return built
    }

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
        synchronized(lock) {
            generation++
            index = null
        }
    }

    /**
     * Case-insensitive exact match on term, preferring an entry tagged with
     * [language] when there's ambiguity (the same spelled word could exist
     * in more than one language's glossary). Falls back to any match if the
     * term's language isn't known. Terms from a connected dictionary only
     * count while that dictionary's language is toggled on in Settings'
     * "Languages shown on import" — that toggle is how a connected
     * dictionary is turned off and on without disconnecting it. Hand-typed
     * terms always apply.
     *
     * The first call after launch or a save builds the index (one full read
     * of glossary.json), so call this off the main thread.
     */
    fun definition(context: Context, term: String, language: SupportedLanguage?): String? {
        val (cached, builtFrom) = synchronized(lock) { index to generation }
        val candidates = if (cached != null) {
            cached[indexKey(term)].orEmpty()
        } else {
            // Built outside the lock so a slow first build doesn't block
            // other callers.
            val built = buildIndex(context)
            synchronized(lock) { if (generation == builtFrom) index = built }
            built[indexKey(term)].orEmpty()
        }
        val enabled = AppSettingsStore.enabledLanguages(context)
        val matches = candidates.filter { it.isManual || it.language in enabled }
        if (language != null) {
            matches.firstOrNull { it.language == language }?.let { return it.definition }
        }
        return matches.firstOrNull()?.definition
    }
}
