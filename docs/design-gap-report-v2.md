# ZeusReader Design Gap Report v2 (Codebase vs `design/DESIGN.md` v2)

> **Audit Scope:** Compares the current Android codebase (`android/app/src/main/`) against [`design/DESIGN.md`](../design/DESIGN.md) v2 ("Ink & Hue"), `design/screens/*.dc.html`, and [`docs/SCOPE-ADDENDUM-v2.md`](SCOPE-ADDENDUM-v2.md).
> **Authority:** `SCOPE-ADDENDUM-v2.md` for features/phases; `DESIGN.md` v2 for visuals.

---

## 1. Colour System & Theme Architecture

| Area | `DESIGN.md` v2 Requirement | Current Codebase | Gap / Action |
|---|---|---|---|
| **Two-tone Hue system** | Pure `HueTheme(hue: Float, dark: Boolean): ColorScheme` with fixed HSL lightness steps (`surface`, `paper`, `ink`, `inkSoft`, `hue`, `onHue`, `hueContainer`, `outline`, `highlight`), tested for WCAG AA across `0–345` step `15`. | [`Color.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/theme/Color.kt) & [`Theme.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/theme/Theme.kt) use static v1 green/cream palettes (`ZeusPrimaryLight = 0xFF4A5D4E`, `ZeusBackgroundLight = 0xFFFAF8F3`, etc.). | **Missing:** Replace static v1 palettes with `HueTheme(hue, dark)` + `animateFloatAsState` (400 ms) + unit contrast tests. |
| **Hue presets & Auto from cover** | 6 presets (Indigo Dusk `250°`, Saffron `32°`, Moss `140°`, Lagoon `186°`, Rose `345°`, Plum `290°`) + **Auto from cover** (Palette API dominant swatch hue angle only). | Not implemented. | **Missing:** Add presets + Palette API cover-hue extraction (requires `androidx.palette:palette-ktx` — **ask user before adding**). |
| **Hue Aura** | `HueAura` radial gradient background behind Library, Passages, Home, Settings (not behind reading text); ±10° 14 s drift; intensity `0–100%` (default `40%`); disabled by Reduce Motion / Battery Saver / E-ink / `masterAnimations = false`. | Not implemented. | **Missing:** Implement `HueAura` composable + reading-page faint top vignette (Paper/Dusk/Night only). |
| **Language & Warning tags** | **HI = hue filled**, **GU = ink filled** (always with text label); warnings = `hueContainer` badge + warning icon + text. No third colour in chrome. | [`DictionaryBottomSheet.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/dictionary/DictionaryBottomSheet.kt) & [`TranslatedReadingOverlay.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/reader/TranslatedReadingOverlay.kt) use generic M3 chips/cards without explicit HI=hue / GU=ink / `hueContainer` warning styling. | **Divergent:** Standardize HI/GU tags and OCR warning badges (`Scanned-OCR.dc.html`). |
| **Hardcoded hex colours** | All UI chrome derives from `HueTheme` / `ReaderSurface`. | [`ImageBookReaderScreen.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/reader/ImageBookReaderScreen.kt#L158) hardcodes `Color(0xFF141414)`; warm-light overlays in `EpubReaderScreen.kt`, `PdfReaderScreen.kt`, `HtmlReaderScreen.kt` hardcode `Color(0xFFFFA726)`. | **Cleanup:** Move `ImageBookReaderScreen` background to theme/surface; keep warm-light tint as reading optical filter only. |

---

## 2. Reading Surfaces (`ReaderSurface`)

| `DESIGN.md` v2 Surface | Current `ReaderThemeMode` ([`ReaderPreferences.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/reader/prefs/ReaderPreferences.kt#L24-L35)) | Gap / Action |
|---|---|---|
| **Paper** (`hsl(h,30%,98%)` bg, `hsl(h,32%,13%)` text, `hsl(h,60%,90%)` highlight) | `DAY` (`0xFFFAF8F3`, `0xFF1C1C19`) | Replace with hue-derived `ReaderSurface.PAPER`. |
| **Dusk** (`hsl(h,18%,88%)` bg, `hsl(h,30%,14%)` text, `hsl(h,50%,79%)` highlight) | `SEPIA` (`0xFFF4ECD8`, `0xFF3B2F23`) — **v1 Sepia is obsolete** | Replace `SEPIA` with hue-derived `ReaderSurface.DUSK` + DataStore migration `SEPIA → DUSK`. |
| **Night** (`hsl(h,18%,12%)` bg, `hsl(h,14%,84%)` text, `hsl(h,35%,26%)` highlight) | `NIGHT` (`0xFF181A1B`, `0xFFE2E0DB`) | Replace with hue-derived `ReaderSurface.NIGHT`. |
| **OLED** (`#000000` bg, `hsl(h,10%,78%)` text, `hsl(h,35%,20%)` highlight) | `PURE_BLACK_OLED` (`0xFF000000`, `0xFFE6E4DF`) | Replace with hue-derived `ReaderSurface.OLED`. |
| **E-ink** (`#FFFFFF` bg, `#000000` text, underline-only highlight, no hue/blur/motion) | `E_INK` (`0xFFFFFFFF`, `0xFF000000`) | Add underline-only highlight & disable hue/blur/motion when `EINK` is active. |

---

## 3. Typography & Bundled Fonts

| Requirement (`DESIGN.md` §4) | Current State | Gap / Action |
|---|---|---|
| **Display titles, passage text, cards:** `Fraunces` (italic for quotes) | Missing from `res/font/`. [`Type.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/theme/Type.kt) uses `FontFamily.Serif` / `FontFamily.SansSerif`. | **Missing:** Bundle `Fraunces` (regular + italic OFL) and wire `ZeusTypography` to `Fraunces` (display/titles) & `Atkinson Hyperlegible` (UI). |
| **Indic passages on cards:** `Noto Serif Devanagari`, `Noto Serif Gujarati` | Missing from `res/font/`. Only `noto_sans_devanagari_regular.ttf` and `noto_sans_gujarati_regular.ttf` are bundled. | **Missing:** Bundle `Noto Serif Devanagari` and `Noto Serif Gujarati` (OFL). |
| **Reading EN / UI / Indic:** `Literata` (18sp/1.65), `Atkinson Hyperlegible`, `Noto Sans Devanagari` (1.85), `Noto Sans Gujarati` (1.80) | Bundled in `res/font/` and exposed in [`ReaderFontChoice`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/reader/prefs/ReaderPreferences.kt#L37-L53). | **Gap:** Verify `includeFontPadding = false`, `LineHeightStyle(Alignment.Center, Trim.None)`, and per-paragraph `localeList` across reader & card text. |

---

## 4. Preferences & `AppUiConfig` v2

| Requirement (`DESIGN.md` §9 & `SCOPE-ADDENDUM-v2` §A) | Current State | Gap / Action |
|---|---|---|
| `AppUiConfig` v2 (`appTheme`, `themeHue`, `hueFromCover`, `auraIntensity`, `readerSurface`, `focusRibbon`, `glassUi`, `aestheticElements`, `animatedGlare`, `animatedBorders`, `masterAnimations`, `embossedEffects`, `pointerHoverEffects`, `appLanguage`) | Only `ReaderSettings` exists in [`ReaderPreferences.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/reader/prefs/ReaderPreferences.kt#L60-L74); no app-level `AppUiConfig` DataStore, no Settings screen, no v1→v2 migration. | **Missing:** Create `AppUiConfig` DataStore + v1 migration (`DAY→PAPER`, `SEPIA→DUSK`, `NIGHT/OLED/E_INK` preserved) + **Settings** screen (`Settings.dc.html`). |

---

## 5. Signature Reading Moments (`DESIGN.md` §5)

| Moment | Current State | Gap / Action |
|---|---|---|
| **1. Fore-edge progress** (~40 hairlines at right margin, `12dp` wide, tap/drag jump, TalkBack `"X percent read"`) | Standard progress bar / page indicator only. | **Missing:** Build `ForeEdgeProgress` composable for EPUB & PDF readers. |
| **2. Focus ribbon** (toggle in Display panel; 3-line band at 100% + hairline hue rules, rest 35%; moves with taps/volume keys) | Not implemented. | **Missing:** Add `focusRibbon` toggle in `TypographyBottomSheet` + overlay/band in reader. |
| **3. Margin marks** (`4dp` hue bar in left margin beside saved passages/highlights; tap opens detail) | Highlights exist in DB/sheet, no left-margin `4dp` bar. | **Missing:** Render margin marks in reader & wire tap to detail. |
| **4. Ribbon bookmark** (top-right hue ribbon `40dp` / `64dp` when chrome visible; pull down = bookmark + toolbar button) | Not implemented. | **Missing:** Build `RibbonBookmark` gesture + toolbar toggle. |
| **5. Chapter breath** (end-of-chapter card: time read, passages saved, words learned, next chapter) | Not implemented. | **Missing:** Build `ChapterBreathCard` (shows `0` passages saved in 3R, live count in 3B/9). |

---

## 6. Navigation, Screens & Components (`DESIGN.md` §§6–7)

| Screen / Component | `DESIGN.md` v2 Reference | Current Codebase | Gap / Action |
|---|---|---|---|
| **Bottom Navigation Bar** | Short navigation bar: `Home`, `Library`, `Passages`, `Vocab`, `Settings`. | [`Navigation.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/Navigation.kt) has only `LibraryRoute`, `VocabularyRoute`, `PrepareOfflineRoute` with top-bar icon buttons (no bottom `NavigationBar`, no `SettingsRoute`, no `PassagesRoute`). | **Missing:** Add `Settings` screen in Phase 3R and `Passages` tab/navigation bar in Phase 3B (keep Home/Library/Vocab/Settings wired cleanly). |
| **Reader Chrome** | `Reader.dc.html`: hidden chrome shows only page + time left, fore-edge, ribbon; tap centre → top pill + floating toolbar (`Contents`, `Aa`, `Translate`, `Passages`, `Listen`). | `EpubReaderScreen.kt`, `PdfReaderScreen.kt`, `ImageBookReaderScreen.kt` use standard `BottomAppBar` + top bar. | **Divergent (3R):** Replace with top pill + M3 Expressive floating toolbar (`52dp` buttons, `180ms` chrome fade). |
| **Typography Panel** | `Typography.dc.html`: 5 `ReaderSurface`s, fonts, size, line height, margins, layout, justify, publisher override, **Focus ribbon** switch. | [`TypographyBottomSheet.kt`](../android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/reader/TypographyBottomSheet.kt) has v1 `ReaderThemeMode`, lacks `Focus ribbon` switch and justify/publisher override parity. | **Divergent (3R):** Update to `ReaderSurface` + add `Focus ribbon` switch. |
| **Selection & Save Sheet** | `Selection.dc.html`: floating toolbar (`Highlight · Save passage · Translate · More`) + Save sheet. | Basic highlight/dictionary actions; no `Save passage` flow. | **Missing (3B):** Implement in Phase 3B. |
| **Passages, PassageDetail, CardStudio** | `Passages.dc.html`, `PassageDetail.dc.html`, `CardStudio.dc.html`, `CardExports.dc.html`. | Not implemented (`ZeusReaderDatabase` is at version `7` with no `PassageFolder`/`Passage`/`PassageTag` tables). | **Missing (3B):** Implement Room migration v7→v8, Passages tab, `PassageDetail`, `CardStudio`, and `MediaStore`/`FileProvider` sharing. |

---

## 7. Dependencies & Gates Check

- **New dependency needed for Phase 3R:** `androidx.palette:palette-ktx` (for "Auto from cover" in `DESIGN.md` §2 & `SCOPE-ADDENDUM-v2.md` §A). **Not yet in [`libs.versions.toml`](../android/gradle/libs.versions.toml)** — will ask for explicit approval before adding.
- **No extra dependencies needed for Phase 3B:** `rememberGraphicsLayer` (Compose UI), `RenderEffect` (Android API 31+), `MediaStore`, `FileProvider`, `Room`, and `WorkManager` are already available in the current dependency set.
- **Quality gates to establish in 3R/3B:** `HueTheme` contrast unit test (`0–345` step `15`), DataStore migration test, Roborazzi matrix (`hues {32, 140, 250} × light/dark × 100%/200% × EN/HI/GU`), and Share Card golden contrast tests (white/black cover).
