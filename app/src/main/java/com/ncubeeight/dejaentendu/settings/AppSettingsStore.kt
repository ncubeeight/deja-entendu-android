package com.ncubeeight.dejaentendu.settings

import android.content.Context
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage

/**
 * Small persisted app preferences, backed by SharedPreferences — mirrors
 * iOS's AppSettings.swift (@AppStorage-backed UserDefaults). Centralizing
 * the keys and encode/decode helpers here keeps every call site in sync,
 * same reasoning as the iOS file.
 */
object AppSettingsStore {
    private const val PREFS_NAME = "app_settings"
    private const val ENABLED_LANGUAGES_KEY = "enabled_languages"
    private const val COLOR_SCHEME_KEY = "color_scheme"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Empty/unparseable means "everything enabled" — same default as iOS before Settings is ever visited. */
    fun enabledLanguages(context: Context): Set<SupportedLanguage> {
        val raw = prefs(context).getString(ENABLED_LANGUAGES_KEY, null) ?: return SupportedLanguage.entries.toSet()
        val parsed = raw.split(",").mapNotNull { name ->
            SupportedLanguage.entries.firstOrNull { it.name == name }
        }.toSet()
        return parsed.ifEmpty { SupportedLanguage.entries.toSet() }
    }

    fun setEnabledLanguages(context: Context, languages: Set<SupportedLanguage>) {
        prefs(context).edit().putString(ENABLED_LANGUAGES_KEY, languages.joinToString(",") { it.name }).apply()
    }

    /** enabledLanguages(context), alphabetized — the presentation order for every language picker. */
    fun enabledLanguagesSorted(context: Context): List<SupportedLanguage> {
        val enabled = enabledLanguages(context)
        return SupportedLanguage.entries.filter { it in enabled }.sortedBy { it.displayName }
    }

    /**
     * Enabled languages that ML Kit GenAI Speech Recognition actually
     * supports — coverage is uneven across the language list (verified
     * against Google's docs, 2026-08-31), so audio import can't just offer
     * every enabled language the way typed-text import can. Falls back to
     * every speech-capable language if the user has disabled all of them,
     * so this is never empty.
     */
    fun audioImportLanguages(context: Context): List<SupportedLanguage> {
        val enabled = enabledLanguagesSorted(context).filter { it.speechRecognitionLocale != null }
        return enabled.ifEmpty { SupportedLanguage.entries.filter { it.speechRecognitionLocale != null }.sortedBy { it.displayName } }
    }

    /** Same reasoning as [audioImportLanguages], for ML Kit Text Recognition v2's script coverage. */
    fun photoImportLanguages(context: Context): List<SupportedLanguage> {
        val enabled = enabledLanguagesSorted(context).filter { it.ocrScript != null }
        return enabled.ifEmpty { SupportedLanguage.entries.filter { it.ocrScript != null }.sortedBy { it.displayName } }
    }

    fun colorScheme(context: Context): AppColorScheme {
        val raw = prefs(context).getString(COLOR_SCHEME_KEY, null) ?: return AppColorScheme.SYSTEM
        return AppColorScheme.entries.firstOrNull { it.name == raw } ?: AppColorScheme.SYSTEM
    }

    fun setColorScheme(context: Context, scheme: AppColorScheme) {
        prefs(context).edit().putString(COLOR_SCHEME_KEY, scheme.name).apply()
    }
}
