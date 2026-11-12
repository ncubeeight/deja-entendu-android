package com.ncubeeight.dejaentendu.samples

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json

/**
 * Names the user gave their samples, keyed by sample id. Kept apart from
 * the three sample stores so renaming needs no change to their saved
 * formats. Mirrors iOS's SampleTitleStore (AnySample.swift).
 *
 * Held in Compose snapshot state, so any composable that reads a title
 * (AnySample.title on Samples and Home, or a Sample's page) redraws, and
 * the Samples list re-sorts, as soon as a Sample is renamed, with no
 * extra wiring. [init] must run before any title is read
 * (MainActivity.onCreate).
 */
object SampleTitleStore {
    private const val PREFS_NAME = "sample_titles"
    private const val KEY = "customTitlesJSON"
    private val json = Json { ignoreUnknownKeys = true }

    private var titles by mutableStateOf<Map<String, String>>(emptyMap())

    fun init(context: Context) {
        val raw = prefs(context).getString(KEY, null) ?: return
        titles = try {
            json.decodeFromString<Map<String, String>>(raw)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun title(id: String): String? = titles[id]

    /** Saves a new name; null, blank, or [defaultTitle] goes back to the default. */
    fun setTitle(context: Context, id: String, title: String?, defaultTitle: String? = null) {
        val trimmed = title?.trim().orEmpty()
        val updated = titles.toMutableMap()
        if (trimmed.isEmpty() || trimmed == defaultTitle) updated.remove(id) else updated[id] = trimmed
        if (updated == titles) return
        titles = updated
        prefs(context).edit().putString(KEY, json.encodeToString(updated)).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
