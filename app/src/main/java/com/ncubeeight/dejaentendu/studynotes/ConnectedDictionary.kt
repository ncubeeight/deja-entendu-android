package com.ncubeeight.dejaentendu.studynotes

import android.content.Context
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Metadata for the user's own dictionary file, connected from a file
 * picker rather than typed in by hand or downloaded from a language pack.
 * Only one can be connected at a time — connecting a new file replaces
 * it, and its terms replace the previous connection's terms in the
 * glossary (hand-typed GlossaryEntry rows are untouched, tracked via
 * GlossarySource). Mirrors iOS's ConnectedDictionary.swift, minus the
 * bookmarkData/security-scoped-resource bookkeeping iOS needs to keep a
 * reference to the original file alive — Android doesn't need continued
 * access to it, since the parsed terms are already copied into
 * GlossaryStore at connect time.
 */
@Serializable
data class ConnectedDictionary(
    val fileName: String,
    val language: SupportedLanguage,
    val connectedAtEpochMillis: Long,
    val termCount: Int,
)

object ConnectedDictionaryStore {
    private const val FILE_NAME = "connected-dictionary.json"
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun load(context: Context): ConnectedDictionary? {
        val file = file(context)
        if (!file.exists()) return null
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun save(context: Context, dictionary: ConnectedDictionary) {
        file(context).writeText(json.encodeToString(dictionary))
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
