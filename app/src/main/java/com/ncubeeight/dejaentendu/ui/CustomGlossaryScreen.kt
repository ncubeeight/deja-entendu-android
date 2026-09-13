package com.ncubeeight.dejaentendu.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.studynotes.GlossaryEntry
import com.ncubeeight.dejaentendu.studynotes.GlossaryStore

/**
 * Lets the user manage their own term/definition pairs — specialized
 * vocabulary an on-device model might not know, or just a personal
 * glossary they want treated as authoritative. Looked up ahead of
 * FlashcardGenerator on the flashcard screen. Mirrors iOS's
 * CustomGlossaryView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGlossaryScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(GlossaryStore.load(context)) }
    var isAddSheetVisible by remember { mutableStateOf(false) }

    OnResume { entries = GlossaryStore.load(context) }

    fun delete(entry: GlossaryEntry) {
        entries = entries.filterNot { it.id == entry.id }
        GlossaryStore.save(context, entries)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Custom Glossary") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddSheetVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add a term")
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
                    Text("No custom terms yet")
                    Text(
                        "Add a term and your own definition — it'll be shown on that word's flashcard instead of (or alongside) an on-device guess.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(entry.term, fontWeight = FontWeight.SemiBold)
                                Text(
                                    entry.language.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        supportingContent = { Text(entry.definition) },
                        trailingContent = {
                            IconButton(onClick = { delete(entry) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete")
                            }
                        },
                    )
                }
            }
        }
    }

    if (isAddSheetVisible) {
        AddGlossaryEntrySheet(
            onDismiss = { isAddSheetVisible = false },
            onSaved = {
                isAddSheetVisible = false
                entries = GlossaryStore.load(context)
            },
        )
    }
}
