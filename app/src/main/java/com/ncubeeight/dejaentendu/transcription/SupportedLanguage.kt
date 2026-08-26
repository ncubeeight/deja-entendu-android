package com.ncubeeight.dejaentendu.transcription

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Languages the app supports, mirroring iOS's SupportedLanguage.swift.
 * Adding a language means adding a case here — pickers, prompts, and
 * transcription should all read from this list.
 *
 * @Serializable on a parameterized enum only serializes the constant name
 * (e.g. "JAPANESE") — the constructor properties below aren't touched by
 * kotlinx.serialization's default enum handling, so this is safe as-is.
 */
@Serializable
enum class SupportedLanguage(val displayName: String, val locale: Locale, val speechRecognitionLocale: Locale) {
    CHINESE_TRADITIONAL(
        "Chinese (Traditional)",
        Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("TW").build(),
        // ML Kit GenAI Speech Recognition's supported-locale list uses the
        // "cmn" (Mandarin) macrolanguage tag, not "zh" — different from the
        // locale tag used elsewhere (ICU segmentation, Nano prompts).
        Locale.forLanguageTag("cmn-Hant-TW"),
    ),
    CHINESE_SIMPLIFIED(
        "Chinese (Simplified)",
        Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build(),
        Locale.forLanguageTag("cmn-Hans-CN"),
    ),
    JAPANESE("Japanese", Locale.JAPAN, Locale.forLanguageTag("ja-JP")),
    GERMAN("German", Locale.GERMANY, Locale.forLanguageTag("de-DE")),
    FRENCH("French", Locale.FRANCE, Locale.forLanguageTag("fr-FR")),
}
