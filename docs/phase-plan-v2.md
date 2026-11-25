# ZeusReader Phase Plan v2 (Ink & Hue + Passages & Share Cards)

> **Authority & Precedence:**
> - **Features & Phase Placement:** [`docs/SCOPE-ADDENDUM-v2.md`](SCOPE-ADDENDUM-v2.md)
> - **Visuals & Tokens:** [`design/DESIGN.md`](../design/DESIGN.md) (v2 "Ink & Hue") and `design/screens/*.dc.html`
> - **Core Architecture & Constraints:** [`GEMINI.md`](../GEMINI.md)
> - **Obsolete:** All v1 design tokens (v1 Day/Sepia/Night/OLED/E-ink colours, green/terracotta language tags).

---

## Current Baseline

- **Phases 0–6 are implemented and committed** on `main` (most recent feature commit: `ad9f7dc feat(translation): implement Phase 6 translated reading mode and OCR fallback`, followed by `0ba7dce docs: design v2 Ink & Hue + passages scope addendum`).
- Because **Phase 3 is already done**, `SCOPE-ADDENDUM-v2.md` §A mandates inserting **Phase 3R (Re-theme)** and **Phase 3B (Passages & Share Cards)** right after the current phase, before resuming **Phase 7**.
- **Paused WIP in working tree:** `server/pyproject.toml` and `server/app/{__init__,auth,config,db}.py` (Phase 7 scaffolding) will remain paused until Phase 3R and Phase 3B are verified and committed.
- **Rules in effect:** Material 3 only; no third colour in chrome; touch targets $\ge 48\text{dp}$; all Phase 3R/3B features 100% offline; ask before adding any new dependency; never build screens for features outside `SCOPE-ADDENDUM-v2.md`; one commit per sub-step with addendum acceptance tests run after each.

---

## Updated Phase List

### Completed Phases (0–6)

| Phase | Summary | Status |
|---|---|---|
| **0. Setup, Harness & Connector Proof** | Repo scaffold, harness/hooks, Gradle catalogs, Hilt/Room/Compose shell, connector spike | Done |
| **1. Library, Import & PDF/Images** | Local import, SHA-256 dedup, magic-byte/container checks, `Work/Edition/FileAsset`, MuPDF PDF + image books | Done |
| **2. EPUB (Readium) & Shared Shell** | `ReaderEngine`, Readium EPUB (active content blocked), unified `Locator`, `:reader` process, TOC, search | Done |
| **3. Ergonomic Reader & First Private Release** | One-handed reader ergonomics, highlights/notes/Markdown export, PDF crop/zoom/reflow | Done (`d29a9d5`) |
| **4. Expanded Formats** | XPS/OXPS, CBZ, sandboxed HTML, guarded TIFF (30 MB / 100 pp / RAM cap), `.oxp`/unknown tracking | Done (`db9fc70`) |
| **5. Offline Dictionary & Vocab** | WordNet + `kaikki.org` Hi/Gu SQLite, *Prepare for offline reading*, ML Kit `en→hi`/`en→gu`, TTS, SM-2 flashcards | Done (`8ca28b8`) |
| **6. Translated Reading Mode** | Sentence-aligned translation (EPUB, text PDF, scanned PDF OCR), passage-anchored cache, Interlinear/Paired modes, glossary | Done (`ad9f7dc`) |

---

### Pre-Implementation Gate (After Plan Approval)

- **Design Gap Audit:** Produce [`docs/design-gap-report-v2.md`](design-gap-report-v2.md) comparing the current Android codebase against `design/DESIGN.md` v2 and `design/screens/*.dc.html` (tokens, typography, surfaces, navigation, components, reading moments, accessibility, and missing/divergent screens).

---

### Phase 3R — Re-theme: Ink & Hue & Signature Reading Moments (NEW)

