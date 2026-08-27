package com.ncubeeight.dejaentendu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ncubeeight.dejaentendu.settings.AppColorScheme
import com.ncubeeight.dejaentendu.settings.AppSettingsState
import com.ncubeeight.dejaentendu.settings.AppSettingsStore
import com.ncubeeight.dejaentendu.ui.AppNavigation
import com.ncubeeight.dejaentendu.ui.theme.DejaEntenduTheme

/**
 * App root: hosts the bottom-tab navigation shell (Home / Audio Samples /
 * Vocabulary / Settings), mirroring iOS's HomeView.swift. The on-device
 * LLM/transcription smoke tests that used to live here have served their
 * purpose (both checkpoints passed on a real Pixel 10 — see README.md/
 * ROADMAP.md) and are gone now that real screens exercise those generators.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                AppSettingsState.colorScheme.value = AppSettingsStore.colorScheme(context)
            }

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (AppSettingsState.colorScheme.value) {
                AppColorScheme.SYSTEM -> systemDark
                AppColorScheme.LIGHT -> false
                AppColorScheme.DARK -> true
            }

            DejaEntenduTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
