package com.ncubeeight.dejaentendu.samples

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ncubeeight.dejaentendu.transcription.OcrScript
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Copies a picked photo into the app's own storage and runs on-device
 * ML Kit text recognition on it. The Android analog of iOS's
 * ImageIngestion.swift (Vision's VNRecognizeTextRequest) — recognizer
 * selection per script verified against Google's ML Kit docs, 2026-08-27.
 */
object ImageIngestion {

    suspend fun copyAndRecognizeText(
        context: Context,
        sourceUri: Uri,
        language: SupportedLanguage,
    ): ImportedImageSample {
        val recognizedText = recognizeText(context, sourceUri, language)

        val originalFilename = queryDisplayName(context, sourceUri) ?: "Scanned Photo"
        val id = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "ImportedImages").apply { mkdirs() }
        val extension = originalFilename.substringAfterLast('.', missingDelimiterValue = "jpg")
        val destFile = File(dir, "$id.$extension")

        val input = context.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Couldn't open $sourceUri")
        input.use { stream ->
            destFile.outputStream().use { output -> stream.copyTo(output) }
        }

        return ImportedImageSample(
            id = id,
            originalFilename = originalFilename,
            localPath = destFile.absolutePath,
            recognizedText = recognizedText,
            importedAtEpochMillis = System.currentTimeMillis(),
            language = language,
        )
    }

    private suspend fun recognizeText(context: Context, uri: Uri, language: SupportedLanguage): String {
        val recognizer = TextRecognition.getClient(recognizerOptionsFor(language))
        val image = InputImage.fromFilePath(context, uri)
        val visionText: Text = recognizer.process(image).await()
        return visionText.text
    }

    private fun recognizerOptionsFor(language: SupportedLanguage): TextRecognizerOptionsInterface =
        when (language.ocrScript) {
            OcrScript.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            OcrScript.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            OcrScript.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            OcrScript.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            OcrScript.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            // The photo-import language picker only ever offers languages
            // with a non-null ocrScript, so this indicates a caller bug,
            // not a reachable user-facing state.
            null -> throw IllegalArgumentException("${language.displayName} has no OCR recognizer available")
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

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