**Depends On:** Phase 3 (already done; runs after Phase 6).  
**Reference:** `SCOPE-ADDENDUM-v2.md` §A, `design/DESIGN.md` §§1–7, 9–10.

#### Sub-steps (one commit per sub-step)

1. **`HueTheme`, Presets & Auto-from-cover:**
   - Implement pure `HueTheme(hue: Float, dark: Boolean): ColorScheme` with fixed HSL lightness steps from `DESIGN.md` §2.
   - Support 6 presets: Indigo Dusk (`250°`, default), Saffron (`32°`), Moss (`140°`), Lagoon (`186°`), Rose (`345°`), Plum (`290°`), plus **Auto from cover** (Palette API dominant swatch hue angle only; lightness/saturation fixed by table).
   - Animate hue transitions via `animateFloatAsState` (400 ms) when animations are enabled.
   - Enforce two-tone chrome rule: no third colour anywhere; language tags `HI = hue filled`, `GU = ink filled`; warnings = `hueContainer` badge + warning icon + text.
2. **Reading Surfaces & Bundled Fonts:**
   - Implement the 5 `ReaderSurface` palettes (`Paper`, `Dusk`, `Night`, `OLED`, `E-ink`), replacing v1 Day/Sepia/Night/OLED/E-ink tokens.
   - Bundle OFL fonts `Fraunces` (display titles, passage text, cards), `Noto Serif Devanagari`, and `Noto Serif Gujarati` alongside existing `Literata`, `Atkinson Hyperlegible`, `Noto Sans Devanagari`, and `Noto Sans Gujarati`.
   - Apply Indic text rules (`includeFontPadding = false`, `LineHeightStyle(Center, Trim.None)`, per-paragraph `localeList`).
3. **`AppUiConfig` v2 & DataStore Migration:**
   - Update `AppUiConfig` to v2 schema (`appTheme`, `themeHue`, `hueFromCover`, `auraIntensity`, `readerSurface`, `focusRibbon`, `glassUi`, `aestheticElements`, `animatedGlare`, `animatedBorders`, `masterAnimations`, `embossedEffects`, `pointerHoverEffects`, `appLanguage`).
   - Implement DataStore migration from v1 preserving user surface choice (`Day → Paper`, `Sepia → Dusk`, `Night → Night`, `OLED → OLED`, `E-ink → E-ink`).
4. **`HueAura` & Screen Re-theming:**
   - Implement `HueAura` composable behind Library, Home, Vocab, Settings (not behind reading text; faint top vignette on Paper/Dusk/Night reading surface only, none on OLED/E-ink).
   - Respect ±10° 14 s drift, intensity slider (0–100%, default 40%), and immediate disablement under Reduce Motion, Battery Saver, E-ink, or `masterAnimations = false`.
   - Update existing screens (`Library`, `Reader`, `Typography`, `AltB-Sheet`, `AltC-Paired`, `AltA-Inline`, `Scanned-OCR`, `Settings`, `TwoPane`) to Material 3 Expressive components, 8dp grid, radii `12/20/28/full`, and targets $\ge 48\text{dp}$ (reader toolbar `52dp`, primary actions `56dp`).
5. **Signature Reading Moments:**
   - **Fore-edge progress:** ~40 hairlines at right margin (`12dp` wide); read = `1dp` light, remaining = `2dp` ink-soft, current = `3dp` hue; tap/drag to jump; TalkBack `"X percent read"`.
   - **Focus ribbon:** toggle in Display panel; keeps 3-line band at 100% opacity with hairline hue rules, rest at 35%; moves with taps/volume keys.
   - **Margin marks:** `4dp` hue bar in left margin beside saved highlights/passages; tap opens detail.
   - **Ribbon bookmark:** hue ribbon at top-right (`40dp` tall, `64dp` when chrome visible); pull down = bookmark + toolbar button.
   - **Chapter breath:** end-of-chapter card showing time read, passages saved (0 until 3B), words learned, next chapter.

