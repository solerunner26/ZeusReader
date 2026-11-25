# ZeusReader — Gemini Execution & Design Handoff Package

## 1. Change Summary (`CLAUDE.md` → `GEMINI.md`)
- **Transitioned to 100% Gemini execution:** Renamed `CLAUDE.md` to `GEMINI.md`, replaced Claude Code `/plugin` and Claude Design references with **Gemini CLI** (`@google/gemini-cli`), **Google Stitch**, and standard `android/skills` CLI commands.
- **Wired your GitHub repo & hardware:** Bound the project to `https://github.com/solerunner26/ZeusReader.git`, updated the app name to **ZeusReader** (`com.solerunner26.zeusreader`), set owner to Karan Agrawal, and aligned the primary target device to **Google Pixel 10 Pro**.
- **Integrated Harness & Hooks:** Added `HARNESS_AND_HOOKS.md` covering Gemini CLI safety/UI hooks, Git pre-commit guards, and the PowerShell test/ADB harness.
- **Unified Open SLUM & online acquisition:** Integrated Open SLUM status monitoring, pluggable provider connectors (with optional DoH/proxy for Indian ISP blocks), two-stage transfer states (`ReadyOnServer` vs `AvailableOffline`), and browser handoff (`Share → ZeusReader`) directly into Phase 0 and Phase 7.

"Scope is extended by docs/SCOPE-ADDENDUM-v2.md; UI follows design/DESIGN.md v2 (replaces v1)."
---

## 2. How Gemini Will Build & Push This App (Feasibility & Setup Plan)

Run **Gemini CLI** locally on your Windows 11 laptop. Gemini CLI automatically reads `GEMINI.md`, creates the harness/hooks, writes code, runs `./gradlew check` and `pytest`, and commits/pushes directly to `https://github.com/solerunner26/ZeusReader.git`.

### One-Time Local Setup (5 Minutes on Windows 11)
1. **Clone your repo & drop in the 3 `.md` files:**
   ```powershell
   git clone https://github.com/solerunner26/ZeusReader.git
   cd ZeusReader
   # Save GEMINI.md, HANDOFF_PACKAGE.md, and HARNESS_AND_HOOKS.md here
   ```
2. **Install Gemini CLI:**
   ```powershell
   npm install -g @google/gemini-cli
   gemini
   ```
   Sign in with your Google account when prompted.
3. **Verify local dev tools (free):**
   - **Android Studio + Android SDK** (with `adb` and USB/Wireless Debugging enabled on your **Google Pixel 10 Pro**)
   - **Node.js 18+** (for Gemini CLI and the `.gemini/hooks/*.js` scripts)
   - **Python 3.12+** (for the Phase 0 SLUM connector spike and FastAPI server)
   - **GitHub CLI (`gh auth login`)** or SSH key so Gemini CLI can push commits cleanly to `ZeusReader.git`
   - **Google Stitch** (web) for generating the Phase 0 reading prototypes before Phase 1 UI work

---

## 3. Compact Tool Table

| Tool / Upstream Source | Class | Purpose | Phase(s) | Observable Output | Local / Free Fallback & Constraints |
|---|---|---|---|---|---|
| **Gemini CLI** (`@google/gemini-cli`) | Agent CLI | End-to-end coding, Gradle/pytest runs, git push | 0+ | Working code, passing tests, commits in `ZeusReader.git` | Free tier with Google account on Windows 11 |
| `google/elephant-goldfish-model` | Agent skill | High-impact spec, schema & ADR review | 0, 2, 7 | Written risk/gap critique | Free local skill/prompt; run only at major gates |
| **Google Stitch** (`stitch-skills`) | Design / Skill | 3 reading prototypes, design system & `DESIGN.md` | 0–1 | Interactive prototypes + M3 token handoff | Free tier / local Markdown tokens |
| `android/skills` (`devtools/android-cli`) | CLI / Skills | Native adaptive Compose patterns & Android CLI | 0+ | Verified adaptive layouts & CLI checks | Free local; verify via `android skills list` |
| `upstash/context7` | MCP Server | Version-exact library docs (Readium, Room, Compose) | 0+ | Targeted API snippets | Free tier; fallback: official web docs |
| `epubtest.org/test-books` | Fixtures | Standard EPUB reading-system compliance | 2–4 | Pass/fail matrix on test books | Free local open-source `.epub` corpus |
| `takahirom/roborazzi` | Test library | Compose visual regression (themes, 200% text, 3 scripts) | 1, 3, 5, 6 | Baseline PNG diffs on JVM (Windows 11) | Free local via Robolectric; no emulator needed |
| `mobile-dev-inc/Maestro` | Testing CLI | 3 core E2E journeys on physical **Pixel 10 Pro** | 3, 6, 7 | Passing YAML journey logs | Free local CLI on Windows 11 over ADB |
| `Android Developers` (Compose A11y, Macrobenchmark, `apksigner`) | Libraries / CLI | Accessibility checks, startup/jank benchmarks, APK signing | 3, 9 | A11y pass, JSON perf traces, verified APK | Free local via Android SDK & Gradle |
| **GitHub Actions** (`gradle/actions`) | CI Plugin | Automated JVM/lint/fixture checks + signed release APK | 0, 3, 7+ | Passing CI runs + private signed APK artifact | GitHub free minutes (store keystore in Secrets) |

