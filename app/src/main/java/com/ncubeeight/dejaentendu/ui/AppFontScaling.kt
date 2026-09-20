package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.ncubeeight.dejaentendu.settings.AppSettingsState

/**
 * Applies the in-app text size (Settings > Text Size) on top of the
 * device's own font size. Computed from the system's fontScale rather than
 * from whatever LocalDensity currently holds, so it's safe to apply more
 * than once — the app root does, and every window-based composable below
 * does again, because sheets, dialogs, and menus render in their own
 * window and don't inherit the root override.
 */
@Composable
fun WithAppFontScale(content: @Composable () -> Unit) {
    val systemFontScale = LocalConfiguration.current.fontScale
    val density = LocalDensity.current
    val scaled = Density(density.density, systemFontScale * AppSettingsState.fontScale.value.multiplier)
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        WithAppFontScale { Column(content) }
    }
}

@Composable
private fun Column(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(content = content)
}

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        WithAppFontScale { Column(content) }
    }
}

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { WithAppFontScale(confirmButton) },
        title = { WithAppFontScale(title) },
        text = { WithAppFontScale(text) },
    )
}
