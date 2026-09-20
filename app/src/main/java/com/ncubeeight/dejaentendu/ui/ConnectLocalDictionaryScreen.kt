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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.ncubeeight.dejaentendu.transcription.CustomLanguageStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Connects a dictionary file the user already owns — picked from Files/
 * Downloads/a cloud provider — as an addition to the glossary, rather than
 * typing each term in by hand (AddGlossaryEntrySheet). Any number of
 * dictionaries can be connected; each keeps its own terms (tagged via
 * GlossaryEntry.dictionaryId) so it can be browsed or disconnected on its own
 * from the Custom Glossary page. The connected dictionary's language also
 * becomes the default pre-selected language for new Samples. If the file is
 * in a language the app doesn't list — Navajo, say — choosing "Other
 * language…" adds it as a new language that then appears in Settings'
 * "Languages shown on import" like any other, where it can be toggled on and
 * off. Mirrors iOS's ConnectLocalDictionaryView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectLocalDictionaryScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // null means "Other language…" — the name is typed into otherLanguageName.
    var choice by remember {
        mutableStateOf<SupportedLanguage?>(
            AppSettingsStore.preferredDefaultLanguage(context, AppSettingsStore.enabledLanguagesSorted(context))
        )
    }
    var otherLanguageName by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var parsedEntries by remember { mutableStateOf<List<DictionaryFileParser.ParsedEntry>>(emptyList()) }
    var importError by remember { mutableStateOf<String?>(null) }

    val trimmedOtherName = otherLanguageName.trim()

    // Every supported language, not just the ones currently shown on Samples
    // import — connecting a dictionary is often exactly how a language not
    // yet enabled gets turned on, so the picker shouldn't be limited to what's
    // already enabled.
    val allLanguagesSorted = remember { SupportedLanguage.entries.sortedBy { it.displayName } }

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
                importError = if (entries.isEmpty()) "Couldn't find any term/definition pairs in that file." else null
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

    // Terms the user typed by hand win over a same-named dictionary term.
    // Loaded once — the glossary can be large if dictionaries are connected.
    val manualKeys = remember {
        GlossaryStore.load(context)
            .filter { it.source == GlossarySource.MANUAL }
            .map { "${it.language.name}::${it.term.lowercase()}" }
            .toSet()
    }
    val duplicateCount = run {
        val language = choice ?: SupportedLanguage.entries.firstOrNull { it.displayName.equals(trimmedOtherName, ignoreCase = true) }
        if (language == null) 0 else parsedEntries.count { "${language.name}::${it.term.lowercase()}" in manualKeys }
    }

    fun connect() {
        val name = fileName ?: return
        val picked = choice
        if (picked == null && trimmedOtherName.isEmpty()) return
        isSaving = true
        scope.launch {
            withContext(Dispatchers.IO) {
                val language = picked ?: CustomLanguageStore.register(trimmedOtherName)
                val existing = GlossaryStore.load(context)
                val seenKeys = existing
                    .filter { it.source == GlossarySource.MANUAL }
                    .map { "${it.language.name}::${it.term.lowercase()}" }
                    .toMutableSet()
                val dictionaryId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val newEntries = ArrayList<GlossaryEntry>(parsedEntries.size)
                for (parsed in parsedEntries) {
                    val key = "${language.name}::${parsed.term.lowercase()}"
                    if (!seenKeys.add(key)) continue
                    newEntries.add(
                        GlossaryEntry(
                            term = parsed.term,
                            definition = parsed.definition,
                            language = language,
                            addedAtEpochMillis = now,
                            source = GlossarySource.CONNECTED_DICTIONARY,
                            dictionaryId = dictionaryId,
                        )
                    )
                }
                GlossaryStore.save(context, newEntries + existing)
                ConnectedDictionaryStore.add(
                    context,
                    ConnectedDictionary(
                        id = dictionaryId,
                        fileName = name,
                        language = language,
                        connectedAtEpochMillis = now,
                        termCount = newEntries.size,
                    ),
                )
                AppSettingsStore.setPreferredDefaultLanguage(context, language)

                // Show up as a Samples language immediately — connecting a
                // dictionary is often exactly how a language gets turned on
                // for the first time (or, for a custom one, created).
                AppSettingsStore.setEnabledLanguages(context, AppSettingsStore.enabledLanguages(context) + language)
            }
            onDone()
        }
    }

    val canConnect = parsedEntries.isNotEmpty() && !isParsing && !isSaving && (choice != null || trimmedOtherName.isNotEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Local Dictionary") },
                actions = {
                    TextButton(onClick = onDone, enabled = !isSaving) { Text("Cancel") }
                    TextButton(onClick = { connect() }, enabled = canConnect) { Text("Connect") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Every term in the file is tagged with this language. Once connected, it becomes the default for new Samples and is shown in Settings' language list, where you can turn it on and off. A language that isn't listed is added there for you.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Language", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth().selectable(selected = choice == null, onClick = { choice = null }).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = choice == null, onClick = { choice = null })
                Text("Other language…")
            }
            if (choice == null) {
                OutlinedTextField(
                    value = otherLanguageName,
                    onValueChange = { otherLanguageName = it },
                    label = { Text("Language name (e.g. Navajo)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider()
            for (option in allLanguagesSorted) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(selected = option == choice, onClick = { choice = option }).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == choice, onClick = { choice = option })
                    Text(option.displayName)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "text/csv", "text/tab-separated-values")) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(fileName ?: "Select Dictionary File", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "A PDF or plain-text dictionary — either a simple two-column word list, or a compiled dictionary with one entry per line.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            importError?.let { message ->
                Text("Couldn't import that file: $message", color = MaterialTheme.colorScheme.error)
            }

            if (isParsing || isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(if (isSaving) "Adding terms…" else "Parsing…")
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
}
