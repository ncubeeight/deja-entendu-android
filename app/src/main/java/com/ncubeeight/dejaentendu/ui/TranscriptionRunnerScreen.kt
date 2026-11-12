package com.ncubeeight.dejaentendu.ui

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.ncubeeight.dejaentendu.audio.displayTitle
import com.ncubeeight.dejaentendu.samples.SampleTitleStore
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
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionRunnerScreen(
    input: SampleInput,
    /** The saved sample this page shows, so it can be renamed. Null for a page with nothing saved behind it. */
    sampleId: String? = null,
    onViewFlashcard: (VocabularyEntry) -> Unit,
) {
    val context = LocalContext.current

    // The name the Samples list shows before any rename (AnySample.defaultTitle).
    val defaultTitle = when (input) {
        is SampleInput.Audio -> input.recording.displayTitle
        is SampleInput.ReadyText -> input.title
    }
    // Reads SampleTitleStore's snapshot state, so a rename redraws this.
    val title = sampleId?.let { SampleTitleStore.title(it) } ?: defaultTitle
    var renameDraft by remember { mutableStateOf<TextFieldValue?>(null) }
    // One speaker for the page, so starting the transcript stops a word and vice versa.
    val speaker = rememberSampleSpeaker()

    // Text-selection mode. The transcript normally renders as tappable word
    // chips, which native selection can't span, so a long-press offers
    // Select / Select All, swapping the page to one read-only selectable
    // text field until Done (mirrors iOS 6e66a08).
    var isSelecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(TextFieldValue("")) }
    var didCopy by remember { mutableStateOf(false) }
    // Where the Select / Select All menu opens, in root coordinates; null when closed.
    var selectMenuAt by remember { mutableStateOf<Offset?>(null) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

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

    val currentText = when (val current = status) {
        is RunnerStatus.Transcribed -> current.text
        is RunnerStatus.NotesReady -> current.text
        is RunnerStatus.NotesUnavailable -> current.text
        else -> null
    }
    val currentNotes = (status as? RunnerStatus.NotesReady)?.notes
    // Transcript / Translation / Key Vocabulary, shared by the selectable
    // view and the shared text so the two always match.
    val exportText = currentText?.let { exportSections(it, currentNotes) }

    fun enterSelection(selectAll: Boolean) {
        val text = exportText ?: return
        speaker.stop()
        selection = TextFieldValue(text, selection = if (selectAll) TextRange(0, text.length) else TextRange(0))
        didCopy = false
        isSelecting = true
    }
    val onContentLongPress: (Offset) -> Unit = { rootOffset -> if (exportText != null && !isSelecting) selectMenuAt = rootOffset }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                if (sampleId != null) {
                    // The title with a pencil beside it, so someone with dozens
                    // of "Scanned Photo" samples can give each a memorable name.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clickable(onClickLabel = "Rename Sample") {
                                // Opens with the current name selected, so typing replaces it.
                                renameDraft = TextFieldValue(title, selection = TextRange(0, title.length))
                            }
                            .semantics(mergeDescendants = true) {},
                    ) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Filled.Edit, contentDescription = "Rename Sample", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }, actions = {
                if (exportText != null) {
                    // Mail, Messages, Keep, Drive, etc. all come from the system share sheet.
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, "$title\nLanguage: ${input.language.displayName}\n\n$exportText")
                        }
                        context.startActivity(Intent.createChooser(send, "Share Sample"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Sample")
                    }
                }
            })
        },
        bottomBar = {
            if (isSelecting) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { isSelecting = false }) { Text("Done") }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { selection = selection.copy(selection = TextRange(0, selection.text.length)) }) { Text("Select All") }
                        TextButton(onClick = {
                            val range = selection.selection
                            val copied = if (range.collapsed) selection.text else selection.text.substring(range.min, range.max)
                            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title, copied))
                            didCopy = true
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                didCopy = false
                            }
                        }) {
                            Icon(if (didCopy) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (didCopy) "Copied" else "Copy")
                        }
                    }
                }
            }
        },
    ) { padding ->
      Box(
          modifier = Modifier
              .fillMaxSize()
              .padding(padding)
              .onGloballyPositioned { contentOrigin = it.positionInRoot() },
      ) {
        if (isSelecting && exportText != null) {
            // Read-only text field rather than SelectionContainer: it brings the
            // native handles and Copy / Share toolbar, and lets Select All set
            // the selection programmatically.
            BasicTextField(
                value = selection,
                onValueChange = { selection = it.copy(text = selection.text) },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.adaptiveInk),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            )
        } else {
        var columnCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned { columnCoordinates = it }
                .pointerInput(exportText) {
                    detectTapGestures(onLongPress = { local ->
                        columnCoordinates?.let { onContentLongPress(it.localToRoot(local)) }
                    })
                }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val current = status) {
                is RunnerStatus.AwaitingPermission -> Text("Microphone permission is needed to run on-device transcription.")
                is RunnerStatus.Transcribing -> {
                    CircularProgressIndicator()
                    val label = if (input is SampleInput.Audio) "Transcribing" else "Preparing"
                    Text("$label (${input.language.displayName})…", color = AppColors.adaptiveInkSoft)
                }
                is RunnerStatus.Transcribed -> {
                    TranscriptBlock(current.text, input.language, speaker, onContentLongPress, ::addToVocabulary, onViewFlashcard)
                    CircularProgressIndicator()
                    Text("Generating study notes…", color = AppColors.adaptiveInkSoft)
                }
                is RunnerStatus.NotesReady -> {
                    TranscriptBlock(current.text, input.language, speaker, onContentLongPress, ::addToVocabulary, onViewFlashcard)
                    StudyNotesBlock(current.notes)
                }
                is RunnerStatus.NotesUnavailable -> {
                    TranscriptBlock(current.text, input.language, speaker, onContentLongPress, ::addToVocabulary, onViewFlashcard)
                    Text("Study notes unavailable: ${current.reason}", color = AppColors.adaptiveInkSoft)
                }
                is RunnerStatus.Failed -> Text(current.reason, color = AppColors.coral)
            }
        }
        }

        // Long-press menu, opened where the press landed.
        selectMenuAt?.let { at ->
            val local = at - contentOrigin
            Box(Modifier.offset { IntOffset(local.x.toInt(), local.y.toInt()) }.size(1.dp)) {
                AppDropdownMenu(expanded = true, onDismissRequest = { selectMenuAt = null }) {
                    DropdownMenuItem(
                        text = { Text("Select") },
                        leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null) },
                        onClick = { selectMenuAt = null; enterSelection(selectAll = false) },
                    )
                    DropdownMenuItem(
                        text = { Text("Select All") },
                        leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                        onClick = { selectMenuAt = null; enterSelection(selectAll = true) },
                    )
                }
            }
        }
      }
    }

    val draft = renameDraft
    if (draft != null && sampleId != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AppAlertDialog(
            onDismissRequest = { renameDraft = null },
            title = { Text("Rename Sample") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { renameDraft = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                    Text("Leave it empty to go back to \"$defaultTitle\".", color = AppColors.adaptiveInkSoft)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Saving the default name (or nothing) clears the custom one.
                    SampleTitleStore.setTitle(context, sampleId, draft.text, defaultTitle)
                    renameDraft = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameDraft = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptBlock(
    text: String,
    language: SupportedLanguage,
    speaker: SampleSpeaker,
    onLongPress: (Offset) -> Unit,
    onAddToVocabulary: (String) -> VocabularyEntry,
    onViewFlashcard: (VocabularyEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Read-aloud for the whole transcript, with the pace menu beside it,
        // mirroring iOS's speak button on the Sample page (9a3e5cf).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transcript", color = AppColors.adaptiveInk, modifier = Modifier.weight(1f))
            SpeakButton(speaker, TRANSCRIPT_UTTERANCE_ID, text, language)
        }
        Text("Tap a word to look it up.", color = AppColors.adaptiveInkSoft)

        for (sentence in TranscriptSegmentation.sentences(text, language.locale)) {
            // Mirrored for right-to-left sentences, so the first word sits at
            // the right edge and lines wrap leftward (iOS e3c531c).
            val direction = if (TranscriptSegmentation.isRightToLeft(sentence, language)) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for ((index, word) in TranscriptSegmentation.words(sentence, language.locale).withIndex()) {
                        val background = AppColors.rainbow[index % AppColors.rainbow.size]
                        TranscriptWordToken(word, sentence, language, background, speaker, onLongPress, onAddToVocabulary, onViewFlashcard)
                    }
                }
            }
        }
    }
}

private const val TRANSCRIPT_UTTERANCE_ID = "transcript"

/** Transcript, then Translation and Key Vocabulary when study notes exist — as plain text for selecting, copying and sharing. */
private fun exportSections(transcript: String, notes: StudyNotes?): String = buildList {
    add("Transcript\n${transcript.trim()}")
    if (notes != null) {
        add("Translation\n${notes.englishTranslation.trim()}")
        if (notes.keyVocabulary.isNotEmpty()) add("Key Vocabulary\n" + notes.keyVocabulary.joinToString("\n") { "• $it" })
    }
}.joinToString("\n\n")

/**
 * Speak/stop toggle plus the speech-pace menu, disabled when the device has
 * no voice for [language] — same controls as a Samples list row.
 */
@Composable
private fun SpeakButton(speaker: SampleSpeaker, id: String, text: String, language: SupportedLanguage) {
    val isSpeaking = speaker.speakingId == id
    val canSpeak = speaker.canSpeak(language)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { speaker.toggle(id, text, language) }, enabled = isSpeaking || canSpeak) {
            Icon(
                if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Stop speaking" else "Speak",
            )
        }
        SpeechPaceMenu(enabled = canSpeak)
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranscriptWordToken(
    word: String,
    sentence: String,
    language: SupportedLanguage,
    background: Color,
    speaker: SampleSpeaker,
    onLongPress: (Offset) -> Unit,
    onAddToVocabulary: (String) -> VocabularyEntry,
    onViewFlashcard: (VocabularyEntry) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var glossState by remember { mutableStateOf<GlossState?>(null) }
    var addedEntry by remember { mutableStateOf<VocabularyEntry?>(null) }
    val scope = rememberCoroutineScope()
    var tokenCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    Text(
        word,
        color = AppColors.ink,
        modifier = Modifier
            .background(background, RoundedCornerShape(5.dp))
            .onGloballyPositioned { tokenCoordinates = it }
            .combinedClickable(
                // Long-pressing a word offers Select / Select All like the rest of the page.
                onLongClick = { tokenCoordinates?.let { onLongPress(it.localToRoot(Offset(0f, it.size.height.toFloat()))) } },
            ) {
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
            }
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )

    AppDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        Column(
            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word, color = AppColors.adaptiveInk, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                SpeakButton(speaker, "word:$word", word, language)
            }

            val entry = addedEntry
            if (entry == null) {
                when (val current = glossState) {
                    is GlossState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Looking up…", color = AppColors.adaptiveInkSoft)
                        }
                    }
                    is GlossState.Ready -> Text(current.gloss, color = AppColors.adaptiveInkSoft)
                    is GlossState.Failed -> Text("Lookup unavailable", color = AppColors.adaptiveInkSoft)
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
                    Text("Added to Vocabulary", color = AppColors.adaptiveInk)
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
        Text("Translation", color = AppColors.adaptiveInk)
        Text(notes.englishTranslation, color = AppColors.adaptiveInkSoft)

        Text("Key Vocabulary", color = AppColors.adaptiveInk)
        for (word in notes.keyVocabulary) {
            Text("• $word", color = AppColors.adaptiveInkSoft)
        }
    }
}
