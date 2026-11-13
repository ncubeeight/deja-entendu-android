package com.ncubeeight.dejaentendu.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.produceState
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.settings.AppColorScheme
import com.ncubeeight.dejaentendu.settings.AppFontScale
import com.ncubeeight.dejaentendu.settings.AppSettingsState
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.settings.SpeechPace
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionaryStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/** Mirrors iOS's SettingsView.swift: appearance, Custom Glossary, Connect Local Dictionary, then a searchable language filter. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onCustomGlossaryClick: () -> Unit, onConnectDictionaryClick: () -> Unit) {
    val context = LocalContext.current
    var colorScheme by remember { mutableStateOf(AppSettingsStore.colorScheme(context)) }
    val connectedDictionaries = remember { ConnectedDictionaryStore.load(context) }
    // This screen (unlike Home/Vocabulary/Flashcard) doesn't hardcode
    // AppColors on top of a hardcoded AppColors.background — it follows
    // MaterialTheme's live scheme, so its text must use MaterialTheme's
    // semantic colors too, or it goes dark-on-dark under the dark scheme.
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val fontScale = AppSettingsState.fontScale.value

    fun selectFontScale(scale: AppFontScale) {
        AppSettingsStore.setFontScale(context, scale)
        AppSettingsState.fontScale.value = scale
    }

    val speechPace = AppSettingsState.speechPace.value
    val previewTts = rememberTextToSpeech()

    fun selectColorScheme(scheme: AppColorScheme) {
        colorScheme = scheme
        AppSettingsStore.setColorScheme(context, scheme)
        AppSettingsState.colorScheme.value = scheme
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appearance", color = onSurfaceVariant)
                // One line at normal sizes; whole options wrap to the next
                // line (rather than breaking mid-word) at the largest text sizes.
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (scheme in AppColorScheme.entries) {
                        Row(
                            modifier = Modifier
                                .selectable(selected = scheme == colorScheme, onClick = { selectColorScheme(scheme) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = scheme == colorScheme, onClick = { selectColorScheme(scheme) })
                            Text(scheme.displayName, color = onBackground)
                        }
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Text Size", color = onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { selectFontScale(AppFontScale.entries[fontScale.ordinal - 1]) },
                        enabled = fontScale.ordinal > 0,
                    ) { Text("A−") }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fontScale.displayName, color = onBackground)
                        Text(fontScale.percentLabel, color = onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = { selectFontScale(AppFontScale.entries[fontScale.ordinal + 1]) },
                        enabled = fontScale.ordinal < AppFontScale.entries.lastIndex,
                    ) { Text("A+") }
                }
                Text(
                    "Makes text larger or smaller across the whole app — buttons, Home, Samples, Vocabulary, and flashcards. Applies on top of your phone's own font size.",
                    color = onSurfaceVariant,
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Speech Speed", color = onSurfaceVariant)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (pace in SpeechPace.entries) {
                        Row(
                            modifier = Modifier
                                .selectable(selected = pace == speechPace, onClick = { selectSpeechPace(context, pace) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = pace == speechPace, onClick = { selectSpeechPace(context, pace) })
                            Text(pace.displayName, color = onBackground)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        previewTts?.let { engine ->
                            engine.setLanguage(SupportedLanguage.ENGLISH.locale)
                            engine.applySpeechPace()
                            engine.speak("This is how fast words will be read aloud.", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    },
                    enabled = previewTts != null,
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text("Preview", modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    "Applies everywhere text is read aloud — Samples and Vocabulary flashcards. You can also change it from the speed icon next to any speak button.",
                    color = onSurfaceVariant,
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCustomGlossaryClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Book, contentDescription = null, tint = onBackground)
                    Text(
                        "Custom Glossary",
                        color = onBackground,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConnectDictionaryClick)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (connectedDictionaries.isNotEmpty()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2BBAA3))
                    } else {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = onBackground)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Connect Local Dictionary", color = onBackground)
                        if (connectedDictionaries.isNotEmpty()) {
                            Text(
                                connectedDictionaries.joinToString(", ") { it.language.displayName },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = onSurfaceVariant,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            )
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurfaceVariant)
                }

                Text(
                    "Add your own term definitions — useful for specialized vocabulary an on-device model might not know, and still works on devices without one. Connect a dictionary file from Files to bulk-import its terms instead of typing them in one at a time.",
                    color = onSurfaceVariant,
                )
            }

            HorizontalDivider()

            LanguageTogglesSection()

            AboutSection(onBackground, onSurfaceVariant)
        }
    }
}

/**
 * App version and the on-device model AICore is serving. Gemini Nano is
 * updated by Google Play on its own schedule (nano-v3 vs nano-v4, full vs
 * fast), so testers' reports can only be matched to a model if they can
 * see which one they had.
 */
@Composable
private fun AboutSection(onBackground: androidx.compose.ui.graphics.Color, onSurfaceVariant: androidx.compose.ui.graphics.Color) {
    val context = LocalContext.current
    val versionText = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    val modelName by produceState("Checking…") {
        value = try {
            com.google.mlkit.genai.prompt.Generation.getClient().getBaseModelName().ifBlank { "Not available" }
        } catch (e: Exception) {
            "Not available"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("About", color = onBackground, fontWeight = FontWeight.SemiBold)
        Text("Version $versionText", color = onSurfaceVariant)
        Text("On-device model: $modelName", color = onSurfaceVariant)
    }
}
