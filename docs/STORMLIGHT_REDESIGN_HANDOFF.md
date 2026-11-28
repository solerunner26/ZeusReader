# Stormlight v4 redesign — handoff (Claude → Antigravity / owner)

Branch: `claude/jolly-cori-h91uzt` · version `0.7.0-private` (versionCode 7).
**Not merged to `main`, no GitHub Release published** — the owner will merge with Antigravity's consent.

## 1. What changed (UI layer only)
| Area | Files | Summary |
|---|---|---|
| Theme | `ui/theme/ZeusColors.kt`, `ZeusTheme.kt`, `Theme.kt`, `HueAura.kt`, `LiquidGlass.kt`, `ZeusSpacing.kt` (+`ZeusSizes`), `ZeusShapes.kt`, `ZeusType.kt`, `ZeusMotion.kt` | **5 Theme Packs** (= the 5 skies: Thunder Violet/Storm, Northern Mint/Aurora, Rose Dusk/Sunset, Ocean Rain/Monsoon, Lava Gold/Ember), each with **Night** (dark two-tone) and **Daylight** (pale two-tone, ink text) moods. Neon borders (two-tone, animated sweep), gradient shimmer on primary pills/headings, star-field sky. All semantic colours via `ZeusTheme.colors` (`textOnSky`, `accentOnSky`, glass, chips, dock…). E-ink chrome = pure black on white, no motion. |
| Components | `ui/components/ZeusComponents.kt`, `BookCover.kt` | Shared Stormlight kit: top bar, gradient/glass pills, glass icon buttons, chips, segmented control, sticker chips, list rows, switch/slider rows, text fields (`textOnSky` colours), bottom sheet, dialog, empty state, progress ring, snackbar, `rememberDockClearance()` (dock + nav-bar inset), `CappedFontScale`. `BookCover` crops real covers (2:3, top-aligned) or draws a generated two-tone cover. |
| Navigation | `Navigation.kt` | Floating glass dock with neon edge; hides while the keyboard is open; label font-scale capped (fixed 68dp height). Update dialog restyled. |
| Home | `ui/home/HomeScreen.kt` | Greeting + banner (no cropping, aspect-fit), streak sticker (moves to its own row at large font scale), floating 3D cover + charge ring (no longer overlaps the cover title), neon resume card, quick actions, spark + words bento, "On your shelf" row. Stateless `HomeContent` for tests. |
| Olympus | `ui/library/LibraryScreen.kt` | Gradient title, overflow menu (watched folders / offline / words / settings), import orb, search field, cover-flow pager, glass chips, glass grid with progress bars. Grid/snackbar clear the dock. |
| Sparks | `PassagesScreen.kt`, `PassageDetailScreen.kt`, `CardStudioScreen.kt`, `SavePassageSheet.kt` | Star map, constellations, glass spark cards, "Spark faded away · Bring it back", move-to-constellation + dissolve constellation (existing VM functions now reachable). Card Studio: Cover blur / Aurora / Sticker, real book title + author and cover. |
| Words | `ui/vocab/VocabularyScreen.kt` | Review / Saved segmented, flip card (rotationY), Again / Got it / Easy, "All caught up" state. Offline packs screen restyled. |
| Settings / Stats / Online | `SettingsScreen.kt`, `StatsScreen.kt`, `OnlineAcquisitionScreen.kt` | Theme-pack picker, Mood (Daylight/Night/Auto), language, sky, reading, effects (glass, neon, animated neon, glare, motion), tools, updates. Online Books: layout only (ViewModel untouched); pairing token masked with show/hide. |
| Reader | `ui/reader/ReaderChrome.kt` (new), `PdfReaderScreen.kt`, `EpubReaderScreen.kt`, `ImageBookReaderScreen.kt`, `HtmlReaderScreen.kt` | 30dp paper sheet inset 8dp inside the sky; **The Orb** (tap → Constellation tool chain; drag → **Fly** through chapters/pages, slide left to cancel); progress track + prev/next. |
| Reader sheets | `DictionaryBottomSheet.kt` (Zap card), `TypographyBottomSheet.kt` (Vibe check), `TranslatedReadingOverlay.kt` (Translate Lens), `ListenSheet.kt` (new, Listen), `HighlightsSheet.kt` (Glow), `SignatureReadingMoments.kt` (Chapter Strike) | Zap shows English + हिन्दी + ગુજરાતી together, Word / Whole sentence. Listen = karaoke TTS (word boundaries), waveform, ±sentence, speed, voice EN/HI/GU, sleep timer (`TtsSleepTimerController` now actually used). |

