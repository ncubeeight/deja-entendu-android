package com.ncubeeight.dejaentendu.ui

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/**
 * Reads an imported text or photo sample aloud with the device's TextToSpeech
 * in the sample's language — the same engine and language handling the
 * flashcard's speak button uses — one sample at a time, tapping the sample
 * again (or another) stops it. Text is split into chunks because TextToSpeech
 * rejects input longer than getMaxSpeechInputLength().
 */
class SampleSpeaker internal constructor(
    private val tts: TextToSpeech?,
    private val speakingIdState: () -> String?,
    private val setSpeakingId: (String?) -> Unit,
) {
    val speakingId: String? get() = speakingIdState()

    /** False when there's no engine yet or the device has no voice for the language. */
    fun canSpeak(language: SupportedLanguage): Boolean {
        val engine = tts ?: return false
        return engine.isLanguageAvailable(language.locale) >= TextToSpeech.LANG_AVAILABLE
    }

    fun toggle(id: String, text: String, language: SupportedLanguage) {
        val engine = tts ?: return
        if (speakingId == id) {
            engine.stop()
            setSpeakingId(null)
            return
        }
        engine.stop()
        engine.setLanguage(language.locale)
        setSpeakingId(id)
        val chunks = chunk(text, (TextToSpeech.getMaxSpeechInputLength() - 100).coerceAtLeast(500))
        chunks.forEachIndexed { index, part ->
            val utteranceId = if (index == chunks.lastIndex) "$id$LAST" else "$id$MORE$index"
            engine.speak(part, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    private fun chunk(text: String, limit: Int): List<String> {
        val cleaned = text.trim()
        if (cleaned.length <= limit) return listOf(cleaned)
        val parts = ArrayList<String>()
        var rest = cleaned
        while (rest.length > limit) {
            val window = rest.substring(0, limit)
            val cut = maxOf(window.lastIndexOf('\n'), window.lastIndexOf(". "), window.lastIndexOf('。'), window.lastIndexOf(' ')).takeIf { it > limit / 2 } ?: limit
            parts.add(rest.substring(0, cut + 1).trim())
            rest = rest.substring(cut + 1)
        }
        if (rest.isNotBlank()) parts.add(rest.trim())
        return parts
    }

    internal companion object {
        const val LAST = "#last"
        const val MORE = "#"
    }
}

@Composable
fun rememberSampleSpeaker(): SampleSpeaker {
    val tts = rememberTextToSpeech()
    var speakingId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(tts) {
        val main = Handler(Looper.getMainLooper())
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            // Only the final chunk's completion (or any failure) ends the
            // "speaking" state — and only if it's still this sample's, so an
            // older sample being stopped doesn't clear a newer one.
            override fun onDone(utteranceId: String?) {
                if (utteranceId?.endsWith(SampleSpeaker.LAST) == true) {
                    val id = utteranceId.removeSuffix(SampleSpeaker.LAST)
                    main.post { if (speakingId == id) speakingId = null }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                val id = utteranceId?.substringBefore(SampleSpeaker.MORE)
                main.post { if (speakingId == id) speakingId = null }
            }
        })
        onDispose { speakingId = null }
    }

    return remember(tts) { SampleSpeaker(tts, { speakingId }, { speakingId = it }) }
}
