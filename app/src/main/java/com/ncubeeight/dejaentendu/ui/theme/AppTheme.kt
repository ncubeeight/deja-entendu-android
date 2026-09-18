package com.ncubeeight.dejaentendu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Whether the app is currently rendering its dark theme — set by
 * DejaEntenduTheme from the *resolved* darkTheme flag (system setting or
 * this app's own Appearance override in Settings), so AppColors.home*
 * below can adapt the same way MaterialTheme's own colors do.
 */
val LocalDarkTheme = compositionLocalOf { false }

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

    // Adaptive counterparts for the Home tab's own background/card/text
    // colors — Home was the only screen painting a fixed light surface.
    // Without these, dark mode left Home's status bar area and card text
    // sitting on a surface that never actually went dark. background/
    // surface/ink/inkSoft/line above stay fixed deliberately: ink also has
    // to stay dark everywhere it sits against the always-light `rainbow`
    // word-highlight colors in transcript word tokens, so making it
    // adaptive would reintroduce that exact contrast bug there.
    // Mirrors iOS's AppTheme.homeBackground/etc.
    private val homeBackgroundLight = Color(0xFFFBF6EF)
    private val homeBackgroundDark = Color(0xFF17130F)
    private val homeSurfaceLight = Color(0xFFFFFDF9)
    private val homeSurfaceDark = Color(0xFF241F18)
    private val homeInkLight = Color(0xFF241C16)
    private val homeInkDark = Color(0xFFF5EFE6)
    private val homeInkSoftLight = Color(0xFF7A6F63)
    private val homeInkSoftDark = Color(0xFFAEA192)
    private val homeLineLight = Color(0xFFECE3D8)
    private val homeLineDark = Color(0xFF39332A)

    val homeBackground: Color @Composable get() = if (LocalDarkTheme.current) homeBackgroundDark else homeBackgroundLight
    val homeSurface: Color @Composable get() = if (LocalDarkTheme.current) homeSurfaceDark else homeSurfaceLight
    val homeInk: Color @Composable get() = if (LocalDarkTheme.current) homeInkDark else homeInkLight
    val homeInkSoft: Color @Composable get() = if (LocalDarkTheme.current) homeInkSoftDark else homeInkSoftLight
    val homeLine: Color @Composable get() = if (LocalDarkTheme.current) homeLineDark else homeLineLight

    val coral = Color(0xFFFF6B4A)
    val coralSoft = Color(0xFFFFE4DA)
    val teal = Color(0xFF2BBAA3)
    val tealSoft = Color(0xFFDFF6F1)
    // Added for SampleKind's text-sample tint, mirroring iOS's AppTheme.swift
    // (which has these two but our earlier port of this file didn't carry
    // them over since nothing used them yet).
    val sky = Color(0xFF3D84D6)
    val skySoft = Color(0xFFE1EDFB)
    val gold = Color(0xFFE8A93D)
    val goldSoft = Color(0xFFFCEFD7)
    val lavender = Color(0xFF8B7CD6)
    val lavenderSoft = Color(0xFFEAE6FA)
    val rose = Color(0xFFE0609E)
    val roseSoft = Color(0xFFFBE3EE)

    /**
     * Soft backgrounds cycled across consecutive parsed transcript words so
     * word boundaries stay visible even in scripts with no whitespace
     * between words (Japanese, Chinese) — the same six-hue set used
     * elsewhere just at reduced saturation, so it reads as an accent rather
     * than competing with the ink/coral UI. Mirrors iOS's AppTheme.rainbow.
     */
    val rainbow: List<Color> = listOf(coralSoft, goldSoft, tealSoft, skySoft, lavenderSoft, roseSoft)

    // Matches the blue-green header gradient on the GitHub Pages site
    // (jekyll-theme-cayman's .page-header: linear-gradient(120deg, #155799, #159957)).
    val headerGradientStart = Color(0xFF155799)
    val headerGradientEnd = Color(0xFF159957)
}

private val DejaEntenduLightColorScheme = lightColorScheme(
    primary = AppColors.coral,
    background = AppColors.background,
    surface = AppColors.surface,
    onBackground = AppColors.ink,
    onSurface = AppColors.ink,
)

// iOS's AppTheme has no dark variant at all — Home/Vocabulary/Flashcard
// etc. hardcode AppTheme.* colors regardless of the system's light/dark
// setting there, so its Settings > Appearance toggle only ever affects
// unstyled system chrome (Forms, alerts, the keyboard), never the custom
// screens. This dark scheme mirrors that same scope of effect on Android:
// it governs Material3's default/unstyled components, while our own
// AppColors-styled screens stay visually identical either way, same as iOS.
private val DejaEntenduDarkColorScheme = darkColorScheme(
    primary = AppColors.coral,
    background = Color(0xFF1C1712),
    surface = Color(0xFF241C16),
    onBackground = Color(0xFFEFE6DC),
    onSurface = Color(0xFFEFE6DC),
)

@Composable
fun DejaEntenduTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DejaEntenduDarkColorScheme else DejaEntenduLightColorScheme,
            content = content,
        )
    }
}
