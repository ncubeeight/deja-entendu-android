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

    fun colorScheme(context: Context): AppColorScheme {
        val raw = prefs(context).getString(COLOR_SCHEME_KEY, null) ?: return AppColorScheme.SYSTEM
        return AppColorScheme.entries.firstOrNull { it.name == raw } ?: AppColorScheme.SYSTEM
    }

    fun setColorScheme(context: Context, scheme: AppColorScheme) {
        prefs(context).edit().putString(COLOR_SCHEME_KEY, scheme.name).apply()
    }
}
