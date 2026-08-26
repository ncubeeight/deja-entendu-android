package com.ncubeeight.dejaentendu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ncubeeight.dejaentendu.ui.AppNavigation
import com.ncubeeight.dejaentendu.ui.theme.DejaEntenduTheme

/**
 * App root: hosts the bottom-tab navigation shell (Home / Audio Samples /
 * Vocabulary), mirroring iOS's HomeView.swift. The on-device LLM/
 * transcription smoke tests that used to live here have served their
 * purpose (both checkpoints passed on a real Pixel 10 — see README.md/
 * ROADMAP.md) and are gone now that real screens exercise those generators.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DejaEntenduTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
