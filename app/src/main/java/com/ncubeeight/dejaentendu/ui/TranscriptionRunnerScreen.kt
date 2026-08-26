package com.ncubeeight.dejaentendu.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.studynotes.StudyNoteGenerator
import com.ncubeeight.dejaentendu.studynotes.StudyNotes
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import com.ncubeeight.dejaentendu.transcription.AudioDecoder
import com.ncubeeight.dejaentendu.transcription.SpeechTranscriberService
import com.ncubeeight.dejaentendu.transcription.TranscriptSegmentation
import com.ncubeeight.dejaentendu.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.io.File

private sealed interface RunnerStatus {
    data object AwaitingPermission : RunnerStatus
    data object Transcribing : RunnerStatus
    data class Transcribed(val text: String) : RunnerStatus
    data class NotesReady(val text: String, val notes: StudyNotes) : RunnerStatus
    data class NotesUnavailable(val text: String, val reason: String) : RunnerStatus
    data class Failed(val reason: String) : RunnerStatus
}

/**
 * End-to-end screen: takes an ImportedRecording, decodes + transcribes it,
 * then generates study notes. Mirrors iOS's TranscriptionRunnerView.swift.
 * Tapping a transcript word adds it straight to Vocabulary with a Snackbar
 * confirmation — a simpler stand-in for iOS's tap-then-confirm popover.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranscriptionRunnerScreen(recording: ImportedRecording) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    var status by remember(recording.id) { mutableStateOf<RunnerStatus>(RunnerStatus.AwaitingPermission) }

    LaunchedEffect(recording.id, hasAudioPermission) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }

        status = RunnerStatus.Transcribing
        val pcmFile = File(context.cacheDir, "${recording.id}.pcm")
        status = try {
            // decodeToRawPcm16Mono16k is a plain blocking function (file
            // I/O + a synchronous MediaCodec loop), not a suspend fun — it
            // must be pushed off the coroutine's default Main dispatcher or
            // it blocks the UI thread. Only surfaced with a real multi-
            // minute recording; a 5-second test clip decoded fast enough
            // to never trip the ANR watchdog.
            withContext(Dispatchers.IO) {
                AudioDecoder.decodeToRawPcm16Mono16k(recording.localPath, pcmFile)
            }
            val transcript = SpeechTranscriberService.transcribe(pcmFile, recording.language)
            RunnerStatus.Transcribed(transcript)
        } catch (e: Exception) {
            RunnerStatus.Failed(e.message ?: e.toString())
        } finally {
            pcmFile.delete()
        }

        val transcribed = status as? RunnerStatus.Transcribed ?: return@LaunchedEffect
        status = try {
            val notes = StudyNoteGenerator.generateNotes(transcribed.text, recording.language)
            RunnerStatus.NotesReady(transcribed.text, notes)
        } catch (e: Exception) {
            RunnerStatus.NotesUnavailable(transcribed.text, e.message ?: e.toString())
        }
    }

    fun addToVocabulary(word: String) {
        val entries = VocabularyStore.load(context)
        val updated = listOf(
            VocabularyEntry(text = word, addedAtEpochMillis = System.currentTimeMillis(), language = recording.language)
        ) + entries
        VocabularyStore.save(context, updated)
        scope.launch { snackbarHostState.showSnackbar("Added \"$word\" to Vocabulary") }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val current = status) {
                is RunnerStatus.AwaitingPermission -> Text("Microphone permission is needed to run on-device transcription.")
                is RunnerStatus.Transcribing -> {
                    CircularProgressIndicator()
                    Text("Transcribing (${recording.language.displayName})…", color = AppColors.inkSoft)
                }
                is RunnerStatus.Transcribed -> {
                    TranscriptBlock(current.text, recording.language, ::addToVocabulary)
                    CircularProgressIndicator()
                    Text("Generating study notes…", color = AppColors.inkSoft)
                }
                is RunnerStatus.NotesReady -> {
                    TranscriptBlock(current.text, recording.language, ::addToVocabulary)
                    StudyNotesBlock(current.notes)
                }
                is RunnerStatus.NotesUnavailable -> {
                    TranscriptBlock(current.text, recording.language, ::addToVocabulary)
                    Text("Study notes unavailable: ${current.reason}", color = AppColors.inkSoft)
                }
                is RunnerStatus.Failed -> Text(current.reason, color = AppColors.coral)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptBlock(
    text: String,
    language: com.ncubeeight.dejaentendu.transcription.SupportedLanguage,
    onWordTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transcript", color = AppColors.ink)
        Text("Tap a word to add it to your Vocabulary list.", color = AppColors.inkSoft)

        for (sentence in TranscriptSegmentation.sentences(text, language.locale)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (word in TranscriptSegmentation.words(sentence, language.locale)) {
                    Text(
                        word,
                        color = AppColors.ink,
                        modifier = Modifier.clickable { onWordTap(word) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyNotesBlock(notes: StudyNotes) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Translation", color = AppColors.ink)
        Text(notes.englishTranslation, color = AppColors.inkSoft)

        Text("Key Vocabulary", color = AppColors.ink)
        for (word in notes.keyVocabulary) {
            Text("• $word", color = AppColors.inkSoft)
        }

        Text("Note", color = AppColors.ink)
        Text(notes.grammarNote, color = AppColors.inkSoft)
    }
}
