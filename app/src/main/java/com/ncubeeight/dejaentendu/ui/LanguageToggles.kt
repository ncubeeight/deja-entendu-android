package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.studynotes.ConnectedDictionaryStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/**
 * The searchable list of language toggles, shared by Settings and the
 * "Add or Remove Languages" sheet every Sample language picker opens
 * (mirrors iOS's LanguageTogglesSection, 31b7268).
 */
@Composable
fun LanguageTogglesSection(onChanged: (Set<SupportedLanguage>) -> Unit = {}) {
    val context = LocalContext.current
    var enabledLanguages by remember { mutableStateOf(AppSettingsStore.enabledLanguages(context)) }
    var searchText by remember { mutableStateOf("") }
    val connectedDictionaries = remember { ConnectedDictionaryStore.load(context) }
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val filteredLanguages = remember(searchText) {
        val sorted = SupportedLanguage.entries.sortedBy { it.displayName }
        if (searchText.isBlank()) sorted else sorted.filter { it.displayName.contains(searchText, ignoreCase = true) }
    }

    fun setLanguageEnabled(language: SupportedLanguage, isOn: Boolean) {
        val updated = enabledLanguages.toMutableSet()
        if (isOn) updated.add(language) else updated.remove(language)
        // Never let the picker go empty — same guard as iOS's SettingsView.
        if (updated.isEmpty()) return
        enabledLanguages = updated
        AppSettingsStore.setEnabledLanguages(context, updated)
        onChanged(updated)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Languages shown on import", color = onSurfaceVariant)
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search Languages") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (filteredLanguages.isEmpty()) {
            Text("No languages match \"$searchText\".", color = onSurfaceVariant)
        } else {
            for (language in filteredLanguages) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(language.displayName, color = onBackground)
                        if (connectedDictionaries.any { it.language == language }) {
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
            "Turn off languages you don't use to simplify the picker. At least one must stay on. A language added by connecting a dictionary appears here too — turning it off hides it from the picker and pauses that dictionary's definitions without disconnecting it.",
            color = onSurfaceVariant,
        )
    }
}

/**
 * "Add or Remove Languages" — shown in every Sample language picker so new
 * users find the toggles without going to Settings.
 */
@Composable
fun ManageLanguagesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Add or Remove Languages")
    }
}

/** The language toggles in a full-height sheet over the picker that opened it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLanguagesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add or Remove Languages",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            LanguageTogglesSection()
        }
    }
}
