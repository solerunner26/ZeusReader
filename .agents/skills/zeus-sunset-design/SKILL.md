---
name: zeus-sunset-design
description: Use for ANY UI, theme, layout, colour, typography, animation, icon or screen work in ZeusReader. This is the owner-approved "Sunset" design system and it replaces Stormlight v4.
---

# ZeusReader Sunset design system

1. Read `design/sunset/DESIGN.md` and open `design/sunset/zeusreader-sunset-mockup.html` (the approved visual reference).
2. Use only tokens from `android/app/src/main/kotlin/com/solerunner26/zeusreader/ui/theme/` (`ZeusTheme.colors`,
   `ZeusType`, `ZeusSpacing`, `ZeusSizes`, `ZeusShapes`, `ZeusMotion`) and components from `ui/components/`.
3. Five theme packs (Sunset default, Emerald, Ocean, Iris, Rose), each Day + Night; E-ink monochrome.
4. Before finishing: `./gradlew uiCheck` from `android/` (token guard, contrast, snapshot matrix, unit tests).
5. Never touch the SLUM connector / relay / sync / data / reader engines for UI work.
