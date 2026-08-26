package com.ncubeeight.dejaentendu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.audio.AudioIngestion
import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.audio.ImportedRecordingStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Audio Samples tab: pick audio/video files out of the system Files
 * picker, tag them with a spoken language, and list them. Mirrors iOS's
 * VoiceMemoImportView.swift (minus the Share Extension import path, which
 * doesn't have an Android equivalent built yet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(onRecordingClick: (ImportedRecording) -> Unit) {
    val context = LocalContext.current
    var recordings by remember { mutableStateOf(ImportedRecordingStore.load(context)) }
    var isLanguageSheetVisible by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf(SupportedLanguage.entries.first()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    OnResume { recordings = ImportedRecordingStore.load(context) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        // AudioIngestion does blocking file I/O (copyTo) — same reasoning
        // as AudioDecoder in TranscriptionRunnerScreen: this callback runs
        // on the main thread, not a coroutine, so it must be pushed onto
        // Dispatchers.IO explicitly rather than called directly.
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
                recordings = imported + recordings
                ImportedRecordingStore.save(context, recordings)
            }
        }
    }

    fun delete(recording: ImportedRecording) {
        recordings = recordings.filterNot { it.id == recording.id }
        ImportedRecordingStore.save(context, recordings)
        File(recording.localPath).delete()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Audio Samples") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isLanguageSheetVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Import from Files")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No recordings yet. Tap + to import from Files.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(recordings, key = { it.id }) { recording ->
                    ListItem(
                        headlineContent = { Text(recording.originalFilename) },
                        supportingContent = {
                            Text("${recording.language.displayName} · ${formatDate(recording.importedAtEpochMillis)}")
                        },
                        trailingContent = {
                            IconButton(onClick = { delete(recording) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete")
                            }
                        },
                        modifier = Modifier.clickable { onRecordingClick(recording) },
                    )
                }
            }
        }
    }

    if (isLanguageSheetVisible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { isLanguageSheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("What language is this recording in?")
                for (language in SupportedLanguage.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == pendingLanguage,
                                onClick = { pendingLanguage = language },
                            )
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
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
