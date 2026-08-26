# Roadmap

Ported from the [iOS repo's roadmap](https://github.com/ncubeeight/deja-entendu/blob/main/ROADMAP.md) — the product direction is shared across both platforms, even though the codebases are separate. iOS-specific implementation notes below have been reworded for Android; nothing here has been built yet on either platform.

## Feature parity with iOS (the actual first milestone) — done

Status as of 2026-08-26 — the full milestone is built and device-verified
live on a real Pixel 10 Pro: on-device LLM (study notes/word gloss/
flashcard generators), file-based transcription (decode + resample + ML
Kit GenAI Speech Recognition), Vocabulary List / Add Word / Flashcard
screens, the Upload screen (SAF file picker + language-select sheet), the
Home screen (port of iOS's `HomeSummaryView`), and the bottom-tab
navigation shell. See the README's "On-device LLM" and "File-based
transcription" sections for exactly what was verified and the bugs that
surfaced along the way — several only showed up once tested against a
real, realistic-length recording rather than a short synthetic clip.

**Deliberately deferred**, not part of this milestone: a Settings tab
(language filter / theme — `AppSettings.swift` on iOS), and an Android
equivalent of iOS's Share Extension (share text/audio into the app from
another app, e.g. Translate or Voice Memos).

The transcription pipeline was first proven against a synthesized test
clip (macOS `say -v Thomas -o clip.aiff "..."` piped through
`afconvert -f m4af -d aac clip.aiff clip.m4a`), then against a real
~25-minute recording pulled from Google Drive via the actual Upload
screen's file picker — the real recording is what surfaced the ANR and
OOM bugs documented in the README, since the short synthetic clip decoded
too fast to trip either one.

## Full-transcript translation view

Alongside a future word-by-word tooltip lookup (tap a term, see its gloss), offer a toggle to see the *entire* transcript translated into the user's native language at once — for when someone wants full comprehension of a recording rather than looking up individual unfamiliar terms.

Open questions to resolve when this gets picked up:
- Where does this live in the UI — a toggle on the transcript screen, a separate screen, both?
- Does it reuse the same on-device model session as study notes, or need its own (full-transcript translation is a bigger prompt than a single word's gloss, and on-device models generally have small context windows, so long recordings may need chunking)?
- Should it cache the full translation once generated, or regenerate each time the screen appears?

## Flashcards from tooltip lookups

After the full-transcript translation toggle above: let a user turn any word-lookup tooltip into a flashcard. Each flashcard is a dedicated page for one term showing:
- The original term
- Its translation
- A pronunciation guide
- An example sentence using the term, with the term highlighted within it

Open questions to resolve when this gets picked up:
- Storage: likely a local on-device-only store, mirroring whatever pattern the vocabulary list ends up using once it exists on this platform.
- Generation: the on-device LLM would need to produce the pronunciation guide + example sentence, not just a short gloss — a bigger prompt/response than a single-word lookup.
- Review flow: is this just a browsable list of cards, or does it grow into spaced-repetition-style review later?
