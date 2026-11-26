# ZeusReader — Pending Work & Claude Code Handoff

> **Created:** 2026-11-25  
> **Current `HEAD`:** `d12ad1f` (`main`)  
> **Authoritative Specs:** `GEMINI.md`, `docs/SCOPE-ADDENDUM-v2.md`, `design/DESIGN.md` (v2 "Ink & Hue"), and `design/screens/*.dc.html`.

---

## 1. Verified Baseline (Already Implemented)

- **Android (`./gradlew.bat testDebugUnitTest lintDebug assembleRelease`):** Builds cleanly with R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`), all unit tests and lint checks pass.
- **Server (`python -m pytest -q`):** 12 tests pass (`server/tests/`).
- **Implemented modules:** Core library/importer, MuPDF PDF/ImageBook/XPS/OXPS/CBZ/TIFF, Readium EPUB (`:reader` process), Sandboxed HTML, Offline WordNet+Kaikki dictionary & SM-2 flashcards, Translated Reading Mode + OCR fallback, Ink & Hue `HueTheme` + `HueAuraBackground` + `AppUiConfig` v2, Passages + `CardStudio`, `StatsScreen`, `TtsSleepTimer`, `:benchmark` module, `OnlineAcquisitionScreen`, `server/app/` FastAPI endpoints, `server/Dockerfile`, and `server/docker-compose.yml`.

---

## 2. UI/UX Issues & Design v2 Gaps (Priority #1)

The user reported **"Screen showing me half in the UI"** and that UI/UX polish is incomplete against `design/DESIGN.md` and `design/screens/*.dc.html`:

1. **Nested `Scaffold` / Double Insets & Clipping ("Half Screen" Bug):**
   - `android/app/src/main/kotlin/com/solerunner26/zeusreader/Navigation.kt` wraps `NavDisplay` in a `Scaffold(bottomBar = ...)` and applies `.padding(padding)` to the container `Box`.
   - Individual screens (`LibraryScreen`, `PassagesScreen`, `SettingsScreen`, `VocabularyScreen`, `StatsScreen`, `OnlineAcquisitionScreen`) each render their own inner `Scaffold` **without** `contentWindowInsets = WindowInsets(0)`, causing double system-bar/navigation-bar inset consumption and squeezed/half-height content on some screens.
   - Fix: Strip redundant inner `Scaffold` insets (`contentWindowInsets = WindowInsets(0)`) or unify top/bottom chrome cleanly across all screens, and verify on a real device/emulator.

2. **Missing/Incomplete `design/DESIGN.md` v2 Screen Parity (`design/screens/*.dc.html`):**
   - **Home vs. Library (`Library.dc.html`):** `Navigation.kt` currently routes both `Home` and `Library` tabs to `LibraryRoute` (and `Library` tab `selected = false`). `LibraryScreen.kt` still uses v1-style vertical cards instead of the v2 `Library.dc.html` layout (Aura blobs, glass docked search bar, filter chips, horizontal continue-reading carousel, and book-cover grid).
   - **Reader Chrome (`Reader.dc.html` & `Typography.dc.html`):** Verify `EpubReaderScreen.kt`, `PdfReaderScreen.kt`, `ImageBookReaderScreen.kt`, and `TypographyBottomSheet.kt` match the v2 immersive chrome (hidden chrome showing only page + time left, fore-edge, ribbon; tap centre $\rightarrow$ top pill + M3 Expressive 52dp floating toolbar: `Contents`, `Aa`, `Translate`, `Passages`, `Listen`).
   - **Selection & Translation Sheets (`Selection.dc.html`, `AltB-Sheet.dc.html`, `AltC-Paired.dc.html`, `Scanned-OCR.dc.html`):** Audit HI (`hue` filled) / GU (`ink` filled) tags, OCR warning badge (`hueContainer` + warning icon + text), and two-pane (`TwoPane.dc.html`) adaptive layout.
   - **Theme Propagation in New Screens:** `StatsScreen.kt` and `OnlineAcquisitionScreen.kt` currently pass `AppUiConfig()` + `isSystemInDarkTheme()` to `HueAuraBackground` instead of observing `AppUiConfigViewModel.config`. Wire them to the shared `AppUiConfig` state so user hue/theme/aura settings apply everywhere.

3. **Real-Device Visual & Accessibility Verification (Unverified on Hardware):**
   - Run Roborazzi snapshot updates/diffs across `hues {32, 140, 250} × light/dark × 100%/200% font scale × EN/HI/GU` and visually inspect the generated PNGs.
   - Full TalkBack focus-order & $\ge 48\text{dp}$ touch-target pass on physical **Google Pixel 10 Pro**.

---

## 3. Pending Feature & Infrastructure Work

### Phase 7 — Private Server, Acquisition & Sync (Remaining)
- **Oracle Cloud Always Free (Singapore, ARM64):** User still needs to sign in at `https://www.oracle.com/cloud/sign-in.html`, verify Always Free Ampere A1 (`VM.Standard.A1.Flex`) quota in `ap-singapore-1`, provision Ubuntu ARM64 VM, install Docker + Tailscale + UFW, and deploy `server/docker-compose.yml`.
- **QR Pairing:** Add QR code scanner/generator for Tailscale host + pairing token (`SettingsScreen` / `OnlineAcquisitionScreen`).
- **Browser Handoff (`Share → ZeusReader`):** Complete matching of browser-downloaded files shared via `Intent.ACTION_SEND` to pending `NeedsBrowser` transfer jobs.
- **Stage 2 Download (Server → Phone):** Wire `OnlineAcquisitionViewModel` to download `ReadyOnServer` files from `/v1/transfers/{job_id}/file`, verify SHA-256 + magic bytes, and import into local `LibraryRepository` (`AvailableOffline`).
- **Two-Mode Encrypted Backup & Clean-Install Restore Rehearsal:** Add UI trigger in Settings/Online screen for exporting/restoring both `library_data_only` and `library_data_plus_books` backups and test on a clean install.
- **Maestro E2E:** Run `search → edition → transfer → offline` flow on Pixel 10 Pro.

### Phase 8 — Optional Server AI (Remaining)
- Wire real **Ollama** backend in `server/app/main.py` (`Sarvam-Translate` Q4 GGUF + 4B–8B instruct model + `sqlite-vec` RAG) replacing the current stub responses on `/v1/ai/explain-passage` and `/v1/ai/suggest-tags`.
- Add remaining AI endpoints: chapter summaries, smart search, auto-metadata (with user approval), ask-the-book RAG with locator citations, vocab quizzes, and optional server OCR.
- Run and record host AI benchmark table (Hi/Gu quality, latency, peak RAM).

### Phase 9 — Polish, Benchmarks & Hardening (Remaining)
- **Physical Pixel 10 Pro Macrobenchmark:** Connect device via ADB and run `:benchmark:connectedBenchmarkAndroidTest` (verify cold startup `< 1.5s` and frame timing).
- **Signed Private Release APK:** Configure release keystore (outside repo), run `assembleRelease`, verify with `apksigner verify --verbose`, generate SHA-256, and test **(1) clean install** + **(2) in-place upgrade** on Pixel 10 Pro.
- **Chapter Breath Live Count:** Wire `ChapterBreathCard` in `EpubReaderScreen` / `PdfReaderScreen` to display real `passagesSaved` count for the current chapter from `PassageDao`.

### Phase 10 — Optional Legacy Formats (Remaining)
- Connect `server/app/converter.py` to actual Calibre (`ebook-convert`) and LibreOffice headless execution inside the network-isolated `converter` container (`server/docker-compose.yml`).
- Add Android upload/convert UI flow for `.mobi`, `.azw3`, `.kf8`, `.azw`, `.doc`, `.docx` and verify with real fixture files.

---

## 4. Repo & Working Tree Housekeeping

- **Untracked files in repo root/docs/design** (from `ZeusReader-design-handoff-v2.zip` + planning):
  - Commit `design/DESIGN.md`, `design/screens/`, `docs/SCOPE-ADDENDUM-v2.md`, `docs/design-gap-report-v2.md`, `docs/phase-plan-v2.md`, and `docs/PENDING-HANDOFF.md` (and remove or `.gitignore` `ZeusReader-design-handoff-v2.zip` and `ZeusReader-design-handoff-v2/`).
- **CRLF line-ending diffs:** Normalize or restore `README.md`, `fixtures/*.json`, and `zeusreader_gemini_execution_handoff_package.md`.
- **Missing phase docs:** `docs/phase-0.md` through `docs/phase-8.md` and `docs/releases/` were never written (only `docs/phase-9.md` exists).
