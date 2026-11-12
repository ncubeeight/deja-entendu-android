package com.ncubeeight.dejaentendu.settings

/**
 * How fast TextToSpeech reads text aloud — one global choice applied to
 * every speak button (Samples list, flashcard term and example). Mirrors
 * iOS's SpeechPace in AppSettings.swift. Unlike iOS's AVSpeechUtterance.rate,
 * TextToSpeech.setSpeechRate is a true multiplier, so these are exact.
 */
enum class SpeechPace(val rate: Float, val displayName: String) {
    SLOWER(0.5f, "Slower"),
    NORMAL(1.0f, "Normal"),
    FASTER(1.5f, "Faster"),
}
