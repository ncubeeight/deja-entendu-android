package com.ncubeeight.dejaentendu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.audio.AudioIngestion
import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.audio.ImportedRecordingStore
import com.ncubeeight.dejaentendu.samples.AnySample
import com.ncubeeight.dejaentendu.samples.ImportedImageSampleStore
import com.ncubeeight.dejaentendu.samples.ImportedTextSample
import com.ncubeeight.dejaentendu.samples.ImportedTextSampleStore
import com.ncubeeight.dejaentendu.samples.SampleKind
import com.ncubeeight.dejaentendu.samples.SampleTextGenerator
import com.ncubeeight.dejaentendu.samples.id
import com.ncubeeight.dejaentendu.samples.kind
import com.ncubeeight.dejaentendu.samples.importedAtEpochMillis
import com.ncubeeight.dejaentendu.samples.subtitle
import com.ncubeeight.dejaentendu.samples.title
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class SampleFilter(val label: String, val kind: SampleKind?) {
    ALL("All", null),
    AUDIO(SampleKind.AUDIO.label, SampleKind.AUDIO),
    TEXT(SampleKind.TEXT.label, SampleKind.TEXT),
    IMAGE(SampleKind.IMAGE.label, SampleKind.IMAGE),
}

/**
 * The merged "Samples" tab — audio recordings, pasted/typed text, and OCR'd
 * photos all in one filterable, color-coded list. Replaces the old
 * audio-only UploadScreen; its Files-import flow lives on here unchanged.
 * Mirrors iOS's SamplesView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplesScreen(onSampleClick: (AnySample) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var audioRecordings by remember { mutableStateOf(ImportedRecordingStore.load(context)) }
    var textSamples by remember { mutableStateOf(ImportedTextSampleStore.load(context)) }
    var imageSamples by remember { mutableStateOf(ImportedImageSampleStore.load(context)) }
    var filter by remember { mutableStateOf(SampleFilter.ALL) }

    var isAddDialogVisible by remember { mutableStateOf(false) }
    var isTextImportVisible by remember { mutableStateOf(false) }
    var isImageImportVisible by remember { mutableStateOf(false) }
    var isLanguageSheetVisible by remember { mutableStateOf(false) }

    // Generated-sample state — no speech/OCR gating, every enabled language works.
    var isGenerateLanguageSheetVisible by remember { mutableStateOf(false) }
    var pendingGenerateLanguage by remember { mutableStateOf(AppSettingsStore.enabledLanguagesSorted(context).first()) }
    var isGeneratingSample by remember { mutableStateOf(false) }

    // Only languages ML Kit GenAI Speech Recognition actually supports are
    // offered here — audio transcription isn't universal the way typed
    // text/Generate Sample are. Falls back to every speech-capable language
    // if the user has disabled all of them in Settings, so this picker is
    // never empty.
    var enabledLanguages by remember {
        mutableStateOf(AppSettingsStore.audioImportLanguages(context))
    }
    var pendingLanguage by remember { mutableStateOf(enabledLanguages.first()) }

    OnResume {
        // Reloaded every time this tab appears — otherwise a deletion made
        // from Home's "Continue studying" section wouldn't show up here.
        audioRecordings = ImportedRecordingStore.load(context)
        textSamples = ImportedTextSampleStore.load(context)
        imageSamples = ImportedImageSampleStore.load(context)
        enabledLanguages = AppSettingsStore.audioImportLanguages(context)
    }

    val allSamples = (audioRecordings.map(AnySample::Audio) + textSamples.map(AnySample::Text) + imageSamples.map(AnySample::Image))
        .sortedByDescending { it.importedAtEpochMillis }
    val filteredSamples = filter.kind?.let { kind -> allSamples.filter { it.kind == kind } } ?: allSamples

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = mutableListOf<ImportedRecording>()
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    try {
                        imported.add(
                            AudioIngestion.copyIntoAppStorage(context, uri, ImportedRecording.Source.FILES_IMPORTER, pendingLanguage)
                        )
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) { snackbarHostState.showSnackbar(e.message ?: e.toString()) }
                    }
                }
            }
            if (imported.isNotEmpty()) {
                audioRecordings = imported + audioRecordings
                ImportedRecordingStore.save(context, audioRecordings)
            }
        }
    }

    fun delete(sample: AnySample) {
        when (sample) {
            is AnySample.Audio -> {
                audioRecordings = audioRecordings.filterNot { it.id == sample.recording.id }
                ImportedRecordingStore.save(context, audioRecordings)
                File(sample.recording.localPath).delete()
            }
            is AnySample.Text -> {
                textSamples = textSamples.filterNot { it.id == sample.sample.id }
                ImportedTextSampleStore.save(context, textSamples)
            }
            is AnySample.Image -> {
                imageSamples = imageSamples.filterNot { it.id == sample.sample.id }
                ImportedImageSampleStore.save(context, imageSamples)
                File(sample.sample.localPath).delete()
            }
        }
    }

    fun presentLanguageSheet() {
        if (pendingLanguage !in enabledLanguages) pendingLanguage = enabledLanguages.first()
        isLanguageSheetVisible = true
    }

    fun presentGenerateLanguageSheet() {
        val allEnabled = AppSettingsStore.enabledLanguagesSorted(context)
        if (pendingGenerateLanguage !in allEnabled) pendingGenerateLanguage = allEnabled.first()
        isGenerateLanguageSheetVisible = true
    }

    fun generateSample() {
        isGeneratingSample = true
        scope.launch {
            try {
                val text = SampleTextGenerator.generateParagraph(pendingGenerateLanguage)
                val sample = ImportedTextSample(
                    body = text,
                    importedAtEpochMillis = System.currentTimeMillis(),
                    language = pendingGenerateLanguage,
                )
                textSamples = listOf(sample) + textSamples
                ImportedTextSampleStore.save(context, textSamples)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: e.toString())
            }
            isGeneratingSample = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Samples") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddDialogVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Sample")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SampleFilter.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = filter == option,
                            onClick = { filter = option },
                            shape = SegmentedButtonDefaults.itemShape(index, SampleFilter.entries.size),
                        ) {
                            Text(option.label)
                        }
                    }
                }

                if (filteredSamples.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No samples yet. Import a recording, add text, or scan a photo below.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredSamples, key = { it.id }) { sample ->
                            SampleRow(sample, onClick = { onSampleClick(sample) }, onDelete = { delete(sample) })
                        }
                    }
                }
            }

            if (isGeneratingSample) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Generating sample…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (isAddDialogVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { isAddDialogVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add a Sample")
                TextButton(
                    onClick = {
                        isAddDialogVisible = false
                        presentLanguageSheet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import Recording") }
                TextButton(
                    onClick = { isAddDialogVisible = false; isTextImportVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add Text") }
                TextButton(
                    onClick = { isAddDialogVisible = false; isImageImportVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Scan Photo") }
                TextButton(
                    onClick = {
                        isAddDialogVisible = false
                        presentGenerateLanguageSheet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Generate Sample") }
            }
        }
    }

    if (isLanguageSheetVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { isLanguageSheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("What language is this recording in?")
                for (language in enabledLanguages) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == pendingLanguage, onClick = { pendingLanguage = language })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == pendingLanguage, onClick = { pendingLanguage = language })
                        Text(language.displayName)
                    }
                }
                Button(
                    onClick = {
                        isLanguageSheetVisible = false
                        filePickerLauncher.launch(arrayOf("audio/*", "video/mp4"))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("Continue")
                }
            }
        }
    }

    if (isGenerateLanguageSheetVisible) {
        val sheetState = rememberModalBottomSheetState()
        val generateLanguages = remember { AppSettingsStore.enabledLanguagesSorted(context) }
        ModalBottomSheet(onDismissRequest = { isGenerateLanguageSheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("What language would you like the sample in?")
                Text(
                    "A short, simple practice paragraph will be generated on-device — no recording or file needed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                for (language in generateLanguages) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == pendingGenerateLanguage,
                                onClick = { pendingGenerateLanguage = language },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == pendingGenerateLanguage, onClick = { pendingGenerateLanguage = language })
                        Text(language.displayName)
                    }
                }
                Button(
                    onClick = {
                        isGenerateLanguageSheetVisible = false
                        generateSample()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("Generate")
                }
            }
        }
    }

    if (isTextImportVisible) {
        TextImportSheet(
            onDismiss = { isTextImportVisible = false },
            onSaved = {
                isTextImportVisible = false
                textSamples = ImportedTextSampleStore.load(context)
            },
        )
    }

    if (isImageImportVisible) {
        ImageImportSheet(
            onDismiss = { isImageImportVisible = false },
            onSaved = {
                isImageImportVisible = false
                imageSamples = ImportedImageSampleStore.load(context)
            },
        )
    }
}

@Composable
private fun SampleRow(sample: AnySample, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(sample.title) },
        supportingContent = { Text(sample.subtitle) },
        leadingContent = {
            Box(
                modifier = Modifier.size(32.dp).background(sample.kind.tintSoft, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sample.kind.icon, contentDescription = null, tint = sample.kind.tint, modifier = Modifier.size(16.dp))
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Delete")
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
