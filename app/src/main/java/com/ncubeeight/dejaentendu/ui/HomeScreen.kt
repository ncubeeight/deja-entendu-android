package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.audio.ImportedRecordingStore
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import com.ncubeeight.dejaentendu.ui.theme.AppColors
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private data class PlaceholderWord(val language: SupportedLanguage, val word: String, val gloss: String)

private val placeholderWords = listOf(
    PlaceholderWord(SupportedLanguage.CHINESE_TRADITIONAL, "謝謝", "thank you"),
    PlaceholderWord(SupportedLanguage.CHINESE_SIMPLIFIED, "你好", "hello"),
    PlaceholderWord(SupportedLanguage.JAPANESE, "こんにちは", "hello"),
    PlaceholderWord(SupportedLanguage.GERMAN, "Danke", "thank you"),
    PlaceholderWord(SupportedLanguage.FRENCH, "Bonjour", "hello"),
)

/**
 * Home tab: a quick-glance summary, mirroring iOS's HomeSummaryView.swift
 * — a gradient banner, recent recordings ("Continue studying"), and a
 * vocabulary preview grid ("Words to review"), with placeholder words
 * shown only until the user has real vocabulary.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onRecordingClick: (ImportedRecording) -> Unit,
    onEntryClick: (VocabularyEntry) -> Unit,
    onImportRecordingClick: () -> Unit,
) {
    val context = LocalContext.current
    var recordings by remember { mutableStateOf(ImportedRecordingStore.load(context)) }
    var vocabulary by remember { mutableStateOf(VocabularyStore.load(context)) }
    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isAddWordVisible by remember { mutableStateOf(false) }

    OnResume {
        recordings = ImportedRecordingStore.load(context)
        vocabulary = VocabularyStore.load(context)
    }

    fun deleteRecording(recording: ImportedRecording) {
        recordings = recordings.filterNot { it.id == recording.id }
        ImportedRecordingStore.save(context, recordings)
        File(recording.localPath).delete()
    }

    fun deleteVocabulary(entry: VocabularyEntry) {
        vocabulary = vocabulary.filterNot { it.id == entry.id }
        VocabularyStore.save(context, vocabulary)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TitleBanner()

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            ContinueStudyingSection(recordings, onRecordingClick, ::deleteRecording)
            WordsToReviewSection(vocabulary, onEntryClick, ::deleteVocabulary, onAddClick = { isAddSheetVisible = true })
        }
    }

    if (isAddSheetVisible) {
        AlertDialog(
            onDismissRequest = { isAddSheetVisible = false },
            title = { Text("Add to Déjà Entendu") },
            text = { Text("Import a recording, or add a word manually.") },
            confirmButton = {
                TextButton(onClick = {
                    isAddSheetVisible = false
                    onImportRecordingClick()
                }) { Text("Import a Recording") }
            },
            dismissButton = {
                TextButton(onClick = {
                    isAddSheetVisible = false
                    isAddWordVisible = true
                }) { Text("Add a Word") }
            },
        )
    }

    if (isAddWordVisible) {
        AddVocabularyWordSheet(
            onDismiss = { isAddWordVisible = false },
            onSaved = {
                isAddWordVisible = false
                vocabulary = VocabularyStore.load(context)
            },
        )
    }
}

@Composable
private fun TitleBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = listOf(AppColors.headerGradientStart, AppColors.headerGradientEnd)))
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Déjà Entendu",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "You heard it before.\nLet's try to remember it.",
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContinueStudyingSection(
    recordings: List<ImportedRecording>,
    onRecordingClick: (ImportedRecording) -> Unit,
    onDelete: (ImportedRecording) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Continue studying", color = AppColors.ink, fontWeight = FontWeight.Bold)

        if (recordings.isEmpty()) {
            Text(
                "Recordings you import will show up here once they're saved between launches.",
                color = AppColors.inkSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (recording in recordings.take(3)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surface, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).clickable { onRecordingClick(recording) },
                        ) {
                            Text(recording.originalFilename, color = AppColors.ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${recording.language.displayName} · ${formatDate(recording.importedAtEpochMillis)}",
                                color = AppColors.inkSoft,
                            )
                        }
                        IconButton(onClick = { onDelete(recording) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", tint = AppColors.inkSoft)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsToReviewSection(
    vocabulary: List<VocabularyEntry>,
    onEntryClick: (VocabularyEntry) -> Unit,
    onDelete: (VocabularyEntry) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Words to review", color = AppColors.ink, fontWeight = FontWeight.Bold)

        if (vocabulary.isEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (item in placeholderWords) {
                    WordCard(word = item.word, subtitle = "${item.gloss} · ${item.language.displayName}")
                }
                AddWordCard(onAddClick)
            }
            Text(
                "These are just examples — add your own to replace them.",
                color = AppColors.inkSoft,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (entry in vocabulary.take(9)) {
                    Box {
                        WordCard(word = entry.text, subtitle = null, onClick = { onEntryClick(entry) })
                        IconButton(
                            onClick = { onDelete(entry) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", tint = AppColors.inkSoft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(word: String, subtitle: String?, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Text(word, color = AppColors.ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, color = AppColors.inkSoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AddWordCard(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(AppColors.coralSoft, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = AppColors.coral)
        Text("Add your own", color = AppColors.inkSoft)
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
