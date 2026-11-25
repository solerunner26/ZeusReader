# DESIGN.md — ZeusReader UI/UX v2 "Ink & Hue"

> **v2 replaces v1 completely.** Ignore any earlier colours, the green/terracotta language tags and the Sepia theme.
> Visual reference: `design/screens/*.dc.html` (exact values in the markup) + `design/screens/png/` if exported.
> Feature scope for anything new here lives in **`docs/SCOPE-ADDENDUM-v2.md`**. A screen is built only in the phase that addendum names.

---

## 1. Concept
**Two tones, nothing else.** *Ink* carries every word. One *Hue* carries mood, focus and action. Behind the app the hue breathes as a soft **Hue Aura** (radial glows that drift ±10° in hue over 14 s). Optionally the hue follows the **cover of the book you are reading**.

Rules:
- No third colour anywhere in app chrome. States are expressed with **hue vs ink**, filled vs outlined, and lightness — never new colours.
- Book covers are the only multi-colour content (they are the user's books).
- Language tags: **HI = hue filled**, **GU = ink filled**, always with the text label.
- Warnings (e.g. low OCR confidence) = hue-container badge **plus a warning icon and text**, never a new colour.

## 2. Colour system — derived from one hue `h`
Build the Compose `ColorScheme` from `h` with **fixed lightness steps** so every hue keeps WCAG AA.

| Role | Light | Dark |
|---|---|---|
| surface (app bg) | `hsl(h,28%,95%)` | `hsl(h,24%,8%)` |
| paper (cards, sheets) | `hsl(h,30%,98%)` | `hsl(h,20%,11%)` |
| ink (onSurface) | `hsl(h,32%,13%)` | `hsl(h,18%,90%)` |
| inkSoft (onSurfaceVariant) | `hsl(h,14%,34%)` | `hsl(h,12%,68%)` |
| hue (primary) | `hsl(h,52%,38%)` | `hsl(h,62%,72%)` |
| onHue (onPrimary) | `#FFFFFF` | `hsl(h,35%,10%)` |
| hueContainer | `hsl(h,45%,91%)` | `hsl(h,30%,20%)` |
| outline | `hsl(h,20%,85%)` | `hsl(h,14%,20%)` |
| highlight (reading) | `hsl(h,60%,88%)` | `hsl(h,35%,26%)` |

**Presets:** Indigo Dusk 250° (default) · Saffron 32° · Moss 140° · Lagoon 186° · Rose 345° · Plum 290° · **Auto from cover** (Palette API dominant swatch → take only its hue angle; lightness/saturation always come from the table above).
Implement as `HueTheme(hue: Float, dark: Boolean): ColorScheme` (pure function, unit-tested for contrast at every 15°). Animate hue changes with `animateFloatAsState` (400 ms) when animations are on.

### Hue Aura (background effect)
Layer behind Library, Passages, Home, Settings (NOT behind reading text):
- Light: radial gradients `hsla(h,75%,78%,0.6)` at top-left and `hsla(h+16,70%,84%,0.5)` at right-middle over `surface`.
- Dark: same shapes at `hsla(h,70%,45%,0.4)` / `hsla(h+16,65%,40%,0.3)`.
- Drift: `hue-rotate` ±10° over 14 s (Compose: animate the hue value fed to the gradient). Off when Reduce Motion, Battery Saver, E-ink or Master animations = off.
- Intensity slider in Settings (0–100%, default 40%).
- On the reading page only a faint top vignette (Paper/Dusk/Night), none on OLED/E-ink.

## 3. Reading surfaces
| Surface | Background | Text | Highlight |
|---|---|---|---|
| Paper | `hsl(h,30%,98%)` | `hsl(h,32%,13%)` | `hsl(h,60%,90%)` |
| Dusk | `hsl(h,18%,88%)` | `hsl(h,30%,14%)` | `hsl(h,50%,79%)` |
| Night | `hsl(h,18%,12%)` | `hsl(h,14%,84%)` | `hsl(h,35%,26%)` |
| OLED | `#000000` | `hsl(h,10%,78%)` | `hsl(h,35%,20%)` |
| E-ink | `#FFFFFF` | `#000000` | underline only; no hue, no blur, no motion |

## 4. Typography
| Use | Font |
|---|---|
| Reading EN | Literata 18sp / 1.65 (14–30sp) |
| Reading HI | Noto Sans Devanagari, line height 1.85 |
| Reading GU | Noto Sans Gujarati, line height 1.80 |
| UI | Atkinson Hyperlegible |
| Display titles, passage text, cards | **Fraunces** (italic for quotes) |
| Indic passages on cards | Noto Serif Devanagari / Noto Serif Gujarati |
All fonts bundled (OFL). Indic: `includeFontPadding=false`, `LineHeightStyle(Center, Trim.None)`, per-paragraph `localeList`.

## 5. Signature reading moments
1. **Fore-edge progress** — vertical stack of ~40 hairlines at the right margin (12dp wide). Read pages = 1dp light lines, remaining = 2dp ink-soft lines, current = 3dp hue line. Tap/drag to jump. TalkBack: "37 percent read".
2. **Focus ribbon** (toggle in Display panel) — keeps a 3-line band at full opacity with hairline hue rules; other text at 35%. Moves with taps/volume keys.
3. **Margin marks** — 4dp hue bar in the left margin beside saved passages / highlights; tap opens the passage.
4. **Ribbon bookmark** — hue ribbon hanging at top-right (40dp tall, 64dp when chrome visible). Pull down = bookmark; also a toolbar button.
5. **Chapter breath** — end-of-chapter card: time read, passages saved, words learned, next chapter.
6. **Passages & share cards** — see §7 and the scope addendum.

## 6. Components (Material 3 Expressive)
Floating toolbar (reader bottom; selection toolbar) · Button group · Split button · FAB / FAB menu (Passages "New") · Short navigation bar (Home, Library, Passages, Vocab, Settings) · Carousel (folders, continue reading) · Docked search bar · Modal bottom sheet (dictionary, save passage, display) · Segmented buttons · Expressive slider · Loading indicator · Snackbar with Undo · Switch rows.
Shape: radii 12 / 20 / 28 / full · 8dp grid · targets ≥ 48dp (reader toolbar 52dp, primary actions 56dp).
Motion: chrome fade 180 ms, sheets spring (0.85), hue change 400 ms, nothing moves under anchored text.

## 7. Screens

### Page "Reading & translation"
| File | Notes |
|---|---|
| `Reader.dc.html` | Immersive. Hidden chrome shows only page number + time left, fore-edge, ribbon. Tap centre → top pill (back, title+author+%, search, bookmark) + floating toolbar (Contents, Aa, **Translate**, Passages, Listen). |
| `Selection.dc.html` | **NEW** Long-press-drag selects; ink floating toolbar: Highlight · **Save passage** (hue) · Translate · More (Define, Copy, Create card). Save sheet: quote preview, folder chips (+ New folder), note, Save / Save & make card. |
| `AltB-Sheet.dc.html` | Dictionary sheet (chosen). Word/Sentence tabs; HI hue tag, GU ink tag. |
| `AltC-Paired.dc.html` | Translated Reading Mode (chosen), Dusk surface shown. |
| `Typography.dc.html` | Display panel: size, fonts, 5 surfaces, line height, margins, layout, justify, publisher override. Add Focus ribbon switch. |
| `AltA-Inline.dc.html` | Optional inline mode. |
| `Scanned-OCR.dc.html` | OCR card: hue-container badge with warning icon + "OCR 78% · check". |
| `Scale200.dc.html` | 200% font scale in EN/HI/GU. |
| `TwoPane.dc.html` | Landscape / Medium+ two-pane. |

### Page "Passages & cards (NEW)"
| File | Notes |
|---|---|
| `Passages.dc.html` | Aura bg, title + count, search, folder carousel, sort segmented (Recent / By book / Shared), passage cards (2-line Fraunces italic quote, source, folder chip, Share-as-card, Delete). Delete → Snackbar "Passage deleted · Undo" (5 s). FAB "New" → menu: New passage (manual), New folder. Long-press card → multi-select (Move, Delete, Share). |
| `PassageDetail.dc.html` | Edit text (trim allowed, locator kept), source card with **Open** (jumps to locator), note, folder dropdown, tags, dates. Bottom button group: Delete (confirm dialog), Move, **Create card**. |
| `CardStudio.dc.html` | Live preview; Style: **Cover blur / Ink / Paper**; Size: **1:1 (1080×1080) / 4:5 (1080×1350) / 9:16 (1080×1920)**; format PNG/JPG; readability check line; Save to gallery / Share…. |
| `CardExports.dc.html` | Reference renders in EN / HI / GU. |

### Page "Library & settings"
| File | Notes |
|---|---|
| `Library.dc.html` | Aura blobs, glass search, filter chips, continue-reading, covers grid, 5-item nav incl. **Passages**. |
| `Settings.dc.html` | App theme (Light/Dark/System), App language, **Theme hue** (6 presets + Auto from cover), **Hue from book cover** switch, **Hue aura** slider, then the AppUiConfig switches. |

### Page "Foundations"
`Main.dc.html` — presets, tone roles, surfaces, signature moments, components.

## 8. Share-card rendering rules (readability)
- **Cover blur**: cover image scaled to fill, blur 18dp (API 31+ `RenderEffect`; older: downscale ×8 + box blur), then **black scrim gradient 55%→68%**, text **#FFFFFF**. Guarantees ≥ 4.5:1 on any cover.
- **Ink**: background `hsl(h,30%,11%)` + faint hue glow; text #FFFFFF.
- **Paper**: background `hsl(h,30%,96%)`, text `hsl(h,32%,12%)`, cover shown sharp in the top 46% fading into paper.
- Always on card: passage, **book title**, **author**, small cover thumbnail. Optional: translation label, tiny "ZeusReader" mark (off by default).
- Passage font: Fraunces italic (EN) / Noto Serif Devanagari / Noto Serif Gujarati. Auto-shrink to fit; minimum 15px at 1080-wide equivalent; if still too long, ask the user to trim.
- Padding 8.6% of width; quote mark in hue (Ink/Paper) or white (Cover).

## 9. AppUiConfig v2
```kotlin
data class AppUiConfig(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val themeHue: Float = 250f,
    val hueFromCover: Boolean = false,
    val auraIntensity: Float = 0.4f,
    val readerSurface: ReaderSurface = ReaderSurface.PAPER, // PAPER, DUSK, NIGHT, OLED, EINK
    val focusRibbon: Boolean = false,
    val glassUi: Boolean = true,
    val aestheticElements: Boolean = true,   // aura + blobs
    val animatedGlare: Boolean = false,
    val animatedBorders: Boolean = false,
    val masterAnimations: Boolean = true,
    val embossedEffects: Boolean = false,
    val pointerHoverEffects: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.EN
)
```

## 10. Quality gates
- Unit test: `HueTheme` contrast ≥ 4.5:1 (text) and ≥ 3:1 (icons) for hues 0–345 step 15, light and dark.
- Roborazzi: every screen × hues {32, 140, 250} × light/dark × font scale 100/200% × EN/HI/GU.
- Share cards: golden-image tests for 3 styles × 3 sizes × 3 scripts; contrast sampled on a white cover and a black cover.
- Touch targets ≥ 48dp; icon buttons have contentDescription; snackbar Undo reachable by TalkBack.
