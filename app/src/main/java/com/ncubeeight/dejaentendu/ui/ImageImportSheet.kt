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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.samples.ImageIngestion
import com.ncubeeight.dejaentendu.samples.ImportedImageSample
import com.ncubeeight.dejaentendu.samples.ImportedImageSampleStore
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Photo -> text import: pick a photo from the library, run it through
 * on-device OCR (ImageIngestion), then let the user correct any
 * recognition mistakes before saving. Mirrors iOS's ImageImportView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageImportSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabledLanguages = remember {
        SupportedLanguage.entries.filter { it in AppSettingsStore.enabledLanguages(context) }
    }
    var language by remember { mutableStateOf(enabledLanguages.first()) }
    var isProcessing by remember { mutableStateOf(false) }
    var ingestedSample by remember { mutableStateOf<ImportedImageSample?>(null) }
    var editableText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        importError = null
        scope.launch {
            try {
                val sample = withContext(Dispatchers.IO) {
                    ImageIngestion.copyAndRecognizeText(context, uri, language)
                }
                ingestedSample = sample
                editableText = sample.recognizedText
            } catch (e: Exception) {
                importError = e.message ?: e.toString()
            } finally {
                isProcessing = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding().padding(20.dp)) {
            Text("Scan Photo")

            Text("Language", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            for (option in enabledLanguages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == language,
                            onClick = { if (ingestedSample == null) language = option },
                            enabled = ingestedSample == null,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == language,
                        onClick = { if (ingestedSample == null) language = option },
                        enabled = ingestedSample == null,
                    )
                    Text(option.displayName)
                }
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch(PickVisualMediaRequestImagesOnly) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(if (ingestedSample == null) "Choose Photo" else "Choose a Different Photo")
            }

            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text("Recognizing text…")
                }
            }

            importError?.let { message ->
                Text("Import failed: $message", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }

            if (ingestedSample != null) {
                Text(
                    "Recognized text",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    placeholder = { Text("Correct anything the scan got wrong before saving.") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(top = 4.dp),
                )
            }

            Column(modifier = Modifier.padding(top = 20.dp)) {
                Button(
                    onClick = {
                        val sample = ingestedSample ?: return@Button
                        val trimmed = editableText.trim()
                        if (trimmed.isNotEmpty()) {
                            val entries = ImportedImageSampleStore.load(context)
                            val updated = listOf(sample.copy(recognizedText = trimmed)) + entries
                            ImportedImageSampleStore.save(context, updated)
                            onSaved()
                        }
                    },
                    enabled = ingestedSample != null && editableText.trim().isNotEmpty(),
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

private val PickVisualMediaRequestImagesOnly =
    androidx.activity.result.PickVisualMediaRequest(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
    )
