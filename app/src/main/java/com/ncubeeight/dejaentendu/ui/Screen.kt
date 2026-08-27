package com.ncubeeight.dejaentendu.ui

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (navigation-compose 2.9's @Serializable
 * route support). Flashcard/Transcription carry an id rather than the full
 * object — entries/recordings live in the JSON stores, not a reactive DB,
 * so destinations re-load by id from the store rather than receiving a
 * live object through the nav graph.
 */
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Upload : Screen

    @Serializable
    data object Vocabulary : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class Flashcard(val entryId: String) : Screen

    @Serializable
    data class Transcription(val recordingId: String) : Screen
}
