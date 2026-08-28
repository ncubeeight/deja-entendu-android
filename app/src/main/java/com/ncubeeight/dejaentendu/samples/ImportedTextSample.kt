package com.ncubeeight.dejaentendu.samples

import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A block of foreign-language text pasted or typed directly into the app —
 * no transcription step needed, it goes straight into the same
 * segmentation/study-notes pipeline a recording's transcript would.
 * Mirrors iOS's ImportedTextSample.swift.
 */
@Serializable
data class ImportedTextSample(
    val id: String = UUID.randomUUID().toString(),
    val body: String,
    val importedAtEpochMillis: Long,
    val language: SupportedLanguage,
) {
    /**
     * Shown in lists in place of a filename — text samples don't have a
     * user-entered title, so the first line (or first ~40 characters)
     * stands in for one.
     */
    val title: String
        get() {
            val firstLine = body.substringBefore("\n")
            return if (firstLine.length <= 40) firstLine else firstLine.take(40) + "…"
        }
}
