package com.ncubeeight.dejaentendu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Ported directly from the iOS app's App/AppTheme.swift so both platforms
 * share one visual identity. Keep these two files in sync by hand — there's
 * no shared source of truth between the Swift and Kotlin codebases.
 */
object AppColors {
    val background = Color(0xFFFBF6EF)
    val surface = Color(0xFFFFFDF9)
    val ink = Color(0xFF241C16)
    val inkSoft = Color(0xFF7A6F63)
    val line = Color(0xFFECE3D8)

    val coral = Color(0xFFFF6B4A)
    val coralSoft = Color(0xFFFFE4DA)
    val teal = Color(0xFF2BBAA3)
    val tealSoft = Color(0xFFDFF6F1)

    // Matches the blue-green header gradient on the GitHub Pages site
    // (jekyll-theme-cayman's .page-header: linear-gradient(120deg, #155799, #159957)).
    val headerGradientStart = Color(0xFF155799)
    val headerGradientEnd = Color(0xFF159957)
}

private val DejaEntenduColorScheme = lightColorScheme(
    primary = AppColors.coral,
    background = AppColors.background,
    surface = AppColors.surface,
    onBackground = AppColors.ink,
    onSurface = AppColors.ink,
)

@Composable
fun DejaEntenduTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DejaEntenduColorScheme,
        content = content,
    )
}
