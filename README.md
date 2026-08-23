# Déjà Entendu — Android

Android counterpart to [ncubeeight/deja-entendu](https://github.com/ncubeeight/deja-entendu) (iOS/macOS, SwiftUI). Kept as a **separate repository** deliberately — the two platforms depend on entirely different frameworks and don't share a build system, so development happens independently.

## Current state: empty scaffold

This repo is a bare Kotlin + Jetpack Compose project — it builds and launches to a placeholder screen, but has none of the actual app features yet. Nothing here has been built out; it's a starting point.

What *is* here, ported over from the iOS repo as reference:
- **`AppColors`** (`app/src/main/java/com/ncubeeight/dejaentendu/ui/theme/AppTheme.kt`) — the exact color palette from iOS's `App/AppTheme.swift`, so both apps look like the same product. Keep these two files in sync by hand; there's no shared source between the Swift and Kotlin code.
- This README and the product roadmap below.

## Why nothing else carried over

None of the iOS app's actual source code can run on Android — different language (Swift vs Kotlin), and no shared frameworks:

| | iOS | Android |
|---|---|---|
| UI | SwiftUI | Jetpack Compose |
| Speech-to-text | Apple `Speech`/`SpeechAnalyzer` | Android `SpeechRecognizer` |
| On-device LLM | Apple `FoundationModels` | Gemini Nano via Google's AICore / ML Kit GenAI APIs |
| Project format | Xcode + XcodeGen (`project.yml`) | Gradle |

The on-device LLM gap is the biggest unknown: Apple's `FoundationModels` gives a general-purpose prompt + structured-output API (`@Generable`/`@Guide`, used throughout the iOS app for study notes and word glosses). Android's equivalent isn't a direct match — Gemini Nano is reachable either through **ML Kit GenAI's task-specific APIs** (summarization, proofreading, rewriting — not general prompting) or through **AICore's more general Prompt API**, which has narrower device support (currently limited to select Pixel/Samsung devices with AICore). Whoever picks this up should evaluate current API availability before assuming feature parity with iOS is possible.

## Product roadmap

See [ROADMAP.md](ROADMAP.md) — ported from the iOS repo's roadmap, since the product direction is shared even though the codebases aren't.

## Privacy

Same policy as iOS — on-device only, no data collection, no backend: see the [Privacy Policy](https://gist.github.com/ncubeeight/460f4d5aa4e850c392e4c04475dcdac1).

## Getting started

Requires a JDK (17+) and Android Studio — neither was available in the environment this scaffold was created in, so **this project has not been built or run**. Open the folder in Android Studio; it will offer to generate the Gradle wrapper and sync automatically. `compileSdk`/`targetSdk` are set to 36 and `minSdk` to 24 in `app/build.gradle.kts` — adjust once real feature work (and its device requirements, especially for on-device LLM access) is scoped.
