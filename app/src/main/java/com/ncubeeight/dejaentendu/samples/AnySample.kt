package com.ncubeeight.dejaentendu.samples

import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.transcription.SampleInput
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A type-erased wrapper over the three kinds of imported sample, so the
 * Samples tab (and Home's "Continue studying") can list, filter, and
 * navigate from them uniformly. Mirrors iOS's AnySample.swift.
 */
sealed interface AnySample {
    data class Audio(val recording: ImportedRecording) : AnySample
    data class Text(val sample: ImportedTextSample) : AnySample
    data class Image(val sample: ImportedImageSample) : AnySample
}

val AnySample.id: String
    get() = when (this) {
        is AnySample.Audio -> recording.id
        is AnySample.Text -> sample.id
        is AnySample.Image -> sample.id
    }

val AnySample.kind: SampleKind
    get() = when (this) {
        is AnySample.Audio -> SampleKind.AUDIO
        is AnySample.Text -> SampleKind.TEXT
        is AnySample.Image -> SampleKind.IMAGE
    }

val AnySample.title: String
    get() = when (this) {
        is AnySample.Audio -> recording.originalFilename
        is AnySample.Text -> sample.title
        is AnySample.Image -> sample.originalFilename
    }

val AnySample.language: SupportedLanguage
    get() = when (this) {
        is AnySample.Audio -> recording.language
        is AnySample.Text -> sample.language
        is AnySample.Image -> sample.language
    }

val AnySample.importedAtEpochMillis: Long
    get() = when (this) {
        is AnySample.Audio -> recording.importedAtEpochMillis
        is AnySample.Text -> sample.importedAtEpochMillis
        is AnySample.Image -> sample.importedAtEpochMillis
    }

val AnySample.subtitle: String
    get() {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
        return "${language.displayName} · ${formatter.format(Instant.ofEpochMilli(importedAtEpochMillis))}"
    }

/**
 * What TranscriptionRunnerScreen needs to run this sample through the
 * shared transcript pipeline — audio needs actual transcription, text and
 * image samples already have their text resolved.
 */
val AnySample.runnerInput: SampleInput
    get() = when (this) {
        is AnySample.Audio -> SampleInput.Audio(recording)
        is AnySample.Text -> SampleInput.ReadyText(title = sample.title, text = sample.body, language = sample.language)
        is AnySample.Image -> SampleInput.ReadyText(
            title = sample.originalFilename,
            text = sample.recognizedText,
            language = sample.language,
        )
    }
