# SCOPE ADDENDUM v2 — Ink & Hue theming + Passages & Share Cards

> This file **extends** the project scope (GEMINI.md / CLAUDE.md). Where it conflicts with earlier theme or UI notes, this file wins.
> It exists so that features and designs stay in parity: every screen in `design/DESIGN.md` v2 maps to a feature and a phase below. **Do not build a screen before its phase.**

---

## A. Feature: Ink & Hue theming (replaces earlier theme work)

**What:** Two-tone theme system generated from a single hue, with presets, "Auto from cover", Hue Aura background and five reading surfaces (Paper, Dusk, Night, OLED, E-ink). Replaces the v1 Day/Sepia/Night/OLED/E-ink colours and the green/terracotta language tags.

**Where it lands:**
- If Phase 3 (reading experience) is **not done yet** → build it as part of Phase 3.
- If Phase 3 is **already done** → new **Phase 3R (Re-theme)** right after the current phase: replace tokens, keep behaviour.

**Deliverables:** `HueTheme(hue, dark)` → `ColorScheme`; `ReaderSurface` palettes; `HueAura` composable; theme hue presets + Auto from cover (Palette API) + aura intensity in Settings; bundled fonts Fraunces, Noto Serif Devanagari, Noto Serif Gujarati (added to existing fonts); `AppUiConfig` v2 fields (see DESIGN.md §9) with DataStore migration from v1.

**Reading moments in the same phase:** fore-edge progress, focus ribbon, margin marks, ribbon bookmark, chapter breath card (DESIGN.md §5).

**Acceptance tests:**
1. Unit: contrast ≥ 4.5:1 text / ≥ 3:1 icons for hues 0–345 (step 15), light + dark.
2. Changing the hue preset recolours the whole app in ≤ 400 ms without restarting; the setting survives app restart.
3. "Hue from book cover" on: opening a book with a green cover changes the hue; turning it off restores the chosen preset.
4. Aura and all motion are off with Reduce Motion, Battery Saver, E-ink or Master animations off.
5. DataStore migration: a v1 install keeps its theme choice (Day→Paper, Sepia→Dusk, Night, OLED, E-ink unchanged).

---

## B. Feature: Passages & Share Cards (NEW)

**What:** Select a passage while reading → save it to a folder → manage passages later (add, edit, move, tag, delete with undo) → share any passage as a readable image card (PNG/JPG) with blurred cover background, book title and author, directly to any app via the Android share sheet.

**Phase:** new **Phase 3B — Passages & Share Cards**, after Phase 3 (needs text selection and highlights) and before Phase 4. Sync/backup of passages is added to **Phase 7** (see B.6).

### B.1 Data model (Room, new migration)
```kotlin
@Entity data class PassageFolder(
  @PrimaryKey val id: String,           // UUID
  val name: String, val sortOrder: Int,
  val createdAt: Long, val updatedAt: Long, val deletedAt: Long? = null)

@Entity(indices = [Index("bookId"), Index("folderId")])
data class Passage(
  @PrimaryKey val id: String,           // UUID
  val bookId: String, val editionId: String?,
  val folderId: String?,                // null = "Unsorted"
  val text: String,                     // editable (user may trim)
  val originalText: String,             // as selected, never edited
  val locatorJson: String,              // unified Locator, for "Open in book"
  val pageLabel: String?, val chapterTitle: String?,
  val language: String,                 // "en", "hi", "gu"
  val note: String?,
  val createdAt: Long, val updatedAt: Long,
  val deletedAt: Long? = null,          // soft delete (Undo + sync tombstone)
  val shareCount: Int = 0)

@Entity(primaryKeys = ["passageId","tag"]) data class PassageTag(val passageId: String, val tag: String)
```
FTS4 table over `text`, `note` and tags for search. Hard-delete soft-deleted rows after 30 days (WorkManager).

### B.2 Capture (reader)
- Text selection in EPUB (Readium) and text PDFs (MuPDF structured text). Scanned PDFs: only after OCR text exists.
- Selection floating toolbar: Highlight · **Save passage** · Translate · More (Define, Copy, Create card).
- Save sheet: quote preview, folder chips + "New folder", optional note, **Save** / **Save & make card**.
- Saved passages show a margin mark in the reader; tapping it opens Passage detail.
- Translated Reading Mode: "Save passage" saves the **translated** text with `language = hi|gu` and keeps the English source in `originalText`.

### B.3 Manage (Passages tab — new item in bottom navigation)
- List with search, sort (Recent / By book / Shared), folder carousel, folder screens.
- **Add:** FAB menu → New passage (manual entry: pick a book from the library, paste/type text, optional page) or New folder.
- **Update:** Passage detail — edit text, note, tags, move to folder; rename/reorder/delete folders (deleting a folder moves its passages to Unsorted after confirmation).
- **Delete:** swipe or Delete button → soft delete + Snackbar "Passage deleted · Undo" (5 s). Multi-select (long-press): Move, Delete, Share.
- **Open in book:** jumps to `locatorJson`.

### B.4 Share cards (Card studio)
- Styles: Cover blur / Ink / Paper. Sizes: 1:1 1080×1080, 4:5 1080×1350, 9:16 1080×1920. Format: PNG (default) or JPG (quality 92).
- Always includes passage, book title, author and cover thumbnail. Readability rules exactly as DESIGN.md §8.
- Render: compose the card off-screen at full pixel size (`rememberGraphicsLayer` → `toImageBitmap()`), not a screenshot of the preview.
- Missing cover → Ink style is preselected (Cover blur disabled with a reason).
- **Save to gallery:** MediaStore `Pictures/ZeusReader/` (no storage permission needed on API 29+).
- **Share…:** write to app cache → `FileProvider` URI → `Intent.ACTION_SEND` (`image/png` or `image/jpeg`), `FLAG_GRANT_READ_URI_PERMISSION`, `ClipData` set, `Intent.createChooser`. Increment `shareCount`.
- Card generation is fully offline. No network, no analytics.

### B.5 Acceptance tests (Phase 3B)
1. Select 2 sentences in an EPUB → Save to new folder "Exam quotes" → appears in Passages with source and page; margin mark visible in the reader.
2. Edit passage text and note, move it to another folder, restart the app → changes persist; **Open** jumps to the exact location.
3. Delete → Undo restores it; delete again and wait 5 s → gone from the list (soft-deleted row remains for sync).
4. Manual "New passage" for a library book saves without a locator; "Open" is disabled with an explanation.
5. Card studio: each style × size exports a PNG and a JPG of the exact pixel size; Hindi and Gujarati passages render without clipped matras.
6. Golden tests: text contrast ≥ 4.5:1 on a pure-white cover and a pure-black cover in Cover blur style.
7. Share… opens the system share sheet; sharing to a messaging app delivers the image; Save to gallery shows it in Photos.
8. All of this works in airplane mode.

### B.6 Additions to later phases
- **Phase 7 (server sync/backup):** sync `PassageFolder`, `Passage`, `PassageTag` with last-write-wins + tombstones (`deletedAt`); include them in encrypted backups and restore tests. Card images are **not** synced (re-render on demand).
- **Phase 8 (AI, optional):** "Explain this passage" and "Suggest tags" on Passage detail.
- **Phase 9 (polish):** chapter-breath card shows "passages saved this chapter".

---

## C. Out of scope (unchanged)
No online book search, edition picker, transfer screens or downloads from external sources. Books enter only by local import.
