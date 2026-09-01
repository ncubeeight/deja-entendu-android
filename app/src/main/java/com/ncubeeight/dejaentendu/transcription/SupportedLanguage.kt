package com.ncubeeight.dejaentendu.transcription

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Which OCR recognizer (ML Kit Text Recognition v2) a language's script
 * needs — null on SupportedLanguage means no recognizer exists for that
 * script at all, so photo-scan import isn't offered for it.
 */
enum class OcrScript { LATIN, CHINESE, JAPANESE, KOREAN, DEVANAGARI }

/**
 * Languages the transcription pipeline can detect and transcribe, mirroring
 * iOS's SupportedLanguage.swift (now 27 languages there) plus Czech, which
 * Gemini Nano supports but Apple Intelligence doesn't. Adding a language
 * means adding a case here — pickers, prompts, and transcription should all
 * read from this list.
 *
 * Coverage is uneven across two independent on-device capabilities, both
 * verified against Google's real ML Kit docs (2026-08-31), not assumed:
 * [speechRecognitionLocale] (ML Kit GenAI Speech Recognition, Advanced/
 * Gemini-Nano tier, this app's target) and [ocrScript] (ML Kit Text
 * Recognition v2, whose script coverage is separate from and doesn't line
 * up with speech coverage — e.g. Norwegian has OCR but no speech
 * recognition, Russian has speech but no OCR). Either being null just
 * means that one import path isn't offered for the language; typed text
 * and "Generate Sample" always work since they only need the LLM.
 *
 * @Serializable on a parameterized enum only serializes the constant name
 * (e.g. "JAPANESE") — the constructor properties below aren't touched by
 * kotlinx.serialization's default enum handling, so this is safe as-is.
 */
@Serializable
enum class SupportedLanguage(
    val displayName: String,
    val locale: Locale,
    val speechRecognitionLocale: Locale?,
    val ocrScript: OcrScript?,
) {
    CHINESE_TRADITIONAL(
        "Chinese (Traditional)",
        Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("TW").build(),
        // ML Kit GenAI Speech Recognition's supported-locale list uses the
        // "cmn" (Mandarin) macrolanguage tag, not "zh" — different from the
        // locale tag used elsewhere (ICU segmentation, Nano prompts).
        Locale.forLanguageTag("cmn-Hant-TW"),
        OcrScript.CHINESE,
    ),
    CHINESE_SIMPLIFIED(
        "Chinese (Simplified)",
        Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build(),
        Locale.forLanguageTag("cmn-Hans-CN"),
        OcrScript.CHINESE,
    ),
    JAPANESE("Japanese", Locale.JAPAN, Locale.forLanguageTag("ja-JP"), OcrScript.JAPANESE),
    GERMAN("German", Locale.GERMANY, Locale.forLanguageTag("de-DE"), OcrScript.LATIN),
    FRENCH("French", Locale.FRANCE, Locale.forLanguageTag("fr-FR"), OcrScript.LATIN),
    SPANISH("Spanish", Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es-ES"), OcrScript.LATIN),
    THAI("Thai", Locale.forLanguageTag("th-TH"), Locale.forLanguageTag("th-TH"), null),
    KOREAN("Korean", Locale.KOREA, Locale.forLanguageTag("ko-KR"), OcrScript.KOREAN),
    VIETNAMESE("Vietnamese", Locale.forLanguageTag("vi-VN"), Locale.forLanguageTag("vi-VN"), OcrScript.LATIN),
    HINDI("Hindi", Locale.forLanguageTag("hi-IN"), Locale.forLanguageTag("hi-IN"), OcrScript.DEVANAGARI),
    TAMIL("Tamil", Locale.forLanguageTag("ta-IN"), null, null),
    GUJARATI("Gujarati", Locale.forLanguageTag("gu-IN"), null, null),
    ITALIAN("Italian", Locale.ITALY, Locale.forLanguageTag("it-IT"), OcrScript.LATIN),
    PORTUGUESE("Portuguese", Locale.forLanguageTag("pt-PT"), Locale.forLanguageTag("pt-PT"), OcrScript.LATIN),
    DANISH("Danish", Locale.forLanguageTag("da-DK"), Locale.forLanguageTag("da-DK"), OcrScript.LATIN),
    DUTCH("Dutch", Locale.forLanguageTag("nl-NL"), Locale.forLanguageTag("nl-NL"), OcrScript.LATIN),
    // No GenAI Speech Recognition entry in either tier as of this
    // language's addition — OCR (Latin) still works.
    NORWEGIAN("Norwegian", Locale.forLanguageTag("nb-NO"), null, OcrScript.LATIN),
    SWEDISH("Swedish", Locale.forLanguageTag("sv-SE"), Locale.forLanguageTag("sv-SE"), OcrScript.LATIN),
    TURKISH("Turkish", Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("tr-TR"), OcrScript.LATIN),
    ENGLISH("English", Locale.US, Locale.forLanguageTag("en-US"), OcrScript.LATIN),
    BENGALI("Bengali", Locale.forLanguageTag("bn-IN"), null, null),
    PUNJABI("Punjabi", Locale.forLanguageTag("pa-IN"), null, null),
    URDU("Urdu", Locale.forLanguageTag("ur-PK"), null, null),
    GREEK("Greek", Locale.forLanguageTag("el-GR"), null, null),
    HEBREW("Hebrew", Locale.forLanguageTag("he-IL"), null, null),
    // Speech recognition works (Advanced tier), but Text Recognition v2 has
    // no Cyrillic-script recognizer — OCR isn't offered for it.
    RUSSIAN("Russian", Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("ru-RU"), null),
    UKRAINIAN("Ukrainian", Locale.forLanguageTag("uk-UA"), null, null),
    // Not in iOS's list — Gemini Nano supports Czech, Apple Intelligence
    // doesn't. No GenAI Speech Recognition entry; OCR works (Latin script).
    CZECH("Czech", Locale.forLanguageTag("cs-CZ"), null, OcrScript.LATIN),
}
