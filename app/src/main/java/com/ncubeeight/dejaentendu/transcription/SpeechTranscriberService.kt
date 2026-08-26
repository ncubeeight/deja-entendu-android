package com.ncubeeight.dejaentendu.transcription

import android.media.AudioFormat
import android.os.ParcelFileDescriptor
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.io.File

class TranscriptionUnavailableException(reason: String) : Exception(
    "On-device transcription isn't available right now: $reason"
)

/**
 * Wraps ML Kit GenAI Speech Recognition for file-based transcription — the
 * Android analog of iOS's SpeechTranscriberService.swift (SpeechAnalyzer).
 * Advanced mode (better quality, Gemini-Nano-powered) is used throughout;
 * it's Pixel-10-class-device-specific, matching this app's existing scope.
 */
object SpeechTranscriberService {
    private const val SAMPLE_RATE = 16_000

    suspend fun transcribe(pcmFile: File, language: SupportedLanguage): String {
        val options = SpeechRecognizerOptions.Builder().apply {
            locale = language.speechRecognitionLocale
            preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
        }.build()

        val recognizer = SpeechRecognition.getClient(options)

        when (val status = recognizer.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                // Same never-completing Flow<DownloadStatus> as the Prompt
                // API's GenerativeModel.download() — .first{ terminal } not
                // .collect{}, confirmed by decompiling the real AAR.
                val outcome = recognizer.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw TranscriptionUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw TranscriptionUnavailableException("model unavailable (status=$status)")
        }

        val pfd = ParcelFileDescriptor.open(pcmFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        // ONE_SHOT: we hand over a complete, already-decoded file, not an
        // active live stream — matches AudioSource.Mode's other option,
        // STREAMING, being for a mic-style ongoing source instead.
        val audioSource = AudioSource.fromPfd(pfd, AudioSource.Mode.ONE_SHOT, audioFormat)

        val request = SpeechRecognizerRequest.Builder().apply {
            this.audioSource = audioSource
        }.build()

        val transcript = StringBuilder()
        try {
            recognizer.startRecognition(request).collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.FinalTextResponse -> transcript.append(response.text)
                    is SpeechRecognizerResponse.ErrorResponse ->
                        throw TranscriptionUnavailableException(response.e.message ?: "unknown error")
                    else -> Unit // PartialTextResponse, CompletedResponse — nothing to accumulate
                }
            }
        } catch (e: GenAiException) {
            throw TranscriptionUnavailableException(e.message ?: "unknown error")
        } finally {
            recognizer.close()
        }

        return transcript.toString()
    }
}
