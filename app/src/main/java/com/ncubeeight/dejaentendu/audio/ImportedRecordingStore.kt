package com.ncubeeight.dejaentendu.audio

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the imported-recordings list to a JSON file in the app's own
 * storage, mirroring iOS's ImportedRecordingStore.swift (Codable + JSON
 * file in Documents). No syncing, no database — matches the rest of the
 * app's on-device-only, small-scale design.
 */
object ImportedRecordingStore {
    private const val FILE_NAME = "imported_recordings.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<ImportedRecording> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, recordings: List<ImportedRecording>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.encodeToString(recordings))
    }
}
