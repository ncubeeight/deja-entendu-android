package com.ncubeeight.dejaentendu.transcription

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale

/**
 * Which OCR recognizer (ML Kit Text Recognition v2) a language's script
 * needs — null on SupportedLanguage means no recognizer exists for that
 * script at all, so photo-scan import isn't offered for it.
 */
enum class OcrScript { LATIN, CHINESE, JAPANESE, KOREAN, DEVANAGARI }

/**
 * Languages the transcription pipeline can detect and transcribe, mirroring
 * iOS's SupportedLanguage.swift (40 languages there as of 2026-09) plus Czech, which
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
/**
 * A language the app can process. The built-ins below are what ML Kit's
 * speech/OCR/LLM coverage was checked against; the user can also add their
 * own by connecting a dictionary in a language that isn't listed (Navajo,
 * say — see CustomLanguageStore), which then appears in Settings like any
 * other. Custom languages have no speech, OCR, or locale support — they
 * work for typed text, glossary lookups, and flashcards.
 *
 * This used to be an enum. It's a class now so the set of languages can grow
 * at runtime, but it deliberately keeps the enum's surface (`entries`,
 * `name`, constants like [FRENCH]) so call sites didn't need to change, and
 * serializes as the same bare name string so previously saved files still
 * decode.
 */
