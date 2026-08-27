package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ncubeeight.dejaentendu.audio.ImportedRecordingStore
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore

private data class TabItem(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Screen.Home, "Home", Icons.Filled.Home),
    TabItem(Screen.Upload, "Audio Samples", Icons.Filled.GraphicEq),
    TabItem(Screen.Vocabulary, "Vocabulary", Icons.AutoMirrored.Filled.MenuBook),
    TabItem(Screen.Settings, "Settings", Icons.Filled.Settings),
)

/**
 * NavHost + bottom tab bar hosting Home / Upload / Vocabulary / Settings,
 * mirroring iOS's HomeView.swift TabView. Flashcard/Transcription are push
 * destinations reached from any tab, not tabs themselves, same as on iOS.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(tab.screen::class) == true,
                        onClick = {
                            navController.navigate(tab.screen) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    onRecordingClick = { navController.navigate(Screen.Transcription(it.id)) },
                    onEntryClick = { navController.navigate(Screen.Flashcard(it.id)) },
                    onImportRecordingClick = {
                        navController.navigate(Screen.Upload) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable<Screen.Upload> {
                UploadScreen(onRecordingClick = { navController.navigate(Screen.Transcription(it.id)) })
            }
            composable<Screen.Vocabulary> {
                VocabularyListScreen(onEntryClick = { navController.navigate(Screen.Flashcard(it.id)) })
            }
            composable<Screen.Settings> {
                SettingsScreen()
            }
            composable<Screen.Flashcard> { entry ->
                val route: Screen.Flashcard = entry.toRoute()
                val context = LocalContext.current
                val vocabularyEntry = VocabularyStore.load(context).firstOrNull { it.id == route.entryId }
                if (vocabularyEntry != null) {
                    VocabularyFlashcardScreen(vocabularyEntry)
                }
            }
            composable<Screen.Transcription> { entry ->
                val route: Screen.Transcription = entry.toRoute()
                val context = LocalContext.current
                val recording = ImportedRecordingStore.load(context).firstOrNull { it.id == route.recordingId }
                if (recording != null) {
                    TranscriptionRunnerScreen(recording)
                }
            }
        }
    }
}
