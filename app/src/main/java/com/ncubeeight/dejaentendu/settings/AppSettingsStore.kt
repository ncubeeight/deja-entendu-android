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
    private const val FONT_SCALE_KEY = "font_scale"
    private const val HAS_COMPLETED_FIRST_HOME_LAUNCH_KEY = "has_completed_first_home_launch"
    private const val DEFAULT_IMPORT_LANGUAGE_KEY = "default_import_language"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * What a first run shows before the user has ever visited Settings —
     * all 40+ languages on read as overwhelming in every picker, so this is
     * a small starter set instead; the rest are one toggle away. Mirrors
     * iOS's AppSettings.defaultEnabledLanguages (plus Portuguese).
     */
    val defaultEnabledLanguages: Set<SupportedLanguage> = setOf(
        SupportedLanguage.FRENCH,
        SupportedLanguage.CHINESE_TRADITIONAL,
        SupportedLanguage.CHINESE_SIMPLIFIED,
        SupportedLanguage.PORTUGUESE,
        SupportedLanguage.JAPANESE,
        SupportedLanguage.GERMAN,
    )

    /** Nothing saved (or nothing parseable) means the starter set above; a saved choice is always kept as-is. */
    fun enabledLanguages(context: Context): Set<SupportedLanguage> {
        val raw = prefs(context).getString(ENABLED_LANGUAGES_KEY, null) ?: return defaultEnabledLanguages
        val parsed = raw.split(",").mapNotNull { name ->
            SupportedLanguage.entries.firstOrNull { it.name == name }
        }.toSet()
        return parsed.ifEmpty { defaultEnabledLanguages }
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

    fun fontScale(context: Context): AppFontScale {
        val raw = prefs(context).getString(FONT_SCALE_KEY, null) ?: return AppFontScale.DEFAULT
        return AppFontScale.entries.firstOrNull { it.name == raw } ?: AppFontScale.DEFAULT
    }

    fun setFontScale(context: Context, scale: AppFontScale) {
        prefs(context).edit().putString(FONT_SCALE_KEY, scale.name).apply()
    }

    fun hasCompletedFirstHomeLaunch(context: Context): Boolean =
        prefs(context).getBoolean(HAS_COMPLETED_FIRST_HOME_LAUNCH_KEY, false)

    fun setHasCompletedFirstHomeLaunch(context: Context) {
        prefs(context).edit().putBoolean(HAS_COMPLETED_FIRST_HOME_LAUNCH_KEY, true).apply()
    }

    /**
     * The language of the most recently connected local dictionary (see
     * ConnectLocalDictionaryScreen), used to pre-select new Sample imports
     * so terms parse against the dictionary the user just connected
     * without re-picking it every time. Falls back to the first enabled
     * language when nothing's connected, or that language has since been
     * turned off in "Languages shown on import".
     */
    fun preferredDefaultLanguage(context: Context, enabledLanguages: List<SupportedLanguage>): SupportedLanguage {
        val raw = prefs(context).getString(DEFAULT_IMPORT_LANGUAGE_KEY, null)
        val saved = raw?.let { name -> SupportedLanguage.entries.firstOrNull { it.name == name } }
        if (saved != null && saved in enabledLanguages) return saved
        return enabledLanguages.firstOrNull() ?: SupportedLanguage.CHINESE_TRADITIONAL
    }

    fun setPreferredDefaultLanguage(context: Context, language: SupportedLanguage) {
        prefs(context).edit().putString(DEFAULT_IMPORT_LANGUAGE_KEY, language.name).apply()
    }

    fun clearPreferredDefaultLanguage(context: Context) {
        prefs(context).edit().remove(DEFAULT_IMPORT_LANGUAGE_KEY).apply()
    }
}
