package com.ncubeeight.dejaentendu.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Mirrors iOS's VocabularyListView's sort menu — entries are kept sorted in place. */
private enum class VocabularySort(val label: String) {
    DATE_NEWEST_FIRST("Date (Newest First)"),
    DATE_OLDEST_FIRST("Date (Oldest First)"),
    LANGUAGE("Language"),
    TITLE("Title");

    fun sort(entries: List<VocabularyEntry>): List<VocabularyEntry> = when (this) {
        DATE_NEWEST_FIRST -> entries.sortedByDescending { it.addedAtEpochMillis }
        DATE_OLDEST_FIRST -> entries.sortedBy { it.addedAtEpochMillis }
        LANGUAGE -> entries.sortedBy { it.language?.displayName.orEmpty().lowercase() }
        TITLE -> entries.sortedBy { it.text.lowercase() }
    }
}

/**
 * Terms added via a tapped transcript word or manual entry. Mirrors iOS's
 * VocabularyListView.swift, including its sort menu, share button, and
 * swipe-to-delete (no Share Extension inbox on Android — see project scope).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyListScreen(onEntryClick: (VocabularyEntry) -> Unit) {
    val context = LocalContext.current
    var sortOption by remember { mutableStateOf(VocabularySort.DATE_NEWEST_FIRST) }
    var entries by remember { mutableStateOf(sortOption.sort(VocabularyStore.load(context))) }
    var isAddWordVisible by remember { mutableStateOf(false) }
    var isSortMenuVisible by remember { mutableStateOf(false) }

    OnResume { entries = sortOption.sort(VocabularyStore.load(context)) }

    fun delete(entry: VocabularyEntry) {
        entries = entries.filterNot { it.id == entry.id }
        VocabularyStore.save(context, entries)
    }

    fun setSortOption(option: VocabularySort) {
        sortOption = option
        entries = option.sort(entries)
        isSortMenuVisible = false
    }

    fun share() {
        val lines = entries.map { entry ->
            val subtitle = listOfNotNull(entry.translation, entry.language?.displayName).joinToString(" · ")
            if (subtitle.isEmpty()) entry.text else "${entry.text} — $subtitle"
        }
        val shareText = (lines + listOf("", "Shared from Déjà Entendu on Android")).joinToString("\n")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "My Déjà Entendu vocabulary")
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary") },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { isSortMenuVisible = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        AppDropdownMenu(expanded = isSortMenuVisible, onDismissRequest = { isSortMenuVisible = false }) {
                            for (option in VocabularySort.entries) {
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = { RadioButton(selected = option == sortOption, onClick = { setSortOption(option) }) },
                                    onClick = { setSortOption(option) },
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { share() }, enabled = entries.isNotEmpty()) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
            )
        },
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
                    VocabularyRow(entry, onEntryClick, ::delete)
                }
            }
        }
    }

    if (isAddWordVisible) {
        AddVocabularyWordSheet(
            onDismiss = { isAddWordVisible = false },
            onSaved = {
                isAddWordVisible = false
                entries = sortOption.sort(VocabularyStore.load(context))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyRow(entry: VocabularyEntry, onEntryClick: (VocabularyEntry) -> Unit, onDelete: (VocabularyEntry) -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(entry)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) {
        ListItem(
            headlineContent = { Text(entry.text) },
            supportingContent = { Text(formatDate(entry.addedAtEpochMillis)) },
            modifier = Modifier.clickable { onEntryClick(entry) },
        )
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
