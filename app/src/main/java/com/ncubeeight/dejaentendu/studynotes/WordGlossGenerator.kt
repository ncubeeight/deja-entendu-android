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

@Generable
data class WordGloss(
    @param:Guide(description = "A concise 1-4 word English gloss/translation for the given word or phrase. No punctuation, no romanization.")
    val englishGloss: String,
)

class WordGlossUnavailableException(reason: String) : Exception(
    "On-device definitions aren't available right now: $reason"
)

/**
 * A quick, lightweight on-device translation for a single tapped transcript
 * word — deliberately much cheaper than FlashcardGenerator's full
 * pronunciation+example generation, so it's fast enough to show in a
 * tap-and-preview popup before the user decides whether to commit the word
 * to their Vocabulary list.
 */
object WordGlossGenerator {

    /**
     * Looks up a short English gloss for a single word/phrase, using the
     * surrounding sentence as context to disambiguate.
     */
    suspend fun gloss(word: String, sentence: String, language: SupportedLanguage): String {
        val model = Generation.getClient()

        when (val status = model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                val outcome = model.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw WordGlossUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw WordGlossUnavailableException("model unavailable (status=$status)")
        }

        if (!model.isStructuredOutputFeatureAvailable()) {
            throw WordGlossUnavailableException("structured output isn't supported on this device")
        }

        val instruction = SystemInstruction(
            """
            You are a compact ${language.displayName}-to-English dictionary. The
            user will give you a word or short phrase in ${language.displayName}
            — treat that as certain, do not reinterpret it as another language.
            Given the sentence it's drawn from for context, respond with only a
            brief, plain English translation for that word or phrase — not a
            translation of the whole sentence.
            """.trimIndent()
        )

        val contentRequest = GenerateContentRequest.Builder(instruction, TextPart("Word: $word\nSentence: $sentence"))
            .apply { maxOutputTokens = 60 }
            .build()

        val request = generateTypedContentRequest(contentRequest, WordGloss::class)

        try {
            val response = model.generateContent(request)
            val gloss = response.candidates.firstOrNull()?.response
                ?: throw WordGlossUnavailableException("no result")
            return gloss.englishGloss
        } catch (e: GenAiException) {
            throw WordGlossUnavailableException(e.message ?: "unknown error")
        }
    }
}
