package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.studynotes.GlossaryEntry
import com.ncubeeight.dejaentendu.studynotes.GlossarySource
import com.ncubeeight.dejaentendu.studynotes.GlossaryStore
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Browses one glossary's terms — a connected dictionary's, or the user's own
 * ([dictionaryId] null). Searchable across terms and definitions (a
 * connected dictionary can be far too long to scroll A–Z), and terms can be
 * selected and added to Vocabulary, where each becomes a flashcard that
 * shows this glossary's definition. Mirrors iOS's GlossaryTermsView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryTermsScreen(dictionaryId: String?, title: String) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var entries by remember { mutableStateOf<List<GlossaryEntry>>(emptyList()) }
    var visibleEntries by remember { mutableStateOf<List<GlossaryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var isSelecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var vocabularyKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMessage by remember { mutableStateOf<String?>(null) }

    fun key(term: String, language: SupportedLanguage?) = "${language?.name.orEmpty()}::${term.lowercase()}"

    // A Vocabulary term with no language recorded still counts as the same
    // term, so it isn't added a second time.
    fun isInVocabulary(term: String, language: SupportedLanguage, keys: Set<String>) =
        key(term, language) in keys || key(term, null) in keys

    LaunchedEffect(dictionaryId) {
        val loaded = withContext(Dispatchers.IO) {
            GlossaryStore.load(context)
                .filter { if (dictionaryId == null) it.source == GlossarySource.MANUAL else it.dictionaryId == dictionaryId }
                .sortedBy { it.term.lowercase() }
        }
        entries = loaded
        visibleEntries = loaded
        vocabularyKeys = withContext(Dispatchers.IO) { VocabularyStore.load(context).map { key(it.text, it.language) }.toSet() }
        isLoading = false
    }

    // Debounced so typing in a very large dictionary doesn't refilter on
    // every keystroke, and filtered off the main thread.
    LaunchedEffect(searchText, entries) {
        val query = searchText.trim()
        if (query.isEmpty()) {
            visibleEntries = entries
            return@LaunchedEffect
        }
        delay(200)
        visibleEntries = withContext(Dispatchers.Default) {
            entries.filter { it.term.contains(query, ignoreCase = true) || it.definition.contains(query, ignoreCase = true) }
        }
    }

    LaunchedEffect(pendingMessage) {
        pendingMessage?.let {
            snackbarHostState.showSnackbar(it)
            pendingMessage = null
        }
    }

    fun addToVocabulary(chosen: List<GlossaryEntry>) {
        val vocabulary = VocabularyStore.load(context)
        val existingKeys = vocabulary.map { key(it.text, it.language) }.toMutableSet()
        val additions = ArrayList<VocabularyEntry>()
        for (entry in chosen) {
            if (isInVocabulary(entry.term, entry.language, existingKeys)) continue
            existingKeys.add(key(entry.term, entry.language))
            additions.add(VocabularyEntry(text = entry.term, addedAtEpochMillis = System.currentTimeMillis(), language = entry.language))
        }
        VocabularyStore.save(context, additions + vocabulary)
        vocabularyKeys = existingKeys
        val skipped = chosen.size - additions.size
        pendingMessage = if (additions.isEmpty()) {
            "${if (chosen.size == 1) "That term is" else "Those terms are"} already in your Vocabulary."
        } else {
            "Added ${additions.size} term${if (additions.size == 1) "" else "s"} to Vocabulary." +
                if (skipped > 0) " $skipped already there." else ""
        }
    }

    fun delete(entry: GlossaryEntry) {
        GlossaryStore.save(context, GlossaryStore.load(context).filterNot { it.id == entry.id })
        entries = entries.filterNot { it.id == entry.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    TextButton(
                        onClick = {
                            isSelecting = !isSelecting
                            if (!isSelecting) selection = emptySet()
                        },
                        enabled = entries.isNotEmpty(),
                    ) { Text(if (isSelecting) "Done" else "Select") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isSelecting && selection.isNotEmpty()) {
                Button(
                    onClick = {
                        addToVocabulary(entries.filter { it.id in selection })
                        selection = emptySet()
                        isSelecting = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("Add ${selection.size} to Vocabulary") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search terms and definitions") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    visibleEntries.isEmpty() -> Text(
                        if (searchText.isBlank()) "No terms" else "No results for \"${searchText.trim()}\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visibleEntries, key = { it.id }) { entry ->
                            TermRow(
                                entry = entry,
                                isInVocabulary = isInVocabulary(entry.term, entry.language, vocabularyKeys),
                                isSelecting = isSelecting,
                                isSelected = entry.id in selection,
                                onToggleSelected = {
                                    selection = if (entry.id in selection) selection - entry.id else selection + entry.id
                                },
                                onAddToVocabulary = { addToVocabulary(listOf(entry)) },
                                onDelete = { delete(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermRow(
    entry: GlossaryEntry,
    isInVocabulary: Boolean,
    isSelecting: Boolean,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onAddToVocabulary: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isSelecting,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(enabled = isSelecting, onClick = onToggleSelected)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelecting) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.term, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.definition,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!isSelecting) {
                if (isInVocabulary) {
                    Icon(Icons.Filled.Check, contentDescription = "In Vocabulary", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    IconButton(onClick = onAddToVocabulary) {
                        Icon(Icons.Filled.LibraryAdd, contentDescription = "Add to Vocabulary")
                    }
                }
            }
        }
    }
}
