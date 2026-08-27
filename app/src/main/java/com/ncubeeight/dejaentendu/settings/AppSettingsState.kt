package com.ncubeeight.dejaentendu.settings

import androidx.compose.runtime.mutableStateOf

/**
 * In-memory mirror of the persisted color-scheme preference. MainActivity's
 * theme wrapper wraps the whole NavHost and stays mounted across bottom-tab
 * navigation (only each tab's own screen gets disposed/recreated), so it
 * can't rely on "re-enter this composable" to pick up a change the way
 * Home/Upload/Vocabulary's own lists do. This shared, mutable holder lets
 * SettingsScreen update the live theme immediately without threading a
 * callback down through the nav graph.
 */
object AppSettingsState {
    val colorScheme = mutableStateOf(AppColorScheme.SYSTEM)
}
