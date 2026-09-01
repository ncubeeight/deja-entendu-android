plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android here — AGP 9's built-in Kotlin
    // support conflicts with it. See root build.gradle.kts comment.
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.ncubeeight.dejaentendu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ncubeeight.dejaentendu"
        // 26 is the ML Kit GenAI Prompt API's own floor. It does NOT mean
        // the LLM features work on every device back to API 26 — that's
        // gated at runtime via checkStatus() (AVAILABLE/DOWNLOADABLE/
        // UNAVAILABLE), same pattern as iOS's SystemLanguageModel.availability.
        // This app is scoped to Pixel 10 and other Gemini-Nano-v3-class
        // devices for the LLM features specifically; older/other devices
        // install fine and just see those features reported unavailable.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // KSP's generated META-INF/services/...GenerableProvider file (which
    // registers @Generable classes like FlashcardDetails for runtime
    // discovery) was being silently dropped from the packaged APK under
    // AGP 9's built-in Kotlin — the file compiled fine into .dex, but the
    // resource never made it past processDebugJavaRes, so the Prompt API
    // failed at runtime with "not a registered @Generable type" even
    // though it always worked before this session's AGP 9 migration.
    // KSP's Kotlin *source* output got the same treatment automatically
    // once android.disallowKotlinSourceSets=false was set, but its
    // *resources* output apparently didn't — wiring it in explicitly here
    // fixes it. Confirmed by a real device test, 2026-08-26.
    sourceSets {
        getByName("debug") {
            resources.directories += "build/generated/ksp/debug/resources"
        }
        getByName("release") {
            resources.directories += "build/generated/ksp/release/resources"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // jvmTarget now defaults to compileOptions.targetCompatibility above
    // under AGP 9's built-in Kotlin support — no separate kotlinOptions{}
    // block needed (that DSL doesn't apply here anymore).

    buildFeatures {
        compose = true
    }

    // kuromoji-ipadic and its transitive kuromoji-core dependency both
    // ship identical META-INF/*.md doc files (license/contributors/notice
    // text, not code) — harmless duplicates, safe to drop the second copy.
    packaging {
        resources {
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    // Extended icon set (GraphicEq, MenuBook, ...) — core-only icons don't
    // cover the bottom-nav tab icons used here.
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // On-device Gemini Nano prompting — the Android analog of iOS's
    // FoundationModels, used for study notes, word glosses, and flashcards.
    // beta2 (originally guessed from docs) doesn't actually have typed/
    // structured output wired up at all — confirmed by decompiling it.
    // beta4 adds GenerateTypedContentRequest/Response and SystemInstruction,
    // which the generator code below depends on. Verified 2026-08-25 by a
    // real build + decompiling both versions, not from docs alone.
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
    // Runtime home of the @Generable/@Guide annotations themselves — a
    // separate artifact from the KSP compiler below, and easy to miss
    // (that's what "Unresolved reference 'Generable'" turned out to be).
    implementation("com.google.mlkit:genai-schema:1.0.0-alpha1")
    // Compiles the @Generable/@Guide-annotated classes into the schema
    // descriptors the Prompt API needs for structured output.
    ksp("com.google.mlkit:genai-schema-compiler:1.0.0-alpha1")

    // Bottom-tab navigation shell (Home / Upload / Vocabulary). Pinned to
    // 2.9.8 rather than 2.10.0: 2.10.0's transitive deps (lifecycle 2.11.0)
    // require compileSdk 37, which isn't published in the standard SDK
    // repository yet (only API 36 exists as of 2026-08-26) — confirmed via
    // a real build, not assumed.
    implementation("androidx.navigation:navigation-compose:2.9.8")
    // JSON persistence for ImportedRecording/VocabularyEntry — mirrors
    // iOS's Codable + JSON-in-Documents pattern.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // On-device file-based transcription — the Android analog of iOS's
    // SpeechAnalyzer/SpeechTranscriber. Alpha; verified against the real
    // AAR (not just docs) on 2026-08-26: SpeechRecognizer.download() has
    // the exact same never-completing Flow<DownloadStatus> as the Prompt
    // API's GenerativeModel.download() — use .first{ terminal state } there
    // too, not .collect{}.
    implementation("com.google.mlkit:genai-speech-recognition:1.0.0-alpha1")
    // Decoding/resampling imported audio (m4a/mp3/mp4) to the 16kHz mono
    // 16-bit PCM the speech recognizer's AudioSource.fromPfd requires uses
    // the platform's own MediaExtractor/MediaCodec — no library needed.
    // Media3's Transformer was tried first but only exports MP4 containers
    // (confirmed via its docs, 2026-08-26); it has no raw/headerless PCM
    // or WAV output, so it can't produce what fromPfd requires.

    // On-device OCR for the Image sample type — the Android analog of
    // iOS's Vision (VNRecognizeTextRequest). Unlike the genai-* family,
    // this is ML Kit's older, stable vision API — verified current version
    // against Maven, 2026-08-27. Script-specific recognizers are separate
    // artifacts; German/French use the base (Latin) one.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    // Added for the language-pack expansion (Korean, Hindi/Devanagari) —
    // version verified against Maven, matching the other script recognizers.
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")

    // A real, dictionary-backed Japanese reading for kanji pronunciation —
    // the Android analog of iOS's CFStringTokenizer-based fix, since
    // Android has no OS-level equivalent. Pure JVM, no native code; only
    // version published on Maven (0.9.0, 2015) but its dictionary data
    // doesn't go stale the way an actively-developed API might.
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
}
