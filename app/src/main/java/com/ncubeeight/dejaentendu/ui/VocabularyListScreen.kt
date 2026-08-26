package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Terms added via a tapped transcript word or manual entry. Mirrors iOS's
 * VocabularyListView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyListScreen(onEntryClick: (VocabularyEntry) -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(VocabularyStore.load(context)) }
    var isAddWordVisible by remember { mutableStateOf(false) }

    OnResume { entries = VocabularyStore.load(context) }

    fun delete(entry: VocabularyEntry) {
        entries = entries.filterNot { it.id == entry.id }
        VocabularyStore.save(context, entries)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vocabulary") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddWordVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add a word")
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("No vocabulary yet")
                    Text("Tap a word in a transcript, or add one manually, to see it here.")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.text) },
                        supportingContent = { Text(formatDate(entry.addedAtEpochMillis)) },
                        trailingContent = {
                            IconButton(onClick = { delete(entry) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete")
                            }
                        },
                        modifier = Modifier.clickable { onEntryClick(entry) },
                    )
                }
            }
        }
    }

    if (isAddWordVisible) {
        AddVocabularyWordSheet(
            onDismiss = { isAddWordVisible = false },
            onSaved = {
                isAddWordVisible = false
                entries = VocabularyStore.load(context)
            },
        )
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
