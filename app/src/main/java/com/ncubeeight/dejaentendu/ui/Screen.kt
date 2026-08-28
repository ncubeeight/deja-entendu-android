package com.ncubeeight.dejaentendu.ui

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (navigation-compose 2.9's @Serializable
 * route support). Flashcard/Transcription carry an id rather than the full
 * object — entries/samples live in the JSON stores, not a reactive DB,
 * so destinations re-load by id from the store rather than receiving a
 * live object through the nav graph.
 */
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Samples : Screen

    @Serializable
    data object Vocabulary : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class Flashcard(val entryId: String) : Screen

    /**
     * [kind] is a lowercase SampleKind name ("audio"/"text"/"image") rather
     * than SampleKind itself — SampleKind carries an ImageVector/Color pair
     * that kotlinx.serialization can't handle, so a plain string stands in
     * for it in the route and gets mapped back to a store lookup.
     */
    @Serializable
    data class Transcription(val kind: String, val sampleId: String) : Screen
}
