package com.ncubeeight.dejaentendu.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Pulls a file the user picked via the SAF document picker (a content://
 * Uri, only briefly guaranteed accessible) into a permanent local copy the
 * app owns. Mirrors iOS's AudioIngestion.swift, minus the security-scoped
 * resource bracketing iOS needs — SAF's ACTION_OPEN_DOCUMENT grants are
 * usable directly via ContentResolver without that dance.
 */
object AudioIngestion {

    fun copyIntoAppStorage(
        context: Context,
        sourceUri: Uri,
        source: ImportedRecording.Source,
        language: SupportedLanguage,
    ): ImportedRecording {
        val resolver = context.contentResolver
        val originalFilename = queryDisplayName(context, sourceUri) ?: "recording"
        val extension = originalFilename.substringAfterLast('.', missingDelimiterValue = "m4a")

        val id = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "ImportedRecordings").apply { mkdirs() }
        val destFile = File(dir, "$id.$extension")

        val input = resolver.openInputStream(sourceUri)
            ?: throw IOException("Couldn't open $sourceUri")
        input.use { stream ->
            destFile.outputStream().use { output -> stream.copyTo(output) }
        }

        return ImportedRecording(
            id = id,
            originalFilename = originalFilename,
            localPath = destFile.absolutePath,
            importedAtEpochMillis = System.currentTimeMillis(),
            source = source,
            language = language,
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
