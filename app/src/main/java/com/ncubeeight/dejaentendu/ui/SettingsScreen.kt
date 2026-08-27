package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ncubeeight.dejaentendu.settings.AppColorScheme
import com.ncubeeight.dejaentendu.settings.AppSettingsState
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/** Mirrors iOS's SettingsView.swift: language filter + appearance picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var enabledLanguages by remember { mutableStateOf(AppSettingsStore.enabledLanguages(context)) }
    var colorScheme by remember { mutableStateOf(AppSettingsStore.colorScheme(context)) }
    // This screen (unlike Home/Vocabulary/Flashcard) doesn't hardcode
    // AppColors on top of a hardcoded AppColors.background — it follows
    // MaterialTheme's live scheme, so its text must use MaterialTheme's
    // semantic colors too, or it goes dark-on-dark under the dark scheme.
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    fun setLanguageEnabled(language: SupportedLanguage, isOn: Boolean) {
        val updated = enabledLanguages.toMutableSet()
        if (isOn) updated.add(language) else updated.remove(language)
        // Never let the picker go empty — same guard as iOS's SettingsView.
        if (updated.isEmpty()) return
        enabledLanguages = updated
        AppSettingsStore.setEnabledLanguages(context, updated)
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
                Text("Languages shown on import", color = onSurfaceVariant)
                for (language in SupportedLanguage.entries) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(language.displayName, color = onBackground, modifier = Modifier.weight(1f))
                        Switch(
                            checked = language in enabledLanguages,
                            onCheckedChange = { setLanguageEnabled(language, it) },
                        )
                    }
                }
                Text(
                    "Turn off languages you don't use to simplify the picker. At least one must stay on.",
                    color = onSurfaceVariant,
                )
            }

            HorizontalDivider()

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
        }
    }
}
