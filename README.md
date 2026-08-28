# Déjà Entendu — Android

Android counterpart to [ncubeeight/deja-entendu](https://github.com/ncubeeight/deja-entendu) (iOS/macOS, SwiftUI). Kept as a **separate repository** deliberately — the two platforms depend on entirely different frameworks and don't share a build system, so development happens independently.

## Current state: feature-parity milestone built and device-verified

As of 2026-08-26, the full core app — Home, Upload, Vocabulary, and the
on-device LLM/transcription pipeline underneath them — is built and
confirmed working on a real Pixel 10 Pro:

- **Data layer**: `audio/ImportedRecording.kt`+`ImportedRecordingStore.kt`+`AudioIngestion.kt`, `studynotes/VocabularyEntry.kt`+`VocabularyStore.kt` — JSON-file persistence in `context.filesDir`, mirroring iOS's `Codable`+JSON-in-Documents pattern.
- **Vocabulary List / Add Word / Flashcard screens** (`ui/VocabularyListScreen.kt`, `ui/AddVocabularyWordSheet.kt`, `ui/VocabularyFlashcardScreen.kt`) — ported from iOS's equivalents, using the already-verified `FlashcardGenerator`.
- **File-based transcription** (`transcription/AudioDecoder.kt`, `transcription/SpeechTranscriberService.kt`) — decodes an imported audio file and transcribes it on-device via ML Kit GenAI Speech Recognition.
- **Samples screen** (`ui/SamplesScreen.kt`) — unified audio/text/image import list (see the dated section below); SAF file picker + language-select sheet for audio, mirroring iOS's `VoiceMemoImportView.swift`/`SamplesView.swift`.
- **Transcription Runner screen** (`ui/TranscriptionRunnerScreen.kt`) — decode/prepare → transcribe → study notes, with tap-a-word-to-add-to-Vocabulary (a Snackbar-confirmed simplification of iOS's tap-then-confirm popover).
- **Home screen** (`ui/HomeScreen.kt`) — port of iOS's `HomeSummaryView.swift`: gradient banner, recent samples (all three kinds), vocabulary preview grid with placeholder words.
- **Navigation shell** (`ui/AppNavigation.kt`, `ui/Screen.kt`) — bottom-tab `NavHost` (Home / Samples / Vocabulary / Settings) using navigation-compose's type-safe `@Serializable` routes; `MainActivity.kt` now just hosts this, no more smoke-test code.
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

**Home screen tile redesign + working placeholder demo (2026-08-27):**
`ui/HomeScreen.kt`'s "Words to review" tiles now show term / translation /
language on three separate rows in a uniformly-sized box (a real 3-column
grid via a small `WordGrid` helper — chunking into rows rather than a
`LazyVerticalGrid`, since the item count here is always small and nesting
a lazy grid inside the screen's own scrollable `Column` isn't worth the
ceremony). Real vocabulary entries use the same 3-row layout, degrading
gracefully when translation/language aren't known yet (e.g. a manually-
typed word before its flashcard has been opened once).

The example/placeholder words are no longer just decorative: tapping one
creates a real `VocabularyEntry` (with that word's language) and opens its
flashcard immediately, generating real pronunciation/translation/example
content on-device — the same path any other vocabulary entry goes through.
Once any word (placeholder-triggered or manually added) exists, this
section switches to showing real vocabulary instead of the example set,
so the demo naturally steps aside as the user's real list grows; from
there they can "×" a demo word out like anything else. Verified live: this
is exactly how it played out testing on-device — tapping "Bonjour"
produced a real flashcard, and Home showed it afterward instead of the
other four placeholders.

**Speak buttons on the Flashcard screen (2026-08-27):** `ui/
VocabularyFlashcardScreen.kt` now has two speaker buttons — next to the
term and next to the example sentence — mirroring iOS's
`VocabularyFlashcardView.swift` (`AVSpeechSynthesizer`). Android's
`android.speech.tts.TextToSpeech` is the equivalent; no new dependency
needed (`ui/rememberTextToSpeech.kt` wraps its async init/shutdown as a
small Composable). Same availability logic as iOS: `isLanguageAvailable()`
gates the button and shows an explanatory caption when a voice for that
language isn't installed on the device, but an entry with *no* known
language (manual entry) is still treated as available and just falls back
to the device's default voice, rather than being blocked. Verified live on
the Pixel 10 — tapped both buttons on a French entry, real TTS engine
(`com.google.android.tts`) audio played, no crash.

**Deliberately deferred** (not part of this build-out): an Android
equivalent of iOS's Share Extension (share-into-app from another app,
e.g. Translate).

**Samples feature — unified audio/text/image import (2026-08-27):** ported
iOS's generalization of the old audio-only import flow into a single
"Samples" concept. `ui/UploadScreen.kt` is gone, replaced by
`ui/SamplesScreen.kt` — a filterable list (`SingleChoiceSegmentedButtonRow`:
All/Audio/Text/Image) combining three JSON stores
(`samples/ImportedTextSample(Store).kt`, `samples/ImportedImageSample
(Store).kt`, plus the existing recording store) via a type-erased
`samples/AnySample.kt` sealed interface, mirroring iOS's `AnySample.swift`.
`ui/TextImportSheet.kt` and `ui/ImageImportSheet.kt` are the add flows
(mirroring `TextImportView.swift`/`ImageImportView.swift`); the shared
transcript → study-notes → tap-word-to-vocabulary pipeline in `ui/
TranscriptionRunnerScreen.kt` now takes a `transcription/SampleInput.kt`
(audio needs real decode+transcribe, text/image already have resolved
text and skip straight to study notes) instead of a bare recording. Home's
"Continue studying" and add-dialog, and the nav shell's Upload tab
(renamed **Samples**, `Icons.Filled.Inbox`), were updated to match.

Image OCR uses **ML Kit Text Recognition v2** in place of iOS's Vision —
verified against real Maven metadata, not assumed docs:
`com.google.mlkit:text-recognition:16.0.1` (Latin/German/French) plus
`text-recognition-chinese`/`text-recognition-japanese`, selected per
`SupportedLanguage` in `samples/ImageIngestion.kt`. **Verified on the
Pixel 10** via a temporary smoke-test screen (`copyAndRecognizeText`
called directly against synthetic French/Japanese/Chinese photos, no UI
in front of it yet) before building `ImageImportSheet.kt` on top — all
three recognized text back essentially verbatim, confirming the
recognizer-selection logic works before layering the pick/review/save UI
on it. The photo-picker → OCR → save round trip through the actual
`ImageImportSheet` UI was exercised as far as opening the picker and
handling cancellation cleanly; picking a specific photo end-to-end
through the picker wasn't automated (ADB-driving Google Photos' picker UI
turned out to be impractical — worth a quick manual check).