#### Exit Condition & Acceptance Tests (Phase 3R)

1. **Unit:** `HueTheme` contrast $\ge 4.5:1$ (text) and $\ge 3:1$ (icons) for hues `0–345` (step `15`), light + dark.
2. **Runtime re-theme:** Changing hue preset recolours the entire app in $\le 400\text{ ms}$ without restarting; persists across app restart.
3. **Auto from cover:** Opening a book with a green cover changes the hue; turning it off restores the chosen preset.
4. **Motion/Aura guards:** Aura and all motion are off when Reduce Motion, Battery Saver, E-ink, or `masterAnimations = false` is active.
5. **DataStore migration:** v1 install preserves theme choice (`Day→Paper`, `Sepia→Dusk`, `Night`, `OLED`, `E-ink`).
6. **Visual & A11y gates:** Roborazzi across screens $\times$ hues `{32, 140, 250}` $\times$ light/dark $\times$ `100%/200%` font scale $\times$ `EN/HI/GU`; touch targets $\ge 48\text{dp}$; TalkBack `contentDescription` verified.

---

### Phase 3B — Passages & Share Cards (NEW)

**Depends On:** Phase 3R (and Phase 3 text selection/highlights).  
**Reference:** `SCOPE-ADDENDUM-v2.md` §B, `design/DESIGN.md` §§7–8, 10.

#### Sub-steps (one commit per sub-step)

1. **Room Schema, FTS4 & 30-day Cleanup:**
   - Add `PassageFolder`, `Passage`, `PassageTag`, and FTS4 table over `text`, `note`, and tags via a validated Room migration.
   - Add WorkManager periodic task to hard-delete soft-deleted rows (`deletedAt` older than 30 days).
2. **Reader Selection & Passage Capture:**
   - Enable text selection in EPUB (Readium) and text PDFs (MuPDF structured text; scanned PDFs only after OCR text exists).
   - Floating selection toolbar (`Highlight · Save passage · Translate · More [Define, Copy, Create card]`) per `Selection.dc.html`.
   - Save sheet: quote preview, folder chips + `New folder`, optional note, `Save` / `Save & make card`.
   - Margin mark tap opens Passage detail; Translated Reading Mode saves translated text (`language = hi|gu`) while keeping English source in `originalText`.
3. **Passages Tab & Passage Detail:**
   - Add **Passages** as the 5th item in the short bottom navigation bar (`Home`, `Library`, `Passages`, `Vocab`, `Settings`).
   - Implement `Passages` screen (`Passages.dc.html`): Aura bg, title + count, search, folder carousel, sort (`Recent` / `By book` / `Shared`), passage cards (2-line Fraunces italic quote, source, folder chip, Share-as-card, Delete with 5 s Undo Snackbar), FAB menu (`New passage` manual entry without locator, `New folder`), long-press multi-select (`Move`, `Delete`, `Share`).
   - Implement `PassageDetail` screen (`PassageDetail.dc.html`): edit text (trim allowed, `originalText` & `locatorJson` preserved), source card with `Open` (jumps to locator; disabled with explanation for manual passages), note, folder dropdown, tags, dates, bottom button group (`Delete` with confirm dialog, `Move`, `Create card`).
4. **Card Studio & Offline Sharing:**
   - Implement `CardStudio` screen (`CardStudio.dc.html`): live preview, styles (`Cover blur` / `Ink` / `Paper`), sizes (`1:1 1080×1080`, `4:5 1080×1350`, `9:16 1080×1920`), format (`PNG` default / `JPG` quality 92).
   - Implement off-screen full-pixel rendering (`rememberGraphicsLayer` $\rightarrow$ `toImageBitmap()`); `Cover blur` uses 18dp blur (`RenderEffect` API 31+; downscale $\times 8$ + box blur on older) + black scrim gradient `55%→68%` + `#FFFFFF` text; missing cover disables `Cover blur` with reason and preselects `Ink`.
   - Auto-shrink passage font (min `15px` at `1080`-wide equivalent; prompt user to trim if still too long); Indic fonts (`Noto Serif Devanagari` / `Noto Serif Gujarati`) without clipped matras.
   - `Save to gallery` via `MediaStore` (`Pictures/ZeusReader/`, no storage permission needed on API 29+); `Share…` via app cache $\rightarrow$ `FileProvider` URI $\rightarrow$ `Intent.ACTION_SEND` + `FLAG_GRANT_READ_URI_PERMISSION` + `ClipData` + `Intent.createChooser`, incrementing `shareCount`. 100% offline.

