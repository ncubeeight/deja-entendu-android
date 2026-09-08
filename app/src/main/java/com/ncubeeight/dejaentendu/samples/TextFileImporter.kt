package com.ncubeeight.dejaentendu.samples

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class TextFileImportException(message: String) : Exception(message)

/**
 * Extracts text from a user-picked PDF or plain-text file, for the Add
 * Text screen's "Select PDF or Text File" button — the Android analog of
 * iOS's TextImportView.handleFileImport (PDFKit for PDFs, String(contentsOf:)
 * for plain text).
 */
object TextFileImporter {

    fun extractText(context: Context, uri: Uri): String {
        val displayName = queryDisplayName(context, uri).orEmpty()
        val mimeType = context.contentResolver.getType(uri)
        val isPdf = mimeType == "application/pdf" || displayName.lowercase().endsWith(".pdf")

        return if (isPdf) extractPdfText(context, uri) else extractPlainText(context, uri)
    }

    private fun extractPdfText(context: Context, uri: Uri): String {
        // Loads font/glyph resources PDFTextStripper needs — idempotent,
        // but only worth doing once per process.
        if (!PDFBoxResourceLoader.isReady()) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }

        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { document -> PDFTextStripper().getText(document) }
        } ?: throw TextFileImportException("Couldn't open that file.")

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw TextFileImportException(
                "Couldn't find any text in that PDF — it may be a scanned document with no text layer."
            )
        }
        return trimmed
    }

    private fun extractPlainText(context: Context, uri: Uri): String {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw TextFileImportException("Couldn't open that file.")

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw TextFileImportException("Couldn't read that file as text.")
        }
        return trimmed
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