@Serializable(with = SupportedLanguage.Serializer::class)
class SupportedLanguage private constructor(
    val name: String,
    val displayName: String,
    val locale: Locale,
    val speechRecognitionLocale: Locale?,
    val ocrScript: OcrScript?,
) {
    val isCustom: Boolean get() = name.startsWith(CustomLanguageStore.PREFIX)

    override fun equals(other: Any?) = other is SupportedLanguage && other.name == name
    override fun hashCode() = name.hashCode()
    override fun toString() = name

    object Serializer : KSerializer<SupportedLanguage> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SupportedLanguage", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: SupportedLanguage) = encoder.encodeString(value.name)

        /** Doesn't require a custom language to still be registered — a saved term tagged with one keeps its tag. */
        override fun deserialize(decoder: Decoder): SupportedLanguage {
            val name = decoder.decodeString()
            return fromName(name) ?: custom(name, CustomLanguageStore.displayNameForKey(name))
        }
    }

    companion object {
    val CHINESE_TRADITIONAL = SupportedLanguage("CHINESE_TRADITIONAL", "Chinese (Traditional)",
        Locale.Builder().setLanguage("zh").setScript("Hant").setRegion("TW").build(),
        // ML Kit GenAI Speech Recognition's supported-locale list uses the
        // "cmn" (Mandarin) macrolanguage tag, not "zh" — different from the
        // locale tag used elsewhere (ICU segmentation, Nano prompts).
        Locale.forLanguageTag("cmn-Hant-TW"),
        OcrScript.CHINESE,)
    val CHINESE_SIMPLIFIED = SupportedLanguage("CHINESE_SIMPLIFIED", "Chinese (Simplified)",
        Locale.Builder().setLanguage("zh").setScript("Hans").setRegion("CN").build(),
        Locale.forLanguageTag("cmn-Hans-CN"),
        OcrScript.CHINESE,)
    val JAPANESE = SupportedLanguage("JAPANESE", "Japanese", Locale.JAPAN, Locale.forLanguageTag("ja-JP"), OcrScript.JAPANESE)
    val GERMAN = SupportedLanguage("GERMAN", "German", Locale.GERMANY, Locale.forLanguageTag("de-DE"), OcrScript.LATIN)
    val FRENCH = SupportedLanguage("FRENCH", "French", Locale.FRANCE, Locale.forLanguageTag("fr-FR"), OcrScript.LATIN)
    val SPANISH = SupportedLanguage("SPANISH", "Spanish", Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es-ES"), OcrScript.LATIN)
    val THAI = SupportedLanguage("THAI", "Thai", Locale.forLanguageTag("th-TH"), Locale.forLanguageTag("th-TH"), null)
    val KOREAN = SupportedLanguage("KOREAN", "Korean", Locale.KOREA, Locale.forLanguageTag("ko-KR"), OcrScript.KOREAN)
    val VIETNAMESE = SupportedLanguage("VIETNAMESE", "Vietnamese", Locale.forLanguageTag("vi-VN"), Locale.forLanguageTag("vi-VN"), OcrScript.LATIN)
    val HINDI = SupportedLanguage("HINDI", "Hindi", Locale.forLanguageTag("hi-IN"), Locale.forLanguageTag("hi-IN"), OcrScript.DEVANAGARI)
    val TAMIL = SupportedLanguage("TAMIL", "Tamil", Locale.forLanguageTag("ta-IN"), null, null)
    val GUJARATI = SupportedLanguage("GUJARATI", "Gujarati", Locale.forLanguageTag("gu-IN"), null, null)
    val ITALIAN = SupportedLanguage("ITALIAN", "Italian", Locale.ITALY, Locale.forLanguageTag("it-IT"), OcrScript.LATIN)
    val PORTUGUESE = SupportedLanguage("PORTUGUESE", "Portuguese", Locale.forLanguageTag("pt-PT"), Locale.forLanguageTag("pt-PT"), OcrScript.LATIN)
    val DANISH = SupportedLanguage("DANISH", "Danish", Locale.forLanguageTag("da-DK"), Locale.forLanguageTag("da-DK"), OcrScript.LATIN)
    val DUTCH = SupportedLanguage("DUTCH", "Dutch", Locale.forLanguageTag("nl-NL"), Locale.forLanguageTag("nl-NL"), OcrScript.LATIN)
    // No GenAI Speech Recognition entry in either tier as of this
    // language's addition — OCR (Latin) still works.
    val NORWEGIAN = SupportedLanguage("NORWEGIAN", "Norwegian", Locale.forLanguageTag("nb-NO"), null, OcrScript.LATIN)
    val SWEDISH = SupportedLanguage("SWEDISH", "Swedish", Locale.forLanguageTag("sv-SE"), Locale.forLanguageTag("sv-SE"), OcrScript.LATIN)
    val TURKISH = SupportedLanguage("TURKISH", "Turkish", Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("tr-TR"), OcrScript.LATIN)
    val ENGLISH = SupportedLanguage("ENGLISH", "English", Locale.US, Locale.forLanguageTag("en-US"), OcrScript.LATIN)
    val BENGALI = SupportedLanguage("BENGALI", "Bengali", Locale.forLanguageTag("bn-IN"), null, null)
    val PUNJABI = SupportedLanguage("PUNJABI", "Punjabi", Locale.forLanguageTag("pa-IN"), null, null)
    val URDU = SupportedLanguage("URDU", "Urdu", Locale.forLanguageTag("ur-PK"), null, null)
    val GREEK = SupportedLanguage("GREEK", "Greek", Locale.forLanguageTag("el-GR"), null, null)
    val HEBREW = SupportedLanguage("HEBREW", "Hebrew", Locale.forLanguageTag("he-IL"), null, null)
    // Speech recognition works (Advanced tier), but Text Recognition v2 has
    // no Cyrillic-script recognizer — OCR isn't offered for it.
    val RUSSIAN = SupportedLanguage("RUSSIAN", "Russian", Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("ru-RU"), null)
    val UKRAINIAN = SupportedLanguage("UKRAINIAN", "Ukrainian", Locale.forLanguageTag("uk-UA"), null, null)
    // Not in iOS's list — Gemini Nano supports Czech, Apple Intelligence
    // doesn't. No GenAI Speech Recognition entry; OCR works (Latin script).
    val CZECH = SupportedLanguage("CZECH", "Czech", Locale.forLanguageTag("cs-CZ"), null, OcrScript.LATIN)

    // Added 2026-09-01 — no official per-language list exists for Gemini
    // Nano/the GenAI Prompt API (confirmed by fetching Google's own docs),
    // so LLM support below is a proxy signal only: whether the language
    // appears on the cloud Gemini Live API's supported-language list (a
    // different, larger, server-side model in the same family), not a
    // guarantee for the on-device model these calls actually use.
    //
    // Indonesian also has beta-tier GenAI Speech Recognition support.
    val INDONESIAN = SupportedLanguage("INDONESIAN", "Indonesian", Locale.forLanguageTag("id-ID"), Locale.forLanguageTag("id-ID"), OcrScript.LATIN)
    val MARATHI = SupportedLanguage("MARATHI", "Marathi", Locale.forLanguageTag("mr-IN"), null, OcrScript.DEVANAGARI)
    val SWAHILI = SupportedLanguage("SWAHILI", "Swahili", Locale.forLanguageTag("sw-KE"), null, OcrScript.LATIN)
    val TAGALOG = SupportedLanguage("TAGALOG", "Tagalog", Locale.forLanguageTag("tl-PH"), null, OcrScript.LATIN)
    val YORUBA = SupportedLanguage("YORUBA", "Yoruba", Locale.forLanguageTag("yo-NG"), null, OcrScript.LATIN)
    val QUECHUA = SupportedLanguage("QUECHUA", "Quechua", Locale.forLanguageTag("qu-PE"), null, OcrScript.LATIN)
    // No OCR recognizer exists for these three scripts at all — same
    // text-only profile as Bengali/Punjabi/Urdu/Greek/Hebrew above.
    val TELUGU = SupportedLanguage("TELUGU", "Telugu", Locale.forLanguageTag("te-IN"), null, null)
    val KANNADA = SupportedLanguage("KANNADA", "Kannada", Locale.forLanguageTag("kn-IN"), null, null)
    val AMHARIC = SupportedLanguage("AMHARIC", "Amharic", Locale.forLanguageTag("am-ET"), null, null)

    // Added 2026-09-20 to catch up with iOS (Finnish/Estonian/Polish/Arabic
    // landed there after the original port). Speech recognition is left
    // unset until each is checked against Google's GenAI Speech Recognition
    // language list — typed text, Generate Sample, and (for the Latin-script
    // ones) photo scan all work regardless. Arabic has no ML Kit OCR script.
    val FINNISH = SupportedLanguage("FINNISH", "Finnish", Locale.forLanguageTag("fi-FI"), null, OcrScript.LATIN)
    val ESTONIAN = SupportedLanguage("ESTONIAN", "Estonian", Locale.forLanguageTag("et-EE"), null, OcrScript.LATIN)
    val POLISH = SupportedLanguage("POLISH", "Polish", Locale.forLanguageTag("pl-PL"), null, OcrScript.LATIN)
    val ARABIC = SupportedLanguage("ARABIC", "Arabic", Locale.forLanguageTag("ar-SA"), null, null)

        /** Built-in languages only, in declaration order. */
        val builtIns: List<SupportedLanguage> by lazy {
            listOf(CHINESE_TRADITIONAL, CHINESE_SIMPLIFIED, JAPANESE, GERMAN, FRENCH, SPANISH, THAI, KOREAN, VIETNAMESE, HINDI, TAMIL, GUJARATI, ITALIAN, PORTUGUESE, DANISH, DUTCH, NORWEGIAN, SWEDISH, TURKISH, ENGLISH, BENGALI, PUNJABI, URDU, GREEK, HEBREW, RUSSIAN, UKRAINIAN, CZECH, INDONESIAN, MARATHI, SWAHILI, TAGALOG, YORUBA, QUECHUA, TELUGU, KANNADA, AMHARIC, FINNISH, ESTONIAN, POLISH, ARABIC)
        }

        /** Built-ins plus any the user added via a connected dictionary — read fresh so a new one shows up everywhere immediately. */
        val entries: List<SupportedLanguage> get() = builtIns + CustomLanguageStore.load()

        fun fromName(name: String): SupportedLanguage? = entries.firstOrNull { it.name == name }

        internal fun custom(name: String, displayName: String) =
            SupportedLanguage(name, displayName, Locale.ROOT, null, null)
    }
}
