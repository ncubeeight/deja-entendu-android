package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoStories
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncubeeight.dejaentendu.audio.ImportedRecordingStore
import com.ncubeeight.dejaentendu.samples.AnySample
import com.ncubeeight.dejaentendu.samples.ImportedImageSampleStore
import com.ncubeeight.dejaentendu.samples.ImportedTextSampleStore
import com.ncubeeight.dejaentendu.samples.id
import com.ncubeeight.dejaentendu.samples.importedAtEpochMillis
import com.ncubeeight.dejaentendu.samples.kind
import com.ncubeeight.dejaentendu.samples.subtitle
import com.ncubeeight.dejaentendu.samples.title
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

private const val WORD_GRID_COLUMNS = 3

/**
 * Home tab: a quick-glance summary, mirroring iOS's HomeSummaryView.swift
 * — a gradient banner, recent recordings ("Continue studying"), and a
 * vocabulary preview grid ("Words to review"), with placeholder words
 * shown only until the user has real vocabulary. Tapping a placeholder
 * word adds it to the real Vocabulary list and opens its flashcard —
 * a working example of the interaction, not just static decoration —
 * so once any of them (or a real word) has been added, this section
 * switches over to showing actual vocabulary instead.
 */
@Composable
fun HomeScreen(
    onSampleClick: (AnySample) -> Unit,
    onEntryClick: (VocabularyEntry) -> Unit,
    onImportRecordingClick: () -> Unit,
    onExampleInteractionClick: () -> Unit,
) {
    val context = LocalContext.current
    var audioRecordings by remember { mutableStateOf(ImportedRecordingStore.load(context)) }
    var textSamples by remember { mutableStateOf(ImportedTextSampleStore.load(context)) }
    var imageSamples by remember { mutableStateOf(ImportedImageSampleStore.load(context)) }
    var vocabulary by remember { mutableStateOf(VocabularyStore.load(context)) }
    var isAddSheetVisible by remember { mutableStateOf(false) }
    var isAddWordVisible by remember { mutableStateOf(false) }
    var isTextImportVisible by remember { mutableStateOf(false) }
    var isImageImportVisible by remember { mutableStateOf(false) }

    OnResume {
        audioRecordings = ImportedRecordingStore.load(context)
        textSamples = ImportedTextSampleStore.load(context)
        imageSamples = ImportedImageSampleStore.load(context)
        vocabulary = VocabularyStore.load(context)
    }

    val samples = (audioRecordings.map(AnySample::Audio) + textSamples.map(AnySample::Text) + imageSamples.map(AnySample::Image))
        .sortedByDescending { it.importedAtEpochMillis }

    fun deleteSample(sample: AnySample) {
        when (sample) {
            is AnySample.Audio -> {
                audioRecordings = audioRecordings.filterNot { it.id == sample.recording.id }
                ImportedRecordingStore.save(context, audioRecordings)
                File(sample.recording.localPath).delete()
            }
            is AnySample.Text -> {
                textSamples = textSamples.filterNot { it.id == sample.sample.id }
                ImportedTextSampleStore.save(context, textSamples)
            }
            is AnySample.Image -> {
                imageSamples = imageSamples.filterNot { it.id == sample.sample.id }
                ImportedImageSampleStore.save(context, imageSamples)
                File(sample.sample.localPath).delete()
            }
        }
    }

    fun deleteVocabulary(entry: VocabularyEntry) {
        vocabulary = vocabulary.filterNot { it.id == entry.id }
        VocabularyStore.save(context, vocabulary)
    }

    fun addPlaceholderAsVocabulary(item: PlaceholderWord) {
        val newEntry = VocabularyEntry(
            text = item.word,
            addedAtEpochMillis = System.currentTimeMillis(),
            language = item.language,
        )
        VocabularyStore.save(context, listOf(newEntry) + VocabularyStore.load(context))
        onEntryClick(newEntry)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExampleInteractionClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoStories, contentDescription = null, tint = AppColors.coral, modifier = Modifier.size(20.dp))
            Text(
                "Example interaction",
                color = AppColors.coral,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        TitleBanner()

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            ContinueStudyingSection(samples, onSampleClick, ::deleteSample)
            WordsToReviewSection(
                vocabulary = vocabulary,
                onEntryClick = onEntryClick,
                onDeleteEntry = ::deleteVocabulary,
                onPlaceholderClick = ::addPlaceholderAsVocabulary,
                onAddClick = { isAddSheetVisible = true },
            )
        }
    }

    if (isAddSheetVisible) {
        AlertDialog(
            onDismissRequest = { isAddSheetVisible = false },
            title = { Text("Add to Déjà Entendu") },
            text = {
                Column {
                    TextButton(onClick = {
                        isAddSheetVisible = false
                        onImportRecordingClick()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Import a Recording") }
                    TextButton(onClick = {
                        isAddSheetVisible = false
                        isAddWordVisible = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add a Word") }
                    TextButton(onClick = {
                        isAddSheetVisible = false
                        isTextImportVisible = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add Text") }
                    TextButton(onClick = {
                        isAddSheetVisible = false
                        isImageImportVisible = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Scan Photo") }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAddSheetVisible = false }) { Text("Cancel") }
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

    if (isTextImportVisible) {
        TextImportSheet(
            onDismiss = { isTextImportVisible = false },
            onSaved = {
                isTextImportVisible = false
                textSamples = ImportedTextSampleStore.load(context)
            },
        )
    }

    if (isImageImportVisible) {
        ImageImportSheet(
            onDismiss = { isImageImportVisible = false },
            onSaved = {
                isImageImportVisible = false
                imageSamples = ImportedImageSampleStore.load(context)
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
    samples: List<AnySample>,
    onSampleClick: (AnySample) -> Unit,
    onDelete: (AnySample) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Continue studying", color = AppColors.ink, fontWeight = FontWeight.Bold)

        if (samples.isEmpty()) {
            Text(
                "Recordings, text, and photos you import will show up here once they're saved between launches.",
                color = AppColors.inkSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (sample in samples.take(3)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surface, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(sample.kind.tintSoft, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(sample.kind.icon, contentDescription = null, tint = sample.kind.tint, modifier = Modifier.size(16.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f).clickable { onSampleClick(sample) }.padding(start = 12.dp),
                        ) {
                            Text(sample.title, color = AppColors.ink, fontWeight = FontWeight.SemiBold)
                            Text(sample.subtitle, color = AppColors.inkSoft)
                        }
                        IconButton(onClick = { onDelete(sample) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", tint = AppColors.inkSoft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordsToReviewSection(
    vocabulary: List<VocabularyEntry>,
    onEntryClick: (VocabularyEntry) -> Unit,
    onDeleteEntry: (VocabularyEntry) -> Unit,
    onPlaceholderClick: (PlaceholderWord) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Words to review", color = AppColors.ink, fontWeight = FontWeight.Bold)

        if (vocabulary.isEmpty()) {
            WordGrid {
                for (item in placeholderWords) {
                    cell {
                        WordCard(
                            word = item.word,
                            translation = item.gloss,
                            languageName = item.language.displayName,
                            onClick = { onPlaceholderClick(item) },
                        )
                    }
                }
                cell { AddWordCard(onAddClick) }
            }
            Text(
                "These are just examples — tap one to see how a flashcard works, or add your own.",
                color = AppColors.inkSoft,
            )
        } else {
            WordGrid {
                for (entry in vocabulary.take(9)) {
                    cell {
                        Box {
                            WordCard(
                                word = entry.text,
                                translation = entry.translation,
                                languageName = entry.language?.displayName,
                                onClick = { onEntryClick(entry) },
                            )
                            IconButton(
                                onClick = { onDeleteEntry(entry) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete", tint = AppColors.inkSoft)
                            }
                        }
                    }
                }
                cell { AddWordCard(onAddClick) }
            }
        }
    }
}

private class WordGridScope {
    val cells = mutableListOf<@Composable () -> Unit>()
    fun cell(content: @Composable () -> Unit) {
        cells.add(content)
    }
}

/**
 * A fixed-3-column grid of equal-width, equal-height cells — the Compose
 * equivalent of iOS's `LazyVGrid(columns: [GridItem(.adaptive(minimum:
 * 110))])`. A real adaptive/lazy grid isn't worth it here: the item count
 * is always small (at most 10), so a manual chunk-into-rows-of-3 avoids
 * the ceremony of nesting a LazyVerticalGrid inside an already-scrollable
 * Column.
 */
@Composable
private fun WordGrid(content: WordGridScope.() -> Unit) {
    val scope = WordGridScope().apply(content)
    for (row in scope.cells.chunked(WORD_GRID_COLUMNS)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (cellContent in row) {
                Box(modifier = Modifier.weight(1f)) { cellContent() }
            }
            repeat(WORD_GRID_COLUMNS - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.heightIn(min = 10.dp))
    }
}

/**
 * Term / translation / language, each on its own row, in a uniformly
 * shaped box regardless of how much of that a given entry actually has
 * (translation is null until a flashcard's been generated once; language
 * is null for manually-typed words) — matches the iOS home screen's tile
 * style while degrading gracefully for real, not-yet-complete entries.
 */
@Composable
private fun WordCard(word: String, translation: String?, languageName: String?, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            word,
            color = AppColors.ink,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (translation != null) {
            Text(
                translation,
                color = AppColors.inkSoft,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (languageName != null) {
            Text(
                languageName,
                color = AppColors.inkSoft,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddWordCard(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .background(AppColors.coralSoft, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = AppColors.coral)
        Text("Add your own", color = AppColors.inkSoft, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
