# ZeusReader — UI/UX Handoff for Claude Code

## 1. Purpose & Strict Scope Boundary
This handoff gives Claude Code everything needed to complete the **Stormlight v4 UI/UX** overhaul (`design/DESIGN.md` + `design/screens/*.dc.html` + `.agents/skills/stormlight-design/SKILL.md`).

### DO NOT TOUCH (Handled Separately)
- **SLUM Connector & Online Acquisition logic:** `relay/`, `android/.../sync/`, `OnlineAcquisitionViewModel.kt`, `PrivateServerClient.kt`, `GitHubAppUpdater.kt`.
- **Database, Importer & Reader Engines:** `android/.../data/`, `android/.../reader/epub/`, `android/.../reader/pdf/`, `android/.../dictionary/`, `android/.../translation/`.
- **UI-only scope:** Work strictly inside `android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/` and `Navigation.kt` (and test files under `android/app/src/test/`).

---

## 2. Build & Test Commands (CRITICAL — Windows / WSL)
Always run Gradle from the **repo root** via `android/build.ps1` with `< /dev/null` (never inline `JAVA_HOME`, never omit `< /dev/null`, never use `--no-daemon` or `--rerun-tasks`):
- Incremental compile check:
  `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 :app:compileDebugKotlin < /dev/null`
- Build APK:
  `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug < /dev/null`
- Unit tests + Lint:
  `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 testDebugUnitTest lintDebug < /dev/null`

---

## 3. What Is Already Implemented
1. **Stormlight v4 Theme Tokens (`ui/theme/`):**
   - `ZeusColors.kt`: 5 Skies (`STORM`, `AURORA`, `SUNSET`, `MONSOON`, `EMBER`) + 4 Papers (`CLOUD`, `NIGHT`, `OLED`, `EINK`) + `stormlightColorScheme(...)`.
   - `ZeusType.kt` + bundled OFL fonts in `res/font/` (`unbounded.ttf`, `bricolage_grotesque.ttf`, `jetbrains_mono.ttf`, `baloo2.ttf`, `baloo_bhai2.ttf`, `literata_regular.ttf`, `noto_serif_devanagari_regular.ttf`, `noto_serif_gujarati_regular.ttf`).
   - `ZeusShapes.kt`, `ZeusSpacing.kt`, `ZeusMotion.kt`, `ZeusTheme.kt`, `LiquidGlass.kt`, `HueAura.kt`, `Theme.kt`.
   - `StormlightContrastTest.kt` verifying ≥ 7:1 contrast on all 5 Skies and 4 Papers.
2. **Brand Assets:**
   - App Icon: `res/drawable-nodpi/zeus_app_logo.png` + `res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`.
   - Home Banner: `res/drawable-nodpi/zeus_home_banner.png`.
3. **Initial Navigation & Home:**
   - `Navigation.kt` uses a 68dp Stormlight Floating Glass Dock (`Home`, `Olympus`, `Sparks`, `Words`).
   - `HomeScreen.kt` renders `zeus_home_banner`, Streak sticker, Profile/Settings button, 3D cover + Charge ring, Glass Resume card, Resurfaced Spark + Words sticker card.
4. **SLUM Online Search Stability & Auto-Clear (Fixed):**
   - `OnlineAcquisitionScreen.kt` uses unique `itemsIndexed` keys (`"${idx}_${item.idempotency_key}"` and `"${idx}_${job.job_id}"`) to prevent `LazyColumn` duplicate-key crashes.
   - `OnlineAcquisitionViewModel.kt` persists `downloaded_job_ids` in `SharedPreferences` and clears `query`, `searchResults`, `transfers`, and book title from `statusMessage` after download completes.

---

