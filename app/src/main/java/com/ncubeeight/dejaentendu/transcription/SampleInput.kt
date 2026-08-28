package com.ncubeeight.dejaentendu.transcription

import com.ncubeeight.dejaentendu.audio.ImportedRecording

/**
 * What TranscriptionRunnerScreen needs to run the shared transcript →
 * vocabulary pipeline. Audio needs an actual transcription pass first;
 * text and OCR'd image samples already have their text ready and skip
 * straight to the shared part (segmentation, tap-to-add-vocabulary, study
 * notes). Mirrors iOS's SampleInput enum in TranscriptionRunnerView.swift.
 */
sealed interface SampleInput {
    data class Audio(val recording: ImportedRecording) : SampleInput
    data class ReadyText(val title: String, val text: String, val language: SupportedLanguage) : SampleInput
}

val SampleInput.title: String
    get() = when (this) {
        is SampleInput.Audio -> recording.originalFilename
        is SampleInput.ReadyText -> title
    }

val SampleInput.language: SupportedLanguage
    get() = when (this) {
        is SampleInput.Audio -> recording.language
        is SampleInput.ReadyText -> language
    }
