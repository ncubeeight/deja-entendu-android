package com.ncubeeight.dejaentendu

/**
 * User-facing explanation for a ML Kit GenAI feature reporting anything
 * other than AVAILABLE/DOWNLOADABLE/DOWNLOADING. On Pixel phones that
 * haven't yet received the real Android AICore from Google Play (the
 * system image ships only a stub with no service in it), checkStatus()
 * returns UNAVAILABLE (0) and every on-device feature fails the same way —
 * which used to surface as a bare "model unavailable (status=0)".
 */
object GenAiStatus {
    fun unavailableMessage(status: Int): String =
        "Gemini Nano isn't available on this phone yet (status=$status). " +
            "Open Google Play, search for \"Android AICore\", and update it " +
            "(also check Settings > System > System update), keep the phone " +
            "on Wi-Fi and charging for a few minutes so the model can " +
            "download, then try again."
}
