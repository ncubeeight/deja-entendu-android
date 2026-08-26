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
import kotlinx.coroutines.flow.first

/** Mirrors iOS's WordGloss (WordGlossGenerator.swift). */
@Generable
data class WordGloss(
    @param:Guide(description = "A concise 1-4 word English gloss for the given word or phrase. No punctuation, no romaji.")
    val englishGloss: String,
)

class WordGlossUnavailableException(reason: String) : Exception(
    "On-device definitions aren't available right now: $reason"
)

object WordGlossGenerator {

    /**
     * Looks up a short English gloss for a single word/clause, using the
     * surrounding line as context — same as iOS's IrohaExplorerView use case.
     */
    suspend fun gloss(word: String, line: String): String {
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
            You are a compact classical-Japanese-to-English dictionary. Given a
            short word or clause and the line of poetry it's drawn from, respond
            with only a brief, plain English gloss for that word — not a
            translation of the whole line.
            """.trimIndent()
        )

        val contentRequest = GenerateContentRequest.Builder(instruction, TextPart("Word: $word\nLine: $line"))
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
