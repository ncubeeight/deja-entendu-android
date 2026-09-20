package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.settings.AppColorScheme
import com.ncubeeight.dejaentendu.settings.AppFontScale
import com.ncubeeight.dejaentendu.settings.AppSettingsState
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionaryStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/** Mirrors iOS's SettingsView.swift: appearance, Custom Glossary, Connect Local Dictionary, then a searchable language filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onCustomGlossaryClick: () -> Unit, onConnectDictionaryClick: () -> Unit) {
    val context = LocalContext.current
    var enabledLanguages by remember { mutableStateOf(AppSettingsStore.enabledLanguages(context)) }
    var colorScheme by remember { mutableStateOf(AppSettingsStore.colorScheme(context)) }
    var languageSearchText by remember { mutableStateOf("") }
    val connectedDictionary = remember { ConnectedDictionaryStore.load(context) }
    // This screen (unlike Home/Vocabulary/Flashcard) doesn't hardcode
    // AppColors on top of a hardcoded AppColors.background — it follows
    // MaterialTheme's live scheme, so its text must use MaterialTheme's
    // semantic colors too, or it goes dark-on-dark under the dark scheme.
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val filteredLanguages = remember(languageSearchText) {
        val sorted = SupportedLanguage.entries.sortedBy { it.displayName }
        if (languageSearchText.isBlank()) sorted
        else sorted.filter { it.displayName.contains(languageSearchText, ignoreCase = true) }
    }

    fun setLanguageEnabled(language: SupportedLanguage, isOn: Boolean) {
        val updated = enabledLanguages.toMutableSet()
        if (isOn) updated.add(language) else updated.remove(language)
        // Never let the picker go empty — same guard as iOS's SettingsView.
        if (updated.isEmpty()) return
        enabledLanguages = updated
        AppSettingsStore.setEnabledLanguages(context, updated)
    }

    val fontScale = AppSettingsState.fontScale.value

    fun selectFontScale(scale: AppFontScale) {
        AppSettingsStore.setFontScale(context, scale)
        AppSettingsState.fontScale.value = scale
    }

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
                for (scheme in AppColorScheme.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = scheme == colorScheme, onClick = { selectColorScheme(scheme) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = scheme == colorScheme, onClick = { selectColorScheme(scheme) })
                        Text(scheme.displayName, color = onBackground)
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
                    "Makes text larger across the whole app — buttons, Home, Samples, Vocabulary, and flashcards. Applies on top of your phone's own font size.",
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
                    if (connectedDictionary != null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2BBAA3))
                    } else {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = onBackground)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Connect Local Dictionary", color = onBackground)
                        if (connectedDictionary != null) {
                            Text(
                                "${connectedDictionary.fileName} · ${connectedDictionary.language.displayName}",
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Languages shown on import", color = onSurfaceVariant)
                OutlinedTextField(
                    value = languageSearchText,
                    onValueChange = { languageSearchText = it },
                    placeholder = { Text("Search Languages") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                if (filteredLanguages.isEmpty()) {
                    Text("No languages match \"$languageSearchText\".", color = onSurfaceVariant)
                } else {
                    for (language in filteredLanguages) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(language.displayName, color = onBackground)
                                if (connectedDictionary?.language == language) {
                                    Text("Connected dictionary", color = onSurfaceVariant, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                }
                            }
                            Switch(
                                checked = language in enabledLanguages,
                                onCheckedChange = { setLanguageEnabled(language, it) },
                            )
                        }
                    }
                }
                Text(
                    "Turn off languages you don't use to simplify the picker. At least one must stay on.",
                    color = onSurfaceVariant,
                )
            }
        }
    }
}
