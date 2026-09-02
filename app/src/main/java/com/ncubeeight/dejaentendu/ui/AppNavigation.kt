package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
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
import com.ncubeeight.dejaentendu.samples.AnySample
import com.ncubeeight.dejaentendu.samples.ImportedImageSampleStore
import com.ncubeeight.dejaentendu.samples.ImportedTextSampleStore
import com.ncubeeight.dejaentendu.samples.id
import com.ncubeeight.dejaentendu.samples.kind
import com.ncubeeight.dejaentendu.samples.runnerInput
import com.ncubeeight.dejaentendu.studynotes.VocabularyStore

private data class TabItem(val screen: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Screen.Home, "Home", Icons.Filled.Home),
    TabItem(Screen.Samples, "Samples", Icons.Filled.Inbox),
    TabItem(Screen.Vocabulary, "Vocabulary", Icons.AutoMirrored.Filled.MenuBook),
    TabItem(Screen.Settings, "Settings", Icons.Filled.Settings),
)

/**
 * Loads whichever store [kind] ("audio"/"text"/"image", a AnySample.kind
 * name lowercased) points at and returns the sample for [sampleId], resolved
 * to the SampleInput TranscriptionRunnerScreen needs to run it.
 */
private fun loadSampleInput(context: android.content.Context, kind: String, sampleId: String): com.ncubeeight.dejaentendu.transcription.SampleInput? {
    val sample: AnySample? = when (kind) {
        "audio" -> ImportedRecordingStore.load(context).firstOrNull { it.id == sampleId }?.let { AnySample.Audio(it) }
        "text" -> ImportedTextSampleStore.load(context).firstOrNull { it.id == sampleId }?.let { AnySample.Text(it) }
        "image" -> ImportedImageSampleStore.load(context).firstOrNull { it.id == sampleId }?.let { AnySample.Image(it) }
        else -> null
    }
    return sample?.runnerInput
}

/**
 * NavHost + bottom tab bar hosting Home / Samples / Vocabulary / Settings,
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
                            // Deliberately no saveState/restoreState here: this
                            // graph mixes tab destinations with push destinations
                            // (Transcription/Flashcard) on top of them, and that
                            // combination has been observed to restore the wrong
                            // back stack entry (landing back on a stale
                            // Transcription screen instead of the tapped tab).
                            // A tab switch should always land cleanly on that
                            // tab's root, so pop everything back to the start
                            // destination first.
                            navController.navigate(tab.screen) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
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
                    onSampleClick = { navController.navigate(Screen.Transcription(it.kind.name.lowercase(), it.id)) },
                    onEntryClick = { navController.navigate(Screen.Flashcard(it.id)) },
                    onImportRecordingClick = {
                        navController.navigate(Screen.Samples) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onExampleInteractionClick = { navController.navigate(Screen.Iroha) },
                )
            }
            composable<Screen.Iroha> {
                IrohaExplorerScreen()
            }
            composable<Screen.Samples> {
                SamplesScreen(onSampleClick = { navController.navigate(Screen.Transcription(it.kind.name.lowercase(), it.id)) })
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
                val input = loadSampleInput(context, route.kind, route.sampleId)
                if (input != null) {
                    TranscriptionRunnerScreen(input, onViewFlashcard = { navController.navigate(Screen.Flashcard(it.id)) })
                }
            }
        }
    }
}
