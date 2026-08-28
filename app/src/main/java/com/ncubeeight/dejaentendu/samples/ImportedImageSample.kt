package com.ncubeeight.dejaentendu.samples

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A photo that's been OCR'd into text via ML Kit Text Recognition — mirrors
 * ImportedRecording's shape (a permanent local copy plus the language
 * tagged up front), with recognizedText standing in for a transcript.
 * Mirrors iOS's ImportedImageSample.swift.
 */
@Serializable
data class ImportedImageSample(
    val id: String = UUID.randomUUID().toString(),
    val originalFilename: String,
    val localPath: String,
    val recognizedText: String,
    val importedAtEpochMillis: Long,
    val language: SupportedLanguage,
)
