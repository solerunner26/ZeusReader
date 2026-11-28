# DESIGN.md — ZeusReader v4 "Stormlight"

> **v4 replaces v1, v2 (Ink & Hue) and v3 (Folio) completely.** Delete older design docs.
> Visual source of truth: `design/screens/*.dc.html` (exact values in the markup) + PNG exports in `design/screens/png/` if present.
> Features & phases: `docs/SCOPE-ADDENDUM-v4.md`. Build a screen only in the phase it names.

## 1. Idea
**Read like the sky is listening. Every book gets its own weather.**
- **Loud outside, calm inside.** Living skies (animated aurora gradients), glow, stickers and big type live *around* the book. The page itself is always quiet paper at full contrast.
- **The Orb.** One glowing control at the thumb. Tap → tools bloom out ("constellation"). Drag along the edge → fly through chapters.
- **Reading feels like a game, not homework:** daily *Charge* ring, streak sticker, *Chapter Strike* celebration.
- **Sparks:** saved passages become stars; collections are *constellations*; any spark becomes a *Spark card* image to post.

## 2. Names (UI copy + code)
| UI | Feature |
|---|---|
| Home | today: floating cover, charge ring, streak, last line, resume, resurfaced spark, words waiting |
| The page / Orb | reader + one floating control |
| Constellation | tool menu blooming from the Orb |
| Fly | Orb drag scrubber with chapter "planets" |
| Zap card | word lookup (meaning + हिन्दी + ગુજરાતી + sound) / sentence translation |
| Translate Lens | Lens (one paragraph at a time, tap to move) · Split (EN + translation) · All हिन्दी/ગુજરાતી |
| Vibe check | sky, paper, size, font, orb hand, page turning |
| Listen | karaoke read-aloud with waveform, speed, voice, sleep timer |
| Chapter Strike | end-of-chapter celebration + stats + next chapter |
| Ask Zeus | AI Q&A with page "receipts", follow-ups (Hindi, Gujarati, simpler, quiz me), story so far |
| Sparks / Constellations | saved passages / folders |
| Spark card | share image (PNG/JPG) |
| Olympus | library (cover-flow, filters, import) |
| Words | vocab review (flip card, Again / Got it / Easy) |
| Settings | language, vibe, reading, packs, listening, server |

## 3. Colour
### Skies (app background mood; auto-picked from the cover's dominant hue, nearest sky)
| Sky | base a → b | glow 1 | glow 2 | accent |
|---|---|---|---|---|
| Storm (default) | `#120B2E → #3A1C8C` | `#7C5CFF` | `#FF4FD8` | `#FFE14D` |
| Aurora | `#04161A → #0C4A4A` | `#3DF5D0` | `#A6FF4D` | `#A6FF4D` |
| Sunset | `#1E0A16 → #5B1240` | `#FF4FD8` | `#FF9A3D` | `#FFB23D` |
| Monsoon | `#06101F → #10306B` | `#3D8BFF` | `#3DF5F5` | `#6FE7FF` |
| Ember | `#180806 → #5A1A0C` | `#FF5A36` | `#FFC24D` | `#FFC24D` |
- Text on skies: `#F4F1FF`; secondary = same at 72–80% opacity. Text on accent/gradient buttons: the sky's base colour `a` (dark) — never white on yellow/lime.
- Glass: `rgba(255,255,255,0.08–0.12)` fill, `rgba(255,255,255,0.16–0.25)` 1dp border, 16–24dp blur (API 31+; fallback: base colour at 85% opacity).
- Sticker chips: `#F4F1FF` or accent fills, near-black text, rotated −4°…+4°, optional 3dp offset shadow in glow colour.
- Primary action = pill with `linear-gradient(135deg, accent, glow2)` + soft glow shadow.

