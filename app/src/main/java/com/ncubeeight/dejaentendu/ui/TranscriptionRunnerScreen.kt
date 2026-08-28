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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ncubeeight.dejaentendu.studynotes.StudyNoteGenerator
import com.ncubeeight.dejaentendu.studynotes.StudyNotes
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import com.ncubeeight.dejaentendu.studynotes.WordGlossGenerator
import com.ncubeeight.dejaentendu.transcription.AudioDecoder
import com.ncubeeight.dejaentendu.transcription.SampleInput
import com.ncubeeight.dejaentendu.transcription.SpeechTranscriberService
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import com.ncubeeight.dejaentendu.transcription.TranscriptSegmentation
import com.ncubeeight.dejaentendu.transcription.language
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
 * End-to-end screen: takes a SampleInput (audio needs actual decode +
 * transcription; text/image samples already have their text resolved and
 * skip straight to study notes), then generates study notes. Mirrors iOS's
 * TranscriptionRunnerView.swift. Tapping a transcript word opens a small
 * lookup popup (fetches a quick on-device translation) rather than adding
 * it to Vocabulary immediately — some users just want to check a word on
 * the spot without committing it to their list, so "Add to Vocabulary" is
 * a deliberate action inside that popup, not the tap itself.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranscriptionRunnerScreen(input: SampleInput, onViewFlashcard: (VocabularyEntry) -> Unit) {
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    var status by remember(input) {
        mutableStateOf<RunnerStatus>(
            if (input is SampleInput.Audio) RunnerStatus.AwaitingPermission else RunnerStatus.Transcribing
        )
    }

    LaunchedEffect(input, hasAudioPermission) {
        when (input) {
            is SampleInput.Audio -> {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@LaunchedEffect
                }

                status = RunnerStatus.Transcribing
                val recording = input.recording
                val pcmFile = File(context.cacheDir, "${recording.id}.pcm")
                status = try {
                    // decodeToRawPcm16Mono16k is a plain blocking function
                    // (file I/O + a synchronous MediaCodec loop), not a
                    // suspend fun — it must be pushed off the coroutine's
                    // default Main dispatcher or it blocks the UI thread.
                    // Only surfaced with a real multi-minute recording; a
                    // 5-second test clip decoded fast enough to never trip
                    // the ANR watchdog.
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
            }
            is SampleInput.ReadyText -> {
                status = RunnerStatus.Transcribed(input.text)
            }
        }

        val transcribed = status as? RunnerStatus.Transcribed ?: return@LaunchedEffect
        status = try {
            val notes = StudyNoteGenerator.generateNotes(transcribed.text, input.language)
            RunnerStatus.NotesReady(transcribed.text, notes)
        } catch (e: Exception) {
            RunnerStatus.NotesUnavailable(transcribed.text, e.message ?: e.toString())
        }
    }

    fun addToVocabulary(word: String): VocabularyEntry {
        val entries = VocabularyStore.load(context)
        val entry = VocabularyEntry(text = word, addedAtEpochMillis = System.currentTimeMillis(), language = input.language)
        VocabularyStore.save(context, listOf(entry) + entries)
        return entry
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val current = status) {
                is RunnerStatus.AwaitingPermission -> Text("Microphone permission is needed to run on-device transcription.")
                is RunnerStatus.Transcribing -> {
                    CircularProgressIndicator()
                    val label = if (input is SampleInput.Audio) "Transcribing" else "Preparing"
                    Text("$label (${input.language.displayName})…", color = AppColors.inkSoft)
                }
                is RunnerStatus.Transcribed -> {
                    TranscriptBlock(current.text, input.language, ::addToVocabulary, onViewFlashcard)
                    CircularProgressIndicator()
                    Text("Generating study notes…", color = AppColors.inkSoft)
                }
                is RunnerStatus.NotesReady -> {
                    TranscriptBlock(current.text, input.language, ::addToVocabulary, onViewFlashcard)
                    StudyNotesBlock(current.notes)
                }
                is RunnerStatus.NotesUnavailable -> {
                    TranscriptBlock(current.text, input.language, ::addToVocabulary, onViewFlashcard)
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
    language: SupportedLanguage,
    onAddToVocabulary: (String) -> VocabularyEntry,
    onViewFlashcard: (VocabularyEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transcript", color = AppColors.ink)
        Text("Tap a word to look it up.", color = AppColors.inkSoft)

        for (sentence in TranscriptSegmentation.sentences(text, language.locale)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (word in TranscriptSegmentation.words(sentence, language.locale)) {
                    TranscriptWordToken(word, sentence, language, onAddToVocabulary, onViewFlashcard)
                }
            }
        }
    }
}

private sealed interface GlossState {
    data object Loading : GlossState
    data class Ready(val gloss: String) : GlossState
    data class Failed(val reason: String) : GlossState
}

/**
 * A single tappable transcript word. Tapping opens a small popup that
 * fetches a quick on-device translation (WordGlossGenerator — much
 * cheaper than a full flashcard generation) and only offers "Add to
 * Vocabulary" as an explicit follow-up action, so looking a word up
 * doesn't itself commit it to the list. Mirrors the shape of iOS's
 * TranscriptWordToken popover, extended with the gloss preview.
 */
@Composable
private fun TranscriptWordToken(
    word: String,
    sentence: String,
    language: SupportedLanguage,
    onAddToVocabulary: (String) -> VocabularyEntry,
    onViewFlashcard: (VocabularyEntry) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var glossState by remember { mutableStateOf<GlossState?>(null) }
    var addedEntry by remember { mutableStateOf<VocabularyEntry?>(null) }
    val scope = rememberCoroutineScope()

    Text(
        word,
        color = AppColors.ink,
        modifier = Modifier.clickable {
            isExpanded = true
            if (glossState == null) {
                glossState = GlossState.Loading
                scope.launch {
                    glossState = try {
                        GlossState.Ready(WordGlossGenerator.gloss(word, sentence, language))
                    } catch (e: Exception) {
                        GlossState.Failed(e.message ?: e.toString())
                    }
                }
            }
        },
    )

    DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        Column(
            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(word, color = AppColors.ink, fontWeight = FontWeight.Bold)

            val entry = addedEntry
            if (entry == null) {
                when (val current = glossState) {
                    is GlossState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Looking up…", color = AppColors.inkSoft)
                        }
                    }
                    is GlossState.Ready -> Text(current.gloss, color = AppColors.inkSoft)
                    is GlossState.Failed -> Text("Lookup unavailable", color = AppColors.inkSoft)
                    null -> Unit
                }
                Button(
                    onClick = { addedEntry = onAddToVocabulary(word) },
                    modifier = Modifier.widthIn(min = 0.dp),
                ) {
                    Text("Add to Vocabulary")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.coral)
                    Text("Added to Vocabulary", color = AppColors.ink)
                }
                TextButton(onClick = {
                    isExpanded = false
                    onViewFlashcard(entry)
                }) {
                    Text("View Flashcard")
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
