package com.ncubeeight.dejaentendu.studynotes

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.flow.first

/**
 * Structured output from Gemini Nano, mirroring iOS's StudyNotes
 * (StudyNoteGenerator.swift). @Generable/@Guide fill in a schema rather
 * than free-associating, same reasoning as FoundationModels on iOS.
 */
@Generable
data class StudyNotes(
    @param:Guide(description = "Natural English translation of the transcript, 1-2 sentences.")
    val englishTranslation: String,

    @param:Guide(description = "Up to 5 notable vocabulary words or phrases from the transcript, written in their original script.", maxItems = 5)
    val keyVocabulary: List<String>,

    @param:Guide(description = "One short, encouraging note about grammar or phrasing, max 2 sentences.")
    val grammarNote: String,
)

class StudyNoteUnavailableException(reason: String) : Exception(
    "On-device study notes aren't available right now: $reason"
)

object StudyNoteGenerator {

    /**
     * Fresh client per call, same reasoning as iOS: don't accumulate
     * conversation history across unrelated transcripts.
     */
    suspend fun generateNotes(transcript: String, language: SupportedLanguage): StudyNotes {
        val model = Generation.getClient()

        when (val status = model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                // first{} stops collecting as soon as a terminal status shows up —
                // the flow itself never completes on its own, so a plain
                // .collect{} here hangs forever even after the download finishes.
                val outcome = model.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw StudyNoteUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw StudyNoteUnavailableException("model unavailable (status=$status)")
        }

        // Structured output (@Generable) is a separate capability gate from
        // plain checkStatus() availability — a device can have the base
        // model but not this feature yet.
        if (!model.isStructuredOutputFeatureAvailable()) {
            throw StudyNoteUnavailableException("structured output isn't supported on this device")
        }

        val instruction = SystemInstruction(
            """
            You help a student studying ${language.displayName}. Given a transcript of
            their spoken practice, translate it and give brief, encouraging feedback.
            Keep responses short.
            """.trimIndent()
        )

        val contentRequest = GenerateContentRequest.Builder(instruction, TextPart("Transcript:\n$transcript"))
            .apply { maxOutputTokens = 300 }
            .build()

        val request = generateTypedContentRequest(contentRequest, StudyNotes::class)

        try {
            val response = model.generateContent(request)
            return response.candidates.firstOrNull()?.response
                ?: throw StudyNoteUnavailableException("no result")
        } catch (e: GenAiException) {
            throw StudyNoteUnavailableException(e.message ?: "unknown error")
        }
    }
}