---

## 4. Phase Table

| Phase | Deliverable | Dependency | Acceptance Test | Evidence |
|---|---|---|---|---|
| **0. Setup, Harness & Connector Proof** | Bootstrap harness/hooks (`HARNESS_AND_HOOKS.md`), scaffold pushed to `ZeusReader.git`, Gradle catalogs, `android/skills`, `DESIGN.md` (3 prototypes), laptop connector spike (SLUM + 1 provider) | None | Hooks active; `run-harness.ps1` passes; app opens empty Library; 3 prototypes compared on Pixel 10 Pro; 1 public-domain book searched, downloaded, magic-byte verified & opened offline; saved fixtures created | `/docs/phase-0.md`, Pixel 10 Pro UX log, live spike log + saved HTTP fixtures |
| **1. Library, Import & PDF/Images** | Picker/share/watched-folder import, SHA-256 dedup, magic-byte/container checks, `Work/Edition/FileAsset`, MuPDF PDF + image books | Phase 0 | Text PDF, 2-col scanned PDF, 5 grouped/reordered JPGs import & open; fake `.pdf` (`.apk`) rejected; duplicate import ignored | JVM + Room tests, Roborazzi library baseline, `/docs/phase-1.md` |
| **2. EPUB & Shared Shell** | `ReaderEngine` + capabilities, Readium EPUB (active content blocked), unified Locator, `:reader` process, TOC, search | Phase 1 | Airplane mode force-stop & restart resumes exact EPUB passage & PDF viewport; malformed PDF fails safely without crashing Library | Instrumented tests, EPUBTest matrix, `/docs/phase-2.md` |
| **3. Ergonomic Reader (First Private Release)** | Winning one-handed UX, typography/themes (OLED/E-ink), edge turns, rotation lock, highlights/Markdown export, PDF crop/reflow, **signed APK** | Phase 2 | Font-size change keeps EPUB passage; highlights persist & export; Maestro `import→read→resume` passes; Pixel 10 Pro TalkBack & either-hand check passes | Signed APK + `apksigner` verify + SHA-256, Roborazzi baselines, Maestro & Pixel 10 Pro logs |
| **4. Expanded Formats** | XPS/OXPS, CBZ, sandboxed HTML, limited TIFF (30 MB / 100 pp / RAM cap), `.oxp` handling | Phase 3 | All supported samples open; 150-page/oversized TIFF rejected; HTML `<script>` & remote image blocked | Format corpus instrumented tests, `/docs/phase-4.md` |
| **5. Offline Dictionary & Vocab** | WordNet + `kaikki.org` Hi/Gu SQLite, *Prepare for offline reading* screen, ML Kit `en→hi`/`en→gu`, dictionary sheet, TTS, vocab/flashcards | Phase 3 | Fresh install prompts missing packs while English works; prepared phone offline shows multi-sense Hi/Gu for ambiguous/missing/"ephemeral"; sentence translates; word saves with context | Unit + Compose tests (100% & 200% scale, 3 scripts), Roborazzi baselines, human Hi/Gu review |
| **6. Translated Reading Mode** | Sentence-aligned EPUB/PDF/OCR translation, passage-anchored cache, 2–3 page pre-translate, 3 display modes, glossary, engine lock, TTS | Phase 5 | 2-col scanned textbook page preserves order or offers side-by-side fallback; font change reuses cache; glossary stays English; Maestro `lookup→translate→return` passes; human Hi/Gu review passes | Cache-key unit tests, Maestro log, first-time vs cached latency log, human review |
| **7. Private Server, Acquisition & Sync** | FastAPI + SQLite + Docker (Windows 11 / optional Oracle ARM64), Tailscale + QR pairing, SLUM reader, provider connector(s), 2-stage transfers, browser handoff, tombstone sync, 1 sample conversion, 2-mode encrypted backup | Phase 6 | Live host Phone→Server→Phone: interrupt each stage, expire link, restart server, browser handoff → 1 edition, 1 offline file, no duplicates; Maestro `search→edition→transfer→offline` passes; both backup restores & offline/prev-API tests pass | Backend pytest, live host log, Maestro log, backup/restore log, in-place APK upgrade test |
| **8. Optional Server AI** | Toggleable Sarvam-Translate, Explain passage, summaries, smart search, auto-metadata, RAG Q&A with citations, vocab quizzes, optional OCR | Phase 7 | Server off leaves offline features intact; server on never swaps visible text mid-read; RAG cites locator or says "not found"; host quality/latency/RAM benchmarked | Host AI benchmark table, API tests, human Hi/Gu comparison |
| **9. Polish & Benchmarks** | Stats, TTS sleep timer/service polish, **Pixel 10 Pro** Macrobenchmark, R8 shrinking, full A11y pass, maintenance checklist | Phase 7 | Pixel 10 Pro release-like build: cold start < 1.5 s, jank-free page turns, cached vs first-time translation measured; in-place APK upgrade preserves all data | Macrobenchmark report, R8 check, Pixel 10 Pro A11y/comfort log, release notes |
| **10. Optional Legacy Formats** | `MOBI`, `AZW3`, `KF8`, DRM-free `AZW`, `DOC`, `DOCX` via server conversion | Phase 7 | DRM-free AZW3 & table-heavy DOCX convert & open; originals preserved & re-convertible; DRM file rejected clearly | Conversion + sandbox/timeout tests, `/docs/phase-10.md` |

