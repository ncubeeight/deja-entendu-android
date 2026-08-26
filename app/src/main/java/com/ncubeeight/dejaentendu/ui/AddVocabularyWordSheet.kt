package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore

/**
 * Manual entry point for the vocabulary list — the counterpart to sharing
 * text in (not yet built on Android), mirroring iOS's
 * AddVocabularyWordView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVocabularyWordSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Add a Word")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Word or phrase") },
                // English autocorrect/auto-capitalization can corrupt input in
                // the other 5 supported languages — same reasoning as iOS's
                // .autocorrectionDisabled()/.textInputAutocapitalization(.never).
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    capitalization = KeyboardCapitalization.None,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            )
            Column {
                Button(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            val entries = VocabularyStore.load(context)
                            val updated = listOf(
                                VocabularyEntry(text = trimmed, addedAtEpochMillis = System.currentTimeMillis())
                            ) + entries
                            VocabularyStore.save(context, updated)
                            onSaved()
                        }
                    },
                    enabled = text.trim().isNotEmpty(),
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
}
