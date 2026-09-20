package com.ncubeeight.dejaentendu.transcription

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Languages the user added by connecting a dictionary file in a language the
 * app doesn't list (ConnectLocalDictionaryScreen's "Other language…") —
 * Navajo, say. They behave like built-in languages everywhere one is picked
 * or toggled, except that speech, OCR, and TTS support is whatever the OS
 * offers for an unrecognized locale — i.e. none. Mirrors iOS's
 * CustomLanguageStore.
 *
 * SupportedLanguage.entries is read from non-UI code with no Context to hand,
 * so this holds the application context, set once at launch by [init].
 */
object CustomLanguageStore {
    const val PREFIX = "custom:"
    private const val PREFS_NAME = "custom_languages"
    private const val KEY = "records"

    @Serializable
    private data class Record(val key: String, val name: String)

    private val json = Json { ignoreUnknownKeys = true }
    private var appContext: Context? = null
    private var cache: List<Record>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun records(): List<Record> {
        cache?.let { return it }
        val context = appContext ?: return emptyList()
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
        val parsed = if (raw == null) emptyList() else try {
            json.decodeFromString(ListSerializer(Record.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
        cache = parsed
        return parsed
    }

    fun load(): List<SupportedLanguage> = records().map { SupportedLanguage.custom(it.key, it.name) }

    fun displayNameForKey(key: String): String =
        records().firstOrNull { it.key == key }?.name ?: key.removePrefix(PREFIX).replaceFirstChar { it.uppercase() }

    /**
     * Returns the language for [name], reusing a built-in or an already
     * registered custom language with the same (case-insensitive) name rather
     * than creating a duplicate.
     */
    fun register(name: String): SupportedLanguage {
        val trimmed = name.trim()
        SupportedLanguage.entries.firstOrNull { it.displayName.equals(trimmed, ignoreCase = true) }?.let { return it }

        val slug = trimmed.lowercase().filter { it.isLetterOrDigit() }
        val key = PREFIX + slug.ifEmpty { java.util.UUID.randomUUID().toString() }
        val updated = records() + Record(key, trimmed)
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY, json.encodeToString(ListSerializer(Record.serializer()), updated))?.apply()
        cache = updated
        return SupportedLanguage.custom(key, trimmed)
    }
}