## 4. Pending UI/UX Gaps for Claude Code to Fix
Review every screen against `design/screens/*.dc.html` and fix:
1. **Alignment, Overlapping Controls & Whitespace:**
   - Audit icon-to-text alignment, button padding, image aspect ratios (`zeus_home_banner.png` scaling/cropping on `HomeScreen.kt`), and card spacing across all screens.
   - Ensure no controls overlap the status bar, navigation bar, or the 68dp Floating Glass Dock (`ZeusSpacing.dockBottomClearance`).
   - Test English, Hindi (`HI`), and Gujarati (`GU`) at `fontScale` `1.0`, `1.3`, and `2.0` (no clipping or overlap).
2. **Complete Stormlight Screen Redesigns (`Phase 4` remaining):**
   - **Olympus (`LibraryScreen.kt`):** Cover-flow / dense glass grid on living sky, glass filter chips, clean import orb, natural-language search bar.
   - **Reader (`PdfReaderScreen.kt`, `EpubReaderScreen.kt`, `ImageBookReaderScreen.kt`, `HtmlReaderScreen.kt`):** 30dp rounded paper sheet inset 8dp inside the sky; replace scattered toolbars with **The Orb (68dp)** + **Constellation** radial menu + **Fly** scrubber; restyle `DictionaryBottomSheet.kt` as **Zap card**, `TypographyBottomSheet.kt` as **Vibe check**, `TranslatedReadingOverlay.kt` as **Translate Lens**, and `TtsSleepTimer.kt` as **Listen**.
   - **Sparks (`PassagesScreen.kt`, `PassageDetailScreen.kt`, `CardStudioScreen.kt`, `SavePassageSheet.kt`):** Star-map header, constellation chips, glass spark cards, and Stormlight Card Studio presets.
   - **Words (`VocabularyScreen.kt`):** Flip-card glass surface, Unbounded counter, `Again / Got it / Easy` pills.
   - **Settings & Online Books (`SettingsScreen.kt`, `StatsScreen.kt`, `OnlineAcquisitionScreen.kt`):** Unified `LiquidGlassCard` sections, high-contrast `OutlinedTextField` colors on dark skies (`textOnSky`), clean icon/text alignment.
3. **Lock-In (`Phase 5`):**
   - Add `uiCheck` Gradle task: Roborazzi snapshots across `{Storm, Aurora, Sunset}` × `{Cloud, OLED, E-ink}` × `fontScale {1.0, 2.0}` × `{EN, HI, GU}`, plus a JVM check banning raw `Color(0x` / `.dp` / `.sp` outside `ui/theme/`.

---

## 5. Cloudflare Worker & Backend Reference (Read-Only Context)
- **Worker Name:** `zeusreader-relay` (source in `/relay/src/index.ts`, config in `/relay/wrangler.toml`)
- **Live Worker URL:** `https://zeusreader-relay.jaldip-sac.workers.dev`
- **Default Pairing Token:** `zeus-dev-pairing-token` (`Authorization: Bearer zeus-dev-pairing-token`)
- **Bindings:**
  - D1 Database: `zeusreader-db` (`database_id = "82fc694a-d6da-4d12-a87f-0dbf33809578"`)
  - KV Namespace: `CACHE` (`id = "d7e2c295443c4f8e984d43b40acd521b"`)
  - Workers AI binding: `AI`
- **Active Routes:**
  - `GET /health`
  - `GET /v1/slum/status`
  - `GET /v1/search?q=...` (returns results sorted `pdf → epub → mobi → azw3`)
  - `POST /v1/transfers`, `GET /v1/transfers`, `GET /v1/transfers/:id/download`, `POST /v1/transfers/:id/cancel`, `POST /v1/transfers/:id/retry`
  - `POST /v1/sync`, `POST /v1/backup`
- **In-App Updater:** `GitHubAppUpdater.kt` polls `https://api.github.com/repos/solerunner26/ZeusReader/releases/latest` (non-prerelease) and compares `tag_name` against `BuildConfig.VERSION_NAME` in `android/app/build.gradle.kts` (`versionCode = 6`, `versionName = "0.6.0-private"`).
