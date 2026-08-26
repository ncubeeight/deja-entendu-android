package com.ncubeeight.dejaentendu.audio

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A recording that has been copied into the app's own storage, mirroring
 * iOS's ImportedRecording.swift. localPath points at a file inside the
 * app's internal storage — safe to reopen anytime, unlike the original
 * picked content:// Uri.
 */
@Serializable
data class ImportedRecording(
    val id: String = UUID.randomUUID().toString(),
    val originalFilename: String,
    val localPath: String,
    val importedAtEpochMillis: Long,
    val source: Source,
    val language: SupportedLanguage,
) {
    @Serializable
    enum class Source { FILES_IMPORTER }
}