#### Exit Condition & Acceptance Tests (Phase 3B)

1. Select 2 sentences in an EPUB $\rightarrow$ Save to new folder `"Exam quotes"` $\rightarrow$ appears in Passages with source and page; margin mark visible in reader.
2. Edit passage text and note, move to another folder, restart app $\rightarrow$ changes persist; **Open** jumps to exact location.
3. Delete $\rightarrow$ Undo restores it; delete again and wait 5 s $\rightarrow$ gone from list (soft-deleted row remains for sync).
4. Manual `"New passage"` for a library book saves without a locator; **Open** is disabled with an explanation.
5. Card studio: each style $\times$ size exports PNG and JPG of exact pixel size; Hindi and Gujarati passages render without clipped matras.
6. Golden tests: 3 styles $\times$ 3 sizes $\times$ 3 scripts; text contrast $\ge 4.5:1$ on a pure-white cover and a pure-black cover in `Cover blur` style.
7. `Share…` opens system share sheet and delivers image; `Save to gallery` shows it in Photos.
8. All features work in airplane mode.

---

### Phase 7 — Private Server, Acquisition & Sync (+ Addendum B.6)

**Depends On:** Phase 6, Phase 3R, Phase 3B.

- **Original `GEMINI.md` Phase 7 deliverables:** FastAPI + SQLite + Docker (Windows 11 + optional Oracle ARM64), Tailscale + QR pairing, Open SLUM status reader, verified provider connector(s), 2-stage transfer queue, browser handoff (`Share → ZeusReader`), tombstone sync, 1 working sample conversion, 2-mode encrypted backup.
- **Addendum B.6 additions:**
  - Sync `PassageFolder`, `Passage`, and `PassageTag` using last-write-wins + tombstones (`deletedAt`).
  - Include `PassageFolder`, `Passage`, and `PassageTag` in encrypted backups and clean-install restore tests.
  - Card images are **not** synced (re-rendered on demand).

---

### Phase 8 — Optional Server AI (+ Addendum B.6)

**Depends On:** Phase 7.

- **Original `GEMINI.md` Phase 8 deliverables:** Independently toggleable server AI (Sarvam-Translate benchmarked before default, Explain this passage, chapter summaries, smart search, auto-metadata, ask-the-book RAG with locator citations, vocab quizzes, optional server OCR).
- **Addendum B.6 additions:**
  - Add **"Explain this passage"** and **"Suggest tags"** actions on `PassageDetail`.

---

### Phase 9 — Polish, Benchmarks & Hardening (+ Addendum B.6)

**Depends On:** Phase 7 (and Phase 3R/3B).

- **Original `GEMINI.md` Phase 9 deliverables:** Stats screen, TTS sleep timer + foreground service polish, Macrobenchmark on Google Pixel 10 Pro, R8 shrinking, full TalkBack/contrast pass, maintenance release checklist.
- **Addendum B.6 additions:**
  - Chapter-breath card displays `"passages saved this chapter"`.

---

### Phase 10 — Optional Legacy Formats

**Depends On:** Phase 7.

- **Unchanged from `GEMINI.md`:** Qualify `MOBI`, `AZW3`, `KF8`, DRM-free `AZW`, `DOC`, `DOCX` via server Calibre/LibreOffice conversion.