Small non-UI touches (all UI-facing, no data/sync/engine logic):
- `reader/prefs/AppUiConfig.kt`: `animatedGlare` / `animatedBorders` now default **on** (Settings → Effects).
- `ui/reader/EpubReaderViewModel.kt`: injects existing `OfflineDictionaryRepository` + `TranslationService` to give EPUB **Zap** and **Listen** (`lookupWord`, `translateSentence`, `saveLookupToVocabulary`, `currentSectionPlainText`). Uses only existing public APIs.
- `ui/theme/AppUiConfigViewModel.kt`: `setThemePack`, `setAnimatedGlare`, `setAnimatedBorders`.
- `android/app/build.gradle.kts`: version bump + `uiCheck` task.

## 2. Bugs found & fixed
1. PDF "Dictionary" button always looked up the hard-coded word **"capillary"** → Zap now uses the tapped word / opens tappable text mode.
2. PDF swipe direction was reversed (left→right went *forward*) → right-to-left = next page.
3. PDF / image-book pan was unclamped (page could be dragged off-screen when zoomed) → clamped.
4. HTML reader reloaded the document on every recomposition (scroll jumped to top) → reloads only when content changes; transparent WebView background (no white flash).
5. EPUB "Save passage" sheet was unreachable (no trigger) → **Spark it** in the Constellation; EPUB had no dictionary/read-aloud → Zap + Listen added.
6. Words: root-tab back arrow popped the only back-stack entry → removed (system back handles it); flashcards looped forever when nothing was due → "All caught up" state.
7. Content lists used `dockBottomClearance` (100dp) without the navigation-bar inset → last items/snackbars hid behind the dock on 3-button nav → `rememberDockClearance()` everywhere.
8. Library top bar had 5 icon actions + title (overlapped on narrow phones / large fonts) → overflow menu.
9. Settings/Stats/Online drew a second animated sky on top of the activity's sky (double animation) → removed; the sky is drawn once.
10. Sky drift animation recomposed the whole app every frame → values now read only in the draw phase.
11. E-ink reader surface never reached app chrome (paper was always Night/Cloud) → E-ink chrome + no motion.
12. Card Studio printed the chapter title as the book title, no author, and Cover blur was permanently disabled → real title/author, cover loaded from `coverPath`.
13. Zap card showed an endless "Looking up…" spinner for an empty word and could show a stale previous word → empty state + result matching.
14. Home banner was clipped by a pill shape and cropped by `FillHeight` → aspect-fit, no clip.
15. Charge ring overlapped the hero cover title; streak sticker was 36dp tall (below touch minimum).
16. Text on generated covers / rings split words at font scale 2.0 → artwork font scale capped.

## 3. Quality gates (Phase 5)
- `StormlightTokenGuardTest` — fails on `Color(0x…)`, hex strings or raw `.dp`/`.sp` in `ui/` (outside `ui/theme/`) and `Navigation.kt`. Currently **0 violations**.
- `DaylightContrastTest` — all 5 packs × Night/Daylight: body ≥ 7:1, secondary & accent ≥ 4.5:1, pill text ≥ 7:1; E-ink ≥ 20:1.
- `StormlightSnapshotMatrixTest` — 8 screens × {Storm, Aurora, Sunset} × {Cloud(Daylight), OLED, E-ink} × fontScale {1.0, 2.0} × {EN, HI, GU} = **432 renders**, each also asserting clickable targets ≥ 36dp (Compose expands hit areas to 48dp).
- `./gradlew uiCheck` (from `android/`) runs all unit tests incl. the above.
- Pre-commit hook: `git config core.hooksPath .githooks` (runs `uiCheck` when `ui/` or `Navigation.kt` changes; works in Git Bash on Windows).
- Baselines: `android/app/src/test/snapshots/stormlight/` is **git-ignored until the owner approves the look**. Then remove that line from `.gitignore`, run `./gradlew :app:recordRoborazziDebug --tests '*StormlightSnapshotMatrixTest*'`, commit, and use `verifyRoborazziDebug` afterwards.

## 4. Not done / for Antigravity
- **Not touched:** `relay/`, `sync/`, `data/`, reader engines, `dictionary/`, `translation/`, `OnlineAcquisitionViewModel.kt`, `PrivateServerClient.kt`, `GitHubAppUpdater.kt`.
- `OnlineAcquisitionViewModel` ships a default relay URL + pairing token in source — consider moving the token out of the APK (security).
- Ask Zeus, Scan, Spread and two-pane screens from `design/screens` have no backing features yet; Fly has no chapter "preview" text (needs engine support).
- On a device, check: Readium fragment inside the rounded paper sheet (if corners are not clipped on some API levels, it is cosmetic only), TTS voice availability for HI/GU, and the Orb drag on gesture-nav phones.
- Build on Windows as before: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug < /dev/null`.
