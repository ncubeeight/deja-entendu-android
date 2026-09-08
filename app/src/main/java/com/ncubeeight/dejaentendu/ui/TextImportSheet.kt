package com.ncubeeight.dejaentendu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.samples.ImportedTextSample
import com.ncubeeight.dejaentendu.samples.ImportedTextSampleStore
import com.ncubeeight.dejaentendu.samples.TextFileImportException
import com.ncubeeight.dejaentendu.samples.TextFileImporter
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manual entry point for a text sample — the Samples-tab counterpart to
 * AddVocabularyWordSheet, but for a whole passage. Saves straight into
 * ImportedTextSampleStore and dismisses. Mirrors iOS's TextImportView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextImportSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Typed text needs neither speech recognition nor OCR, so every
    // enabled language is offered here, unlike audio/photo import.
    val enabledLanguages = remember { AppSettingsStore.enabledLanguagesSorted(context) }
    var body by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(enabledLanguages.first()) }
    var isImportingFile by remember { mutableStateOf(false) }
    var fileImportError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingFile = true
        fileImportError = null
        scope.launch {
            try {
                body = withContext(Dispatchers.IO) { TextFileImporter.extractText(context, uri) }
            } catch (e: TextFileImportException) {
                fileImportError = e.message
            } catch (e: Exception) {
                fileImportError = e.message ?: e.toString()
            } finally {
                isImportingFile = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding().padding(20.dp)) {
            Text("Add Text")
            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain")) },
                enabled = !isImportingFile,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (isImportingFile) "Extracting text…" else "Select PDF or Text File")
            }
            Text(
                "Text is extracted and filled in below, so you can review or edit it before saving.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            fileImportError?.let { message ->
                Text(
                    "Couldn't import that file: $message",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Text") },
                placeholder = { Text("Paste or type a passage in the language you're studying.") },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(top = 12.dp, bottom = 12.dp),
            )
            Text("Language", color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (option in enabledLanguages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = option == language, onClick = { language = option })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == language, onClick = { language = option })
                    Text(option.displayName)
                }
            }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = {
                        val trimmed = body.trim()
                        if (trimmed.isNotEmpty()) {
                            val entries = ImportedTextSampleStore.load(context)
                            val updated = listOf(
                                ImportedTextSample(
                                    body = trimmed,
                                    importedAtEpochMillis = System.currentTimeMillis(),
                                    language = language,
                                )
                            ) + entries
                            ImportedTextSampleStore.save(context, updated)
                            onSaved()
                        }
                    },
                    enabled = body.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
