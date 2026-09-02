package com.ncubeeight.dejaentendu.studynotes

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import kotlinx.coroutines.flow.first

/** Mirrors iOS's FlashcardDetails (FlashcardGenerator.swift). */
@Generable
data class FlashcardDetails(
    @param:Guide(
        description = "A simple phonetic pronunciation guide using plain English spelling, not " +
            "IPA. It must sound out the term's ENTIRE original text from first syllable to " +
            "last — never a truncated stem, prefix, or shortened form. e.g. for the 2-syllable " +
            "term \"Bonjour\", 'boh-ZHOOR' (both syllables); for the 3-syllable term " +
            "\"réfléchi\", 'ray-flay-SHEE' (all three syllables, not just 'ray-flay'). Every " +
            "syllable of the original term must be represented."
    )
    val pronunciation: String,

    @param:Guide(description = "A brief, natural English translation or definition of the term.")
    val translation: String,

    @param:Guide(description = "A short, natural example sentence in the term's own language and script, using the term.")
    val exampleSentence: String,

    @param:Guide(description = "An English translation of the example sentence.")
    val exampleTranslation: String,
)

class FlashcardUnavailableException(reason: String) : Exception(
    "On-device flashcard details aren't available right now: $reason"
)

/**
 * Which romanization/phonetic system the pronunciation guide should use for
 * a given language — stated explicitly and up front in the prompt rather
 * than left for the model to infer. Gemini Nano otherwise tends to
 * pronounce Japanese kanji using Mandarin pinyin readings (kanji and hanzi
 * are visually identical but pronounced completely differently), an issue
 * that also showed up on iOS. Naming the correct system for every language,
 * not just Japanese, gives the model a clear positive instruction to follow
 * instead of a single bolted-on warning.
 */
