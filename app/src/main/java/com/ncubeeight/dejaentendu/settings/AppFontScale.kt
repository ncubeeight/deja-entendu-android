package com.ncubeeight.dejaentendu.settings

/**
 * In-app text size steps, applied on top of whatever font size the device
 * itself is set to (see MainActivity's LocalDensity override) — so it
 * scales every sp-sized piece of text in the app, buttons and content
 * tabs alike, without touching each screen individually.
 */
enum class AppFontScale(val multiplier: Float, val displayName: String) {
    DEFAULT(1.0f, "Default"),
    LARGE(1.15f, "Large"),
    EXTRA_LARGE(1.3f, "Extra Large"),
    HUGE(1.5f, "Huge"),
    MAXIMUM(1.75f, "Maximum");

    val percentLabel: String get() = "${(multiplier * 100).toInt()}%"
}