Two real bugs surfaced while building this, both worth remembering for
any future `ModalBottomSheet` work:
- **Sheets with a keyboard need `Modifier.imePadding()`.** `TextImportSheet`/
  `ImageImportSheet` (and the pre-existing `AddVocabularyWordSheet`) laid
  out their Save/Cancel buttons *behind* where the on-screen keyboard
  physically covers the screen — Compose measured them at their full,
  keyboard-less height, so the buttons existed in the layout but were
  genuinely untappable (the IME is a separate window on top). Fixed by
  adding `.verticalScroll(rememberScrollState()).imePadding()` to each
  sheet's content `Column`, so the sheet actually shrinks/scrolls above
  the keyboard instead of extending behind it.
- **Bottom-nav tab switching could land on the wrong screen.** `ui/
  AppNavigation.kt`'s tab-click handler used the standard `popUpTo(start)
  { saveState = true }; launchSingleTop = true; restoreState = true`
  pattern from Google's own bottom-nav sample. That pattern assumes every
  destination is a direct tab; once a push destination (`Transcription`)
  sits on top of a tab, the combination reproducibly *restored the
  pushed Transcription screen instead of the tapped tab* — tapping
  Home from Transcription, for instance, would silently reopen
  Transcription. Fixed by dropping `saveState`/`restoreState` entirely:
  a tab switch now always pops cleanly back to the start destination
  first. Caught by manually walking Home → sample → tab-bar taps on the
  device — it never showed up as a build or lint issue.

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
