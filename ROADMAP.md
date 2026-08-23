# Roadmap

Ported from the [iOS repo's roadmap](https://github.com/ncubeeight/deja-entendu/blob/main/ROADMAP.md) — the product direction is shared across both platforms, even though the codebases are separate. iOS-specific implementation notes below have been reworded for Android; nothing here has been built yet on either platform.

## Feature parity with iOS (the actual first milestone)

Before either idea below applies, this repo needs the core pipeline iOS already has: audio import, on-device transcription (`SpeechRecognizer`), and on-device study notes. See the README's framework-mapping table and the AICore/ML Kit GenAI caveat before assuming 1:1 parity is possible.

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
