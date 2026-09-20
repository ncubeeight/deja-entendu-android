package com.ncubeeight.dejaentendu.studynotes

import android.content.Context
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Metadata for one of the user's own dictionary files, connected from a file
 * picker rather than typed in by hand. Any number can be connected — each
 * contributes its terms to the glossary tagged with its [id]
 * (GlossaryEntry.dictionaryId), so disconnecting one removes only its own
 * terms. Hand-typed GlossaryEntry rows are never touched. Mirrors iOS's
 * ConnectedDictionary.swift, minus the bookmark bookkeeping iOS needs to
 * keep the original file reachable — the parsed terms are already copied
 * into GlossaryStore at connect time.
 */
@Serializable
data class ConnectedDictionary(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val language: SupportedLanguage,
    val connectedAtEpochMillis: Long,
    val termCount: Int,
)

object ConnectedDictionaryStore {
    private const val FILE_NAME = "connected-dictionaries.json"
    private const val LEGACY_FILE_NAME = "connected-dictionary.json"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Most recently connected first. On first launch after upgrading from the
     * one-dictionary version, migrates that file into the new list and stamps
     * its terms in the glossary with its ID.
     */
    fun load(context: Context): List<ConnectedDictionary> {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            return try {
                json.decodeFromString(ListSerializer(ConnectedDictionary.serializer()), file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        }
        return migrateLegacy(context)
    }

    private fun save(context: Context, dictionaries: List<ConnectedDictionary>) {
        File(context.filesDir, FILE_NAME).writeText(json.encodeToString(ListSerializer(ConnectedDictionary.serializer()), dictionaries))
    }

    fun add(context: Context, dictionary: ConnectedDictionary) = save(context, listOf(dictionary) + load(context))

    fun remove(context: Context, id: String) = save(context, load(context).filterNot { it.id == id })

    private fun migrateLegacy(context: Context): List<ConnectedDictionary> {
        val legacyFile = File(context.filesDir, LEGACY_FILE_NAME)
        if (!legacyFile.exists()) return emptyList()
        val legacy = try {
            json.decodeFromString<ConnectedDictionary>(legacyFile.readText())
        } catch (e: Exception) {
            return emptyList()
        }
        val entries = GlossaryStore.load(context).map {
            if (it.source == GlossarySource.CONNECTED_DICTIONARY && it.dictionaryId == null) it.copy(dictionaryId = legacy.id) else it
        }
        GlossaryStore.save(context, entries)
        save(context, listOf(legacy))
        legacyFile.delete()
        return listOf(legacy)
    }
}
