# GEMINI.md — "ZeusReader" Personal Offline-First Android E-Reader & Private Library

"Scope is extended by docs/SCOPE-ADDENDUM-v2.md; UI follows design/DESIGN.md v2 (replaces v1)."

> Authoritative brief for Gemini CLI / Gemini coding agents. Read this file once at project start, then at each phase read only the relevant section, the previous `/docs/phase-N.md` handoff, and changed files.
> Build **one phase at a time** (§11). Do not start the next phase until the current exit condition passes and evidence is recorded.

## 1. Project Summary & Scope

**ZeusReader** (`com.solerunner26.zeusreader`) is a personal, single-user, offline-first Android e-reader designed to build a daily reading habit through effortless one-handed ergonomics, a calm, high-craft UI, and frictionless multilingual study. **Never published to Play Store** (private signed APK on the owner's **Google Pixel 10 Pro**; AGPL libraries such as MuPDF are acceptable).

* **Owner / only user:** Karan Agrawal (India)
* **GitHub Repository:** `https://github.com/solerunner26/ZeusReader.git`
* **Primary Target Device:** Google Pixel 10 Pro (OLED 120Hz, one-handed portrait primary + adaptive landscape/tablet support)

**Core capabilities:**

* Multi-format offline reader (EPUB, PDF, XPS/OXPS, CBZ, images, HTML, limited TIFF; optional MOBI/AZW3/KF8/DOC/DOCX via private server conversion).
* Edition-aware personal library with multi-facet filters, shelves, reading streaks/stats, and lossless backup/restore.
* User-initiated online book acquisition: search by title, author, or ISBN from Android, choose an edition and format, queue its download on the private server, use browser handoff when required, and transfer a verified copy to the phone for offline reading.
* Bundled offline English dictionary + on-device English → Hindi / English → Gujarati translation + **Translated Reading Mode** (sentence-aligned, cached by passage).
* Optional private-server AI (higher-quality translation, explanations, summaries, RAG Q&A, smart search, auto-metadata, vocab quizzes).
* Private server running on the owner's **Windows 11 laptop** (zero-cost default/fallback) or optional **Oracle Cloud Always Free (Singapore, ARM64)**.

### Scope Boundaries

* **Included:** Local import (picker, share-to-app, watched folder), private-server sync/backup/conversion, user-initiated online acquisition via **Open SLUM** availability status + verified per-provider connectors (§4.3), optional metadata-only lookup (Open Library / Google Books behind a toggle, default ON).
* **Excluded (do NOT build):** Public-library lending, OPDS catalogues, Project Gutenberg integrations, DRM removal, KFX decoding, Play Store release tooling, paid cloud hosting or third-party book upload services (e.g., CloudConvert) in the default path.

## 2. Core Engineering Principles

1. **Standalone phone:** Reading, library, bundled English dictionary, and installed ML Kit packs work in airplane mode. Define two explicit first-run states: *fresh install* (English dictionary works immediately; missing translation packs clearly prompted on **Prepare for offline reading**) and *prepared phone* (full offline Hindi/Gujarati).
2. **Original files preserved:** Originals are immutable; converted copies attach to the same `Edition`.
3. **Unknown metadata stays blank:** Never fabricate fields. AI metadata suggestions require explicit user approval.
4. **Habit-first, signature UI/UX:** Zero-clutter reading canvas, Material 3 Expressive + optional Liquid Glass surfaces, pure-black OLED & E-ink themes, quiet background download notifications while reading, instant exact-passage resume, and ergonomic either-hand controls.
5. **Graceful degradation:** Server offline or AI disabled must never break reading, library, dictionary, or local import.
6. **Evidence over claims:** Distinguish `planned`, `passed`, `failed`, and `unverified`. A mock, saved fixture, or emulator run cannot establish a live provider download or real-device reading comfort.

## 3. Architecture & Data Contracts (Define Before UI)

### 3.1 Tech Stack

* **Android:** Kotlin, Jetpack Compose, Material 3, single-activity Navigation Compose, Hilt DI, Coroutines/Flow, DataStore, WorkManager, Coil, Gradle version catalogs (Min SDK 26, target latest stable).
* **Database:** **Room** (pinned version) with `@Fts4` for full-text search on titles, authors, and descriptions (use ordinary indexed columns for year, language, status, and page ranges).
* **Reader Engines:**
  * **Readium Kotlin Toolkit** — EPUB (reflowable + fixed-layout), TTS navigator, Locator model. Block publication-supplied active content while preserving Readium's internal navigation JS.
  * **MuPDF (Android, AGPL)** — PDF, XPS/OXPS, CBZ, images, limited TIFF.
  * **Sandboxed WebView** — HTM/HTML (JS disabled, all network requests blocked, local relative images allowed).
  * Run document parsing/rendering in a dedicated `:reader` process (`android:process=":reader"`) so native crashes return the user safely to Library with an error dialog.
* **On-device ML & Speech:** Google ML Kit (Translation `en→hi`, `en→gu`; Latin Text Recognition for scanned PDFs; Language ID), Android `TextToSpeech`, ICU `BreakIterator`.
* **Networking:** Retrofit/OkHttp (or Ktor) over **Tailscale** private mesh; pairing token required on every endpoint.
* **Server:** Python 3.12+, **FastAPI**, SQLite (WAL) via SQLModel/SQLAlchemy, Docker Compose (compatible with Windows 11 x86_64 and Ubuntu ARM64 on Oracle). Conversion workers: Calibre CLI (`ebook-convert`) + LibreOffice headless in network-isolated containers. Optional AI: Ollama (`Sarvam-Translate` Q4 GGUF + compact 4B–8B instruct model + `sqlite-vec`), loaded on demand and unloaded when idle.

### 3.2 Canonical Data Contracts

* **Edition & File Identity:** `Work` (canonical title/author grouping) → `Edition` (publisher, year, language, ISBN, edition notes) → `FileAsset` (format, byte size, SHA-256 checksum, origin, conversion lineage). Deduplicate files by SHA-256; never auto-merge two editions solely on matching title + author.
* **Reader Capabilities:** Every `ReaderEngine` declares `Capabilities(textSelection, search, translate, highlight, tts)`. Image books support zoom/bookmarks immediately and gain lookup/translate once OCR yields selectable text.
* **Passage Anchors & Translation Cache:**
  * EPUB resume anchors to passage/progression (survives font-size changes); PDF resume anchors to page + viewport zoom/scroll.
  * Translation cache key: `(fileVersionSha256, chapterOrSectionId, sourcePassageHash, sentenceIndex, targetLang, engineIdVersion, glossaryRevision)`. Font-size changes reuse cached translations; editing the per-book glossary invalidates affected passages; sentences may span page breaks.
* **Two-Stage Transfer State Machine:**
  * *Stage 1 (Provider → Server):* `Queued` → `Waiting` → `Downloading` | `NeedsBrowser` → `Verifying` → `ReadyOnServer` | `Failed` | `Cancelled`.
  * *Stage 2 (Server → Phone):* `NotOnPhone` → `DownloadingToPhone` → `Verifying` → `AvailableOffline` | `TransferFailed`.
  * Guarantees: idempotent duplicate taps (return existing job), atomic temp-file writes with SHA-256 verification before marking ready, expired-link refresh when supported, safe recovery across app kill/device reboot/server restart, low-storage pre-checks.
* **Sync Conflict Model:** Use per-record version numbers + deletion tombstones (never timestamp-only overwrite). Concurrent edits to the same note retain both versions for user review; repeated sync is idempotent.
* **Backup Contracts:** Two explicit modes with a signed/checksummed manifest:
  1. **Library data only:** DB, highlights, notes, vocab, progress, metadata. Restore shows which book files need reconnecting.
  2. **Library data + books:** Lossless full restore on a fresh install.

## 4. Core Subsystems

### 4.1 Supported Formats & Guards

| Status | Formats | Engine / Handling |
| ----- | ----- | ----- |
| Core | `EPUB` | Readium Kotlin |
| Core | `PDF`, `XPS`, `OXPS`, `CBZ` | MuPDF native |
| Core | `JPG`, `JPEG`, `PNG` | Image reader; group into an "image book" & reorder pages |
| Core | `HTM`, `HTML` | Sandboxed WebView (no JS, no network; local images OK) |
| Limited | `TIFF` (single/multi-page) | MuPDF; reject if **byte size > 30 MB**, **pages > 100**, or **decoded bitmap budget exceeded** |
| Optional (Phase 10) | `MOBI`, `AZW3`, `KF8`, DRM-free `AZW` | Server Calibre → EPUB (inspect internal format; keep original) |
| Optional (Phase 10) | `DOC`, `DOCX` | Server LibreOffice → EPUB (reflow) or PDF (tables/equations) |
| Unsupported | `KFX`, DRM-locked files, `.oxp` | Inspect magic bytes; retain `.oxp`/unknown in Unsupported list with clear message |

Keep conversion behind a `Converter` interface so local laptop/Oracle remains default while staying swappable.

### 4.2 Reading Experience, Dictionary & Translated Reading

* **Ergonomics:** One-handed page turns on either edge, large touch targets ($\ge 48\text{dp}$), rotation lock, volume-key turns, edge brightness swipe, warm-light filter, Day / Sepia / Night / Pure-Black OLED / E-ink themes, bundled fonts (`Literata`, `Atkinson Hyperlegible`, `Noto Serif/Sans`, `Noto Sans Devanagari`, `Noto Sans Gujarati`).
* **Fixed-layout extras:** Auto-crop margins, zoom retention across pages, fit-width/fit-page, column zoom, crisp tile re-rendering for diagrams, image-preserving night mode, MuPDF structured-text reflow.
* **Offline Dictionary:** Bundled Princeton WordNet SQLite (English) + bilingual Wiktionary (`kaikki.org`) Hindi/Gujarati SQLite with source, release, attribution, and build date recorded. Show multiple senses; label ML Kit fallback clearly. Save words with book + sentence context to Vocabulary + spaced-repetition flashcards.
* **Translated Reading Mode:** Extract structured text (Readium content iterator / MuPDF structured text with reading order, de-hyphenation, header/footer stripping; ML Kit OCR for scans) → ICU `BreakIterator` sentence segmentation → translate → cache by passage anchor (§3.2). Pre-translate next 2–3 pages asynchronously. Modes: **Interlinear**, **Translation-only** (tap to reveal English), **Side-by-side** (wider windows). Per-book "Don't translate" glossary; keep equations/code/tables in English and diagrams inline.
* **Engine Stability:** User selects `On-device`, `Prefer private server`, or `Retranslate this passage`. Never auto-replace visible text mid-read when connectivity changes.

### 4.3 Online Acquisition (Open SLUM + Provider Connectors)

* **Open SLUM Status Reader:** Periodically polls and caches `open-slum.org` provider availability, distinguishing **catalogue/search domains** from **file-delivery domains**, plus last-check timestamp. If SLUM is unreachable or layout changes, show cached status marked stale. Note: an `UP` badge does not guarantee download speed or title availability.
* **Provider Connector Interface:** Each enabled provider implements a pluggable adapter declaring supported capabilities (`search`, `server_download`, `continue_in_browser`), with optional DNS-over-HTTPS (DoH) / proxy support for Indian ISP domain blocks when running locally.
  * Search runs only on explicit user submission (never background crawling).
  * Return edition-level results (source, title, author, language, year, format, file size). If one provider times out, still return results from others.
  * Persistent SQLite download queue on server honours provider wait times and survives restarts.
* **Browser Handoff:** When a provider requires interactive verification (e.g., CAPTCHA/session-locked link), Android shows **Continue in browser** while preserving the selected edition. If downloaded in the phone browser, **Share → ZeusReader** matches the file to the pending selection (prompting user if ambiguous).
* **Early Proof (Phase 0):** Prove one connector end-to-end on the Windows 11 laptop using one known public-domain title. Record source, edition, actual file type (magic bytes), whether human interaction was needed, and offline open on the **Google Pixel 10 Pro**. Save response fixtures so routine Android development never blocks on external outages.

## 5. Design-First Mandate (First Deliverable)

Before Phase 1 UI coding, complete the design handoff in `/design/DESIGN.md` using **Google Stitch** (`stitch-skills`) and `android/skills` adaptive guidance.

* **Signature Flow 1 (One-Handed Multilingual Reading):** Holding the **Google Pixel 10 Pro** in either hand (including reclining), read an English textbook, look up a word, view Hindi/Gujarati translation, inspect a diagram, and return to the exact source passage.
  * Prototype **three alternatives** on the exact same passage & starting position: **(A)** inline translation beneath selected sentence, **(B)** thumb-reachable bottom sheet keeping source visible, **(C)** paired original + translation (two-pane on wider windows).
  * Specify initial state, interaction, dismissal, restored position, and behaviour across reflowable EPUB, fixed-layout PDF, and uncertain OCR.
  * Validate real English, Hindi, and Gujarati text at **100% and 200% font scale** (matras/vowel marks, line height, wrapping, contrast, $\ge 48\text{dp}$ targets, TalkBack focus order, reduced motion).
  * Benchmark on physical **Google Pixel 10 Pro** against current reader: log lost-place events, second-hand reaches, accidental page turns, and post-session preference to pick the winner.
* **Signature Flow 2 (Online Acquisition):** Search (`My library` vs `Online sources`) → Edition & Format selection → Transfers (progress, waiting, browser handoff, retry, cancel, `Open book`).
* **Full Screen Inventory (14 screens):** Home, Library, Filters, Book Details, Reader, Typography Panel, Highlights & Notes, Dictionary/Translate Sheet, Translated Reading Mode, Online Search Results, Choose Edition & Format, Transfers, Vocabulary & Flashcards, Settings (incl. *Prepare for offline reading*) + Stats.

## 6. Security, Privacy & Memory Rules

* **Magic-byte validation** on all imports and downloads; reject disguised executables (`.apk`, `.exe`, scripts). Validate ZIP/container structure for EPUB, DOCX, CBZ.
* **Active content blocked:** No JS in PDF/HTML; block publication-supplied active content in EPUB; zero outbound network requests from book content.
* **Memory safety:** Tile-based rendering, max 3 page bitmaps in memory, decoded-memory budget alongside page limits, downsampled thumbnails, graceful `OutOfMemoryError` handling, `:reader` process isolation.
* **Server & Network:** Acquisition workers have outbound network access restricted to configured provider domains with redirect validation (never log temporary download URLs or tokens). Conversion workers run strictly offline (no network, read-only FS except temp dir, CPU/RAM/timeout caps). Tailscale-only access, SSH keys only, UFW firewall, secrets in `.env` (never committed or printed in CI logs).
* **Privacy disclosure:** Local library searches stay on-device. Online searches transmit only the user-entered query to the selected provider.

## 7. Gemini Workflow, Harness, Git Hygiene & Token Efficiency

* **Repository:** `https://github.com/solerunner26/ZeusReader.git`
* **Harness & Hooks:** Bootstrap `.gemini/`, `.githooks/`, and `harness/` from `HARNESS_AND_HOOKS.md` in Phase 0. Run `.\harness\run-harness.ps1` at every phase gate.
* **Git Discipline:** Keep `main` buildable at every commit. Use small, conventional commits (`feat(reader): ...`, `fix(server): ...`) and push verified phase milestones to `origin`. Never commit `.env`, signing keystores (`.jks`/`.p12`), or copyrighted book files.
* **Minimal context load:** Start each phase from the relevant `GEMINI.md` section, previous `/docs/phase-N.md`, and `git diff`. Load only tools/skills needed for the current task.
* **One decision at a time:** Prefer official docs, release notes, maintainer repos, and standards. Record consequential choices in `/docs/adr/ADR-NNN.md` (*question, version/date, source URL, decision, project implication*).
* **Lean iteration:** Use targeted file searches, diffs, and focused unit/instrumented tests during coding. Save full build/test logs locally; report only failing task, root error, and attempted fix. Run the full suite at phase and release gates.
* **Compact handoffs:** `/docs/phase-N.md` records only *changes, tests/evidence status, unresolved issues, and next entry point*.

## 8. Repository Layout

```text
/.gemini        Gemini CLI settings.json & guard/logging hooks
/.githooks      Git pre-commit hook (blocks secrets & >5MB book binaries)
/harness        Pixel 10 Pro device-profile.json & run-harness.ps1
/android        Gradle project (Kotlin, Compose, Readium, MuPDF, Room, Macrobenchmark)
/server         FastAPI + SQLite + Docker Compose (x86_64 Windows & arm64 Oracle) + /connectors
/design         DESIGN.md, Google Stitch prototypes, Material 3 tokens, Pixel 10 Pro UX study notes
/docs           phase-N.md handoffs, /adr, OpenAPI spec, release notes, maintenance checklist
/assets-build   Reproducible scripts to build WordNet & bilingual Hindi/Gujarati SQLite DBs
/fixtures       Shared test corpus & saved provider HTTP fixtures (no copyrighted books in git)
GEMINI.md
HANDOFF_PACKAGE.md
HARNESS_AND_HOOKS.md
```

## 9. Deliberate Tooling Shortlist

Verify exact skill/CLI names and Windows 11 + Gradle compatibility at the phase where each tool enters. Prefer the free local route.

| Job | Primary Tool | Class | Phase | Observable Output | Local/Free Route & Constraints |
| ----- | ----- | ----- | ----- | ----- | ----- |
| Autonomous coding & git push | **Gemini CLI** (`@google/gemini-cli`) | Agent CLI | 0+ | Code edits, Gradle runs, git commits/pushes | Free tier with Google account; runs locally on Windows 11 |
| High-impact spec/ADR review | `google/elephant-goldfish-model` | Agent skill | 0, 2, 7 | Written gap/risk critique on ADRs & schemas | Free local prompt/skill pattern; run at gates only |
| Design alternatives & handoff | **Google Stitch** (`stitch-skills`) | Design / Skill | 0–1 | 3 reading prototypes + `/design/DESIGN.md` | Free tier / local Markdown tokens |
| Adaptive Compose & Android CLI | `android/skills` (`devtools/android-cli`) | CLI / Skills | 0+ | Verified adaptive layouts & CLI diagnostics | Free local; run `android skills list` to confirm |
| Targeted library docs | `upstash/context7` | MCP Server | 0+ | Version-exact API snippets (Readium/Room) | Free tier; fallback: official web docs |
| EPUB compliance fixtures | `epubtest.org/test-books` | Fixtures | 2–4 | Pass/fail matrix on standard EPUB test books | Free local open-source `.epub` files |
| Compose visual regression | `takahirom/roborazzi` | Test library | 1, 3, 5, 6 | Baseline PNGs (themes, 200% scale, 3 scripts) | Free local JVM (Robolectric) on Windows 11 |
| E2E Android user journeys | `mobile-dev-inc/Maestro` | Testing CLI | 3, 6, 7 | Pass/fail YAML flows on Pixel 10 Pro | Free local CLI on Windows 11 + ADB |
| Accessibility, perf & signing | `Android Developers` (Compose A11y, Macrobenchmark, `apksigner`) | Libraries / CLI | 3, 9 | A11y checks, startup/frame traces, verified APK | Free local via Android SDK & Gradle |
| Private CI & release signing | **GitHub Actions** (`gradle/actions`) | CI Plugin | 0, 3, 7+ | Automated JVM/lint checks + signed APK artifact | GitHub free minutes (store keystore in GitHub Secrets) |

## 10. Private Deployment, Release & Maintenance

### 10.1 Android Private Release Pipeline

* **Versioning:** `versionCode` monotonically increasing integer; `versionName` `MAJOR.MINOR.PATCH-private` (e.g., `0.3.0-private`).
* **Signing:** Private JKS/PKCS12 keystore stored encrypted outside the repo (plus offline backup and optional GitHub Actions encrypted secrets for automated signed releases). Never commit keystores or passwords.
* **Build & Verify:** `./gradlew assembleRelease` → verify with `apksigner verify --verbose` → generate SHA-256 checksum → write `/docs/releases/vX.Y.Z.md` → sideload to **Google Pixel 10 Pro** via USB/ADB or private Tailscale share.
* **Upgrade & Recovery Gate:** Every release tests **(1) clean install** and **(2) in-place upgrade** over the previous signed APK with books, highlights, notes, vocabulary, and progress intact. **Bad-update recovery:** export a *Library data only* backup before upgrading; retain the last known-good signed APK; if a Room migration breaks, reinstall the last known-good APK and restore the pre-upgrade backup.

### 10.2 Private Server Deployment & Recovery

* **Local Windows 11 Laptop (default):** Docker Desktop (or native Python venv + Calibre/LibreOffice), accessible to **Google Pixel 10 Pro** via Tailscale or home LAN. Zero cost.
* **Oracle Cloud Singapore (optional, Phase 7+):** Before provisioning, verify current Always Free Ampere A1 quota (documented at 1,500 A1 OCPU-hr + 9,000 GB-hr/month ≈ 2 OCPUs / 12 GB RAM) and ARM64 container compatibility.
* **Operations:** QR pairing (Tailscale host + auth token), `/health` endpoint, storage quota guard, automated SQLite migrations, scheduled encrypted backups, and documented host-loss rebuild rehearsal. Test Android against **(a) server offline** and **(b) previous supported server API version**.

### 10.3 Maintenance Checklist (Every Release & Phase Gate)

1. Dependency & parser security check (MuPDF, Readium, Calibre, LibreOffice, FastAPI).
2. Fixture regression suite (JVM + Room + Roborazzi + backend pytest).
3. In-place upgrade test on physical **Google Pixel 10 Pro**.
4. Backup & clean-install restore rehearsal.
5. Update `/docs/known-issues.md` and tag `last-known-good` signed APK + server image in GitHub Releases.

## 11. Phased Build Plan & Exit Conditions

**Earliest useful privately installed daily-driver:** **Phase 3** (complete offline EPUB/PDF/image reader with one-handed ergonomics, highlights, and first signed private APK). Phases 4–10 layer additional formats, multilingual study, private server/online acquisition, optional AI, benchmarks, and legacy formats.

| Phase | Deliverable | Depends On | Exit Condition (Acceptance Test) | Required Evidence |
| ----- | ----- | ----- | ----- | ----- |
| **0. Setup, Harness & Connector Proof** | Bootstrap harness/hooks (`HARNESS_AND_HOOKS.md`), repo scaffold pushed to `ZeusReader.git`, Gradle catalogs, Hilt/Room/Compose shell, `android/skills` verified, `DESIGN.md` (3 reading prototypes), **laptop connector spike** (SLUM + 1 provider on 1 public-domain title) | None | Hooks active; `.\harness\run-harness.ps1` passes; app launches to empty Library; 3 prototypes tested on Pixel 10 Pro to pick reading UX; 1 public-domain book searched, acquired, verified by magic bytes, and opened offline on Pixel 10 Pro; saved HTTP fixtures created | `/docs/phase-0.md`, Pixel 10 Pro UX log, live spike log + saved fixtures |
| **1. Library, Import & PDF/Images** | Local import (picker, share, watched folder), SHA-256 dedup, magic-byte & container checks, `Work/Edition/FileAsset` schema, MuPDF PDF + JPG/PNG image-book grouping/reordering | Phase 0 | Import 1 text PDF, 1 scanned 2-col PDF, 5 JPGs (grouped & reordered); disguised `.apk` as `.pdf` rejected; re-import does not duplicate | JVM + Room tests, Roborazzi library baseline, `/docs/phase-1.md` |
| **2. EPUB (Readium) & Shared Shell** | `ReaderEngine` + capability flags, Readium EPUB (publication active-content blocked), unified Locator, `:reader` process, TOC, in-book search, EPUBTest fixtures | Phase 1 | Import EPUB + scanned PDF, airplane mode, force-stop & restart → exact passage/viewport resume; malformed PDF fails safely without killing Library | Instrumented reader tests, EPUBTest pass matrix, `/docs/phase-2.md` |
| **3. Ergonomic Reader & First Private Release** | Winning one-handed reading UX from Phase 0, typography/themes (OLED/E-ink), edge turns, $\ge 48\text{dp}$ targets, rotation lock, highlights/notes/Markdown export, PDF crop/zoom/reflow, **first signed private APK** | Phase 2 | Font-size change keeps EPUB passage anchor; highlights persist across restart & export; clean install + Maestro `import→read→resume` passes; Pixel 10 Pro TalkBack & either-hand comfort pass | Signed APK + `apksigner` log + SHA-256, Roborazzi baselines, Maestro log, Pixel 10 Pro manual checklist |
| **4. Expanded Formats** | XPS/OXPS, CBZ, sandboxed HTML (local images), limited TIFF (30 MB / 100 pp / decoded-RAM cap), `.oxp`/unknown handling | Phase 3 | Every supported sample opens; 150-page or oversized TIFF rejected cleanly; HTML `<script>` and remote image both blocked | Instrumented format corpus tests, `/docs/phase-4.md` |
| **5. Offline Dictionary & Vocab** | `/assets-build` scripts (WordNet + `kaikki.org` Hi/Gu SQLite with attribution), *Prepare for offline reading* screen, ML Kit `en→hi`/`en→gu`, dictionary sheet, TTS, vocab + flashcards | Phase 3 | Fresh install: English works offline, missing ML Kit pack prompted cleanly; prepared phone offline: ambiguous word, missing word, and "ephemeral" show multi-sense Hi/Gu meanings; sentence translates; saved word keeps context | Unit + Compose tests (100% & 200% scale, 3 scripts), Roborazzi baselines, human Hi/Gu check |
| **6. Translated Reading Mode** | Sentence-aligned translation (EPUB → text PDF → scanned PDF OCR), passage-anchored cache, pre-translate 2–3 pages, Interlinear/Translation-only/Side-by-side, glossary, engine-stability setting, translated TTS | Phase 5 | 2-col scanned textbook page (diagram + caption + equation) preserves order or offers side-by-side fallback; font-size change reuses cache; glossary term stays English; Maestro `lookup→translate→return` passes; human-reviewed Hi/Gu passages preserve names/numbers/negation | Unit cache-key tests, Maestro log, first-time vs cached latency log, human Hi/Gu review sheet |
| **7. Private Server, Acquisition & Sync** | FastAPI + SQLite + Docker (Windows 11 + optional Oracle ARM64), Tailscale + QR pairing, Open SLUM status reader, verified provider connector(s), 2-stage transfer queue, browser handoff (`Share→ZeusReader`), tombstone sync, 1 working sample conversion, 2-mode encrypted backup | Phase 6 | Phone→Server→Phone live test on actual host: interrupt each stage, expire link, restart server, test browser handoff → 1 edition, 1 verified offline file, zero duplicates; Maestro `search→edition→transfer→offline` passes; data-only & full-backup restores pass on clean install; server offline & previous-API compatibility pass | Backend pytest suite, live host acquisition log, Maestro log, backup/restore rehearsal log, in-place APK upgrade test |
| **8. Optional Server AI** | Independently toggleable: Sarvam-Translate (benchmark before default), Explain this passage, chapter summaries, smart search, auto-metadata, ask-the-book RAG (with locator citations), vocab quizzes, optional server OCR | Phase 7 | Server off → all offline features unaffected; server on → user-selected engine applies without mid-read text swap; RAG cites passage or replies "not found"; Hi/Gu quality, latency & peak RAM benchmarked on actual host | Host AI benchmark table, API tests, human Hi/Gu comparison |
| **9. Polish, Benchmarks & Hardening** | Stats screen, TTS sleep timer + foreground service polish, Macrobenchmark on **Google Pixel 10 Pro**, R8 shrinking, full TalkBack/contrast pass, maintenance release checklist | Phase 7 | Release-like build on Pixel 10 Pro meets startup (< 1.5 s cold) and jank-free page-turn targets; cached vs first-time translation measured separately; in-place upgrade from previous signed APK preserves all data | Macrobenchmark JSON/report, R8 check, Pixel 10 Pro A11y & long-session log, signed release notes |
| **10. Optional Legacy Formats** | Qualify `MOBI`, `AZW3`, `KF8`, DRM-free `AZW`, `DOC`, `DOCX` via server Calibre/LibreOffice conversion | Phase 7 | Representative DRM-free AZW3 and table-heavy DOCX import, convert, and open cleanly; originals preserved & re-convertible; DRM-locked file rejected with clear message | Conversion fixture tests + timeout/sandbox tests, `/docs/phase-10.md` |
## UI rule
- All UI work must load the stormlight-design skill first (.agents/skills/stormlight-design). It overrides generic design skills.
