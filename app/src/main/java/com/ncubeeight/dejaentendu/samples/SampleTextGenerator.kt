package com.ncubeeight.dejaentendu.samples

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
data class SampleParagraph(
    @param:Guide(
        description = "A short, simple 3-5 sentence paragraph for a beginner language " +
            "learner, using common everyday vocabulary and simple present tense. No title, " +
            "no English, no commentary — only the paragraph itself."
    )
    val text: String,
)

class SampleTextGeneratorUnavailableException(reason: String) : Exception(
    "On-device sample generation isn't available right now: $reason"
)

/**
 * Generates a short, innocuous practice paragraph on-device — for a user
 * who wants to try the app's transcript/flashcard flow but doesn't already
 * have a recording, passage, or photo of their own to import. Mirrors iOS's
 * SampleTextGenerator.swift.
 */
object SampleTextGenerator {

    /**
     * A small fixed pool of everyday themes, picked at random each time so
     * repeated generations don't all read the same. Kept deliberately
     * simple and universal (no idioms, slang, or culture-specific
     * references) so they translate cleanly and consistently no matter
     * which language is selected.
     */
    private val themes = listOf(
        "a cat going for a slow walk around a quiet garden",
        "someone making a cup of tea on a rainy morning",
        "a child feeding ducks at a park pond",
        "a family cooking a simple dinner together",
        "a dog playing fetch with a ball in a yard",
        "someone walking to a small market to buy fresh fruit",
        "a person reading a book by a window on a sunny afternoon",
        "two friends taking a short walk and talking about their day",
        "a bird building a nest in a tree outside a house",
        "someone watering plants on a balcony in the morning",
    )

    suspend fun generateParagraph(language: SupportedLanguage): String {
        val model = Generation.getClient()

        when (val status = model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                val outcome = model.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw SampleTextGeneratorUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw SampleTextGeneratorUnavailableException("model unavailable (status=$status)")
        }

        if (!model.isStructuredOutputFeatureAvailable()) {
            throw SampleTextGeneratorUnavailableException("structured output isn't supported on this device")
        }

        val theme = themes.random()

        val instruction = SystemInstruction(
            """
            You are a language-learning content writer. Write a short,
            simple practice paragraph in ${language.displayName}, using its
            native script, about: $theme. Keep it to 3-5 short sentences
            using common everyday vocabulary and simple present tense —
            suitable for a beginner learner. Do not include a title, an
            English translation, or any commentary — only the paragraph
            itself, written entirely in ${language.displayName}.
            """.trimIndent()
        )

        val contentRequest = GenerateContentRequest.Builder(instruction, TextPart("Write the paragraph now."))
            .apply { maxOutputTokens = 300 }
            .build()

        val request = generateTypedContentRequest(contentRequest, SampleParagraph::class)

        try {
            val response = model.generateContent(request)
            val paragraph = response.candidates.firstOrNull()?.response
                ?: throw SampleTextGeneratorUnavailableException("no result")
            return paragraph.text
        } catch (e: GenAiException) {
            throw SampleTextGeneratorUnavailableException(e.message ?: "unknown error")
        }
    }
}