### Paper (reading surface)
| Paper | bg | ink | soft | highlight |
|---|---|---|---|---|
| Cloud | `#F6F4FF` | `#17142B` | `#5B5675` | glow1 @ 18% |
| Night | `#141125` | `#ECE8FA` | `#A9A3C4` | glow1 @ 33% |
| OLED | `#000000` | `#D6D2E6` | `#8E89A6` | glow1 @ 33% |
| E-ink | `#FFFFFF` | `#000000` | `#333333` | grey `#E5E5E5`, **no sky, no glow, no motion** |
The page is a rounded (30dp) sheet inset 8dp from the screen edges, so a sliver of sky always frames it.

## 4. Type (bundle, OFL)
| Role | Font |
|---|---|
| Display, numbers, headlines | **Unbounded** 600–900 |
| UI | **Bricolage Grotesque** 400–800 |
| Reading body EN | **Literata** 18sp/1.7 (14–30) |
| Reading HI / GU | **Noto Serif Devanagari** / **Noto Serif Gujarati** (1.85 / 1.8) |
| Indic UI & stickers | **Baloo 2** / **Baloo Bhai 2** |
| Meta | **JetBrains Mono** |

## 5. Shape, touch, motion
- Radii: pills/orb full · cards 24–34dp · sheets 34dp top. Touch targets ≥ 48dp (orb 68dp, primary buttons 56–62dp).
- Bottom nav: floating glass dock (68dp, 16dp inset): Home, Olympus, Sparks, Words. Active item in accent.
- Motion: aurora blobs drift 14–22s loops; orb pulse 3s; constellation items pop with stagger 40ms (spring overshoot); zap card springs in; strike = one white flash + bolt glow; karaoke word highlight moves with TTS word boundaries. **Reduce Motion / battery saver / E-ink / "Glow & motion" off → no loops, no flashes, instant transitions.** Never animate the reading text itself.

## 6. Screens (design/screens)
**Read:** `Home`, `Reader` (tweaks: sky, paper, focus; tap Orb for constellation: Lens, Zap a word, Vibe & type, Listen, Spark it, Ask Zeus, Fly through), `Fly`, `Zap` (Word / Whole sentence), `Translate` (Lens / Split / All; tap paragraphs; language toggle), `Vibe`, `Listen`, `Strike`, `Ask`, `Scan` (scan beam, confidence ring, dotted unsure words, Lens it / Spark it), `Spread` (landscape EN↔HI with glowing sentence link).
**Sparks:** `Capture` (selection toolbar: Spark it, Glow, Lens, Card, more; constellation picker grid + new; note), `SparksHome` (animated star map with constellation chips; spark list; delete → "Spark faded away · Bring it back"), `SparkEdit` (text, original kept, jump to page, note, constellation, tags, delete/move/make a card), `CardStudio` (Cover blur / Aurora / Sticker × Square 1080² / Post 1080×1350 / Story 1080×1920, PNG/JPG, Save / Share to story), `Cards` (finished EN/HI/GU examples).
**Olympus:** `Olympus` (cover-flow; selected book re-paints the sky; filters Reading/Format/Year/Genre/Author/Pages/Language; + import orb; natural-language search), `Book` (details, files: original kept + reading copy, unknown stays empty), `Words`, `Settings`.

## 7. Readability rules
- Reading text only ever on paper colours above (≥ 7:1).
- On skies, body text `#F4F1FF` (≥ 7:1 on every sky base); small text never below 80% opacity.
- Spark cards: Cover blur = cover blurred 22dp + 55→72% dark veil + white text; Aurora = glows + 38% dark veil + soft text shadow + white text; Sticker = near-black on cream `#FFF4D6`. Golden tests on pure white and pure black covers must give ≥ 4.5:1.
- Title + author always printed on cards.

## 8. Quality gates
Contrast unit tests for all sky × text and paper × ink pairs · Roborazzi for every screen × {Storm, Aurora, Sunset} × {Cloud, OLED, E-ink} × font scale {100, 200%} × {EN, HI, GU} · Macrobenchmark: page turn < 16ms frame time with aurora running · Reduce-Motion snapshot set.
