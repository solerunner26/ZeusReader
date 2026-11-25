# Phase 9 Handoff — Polish, Benchmarks, Hardening & Server Packaging

## Changes
- **Stats & Reading Moments:**
  - Added `StatsScreen`, `StatsViewModel`, and `StatsRoute` (`ui/stats/`) computing library totals, in-progress books, saved passages, vocabulary counts/due reviews, and consecutive reading streak days.
  - Updated `ChapterBreathCard` in `SignatureReadingMoments.kt` to display `"Passages saved this chapter"`.
- **TTS Sleep Timer:**
  - Added `TtsSleepTimerController` and `TtsSleepTimerOption` (`ui/reader/TtsSleepTimer.kt`) with 15m/30m/45m/60m/Off presets and wired it into `TranslatedReadingOverlay.kt`.
- **R8 Shrinking & Macrobenchmark:**
  - Created `android/app/proguard-rules.pro` and enabled `isMinifyEnabled = true` and `isShrinkResources = true` for `release` in `android/app/build.gradle.kts`, plus a `benchmark` buildType.
  - Added `:benchmark` module (`android/benchmark/`) with `StartupAndReaderBenchmark` (`StartupTimingMetric`, `FrameTimingMetric`).
- **Private Server Packaging & Online UI:**
  - Added `server/Dockerfile` and `server/docker-compose.yml` (multi-arch `amd64`/`arm64`, isolated `converter` service with `network_mode: none`).
  - Wired `/v1/convert` in `server/app/main.py` using `OfflineLegacyConverter` + endpoint tests in `server/tests/test_converter.py`.
  - Added `OnlineAcquisitionScreen`, `OnlineAcquisitionViewModel`, `OnlineRoute`, and extended `PrivateServerClient` for SLUM status, online search, transfer queue, and passage sync.

## Tests & Evidence Status
- **Server pytest (`python -m pytest -q`):** `passed` (12 passed in 0.83s).
- **Android (`./gradlew.bat testDebugUnitTest lintDebug assembleRelease`):** `passed` (exit 0; includes `Phase9PolishTest`, R8 minification, `lintVitalRelease`, and `assembleRelease`).
- **Pixel 10 Pro Macrobenchmark & physical signing/upgrade:** `unverified` (no physical Pixel 10 Pro attached via ADB in this session).
- **Oracle Cloud Always Free (Singapore, ARM64):** `unverified` / blocked on user login at `https://www.oracle.com/cloud/sign-in.html` in the browser pane.

## Next Entry Point
1. Sign in to Oracle Cloud Console in the browser pane so the browser agent can verify `ap-singapore-1` Always Free Ampere A1 quota and provision the ARM64 host.
2. Connect Google Pixel 10 Pro via ADB to run `:benchmark:connectedBenchmarkAndroidTest` and verify signed APK installation (`apksigner verify --verbose`).
