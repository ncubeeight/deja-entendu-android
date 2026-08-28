package com.ncubeeight.dejaentendu.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncubeeight.dejaentendu.studynotes.FlashcardDetails
import com.ncubeeight.dejaentendu.studynotes.FlashcardGenerator
import com.ncubeeight.dejaentendu.studynotes.VocabularyEntry
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore
import com.ncubeeight.dejaentendu.ui.theme.AppColors

private sealed interface FlashcardStatus {
    data object Loading : FlashcardStatus
    data class Ready(val details: FlashcardDetails) : FlashcardStatus
    data class Failed(val reason: String) : FlashcardStatus
}

/**
 * A dedicated page for one vocabulary term: pronunciation, translation,
 * and an example sentence with the term highlighted. Generated on-device
 * the first time this opens, then cached on the entry so reopening it is
 * instant. Mirrors iOS's VocabularyFlashcardView.swift, including its two
 * speak buttons (term + example sentence) — Android's TextToSpeech in
 * place of AVSpeechSynthesizer.
 */
@Composable
fun VocabularyFlashcardScreen(entry: VocabularyEntry) {
    val context = LocalContext.current
    val tts = rememberTextToSpeech()

    // Only meaningful when the entry's language is known — without it we'd
    // fall back to the device's default TTS voice, which is available but
    // not reliably correct, so that case counts as "available" rather than
    // blocked. Same reasoning as iOS's isVoiceAvailable.
    val isSpeechAvailable = remember(tts, entry.language) {
        val engine = tts
        val language = entry.language
        when {
            engine == null -> false
            language == null -> true
            else -> engine.isLanguageAvailable(language.locale) >= TextToSpeech.LANG_AVAILABLE
        }
    }

    fun speak(text: String) {
        val engine = tts ?: return
        entry.language?.let { engine.setLanguage(it.locale) }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    var status by remember(entry.id) {
        mutableStateOf(
            if (entry.hasFlashcardDetails) {
                FlashcardStatus.Ready(
                    FlashcardDetails(
                        pronunciation = entry.pronunciation!!,
                        translation = entry.translation!!,
                        exampleSentence = entry.exampleSentence!!,
                        exampleTranslation = entry.exampleTranslation.orEmpty(),
                    )
                )
            } else {
                FlashcardStatus.Loading
            }
        )
    }

    LaunchedEffect(entry.id) {
        if (entry.hasFlashcardDetails) return@LaunchedEffect
        status = try {
            val details = FlashcardGenerator.generateDetails(entry.text, entry.language)
            val allEntries = VocabularyStore.load(context)
            val updated = allEntries.map {
                if (it.id == entry.id) {
                    it.copy(
                        pronunciation = details.pronunciation,
                        translation = details.translation,
                        exampleSentence = details.exampleSentence,
                        exampleTranslation = details.exampleTranslation,
                    )
                } else it
            }
            VocabularyStore.save(context, updated)
            FlashcardStatus.Ready(details)
        } catch (e: Exception) {
            FlashcardStatus.Failed(e.message ?: e.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(entry.text, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = AppColors.ink)
                IconButton(onClick = { speak(entry.text) }, enabled = isSpeechAvailable) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak")
                }
            }
            if (!isSpeechAvailable) {
                Text(
                    "Spoken pronunciation isn't available for ${entry.language?.displayName ?: "this term"} on this device.",
                    color = AppColors.inkSoft,
                )
            }
        }

        when (val current = status) {
            is FlashcardStatus.Loading -> CircularProgressIndicator()
            is FlashcardStatus.Failed -> Text(current.reason, color = AppColors.inkSoft)
            is FlashcardStatus.Ready -> {
                LabeledValue("Pronunciation", current.details.pronunciation)
                LabeledValue("Translation", current.details.translation)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Example", color = AppColors.inkSoft)
                        IconButton(
                            onClick = { speak(current.details.exampleSentence) },
                            enabled = isSpeechAvailable,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak example")
                        }
                    }
                    Text(highlightedExample(current.details.exampleSentence, entry.text))
                    Text(current.details.exampleTranslation, color = AppColors.inkSoft)
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AppColors.inkSoft)
        Text(value, fontSize = 20.sp, color = AppColors.ink)
    }
}

/**
 * Bolds/colors the first case-insensitive occurrence of [term] inside
 * [sentence]. If the model conjugated or otherwise didn't reuse the term
 * verbatim, this just falls back to plain, unhighlighted text — same
 * fallback as iOS's AttributedString-based version.
 */
@Composable
private fun highlightedExample(sentence: String, term: String): AnnotatedString {
    val index = if (term.isEmpty()) -1 else sentence.indexOf(term, ignoreCase = true)
    if (index < 0) return AnnotatedString(sentence)

    return buildAnnotatedString {
        append(sentence.substring(0, index))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
            append(sentence.substring(index, index + term.length))
        }
        append(sentence.substring(index + term.length))
    }
}
