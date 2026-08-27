# Déjà Entendu — Android

Android counterpart to [ncubeeight/deja-entendu](https://github.com/ncubeeight/deja-entendu) (iOS/macOS, SwiftUI). Kept as a **separate repository** deliberately — the two platforms depend on entirely different frameworks and don't share a build system, so development happens independently.

## Current state: feature-parity milestone built and device-verified

As of 2026-08-26, the full core app — Home, Upload, Vocabulary, and the
on-device LLM/transcription pipeline underneath them — is built and
confirmed working on a real Pixel 10 Pro:

- **Data layer**: `audio/ImportedRecording.kt`+`ImportedRecordingStore.kt`+`AudioIngestion.kt`, `studynotes/VocabularyEntry.kt`+`VocabularyStore.kt` — JSON-file persistence in `context.filesDir`, mirroring iOS's `Codable`+JSON-in-Documents pattern.
- **Vocabulary List / Add Word / Flashcard screens** (`ui/VocabularyListScreen.kt`, `ui/AddVocabularyWordSheet.kt`, `ui/VocabularyFlashcardScreen.kt`) — ported from iOS's equivalents, using the already-verified `FlashcardGenerator`.
- **File-based transcription** (`transcription/AudioDecoder.kt`, `transcription/SpeechTranscriberService.kt`) — decodes an imported audio file and transcribes it on-device via ML Kit GenAI Speech Recognition.
- **Upload (Audio Samples) screen** (`ui/UploadScreen.kt`) — SAF file picker + language-select sheet, mirroring iOS's `VoiceMemoImportView.swift`.
- **Transcription Runner screen** (`ui/TranscriptionRunnerScreen.kt`) — decode → transcribe → study notes, with tap-a-word-to-add-to-Vocabulary (a Snackbar-confirmed simplification of iOS's tap-then-confirm popover).
- **Home screen** (`ui/HomeScreen.kt`) — port of iOS's `HomeSummaryView.swift`: gradient banner, recent recordings, vocabulary preview grid with placeholder words.
- **Navigation shell** (`ui/AppNavigation.kt`, `ui/Screen.kt`) — bottom-tab `NavHost` (Home / Upload / Vocabulary / Settings) using navigation-compose's type-safe `@Serializable` routes; `MainActivity.kt` now just hosts this, no more smoke-test code.
- **Settings screen** (`ui/SettingsScreen.kt`, `settings/AppSettingsStore.kt`, `settings/AppSettingsState.kt`, `settings/AppColorScheme.kt`) — language filter (which of the 5 languages show in Upload's picker) + System/Light/Dark theme, mirroring iOS's `AppSettings.swift`/`SettingsView.swift`. Added 2026-08-26.
- **`AppColors`** (`app/src/main/java/com/ncubeeight/dejaentendu/ui/theme/AppTheme.kt`) — the exact color palette from iOS's `App/AppTheme.swift`.

All of the above was walked through live on a real Pixel 10 Pro — added a
word, generated a flashcard, imported a real file via the system file
picker, transcribed a genuine ~25-minute recording, and toggled every
Settings control (language filter, all three theme options) — not just
built.

**Settings implementation notes:**
- iOS's `AppTheme.swift` has **no dark variant at all** — every custom
  screen hardcodes its light-palette colors regardless of the system
  scheme, so iOS's Light/Dark/System toggle only ever affects unstyled
  system chrome (Forms, alerts, the keyboard). `AppTheme.kt`'s new
  `DejaEntenduDarkColorScheme` mirrors that same *scope* of effect: it's a
  real dark `MaterialTheme.colorScheme` for Material's own default
  components, while Home/Vocabulary/Flashcard/Upload keep hardcoding
  `AppColors` either way, exactly like iOS.
- Real bug caught by testing in dark mode: `SettingsScreen.kt` initially
  used the hardcoded light-only `AppColors.ink`/`inkSoft` for its own text
  (it's the one screen that doesn't hardcode an `AppColors.background`
  container, so it correctly follows the live theme) — against the new
  dark scheme's near-black background, that made every label nearly
  invisible. Fixed by switching to `MaterialTheme.colorScheme.onBackground`/
  `onSurfaceVariant`, which adapt automatically.
- The theme preference needs to take effect immediately when changed,
  even though `MainActivity`'s theme wrapper (unlike each bottom-tab
  screen) stays mounted across tab switches and never gets recomposed
  from scratch. Solved with `settings/AppSettingsState.kt`, a small shared
  `mutableStateOf` holder — a lighter alternative to threading a callback
  through the whole nav graph.

**Deliberately deferred** (not part of this build-out): an Android
equivalent of iOS's Share Extension (share-into-app from another app,
e.g. Translate).

## Why nothing else carried over

None of the iOS app's actual source code can run on Android — different language (Swift vs Kotlin), and no shared frameworks:

| | iOS | Android |
|---|---|---|
| UI | SwiftUI | Jetpack Compose |
| Speech-to-text | Apple `Speech`/`SpeechAnalyzer` | Android `SpeechRecognizer` (file-based transcription still needs research — see ROADMAP) |
| On-device LLM | Apple `FoundationModels` | ML Kit GenAI **Prompt API** (Gemini Nano), via AICore |
| Project format | Xcode + XcodeGen (`project.yml`) | Gradle |

## On-device LLM: scoped to Gemini Nano v3 devices (Pixel 10+)

Resolved (2026-08-24): this app targets the ML Kit GenAI **Prompt API**
(`com.google.mlkit:genai-prompt`), not the task-specific summarize/rewrite
APIs. It turns out to be a much closer match to iOS's `FoundationModels`
than expected — it has its own `@Generable`/`@Guide` structured-output
annotations (see `StudyNoteGenerator.kt`, `FlashcardGenerator.kt`,
`WordGlossGenerator.kt` in `app/src/main/java/.../studynotes/`), and Gemini
Nano v3 (Pixel 10 and other 2025/2026 flagships — 12GB+ RAM, Snapdragon 8
Elite/Gen 4, Tensor G5 class chips) gives roughly a 32K-token budget, well
above iOS's 4096.

This is **not** a hard device restriction baked into the manifest/Play
Store listing — minSdk is 26 (the Prompt API's own floor), and the app
installs on any qualifying device. The actual gate is runtime, via
`GenerativeModel.checkStatus()` (`AVAILABLE` / `DOWNLOADABLE` /
`UNAVAILABLE`) — the same pattern iOS uses with
`SystemLanguageModel.default.availability`. On a device below the Nano v3
bar, the LLM features report `UNAVAILABLE` and should show a friendly
message rather than crash; core features (transcription, vocabulary list,
manual entry) don't depend on this at all.

**Build-verified (2026-08-25):** `./gradlew :app:assembleDebug` succeeds
with this setup — Kotlin **2.2.0** (not 2.0.20 — the real `genai-prompt`/
`genai-common` AARs ship Kotlin 2.2.0 metadata and fail to link against
2.0.20), KSP **2.2.0-2.0.2**, and `com.google.mlkit:genai-prompt:1.0.0-beta4`
+ `genai-schema:1.0.0-alpha1` (not beta2 — beta2, the version Google's own
docs examples implied, doesn't actually have `GenerateTypedContentRequest`/
`@Generable` support wired up at all; that only landed in beta4, confirmed
by decompiling both). Google's docs describe an API surface ahead of what
beta2 ships — always check the actual Maven Central/`dl.google.com`
`maven-metadata.xml` for the latest version rather than trusting a
doc-page's dependency snippet verbatim. `GenerativeModel` also exposes
`isStructuredOutputFeatureAvailable()` and `isSystemPromptAvailable()` —
finer-grained capability checks beyond `checkStatus()`, both used in the
generators.

**Device-verified (2026-08-26):** ran end-to-end on a real Pixel 10 Pro via
a temporary smoke-test screen in `MainActivity.kt` (calls
`WordGlossGenerator.gloss("Bonjour", "Bonjour")`) — got back a real
structured `WordGloss("Hello")` from on-device Gemini Nano. This surfaced
one real bug: `GenerativeModel.download()`'s `Flow<DownloadStatus>` never
completes on its own (it keeps the flow open even after emitting
`DownloadCompleted`), so a plain `.collect { }` — what all three generators
originally did — hangs forever, even once the download is actually done.
Fixed by using `.first { it is DownloadStatus.DownloadCompleted || it is
DownloadStatus.DownloadFailed }` instead, which stops collecting as soon as
a terminal status appears. This only showed up on real hardware — the
Gradle build had no way to catch it.

**Multilingual quality checkpoint (2026-08-26):** ran `FlashcardGenerator`
on French, Japanese, and Chinese (Simplified) terms on the Pixel 10 —
all three came back natural and correct (pronunciation guide, translation,
example sentence). No reason to scope the app down from the 5 target
languages.

**Toolchain note:** this project runs **AGP 9.3.2** + **Gradle 9.7.1** (up
from AGP 8.7/Gradle 8.9) — required once `navigation-compose` was added.
AGP 9 has *built-in Kotlin support*, which conflicts with applying
`org.jetbrains.kotlin.android` directly (removed from both
`build.gradle.kts` files) and required `android.disallowKotlinSourceSets=false`
in `gradle.properties` (KSP 2.2.0-2.0.2 still uses the old `kotlin.sourceSets`
DSL for its generated *source* directory). Separately — and this one didn't
show up as a build error at all — AGP 9's built-in Kotlin was silently
dropping KSP's generated `META-INF/services/...GenerableProvider` file
(the thing that makes `@Generable` types discoverable at runtime) from the
packaged APK; fixed by explicitly adding
`resources.directories += "build/generated/ksp/<variant>/resources"` to
each `android.sourceSets` block. If a future KSP release for Kotlin 2.2.0+
fixes this natively, that workaround can likely be dropped — verify by
removing it and checking `unzip -l app-debug.apk | grep GenerableProvider`
still finds the file.

**File-based transcription (2026-08-26):** confirmed working end-to-end on
the Pixel 10 — a synthesized French clip (macOS `say` + `afconvert`) came
back almost verbatim. Three things worth knowing:
- `androidx.media3:media3-transformer` **cannot** produce the file
  `AudioSource.fromPfd` needs — Transformer only exports MP4 containers
  (confirmed via its own docs). `AudioDecoder.kt` uses plain
  `MediaExtractor`/`MediaCodec` instead (stable platform API).
- `android.permission.RECORD_AUDIO` is required even for pure file-based,
  `ONE_SHOT` input with no live microphone involved — omitting it fails
  with `ERROR_TYPE_INSUFFICIENT_PERMISSION`. Declared in the manifest;
  `TranscriptionRunnerScreen.kt` requests it at runtime (API 23+) before
  transcribing.
- **A 5-second test clip hid two crash bugs a real ~25-minute recording
  exposed**: (1) an ANR — `AudioDecoder`'s decode loop is a plain blocking
  function, not a suspend fun, and must be wrapped in
  `withContext(Dispatchers.IO)` or it blocks the UI thread (a 5-second clip
  decodes fast enough to never trip the ANR watchdog; a real recording
  doesn't); (2) an `OutOfMemoryError` — the first version of the resampler
  read the *entire* native-rate decoded file into one `ByteArray` before
  resampling it (~150MB for a 25-minute recording). `AudioDecoder.kt` is
  now a genuinely single-pass streaming decoder: it resamples one
  `MediaCodec` output buffer at a time, carrying only its fractional phase
  and last sample as state, so memory use stays flat regardless of
  recording length. Both bugs were only found by testing against a real,
  realistic-length recording — worth remembering for any future work on
  this pipeline (or similar audio-processing code): a trivial synthetic
  test proves logic, not scale.

## Product roadmap

See [ROADMAP.md](ROADMAP.md) — ported from the iOS repo's roadmap, since the product direction is shared even though the codebases aren't.

## Privacy

Same policy as iOS — on-device only, no data collection, no backend: see the [Privacy Policy](https://gist.github.com/ncubeeight/460f4d5aa4e850c392e4c04475dcdac1).

## Getting started

Requires a JDK (17+) and Android Studio. `./gradlew :app:installDebug` builds and installs cleanly as of 2026-08-26 — this whole app has been run and manually exercised on a real Pixel 10 Pro, not just built. `compileSdk`/`targetSdk` are 36 and `minSdk` is 26 in `app/build.gradle.kts`; the project uses AGP 9.3.2 + Gradle 9.7.1 (see the toolchain note above). Android Studio may warn that AGP doesn't officially support `compileSdk = 36` yet — harmless, or set `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` to silence it.
