---
name: stormlight-fix
description: One-time workflow that restyles the whole ZeusReader app to the Stormlight design and locks it in with screenshot tests and a token check. Run when the user types /stormlight-fix or asks to fix the entire UI.
---

# /stormlight-fix — make the whole app Stormlight, permanently

The Android/Gradle project is in `android/` (run Gradle from there; `design/` and `.agents/` are at the repo root). Load the `stormlight-design` skill first; it wins over every other design skill.
Use `/mobile-uiux-promax` (Superpowers) for planning each screen if it is installed, and the
Google Android skills for Compose correctness. Work in phases. **At the end of each phase stop,
show the user a short summary plus screenshots, and wait for "go".**

## Phase 1: Check the installed skills
Run `agy plugin list` and list which helper skills are active. Read the SKILL.md of every
third-party skill and tell the user if any runs scripts, makes network calls or writes outside the repo.

## Phase 2: Tokens and theme
Build or repair `ui/theme/`: ZeusColors (all 5 Skies + 4 Papers), ZeusType (bundle fonts in
`res/font`), ZeusShapes, ZeusSpacing, ZeusMotion, and one `ZeusTheme {}` wrapper. No screen changes yet.

## Phase 3: Audit
Make a table with one row per screen in the app. Columns: matching `design/screens/*.dc.html` file;
gaps in colour, type, spacing, shape, motion, missing components and accessibility; priority.
Show the table before fixing anything.

## Phase 4: Fix one screen at a time
Order: Home → Reader (+ Fly, Zap, Translate Lens, Listen, Strike, Spread, Scan) → Ask Zeus →
Sparks (SparksHome, Capture, SparkEdit, CardStudio, Cards) → Olympus → Book → Words → Settings.
For each screen: plan, implement with ZeusTheme tokens only, build, run on emulator or device,
compare with the reference, fix overlaps (EN/HI/GU at fontScale 1.3), then
`git commit -m "ui(stormlight): <screen>"`. Keep full feature parity.

## Phase 5: Lock it in
1. Roborazzi screenshot tests for every screen × {Storm, Aurora, Sunset} × {Cloud, OLED, E-ink}
   × fontScale {1.0, 2.0} × {en, hi, gu}, plus a Reduce-Motion set. Record baselines only after the user approves.
2. Contrast unit tests for every sky × text and paper × ink pair (thresholds in DESIGN.md §7).
3. A build check (detekt or lint rule, or a JVM test that scans `src/main`) that fails on
   `Color(0x`, hex colour strings, or raw `.dp`/`.sp` literals outside `ui/theme/`.
4. Compose accessibility checks: touch targets, contentDescription, contrast.
5. A Gradle task `uiCheck` that runs 1–4, and a git pre-commit hook (works on Windows) that runs
   it when files under `ui/` change.

## Constraints
No secrets in code or git. No new network permissions. Don't touch data, sync or AI-router logic
except to expose a field a screen needs to display.

## Build commands
- ALWAYS run Gradle from the repo root using `android/build.ps1` with `< /dev/null`:
  `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug < /dev/null`
  (Never inline `JAVA_HOME` or stopwatch code, never omit `< /dev/null`.)
- After each small change: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 :app:compileDebugKotlin < /dev/null`
- After finishing a screen: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug installDebug < /dev/null`
- Only at the END of a phase: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 testDebugUnitTest lintDebug < /dev/null`
- `assembleRelease`: only when the user asks for a release build. Never during UI work.
- Never use `--no-daemon` or `--rerun-tasks`.