---

## 5. Ready-to-Use Google Stitch Prompt (One-Handed Multilingual Reading Flow)

```text
Design the signature mobile reading experience for "ZeusReader", a calm, habit-forming, offline-first Android e-reader built with Jetpack Compose and Material 3 (with subtle glassmorphic/Liquid Glass floating sheets and pure-black OLED support) for a Google Pixel 10 Pro (portrait one-handed primary, plus adaptive two-pane layout for wider/landscape windows).

SIGNATURE USER SCENARIO
The reader is holding a Google Pixel 10 Pro in one hand (left or right, including while reclining), reading an English science textbook, looks up an unfamiliar word, views a Hindi or Gujarati translation, inspects an inline diagram, and returns to the exact source passage without losing their place or accidentally turning the page. Background book download notifications must remain completely quiet and non-intrusive while reading.

USE THIS EXACT REAL MULTILINGUAL PASSAGE FOR ALL PROTOTYPES
- English source: "Capillary action draws water upward through narrow soil pores against gravity, supplying plant roots during dry intervals."
- Target lookup word: "capillary" (multi-sense noun/adjective)
- Hindi translation (Devanagari): "केशिका क्रिया गुरुत्वाकर्षण के विरुद्ध मिट्टी के संकीर्ण छिद्रों से पानी को ऊपर की ओर खींचती है, जिससे शुष्क अंतराल के दौरान पौधों की जड़ों को पानी मिलता है।"
- Gujarati translation (Gujarati script): "કેશિકા ક્રિયા ગુરુત્વાકર્ષણની વિરુદ્ધ માટીના સાંકડા છિદ્રોમાંથી પાણીને ઉપર તરફ ખેંચે છે, જેનાથી શુષ્ક સમયગાળા દરમિયાન છોડના મૂળને પાણી મળે છે."
- Include a compact labelled diagram ("Fig 4.2: Capillary rise in fine vs. coarse soil pores") right below the paragraph.

PRODUCE THREE COMPARABLE ALTERNATIVES (SAME PASSAGE & STARTING POSITION)
- Alternative A (Inline Expansion): Tapping/long-pressing a sentence expands the Hindi/Gujarati translation smoothly inline directly beneath the sentence, pushing following text down without losing the anchor.
- Alternative B (Thumb-Reachable Sheet): A bottom sheet anchored to the lower 40% thumb zone showing English WordNet meaning, Hindi/Gujarati word meanings, pronunciation button, Save-to-Vocab, and sentence translation—while keeping the highlighted source sentence clearly visible in the upper viewport.
- Alternative C (Paired Passage & Adaptive Two-Pane): Stacked English/translated passage cards in portrait with a subtle visual connector, transitioning to a synchronized side-by-side two-pane view on landscape/tablet windows.

REQUIRED STATES & EDGE CASES FOR EACH ALTERNATIVE
1. Initial reading state (distraction-free canvas, subtle progress indicator, thumb-safe edge turn zones with visible toolbar fallback).
2. Active lookup / translation state + diagram inspection (pinch-zoom modal or expandable card) + dismissal back to the exact restored reading position.
3. Explicitly annotate what changes across: (a) Reflowable EPUB, (b) Fixed-layout scanned PDF, and (c) Uncertain OCR result (confidence badge + quick toggle to view original cropped page).
4. Show both 100% normal font scale and 200% accessibility font scale for English, Hindi, and Gujarati. Ensure generous line height so Devanagari shirorekha/matras and Gujarati vowel marks never clip, all interactive targets are >= 48dp, contrast meets WCAG AA across Day, Sepia, Night, Pure-Black OLED, and E-ink themes, TalkBack focus order is logical, and reduced-motion transitions are specified.

DESIGN HANDOFF OUTPUT
Generate a Material 3 token table (type scale per script using Literata/Atkinson Hyperlegible + Noto Sans Devanagari + Noto Sans Gujarati, colour roles, spacing, touch targets, gesture + visible control mappings) ready to export into `/design/DESIGN.md`, and outline how these tokens extend to Online Search Results, Choose Edition & Format, and Transfers.
```

