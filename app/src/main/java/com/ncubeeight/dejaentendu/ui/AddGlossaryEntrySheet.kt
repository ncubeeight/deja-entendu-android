package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.studynotes.GlossaryEntry
import com.ncubeeight.dejaentendu.studynotes.GlossaryStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/**
 * Add-a-term sheet for the Custom Glossary — term, language (from the
 * user's enabled languages), and a definition shown as-is on that term's
 * flashcard. Mirrors iOS's AddGlossaryEntryView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGlossaryEntrySheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val enabledLanguages = remember { AppSettingsStore.enabledLanguagesSorted(context) }
    var term by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(enabledLanguages.firstOrNull() ?: SupportedLanguage.entries.first()) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(enabledLanguages) {
        if (language !in enabledLanguages) {
            enabledLanguages.firstOrNull()?.let { language = it }
        }
    }

    fun save() {
        val trimmedTerm = term.trim()
        val trimmedDefinition = definition.trim()
        if (trimmedTerm.isEmpty() || trimmedDefinition.isEmpty()) return

        val entries = GlossaryStore.load(context)
        val updated = listOf(
            GlossaryEntry(
                term = trimmedTerm,
                definition = trimmedDefinition,
                language = language,
                addedAtEpochMillis = System.currentTimeMillis(),
            )
        ) + entries
        GlossaryStore.save(context, updated)
        onSaved()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Add Term")
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text("Term") },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            )

            Text("Language", color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (option in enabledLanguages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = option == language, onClick = { language = option })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == language, onClick = { language = option })
                    Text(option.displayName)
                }
            }

            Text(
                "Your Definition",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                placeholder = { Text("Shown on this term's flashcard as-is — nothing is generated or altered.") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(top = 8.dp, bottom = 20.dp),
            )

            Button(
                onClick = { save() },
                enabled = term.trim().isNotEmpty() && definition.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
