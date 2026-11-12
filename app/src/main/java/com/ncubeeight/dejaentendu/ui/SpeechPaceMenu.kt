package com.ncubeeight.dejaentendu.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.ncubeeight.dejaentendu.settings.AppSettingsState
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.settings.SpeechPace

/**
 * Material has no tortoise/hare (iOS uses those SF Symbols), so walking /
 * gauge / running stand in as the speed hint.
 */
val SpeechPace.icon: ImageVector
    get() = when (this) {
        SpeechPace.SLOWER -> Icons.AutoMirrored.Filled.DirectionsWalk
        SpeechPace.NORMAL -> Icons.Filled.Speed
        SpeechPace.FASTER -> Icons.AutoMirrored.Filled.DirectionsRun
    }

/** Persists the choice and updates the live state every SpeechPaceMenu and speak button reads. */
fun selectSpeechPace(context: Context, pace: SpeechPace) {
    AppSettingsStore.setSpeechPace(context, pace)
    AppSettingsState.speechPace.value = pace
}

/**
 * Applies the current global pace right before speaking — read fresh each
 * time so a change made in Settings or a SpeechPaceMenu takes effect on the
 * very next tap.
 */
fun TextToSpeech.applySpeechPace() {
    setSpeechRate(AppSettingsState.speechPace.value.rate)
}

/**
 * The speed icon shown beside speak buttons. It hints at the current speech
 * speed and, when tapped, lets the user change it in place — the same
 * global setting as the Speech Speed section in Settings. Mirrors iOS's
 * SpeechPaceMenu.swift.
 */
@Composable
fun SpeechPaceMenu(enabled: Boolean = true) {
    val context = LocalContext.current
    val pace = AppSettingsState.speechPace.value
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(pace.icon, contentDescription = "Speech speed: ${pace.displayName}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (option in SpeechPace.entries) {
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    trailingIcon = if (option == pace) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                    } else null,
                    onClick = {
                        selectSpeechPace(context, option)
                        expanded = false
                    },
                )
            }
        }
    }
}
