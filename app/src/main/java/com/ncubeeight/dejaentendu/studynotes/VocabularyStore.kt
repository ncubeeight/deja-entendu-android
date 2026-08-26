package com.ncubeeight.dejaentendu.studynotes

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the vocabulary list to a JSON file in the app's own storage.
 * Mirrors iOS's VocabularyStore.swift — same load/save-the-whole-list
 * pattern as ImportedRecordingStore.
 */
object VocabularyStore {
    private const val FILE_NAME = "vocabulary.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<VocabularyEntry> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, entries: List<VocabularyEntry>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.encodeToString(entries))
    }
}
