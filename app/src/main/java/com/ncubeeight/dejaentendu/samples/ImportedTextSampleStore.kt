package com.ncubeeight.dejaentendu.samples

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists pasted/typed text samples the same way ImportedRecordingStore
 * persists recordings — a JSON file in the app's own storage.
 */
object ImportedTextSampleStore {
    private const val FILE_NAME = "text_samples.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<ImportedTextSample> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, samples: List<ImportedTextSample>) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.encodeToString(samples))
    }
}
