---
name: stormlight-design
description: Use for ANY UI, theme, layout, colour, typography, animation, icon or screen work in ZeusReader. This is the app's design system (Stormlight v4) and it OVERRIDES every generic design skill (ui-ux-pro-max, mobile-uiux-promax, Material defaults).
---

# Stormlight design system — ZeusReader

**Read `references/DESIGN.md` in full before touching UI code.** The Android/Gradle project lives in `android/` (relative to the repo root). The pixel-level source of truth is
`design/screens/*.dc.html` in the repo root (open the markup to read exact values).

## Precedence
1. This skill + `design/DESIGN.md` + `design/screens/` (look, feel, names, motion)
2. Google Android / Compose skills (code correctness, performance, accessibility APIs)
3. Generic UI/UX skills (UX laws, checklists) — only where they do not conflict with 1

If a generic skill suggests stock Material colours, flat rectangular buttons, default Roboto, or a
different layout, **ignore it and follow Stormlight.**

## Non-negotiable rules
- All colours, type, shapes, spacing and motion come from `ui/theme/` (`ZeusTheme`, `ZeusColors`,
  `ZeusType`, `ZeusShapes`, `ZeusSpacing`, `ZeusMotion`). No `Color(0x…)`, hex strings or raw
  `.dp`/`.sp` literals in screen code. Add a token instead.
- Skies: Storm (default), Aurora, Sunset, Monsoon, Ember. Papers: Cloud, Night, OLED, E-ink.
  Exact values are in references/DESIGN.md §3.
- Fonts: Unbounded (display/numbers), Bricolage Grotesque (UI), Literata (reading EN),
  Noto Serif Devanagari / Gujarati (reading HI/GU), Baloo 2 / Baloo Bhai 2 (Indic UI), JetBrains Mono (meta).
- Loud outside, calm inside: glow, aurora and stickers live around the book; the reading page is
  always quiet paper (30dp rounded sheet, 8dp inset). Never animate reading text.
- Primary actions are gradient pills (accent → glow2), text in the sky's dark base colour, never white on yellow/lime.
- Touch targets ≥ 48dp (Orb 68dp, primary buttons 56–62dp). Floating glass dock: Home, Olympus, Sparks, Words.
- Contrast: reading text ≥ 7:1; text on skies ≥ 7:1; nothing below 4.5:1.
- Reduce Motion / battery saver / E-ink / "Glow & motion" off → no loops, no flashes, instant transitions.
- Glass blur only on API 31+; fallback = sky base at 85% opacity.
- Hindi and Gujarati strings must never clip or overlap at fontScale 1.3 and 2.0
  (use maxLines + ellipsis, weight(1f), FlowRow).
- Keep the feature names from DESIGN.md §2 (Orb, Constellation, Fly, Zap card, Translate Lens,
  Listen, Chapter Strike, Ask Zeus, Sparks, Constellations, Spark card, Olympus, Words).
- Never remove or rename a feature while restyling. UI changes stay in the UI layer.

## Before you finish any UI task
From `android/` run `gradlew.bat uiCheck` (Windows) or `./gradlew uiCheck`. If it does not exist yet, run the
`/stormlight-fix` workflow, Phase 5. Do not report done while it fails.

## Build commands
- ALWAYS run Gradle from the repo root using `android/build.ps1` with `< /dev/null`:
  `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug < /dev/null`
  (Never inline `JAVA_HOME` or stopwatch code, never omit `< /dev/null`.)
- After each small change: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 :app:compileDebugKotlin < /dev/null`
- After finishing a screen: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 assembleDebug installDebug < /dev/null`
- Only at the END of a phase: `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -File android/build.ps1 testDebugUnitTest lintDebug < /dev/null`
- `assembleRelease`: only when the user asks for a release build. Never during UI work.
- Never use `--no-daemon` or `--rerun-tasks`.


