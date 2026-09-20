package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionary
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionaryStore
import com.ncubeeight.dejaentendu.studynotes.GlossarySource
import com.ncubeeight.dejaentendu.studynotes.GlossaryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The user's glossaries at a glance — every dictionary file they've
 * connected (each in its own language) plus the terms they typed in
 * themselves. Deliberately shows only the list of glossaries, not their
 * contents: a connected dictionary can hold tens of thousands of terms, so
 * tapping one opens GlossaryTermsScreen, where they can search it and pick
 * terms to send to Vocabulary. Looked up ahead of FlashcardGenerator on the
 * flashcard screen. Mirrors iOS's CustomGlossaryView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGlossaryScreen(
    onOpenGlossary: (dictionaryId: String?, title: String) -> Unit,
    onConnectDictionaryClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dictionaries by remember { mutableStateOf<List<ConnectedDictionary>>(emptyList()) }
    var termCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var manualCount by remember { mutableIntStateOf(0) }
    var hasLoaded by remember { mutableStateOf(false) }
    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isAddMenuVisible by remember { mutableStateOf(false) }
    var dictionaryToDisconnect by remember { mutableStateOf<ConnectedDictionary?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val enabledLanguages = remember(refreshTick) { AppSettingsStore.enabledLanguages(context) }

    // A connected dictionary can hold tens of thousands of terms, so the
    // counts are computed off the main thread.
    LaunchedEffect(refreshTick) {
        val (loadedDictionaries, counts, manual) = withContext(Dispatchers.IO) {
            val entries = GlossaryStore.load(context)
            val byDictionary = HashMap<String, Int>()
            var manualTerms = 0
            for (entry in entries) {
                if (entry.source == GlossarySource.MANUAL) manualTerms++
                entry.dictionaryId?.let { byDictionary[it] = (byDictionary[it] ?: 0) + 1 }
            }
            Triple(ConnectedDictionaryStore.load(context), byDictionary, manualTerms)
        }
        dictionaries = loadedDictionaries
        termCounts = counts
        manualCount = manual
        hasLoaded = true
    }
    OnResume { refreshTick++ }

    fun disconnect(dictionary: ConnectedDictionary) {
        scope.launch {
            withContext(Dispatchers.IO) {
                GlossaryStore.save(context, GlossaryStore.load(context).filterNot { it.dictionaryId == dictionary.id })
                ConnectedDictionaryStore.remove(context, dictionary.id)
                // The default Samples language follows the most recently
                // connected dictionary — fall back to the next one, or clear it.
                val latest = ConnectedDictionaryStore.load(context).firstOrNull()
                if (latest != null) AppSettingsStore.setPreferredDefaultLanguage(context, latest.language)
                else AppSettingsStore.clearPreferredDefaultLanguage(context)
            }
            refreshTick++
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Custom Glossary") }) },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { isAddMenuVisible = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
                AppDropdownMenu(expanded = isAddMenuVisible, onDismissRequest = { isAddMenuVisible = false }) {
                    DropdownMenuItem(
                        text = { Text("Connect Dictionary File") },
                        onClick = { isAddMenuVisible = false; onConnectDictionaryClick() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add Term") },
                        onClick = { isAddMenuVisible = false; isAddSheetVisible = true },
                    )
                }
            }
        },
    ) { padding ->
        if (hasLoaded && dictionaries.isEmpty() && manualCount == 0) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No glossaries yet", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Connect a dictionary file, or add a term with your own definition — it'll be shown on that word's flashcard instead of (or alongside) an on-device guess.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (dictionaries.isNotEmpty()) {
                    item(key = "header-dictionaries") {
                        Text(
                            "Connected Dictionaries",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(dictionaries, key = { it.id }) { dictionary ->
                        DictionaryRow(
                            dictionary = dictionary,
                            termCount = termCounts[dictionary.id] ?: dictionary.termCount,
                            isLanguageOn = dictionary.language in enabledLanguages,
                            onClick = { onOpenGlossary(dictionary.id, dictionary.language.displayName) },
                            onDisconnectRequest = { dictionaryToDisconnect = dictionary },
                        )
                    }
                    item(key = "footer-dictionaries") {
                        Text(
                            "Tap a dictionary to browse and search its terms. Turn a dictionary's language off and on in Settings under \"Languages shown on import\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                if (manualCount > 0) {
                    item(key = "manual") {
                        ListItem(
                            headlineContent = { Text("My Terms", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("$manualCount term${if (manualCount == 1) "" else "s"} you added") },
                            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                            modifier = Modifier.clickable { onOpenGlossary(null, "My Terms") },
                        )
                    }
                }
            }
        }
    }

    if (isAddSheetVisible) {
        AddGlossaryEntrySheet(
            onDismiss = { isAddSheetVisible = false },
            onSaved = {
                isAddSheetVisible = false
                refreshTick++
            },
        )
    }

    dictionaryToDisconnect?.let { dictionary ->
        val count = termCounts[dictionary.id] ?: dictionary.termCount
        AppAlertDialog(
            onDismissRequest = { dictionaryToDisconnect = null },
            title = { Text("Disconnect ${dictionary.language.displayName} dictionary?") },
            text = {
                Text("Removes its $count terms from your glossary. Terms you typed in yourself and terms already added to Vocabulary aren't affected.")
            },
            confirmButton = {
                TextButton(onClick = { dictionaryToDisconnect = null; disconnect(dictionary) }) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { dictionaryToDisconnect = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryRow(
    dictionary: ConnectedDictionary,
    termCount: Int,
    isLanguageOn: Boolean,
    onClick: () -> Unit,
    onDisconnectRequest: () -> Unit,
) {
    // Swiping asks for confirmation rather than disconnecting outright (it
    // deletes tens of thousands of terms), so the row always snaps back.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDisconnectRequest()
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Disconnect", tint = Color.White)
            }
        },
    ) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dictionary.language.displayName, fontWeight = FontWeight.SemiBold)
                    if (!isLanguageOn) {
                        Text(
                            "Off",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            },
            supportingContent = { Text("${dictionary.fileName} · $termCount terms", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