private fun pronunciationSystemHint(language: SupportedLanguage): String = when (language) {
    SupportedLanguage.JAPANESE ->
        "Hepburn romaji reflecting authentic Japanese on'yomi/kun'yomi readings " +
            "(e.g. 課題 → 'ka-dai'). Never Mandarin pinyin or Chinese-sounding " +
            "consonant clusters like 'zh', 'q', 'x', 'c', or 'dsh' — those belong " +
            "to Chinese, not Japanese."
    SupportedLanguage.CHINESE_TRADITIONAL, SupportedLanguage.CHINESE_SIMPLIFIED ->
        "Mandarin pinyin readings spelled out in plain English syllables (e.g. " +
            "你好 → 'nee-how'). Never Japanese on'yomi/kun'yomi readings."
    SupportedLanguage.KOREAN ->
        "Revised Romanization of Korean, spelled out in plain English syllables " +
            "(e.g. 안녕하세요 → 'an-nyeong-ha-se-yo')."
    SupportedLanguage.HINDI ->
        "A plain-English phonetic transliteration of the Devanagari pronunciation " +
            "(e.g. नमस्ते → 'nuh-muh-stay')."
    SupportedLanguage.TAMIL ->
        "A plain-English phonetic transliteration of the Tamil pronunciation " +
            "(e.g. வணக்கம் → 'vuh-nuck-kum')."
    SupportedLanguage.GUJARATI ->
        "A plain-English phonetic transliteration of the Gujarati pronunciation " +
            "(e.g. નમસ્તે → 'nuh-muh-stay')."
    SupportedLanguage.BENGALI ->
        "A plain-English phonetic transliteration of the Bengali pronunciation " +
            "(e.g. ধন্যবাদ → 'dhon-no-bad')."
    SupportedLanguage.PUNJABI ->
        "A plain-English phonetic transliteration of the Gurmukhi pronunciation " +
            "(e.g. ਧੰਨਵਾਦ → 'dhun-nuh-vahd')."
    SupportedLanguage.URDU ->
        "A plain-English phonetic transliteration of the Urdu pronunciation " +
            "(e.g. شکریہ → 'shuk-ree-ah')."
    SupportedLanguage.THAI ->
        "A plain-English phonetic transliteration of the Thai pronunciation, " +
            "hyphenating syllables (e.g. สวัสดี → 'sa-wat-dee')."
    SupportedLanguage.VIETNAMESE ->
        "A plain-English phonetic approximation of Vietnamese pronunciation " +
            "(e.g. 'xin chào' → 'sin chow')."
    SupportedLanguage.GREEK ->
        "A plain-English phonetic transliteration of Greek pronunciation " +
            "(e.g. 'ευχαριστώ' → 'ef-hah-ree-STOH')."
    SupportedLanguage.HEBREW ->
        "A plain-English phonetic transliteration of Hebrew pronunciation " +
            "(e.g. 'תודה' → 'toh-DAH')."
    SupportedLanguage.RUSSIAN ->
        "A plain-English phonetic transliteration of Russian pronunciation " +
            "(e.g. 'спасибо' → 'spuh-SEE-buh')."
    SupportedLanguage.UKRAINIAN ->
        "A plain-English phonetic transliteration of Ukrainian pronunciation " +
            "(e.g. 'дякую' → 'DYAH-koo-yoo')."
    SupportedLanguage.GERMAN ->
        "A plain-English phonetic approximation of German pronunciation (e.g. " +
            "'Danke' → 'DAHN-kuh')."
    SupportedLanguage.FRENCH ->
        "A plain-English phonetic approximation of French pronunciation (e.g. " +
            "'Bonjour' → 'boh-ZHOOR')."
    SupportedLanguage.SPANISH ->
        "A plain-English phonetic approximation of Spanish pronunciation (e.g. " +
            "'Gracias' → 'GRAH-see-ahs')."
    SupportedLanguage.ITALIAN ->
        "A plain-English phonetic approximation of Italian pronunciation (e.g. " +
            "'Grazie' → 'GRAHT-see-eh')."
    SupportedLanguage.PORTUGUESE ->
        "A plain-English phonetic approximation of European Portuguese " +
            "pronunciation (e.g. 'Obrigado' → 'oh-bree-GAH-doo')."
    SupportedLanguage.DANISH ->
        "A plain-English phonetic approximation of Danish pronunciation (e.g. " +
            "'Tak' → 'tack')."
    SupportedLanguage.DUTCH ->
        "A plain-English phonetic approximation of Dutch pronunciation (e.g. " +
            "'Dank je' → 'dahnk yuh')."
    SupportedLanguage.NORWEGIAN ->
        "A plain-English phonetic approximation of Norwegian pronunciation " +
            "(e.g. 'Takk' → 'tock')."
    SupportedLanguage.SWEDISH ->
        "A plain-English phonetic approximation of Swedish pronunciation (e.g. " +
            "'Tack' → 'tack')."
    SupportedLanguage.TURKISH ->
        "A plain-English phonetic approximation of Turkish pronunciation (e.g. " +
            "'Teşekkürler' → 'teh-shek-kewr-lehr')."
    SupportedLanguage.CZECH ->
        "A plain-English phonetic approximation of Czech pronunciation (e.g. " +
            "'Děkuji' → 'DYEH-koo-yee')."
    SupportedLanguage.ENGLISH ->
        "A plain-English dictionary-style phonetic respelling (e.g. 'library' " +
            "→ 'LY-brair-ee')."
    SupportedLanguage.INDONESIAN ->
        "A plain-English phonetic approximation of Indonesian pronunciation " +
            "(e.g. 'Terima kasih' → 'tuh-REE-mah KAH-see')."
    SupportedLanguage.MARATHI ->
        "A plain-English phonetic transliteration of the Devanagari " +
            "pronunciation (e.g. धन्यवाद → 'dhun-yuh-vahd')."
    SupportedLanguage.SWAHILI ->
        "A plain-English phonetic approximation of Swahili pronunciation " +
            "(e.g. 'Asante' → 'ah-SAHN-teh')."
    SupportedLanguage.TAGALOG ->
        "A plain-English phonetic approximation of Tagalog pronunciation " +
            "(e.g. 'Salamat' → 'sah-LAH-maht')."
    SupportedLanguage.YORUBA ->
        "A plain-English phonetic approximation of Yoruba pronunciation, " +
            "ignoring tone marks (e.g. 'Ẹ ṣé' → 'eh-SHEH')."
    SupportedLanguage.QUECHUA ->
        "A plain-English phonetic approximation of Quechua pronunciation " +
            "(e.g. 'Añay' → 'ah-NYIGH')."
    SupportedLanguage.TELUGU ->
        "A plain-English phonetic transliteration of the Telugu pronunciation " +
            "(e.g. ధన్యవాదాలు → 'dhun-yuh-vah-dah-lu')."
    SupportedLanguage.KANNADA ->
        "A plain-English phonetic transliteration of the Kannada pronunciation " +
            "(e.g. ಧನ್ಯವಾದಗಳು → 'dhun-yuh-vah-dah-guh-lu')."
    SupportedLanguage.AMHARIC ->
        "A plain-English phonetic transliteration of the Amharic (Ge'ez " +
            "script) pronunciation (e.g. 'አመሰግናለሁ' → 'ah-meh-seh-gih-nah-leh-hu')."
}

/**
 * A compact, script-family-level version of [pronunciationSystemHint] for
 * when the language isn't known up front — listing all 28 languages
 * individually here (as the known-language branch does per-call) would
 * bloat every unknown-language prompt for little benefit, since the model
 * still has to identify the language before any of those per-language
 * examples become relevant.
 */
