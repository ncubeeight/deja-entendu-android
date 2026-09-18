package com.ncubeeight.dejaentendu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.samples.TextFileImportException
import com.ncubeeight.dejaentendu.samples.TextFileImporter
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionary
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionaryStore
import com.ncubeeight.dejaentendu.studynotes.DictionaryFileParser
import com.ncubeeight.dejaentendu.studynotes.GlossaryEntry
import com.ncubeeight.dejaentendu.studynotes.GlossarySource
import com.ncubeeight.dejaentendu.studynotes.GlossaryStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Connects a dictionary file the user already owns — picked from Files/
 * Downloads/a cloud provider — as an addition to the glossary, rather
 * than typing each term in by hand (AddGlossaryEntrySheet). Only one
 * local dictionary is connected at a time: connecting a new file
 * replaces the terms the previous one contributed (tracked via
 * GlossarySource) without touching anything the user typed in
 * themselves. The connected dictionary's language also becomes the
 * default pre-selected language for new Samples going forward. Mirrors
 * iOS's ConnectLocalDictionaryView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectLocalDictionaryScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(ConnectedDictionaryStore.load(context)) }
    var isReplacing by remember { mutableStateOf(false) }

    var enabledLanguages by remember { mutableStateOf(AppSettingsStore.enabledLanguagesSorted(context)) }
    var language by remember {
        mutableStateOf(connected?.language ?: AppSettingsStore.preferredDefaultLanguage(context, enabledLanguages))
    }
    var fileName by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var parsedEntries by remember { mutableStateOf<List<DictionaryFileParser.ParsedEntry>>(emptyList()) }
    var duplicateCount by remember { mutableStateOf(0) }
    var importError by remember { mutableStateOf<String?>(null) }

    fun recomputeDuplicates() {
        val manualKeys = GlossaryStore.load(context)
            .filter { it.source == GlossarySource.MANUAL }
            .map { "${it.language.name}::${it.term.lowercase()}" }
            .toSet()
        duplicateCount = parsedEntries.count { "${language.name}::${it.term.lowercase()}" in manualKeys }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        fileName = TextFileImporter.displayName(context, uri)
        parsedEntries = emptyList()
        isParsing = true
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { TextFileImporter.extractText(context, uri) }
                val entries = withContext(Dispatchers.Default) { DictionaryFileParser.parse(text) }
                parsedEntries = entries
                isParsing = false
                recomputeDuplicates()
                if (entries.isEmpty()) {
                    importError = "Couldn't find any term/definition pairs in that file."
                }
            } catch (e: TextFileImportException) {
                importError = e.message
                fileName = null
                isParsing = false
            } catch (e: Exception) {
                importError = e.message ?: e.toString()
                fileName = null
                isParsing = false
            }
        }
    }

    fun connect() {
        val name = fileName ?: return
        var existing = GlossaryStore.load(context).filterNot { it.source == GlossarySource.CONNECTED_DICTIONARY }
        val seenKeys = existing.map { "${it.language.name}::${it.term.lowercase()}" }.toMutableSet()
        val newEntries = mutableListOf<GlossaryEntry>()
        for (parsed in parsedEntries) {
            val key = "${language.name}::${parsed.term.lowercase()}"
            if (key in seenKeys) continue
            seenKeys.add(key)
            newEntries.add(
                GlossaryEntry(
                    term = parsed.term,
                    definition = parsed.definition,
                    language = language,
                    addedAtEpochMillis = System.currentTimeMillis(),
                    source = GlossarySource.CONNECTED_DICTIONARY,
                )
            )
        }
        existing = newEntries + existing
        GlossaryStore.save(context, existing)

        val connectedCount = existing.count { it.source == GlossarySource.CONNECTED_DICTIONARY }
        ConnectedDictionaryStore.save(
            context,
            ConnectedDictionary(fileName = name, language = language, connectedAtEpochMillis = System.currentTimeMillis(), termCount = connectedCount),
        )
        AppSettingsStore.setPreferredDefaultLanguage(context, language)

        // Show up as a Samples language immediately — connecting a
        // dictionary is often exactly how a language gets turned on for
        // the first time.
        val updatedEnabled = AppSettingsStore.enabledLanguages(context) + language
        AppSettingsStore.setEnabledLanguages(context, updatedEnabled)
        enabledLanguages = AppSettingsStore.enabledLanguagesSorted(context)

        connected = ConnectedDictionaryStore.load(context)
        isReplacing = false
        fileName = null
        parsedEntries = emptyList()
    }

    fun disconnect() {
        val existing = GlossaryStore.load(context).filterNot { it.source == GlossarySource.CONNECTED_DICTIONARY }
        GlossaryStore.save(context, existing)
        ConnectedDictionaryStore.clear(context)
        AppSettingsStore.clearPreferredDefaultLanguage(context)
        connected = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Local Dictionary") },
                actions = {
                    val current = connected
                    if (current == null || isReplacing) {
                        if (current != null) {
                            TextButton(onClick = { isReplacing = false }) { Text("Cancel") }
                        }
                        TextButton(onClick = { connect() }, enabled = parsedEntries.isNotEmpty() && !isParsing) { Text("Connect") }
                    } else {
                        TextButton(onClick = onDone) { Text("Done") }
                    }
                },
            )
        },
    ) { padding ->
        val current = connected
        if (current != null && !isReplacing) {
            ConnectedSummary(
                connected = current,
                enabledLanguages = enabledLanguages,
                onReplace = {
                    fileName = null
                    parsedEntries = emptyList()
                    isReplacing = true
                },
                onDisconnect = { disconnect() },
                onToggleSamplesVisibility = { isOn ->
                    val enabled = AppSettingsStore.enabledLanguages(context).toMutableSet()
                    if (isOn) {
                        enabled.add(current.language)
                    } else if (enabled.size > 1) {
                        enabled.remove(current.language)
                    }
                    AppSettingsStore.setEnabledLanguages(context, enabled)
                    enabledLanguages = AppSettingsStore.enabledLanguagesSorted(context)
                },
                modifier = Modifier.padding(padding),
            )
        } else {
            ConnectForm(
                language = language,
                onLanguageChange = { language = it; recomputeDuplicates() },
                fileName = fileName,
                isParsing = isParsing,
                parsedEntries = parsedEntries,
                duplicateCount = duplicateCount,
                importError = importError,
                onSelectFile = {
                    filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "text/csv", "text/tab-separated-values"))
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ConnectedSummary(
    connected: ConnectedDictionary,
    enabledLanguages: List<SupportedLanguage>,
    onReplace: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleSamplesVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault()) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LabeledRow("File", connected.fileName)
            LabeledRow("Language", connected.language.displayName)
            LabeledRow("Terms", connected.termCount.toString())
            LabeledRow("Connected", formatter.format(Instant.ofEpochMilli(connected.connectedAtEpochMillis)))
            Text(
                "This dictionary's terms are in your Custom Glossary, and ${connected.language.displayName} is now the default language suggested for new Samples.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show as a Samples Language")
                Text(
                    "Lets you pick ${connected.language.displayName} when adding a new audio, text, or photo Sample. Turning this off hides the language from Samples without disconnecting the dictionary.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = connected.language in enabledLanguages, onCheckedChange = onToggleSamplesVisibility)
        }

        HorizontalDivider()

        OutlinedButton(onClick = onReplace, modifier = Modifier.fillMaxWidth()) { Text("Connect a Different File") }
        OutlinedButton(
            onClick = onDisconnect,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Disconnect") }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun ConnectForm(
    language: SupportedLanguage,
    onLanguageChange: (SupportedLanguage) -> Unit,
    fileName: String?,
    isParsing: Boolean,
    parsedEntries: List<DictionaryFileParser.ParsedEntry>,
    duplicateCount: Int,
    importError: String?,
    onSelectFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every supported language, not just the ones currently shown on
    // Samples import — connecting a dictionary is often exactly how a
    // language not yet enabled gets turned on, so the picker shouldn't be
    // limited to what's already enabled.
    val allLanguagesSorted = remember { SupportedLanguage.entries.sortedBy { it.displayName } }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Every term in the file is tagged with this language.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Language", color = MaterialTheme.colorScheme.onSurfaceVariant)
        for (option in allLanguagesSorted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == language, onClick = { onLanguageChange(option) })
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == language, onClick = { onLanguageChange(option) })
                Text(option.displayName)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        OutlinedButton(onClick = onSelectFile, modifier = Modifier.fillMaxWidth()) {
            Text(fileName ?: "Select Dictionary File")
        }
        Text(
            "A PDF or plain-text dictionary — either a simple two-column word list, or a compiled dictionary with one entry per line.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        importError?.let { message ->
            Text("Couldn't import that file: $message", color = MaterialTheme.colorScheme.error)
        }

        if (isParsing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text("Parsing…")
            }
        } else if (fileName != null) {
            Text("${parsedEntries.size} term${if (parsedEntries.size == 1) "" else "s"} found")
            if (duplicateCount > 0) {
                Text(
                    "$duplicateCount already in your glossary as a manual entry, will be kept as-is",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (parsedEntries.isNotEmpty()) {
                Text("Preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    for (entry in parsedEntries.take(5)) {
                        Column {
                            Text(entry.term, fontWeight = FontWeight.SemiBold)
                            Text(
                                entry.definition,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (parsedEntries.size > 5) {
                        Text("+ ${parsedEntries.size - 5} more", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
