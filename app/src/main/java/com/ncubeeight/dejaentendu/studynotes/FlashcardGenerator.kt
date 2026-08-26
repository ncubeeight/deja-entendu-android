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

/** Mirrors iOS's FlashcardDetails (FlashcardGenerator.swift). */
@Generable
data class FlashcardDetails(
    @param:Guide(description = "A simple phonetic pronunciation guide using plain English spelling, not IPA — e.g. 'boh-ZHOOR'.")
    val pronunciation: String,

    @param:Guide(description = "A brief, natural English translation or definition of the term.")
    val translation: String,

    @param:Guide(description = "A short, natural example sentence in the term's own language and script, using the term.")
    val exampleSentence: String,

    @param:Guide(description = "An English translation of the example sentence.")
    val exampleTranslation: String,
)

class FlashcardUnavailableException(reason: String) : Exception(
    "On-device flashcard details aren't available right now: $reason"
)

object FlashcardGenerator {

    /**
     * When [language] is known (the entry came from a tapped transcript
     * word, already tagged with the recording's language), pass it so the
     * model doesn't have to guess — same reasoning as iOS, where a bare word
     * like "commandera" can look like Spanish/Italian as easily as French.
     * Entries without a known language (manual entry, share-in text) leave
     * this null and ask the model to identify it.
     */
    suspend fun generateDetails(term: String, language: SupportedLanguage?): FlashcardDetails {
        val model = Generation.getClient()

        when (val status = model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                val outcome = model.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw FlashcardUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw FlashcardUnavailableException("model unavailable (status=$status)")
        }

        if (!model.isStructuredOutputFeatureAvailable()) {
            throw FlashcardUnavailableException("structured output isn't supported on this device")
        }

        val instructionText = if (language != null) {
            """
            You are a compact language-learning dictionary. The user will give
            you a word or short phrase in ${language.displayName} — treat that
            as certain, do not second-guess or reinterpret it as another
            language even if it also resembles a word in one. Produce a
            pronunciation guide, a translation, and a short natural example
            sentence in ${language.displayName} using the term — plus that
            sentence's English translation.
            """.trimIndent()
        } else {
            """
            You are a compact language-learning dictionary. Given a word or
            short phrase, first identify what language it's in, then produce
            a pronunciation guide, a translation, and a short natural example
            sentence using the term in that language — plus that sentence's
            English translation.
            """.trimIndent()
        }

        val contentRequest = GenerateContentRequest.Builder(SystemInstruction(instructionText), TextPart("Term: $term"))
            .apply { maxOutputTokens = 300 }
            .build()

        val request = generateTypedContentRequest(contentRequest, FlashcardDetails::class)

        try {
            val response = model.generateContent(request)
            return response.candidates.firstOrNull()?.response
                ?: throw FlashcardUnavailableException("no result")
        } catch (e: GenAiException) {
            throw FlashcardUnavailableException(e.message ?: "unknown error")
        }
    }
}
