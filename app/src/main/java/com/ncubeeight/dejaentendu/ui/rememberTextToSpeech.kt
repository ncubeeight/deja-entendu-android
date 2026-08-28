package com.ncubeeight.dejaentendu.ui

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Wraps android.speech.tts.TextToSpeech for the flashcard's speak buttons —
 * the Android analog of iOS's AVSpeechSynthesizer (VocabularyFlashcardView.
 * swift). TextToSpeech's constructor initializes asynchronously, so this
 * returns null until it's actually ready to use.
 */
@Composable
fun rememberTextToSpeech(): TextToSpeech? {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts = engine
            }
        }
        onDispose {
            tts = null
            engine?.stop()
            engine?.shutdown()
        }
    }

    return tts
}