private fun pronunciationSystemGeneralRules(): String = """
    - Chinese (hanzi): Mandarin pinyin.
    - Japanese (kanji/kana): Hepburn romaji (on'yomi/kun'yomi) — never pinyin, even though kanji and hanzi look alike.
    - Korean (hangul): Revised Romanization.
    - Devanagari-script languages (Hindi, Marathi): standard phonetic transliteration.
    - Other Indic-script languages (Tamil, Gujarati, Bengali, Punjabi/Gurmukhi, Telugu, Kannada): standard phonetic transliteration of that script.
    - Urdu (Perso-Arabic script): standard phonetic transliteration.
    - Amharic (Ge'ez script): standard phonetic transliteration.
    - Thai script: phonetic transliteration, hyphenating syllables.
    - Greek, Hebrew, Russian, Ukrainian, and other non-Latin-script languages: a plain-English phonetic transliteration.
    - Latin-script languages (French, German, Spanish, Italian, Portuguese, Danish, Dutch, Norwegian, Swedish, Turkish, Vietnamese, Czech, English, Indonesian, Swahili, Tagalog, Yoruba, Quechua, ...): a plain-English phonetic approximation of that language's actual pronunciation — not an English reading of the spelling.
""".trimIndent()

object FlashcardGenerator {

    /**
     * When [language] is known (the entry came from a tapped transcript
     * word, already tagged with the recording's language), pass it so the
     * model doesn't have to guess — same reasoning as iOS, where a bare word
     * like "commandera" can look like Spanish/Italian as easily as French.
     * Entries without a known language (manual entry, share-in text) leave
     * this null and ask the model to identify it.
     */
    suspend fun generateDetails(term: String, language: SupportedLanguage?): FlashcardDetails {
        val model = Generation.getClient()

        when (val status = model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
                val outcome = model.download().first {
                    it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed
                }
                if (outcome is DownloadStatus.DownloadFailed) {
                    throw FlashcardUnavailableException("model download failed: ${outcome.e.message}")
                }
            }
            else -> throw FlashcardUnavailableException("model unavailable (status=$status)")
        }

        if (!model.isStructuredOutputFeatureAvailable()) {
            throw FlashcardUnavailableException("structured output isn't supported on this device")
        }

        val instructionText = if (language != null) {
            """
            You are a compact language-learning dictionary. Pronunciation
            system for this request: ${pronunciationSystemHint(language)}

            The user will give you a word or short phrase in
            ${language.displayName} — treat that as certain, do not
            second-guess or reinterpret it as another language even if it
            also resembles a word in one. Produce a pronunciation guide (in
            the pronunciation system stated above — no other romanization
            scheme), a translation, and a short natural example sentence in
            ${language.displayName} using the term — plus that sentence's
            English translation. The pronunciation guide must cover the
            term's full length, every syllable from start to finish — never
            just a stem or the first part of a longer word. The example
            sentence must be written entirely in ${language.displayName},
            in its native script, and must contain the term itself
            verbatim. It is NOT the translation field — never write an
            English dictionary definition or explanation there (e.g. for
            "library", writing "A library is a building that houses
            books..." would be wrong — write an actual ${language.displayName}
            sentence like "私は毎日図書館に行きます" instead). Only
            exampleTranslation may be in English.
            """.trimIndent()
        } else {
            """
            You are a compact language-learning dictionary. Given a word or
            short phrase, first identify what language it's in, then produce
            a pronunciation guide, a translation, and a short natural example
            sentence using the term in that language — plus that sentence's
            English translation. The pronunciation guide must cover the
            term's full length, every syllable from start to finish — never
            just a stem or the first part of a longer word. Match the
            romanization system to the language you identified — do not mix
            them up:
            ${pronunciationSystemGeneralRules()}
            The example sentence must be written entirely in the term's own
            language, in its native script, and must contain the term
            itself verbatim. It is NOT the translation field — never write
            an English dictionary definition or explanation there. Only
            exampleTranslation may be in English.
            """.trimIndent()
        }

        val contentRequest = GenerateContentRequest.Builder(SystemInstruction(instructionText), TextPart("Term: $term"))
            .apply { maxOutputTokens = 300 }
            .build()

        val request = generateTypedContentRequest(contentRequest, FlashcardDetails::class)

        try {
            val response = model.generateContent(request)
            val details = response.candidates.firstOrNull()?.response
                ?: throw FlashcardUnavailableException("no result")

            // Kuromoji's dictionary-backed reading overrides whatever the
            // LLM guessed for Japanese — same reasoning as iOS's
            // CFStringTokenizer override, and only for Japanese: other
            // languages don't have this kanji/hanzi ambiguity problem, so
            // the model's own guess (already steered by
            // pronunciationSystemHint above) is left as-is.
            return if (language == SupportedLanguage.JAPANESE) {
                JapaneseReading.romaji(term)?.let { details.copy(pronunciation = it) } ?: details
            } else {
                details
            }
        } catch (e: GenAiException) {
            throw FlashcardUnavailableException(e.message ?: "unknown error")
        }
    }
}