---

## 6. Gemini CLI Kickoff Prompt (Phase 0)

Paste this directly into **Gemini CLI** inside your cloned `ZeusReader` folder:

```text
Read GEMINI.md and HARNESS_AND_HOOKS.md completely. Do not start Phase 1.

Execute Phase 0 in order:
1. First, create every file defined in HARNESS_AND_HOOKS.md (`.gemini/settings.json`, `.gemini/hooks/*.js`, `.githooks/pre-commit`, `harness/device-profile.json`, `harness/run-harness.ps1`) and configure `git config core.hooksPath .githooks`.
2. Verify local tooling on Windows 11 (`android skills list`, Gradle, Python 3.12+, git remote `origin` pointing to https://github.com/solerunner26/ZeusReader.git).
3. Scaffold the repository layout (GEMINI.md §8): `/android` (Kotlin, Compose, Material 3, Gradle version catalog, Hilt, Room with `@Fts4`, empty Library start screen for package `com.solerunner26.zeusreader`), `/server`, `/design`, `/docs/adr`, `/assets-build`, `/fixtures`, and a strict `.gitignore` (excluding `.env`, keystores, and book files). Run `.\harness\run-harness.ps1 -Target android` and confirm it passes.
4. Build a minimal, isolated Phase 0 connector feasibility spike in `/server/spike/` that reads `open-slum.org` availability and runs one end-to-end search + download test on one known public-domain title from this network (with optional DoH/proxy support if needed). Record: source, selected edition, magic-byte file type, whether human/browser interaction was needed, and whether the file opens offline on the Google Pixel 10 Pro. Save sanitized HTTP response fixtures in `/fixtures/` so Android work never blocks on live providers.
5. Commit all Phase 0 work with clean conventional commits and push to `origin`.
6. Do NOT build Phase 1 UI yet—pause after Phase 0 so we can complete the `/design/DESIGN.md` Google Stitch prototypes and Google Pixel 10 Pro UX comparison first.
7. Write `/docs/phase-0.md` with changes, test/evidence status (`passed`/`unverified`), unresolved issues, and the Phase 1 entry point, then stop for my review.
```