# ZeusReader "Sunset" design system (approved by the owner, Nov 2026)

**This replaces Stormlight v4.** The approved interactive reference is `design/sunset/zeusreader-sunset-mockup.html`
(also published privately for the owner at https://claude.ai/artifact/WdzLYHhvxEh9dVbSYNdZND). Match it.

## Idea
Soft geometric modernism with tactile depth: calm neutral canvas, white cards with soft shadows, ONE accent
gradient pair per theme pack, and joyful reward artifacts (Bolt the owl mascot, confetti, crown, trophy, flame,
starburst halos) used around progress and celebrations — never on the reading page.

## Colour
Neutrals (shared by every pack)
| Token | Day | Night |
|---|---|---|
| canvas | Warm Alabaster `#FAF9F6` | Obsidian `#0D0F12` (OLED: `#000000`) |
| card / sheet / input | Pure White `#FFFFFF` | Deep Charcoal `#1A1D24` |
| ink | Deep Charcoal `#1A1D24` | `#F3F4F6` |
| muted | `#646873` | `#A3A7B1` |
| line | `#EEEBE5` | `#262A33` |
| shadow | `0 8 24 rgba(0,0,0,.05)` | `0 8 24 rgba(0,0,0,.35)` |

Theme packs (accent g1 → g2 at 135°; tint = soft icon/chip fill; onAccent = text on the gradient; accentInk = accent text on canvas)
| Pack | g1 | g2 | tint (day) | tint (night) | onAccent | accentInk day / night |
|---|---|---|---|---|---|---|
| **Sunset** (default) | `#FF7A00` | `#FFB800` | `#FFF0E5` | g1 @ 12% | `#1A1D24` | `#A84C00` / `#FF9A3D` |
| Emerald | `#10B981` | `#A3E635` | `#E3F8EE` | g1 @ 12% | `#0B2A1D` | `#047857` / `#34D399` |
| Ocean | `#0EA5E9` | `#67E8F9` | `#E3F4FD` | g1 @ 12% | `#062A3A` | `#0369A1` / `#38BDF8` |
| Iris | `#6D28D9` | `#9333EA` | `#F1EAFE` | g2 @ 14% | `#FFFFFF` | `#6D28D9` / `#C4A1FF` |
| Rose | `#F43F5E` | `#FDBA74` | `#FFE9EC` | g1 @ 13% | `#2A0A12` | `#BE123C` / `#FB7185` |
Semantic: ok `#14A35A`/`#3DD68C`, info `#3B6FE0`/`#7DA2FF`, rose `#E0436A`/`#FF7A99` (each with a 12% tint).
E-ink: pure black on white, no gradients, no shadows, no motion.

## Type
Plus Jakarta Sans (UI, 400–800) · Literata (reading EN) · Noto Sans/Serif Devanagari & Gujarati (HI/GU).
Scale: display 30/800 · title 24/800 · headline 17/800 · body 14–15 · label 13/700 · caption 12 · eyebrow 10.5/800 caps +0.09em.

## Shape & spacing
Cards 20dp radius · inputs 14dp · sheets 30dp top · pills/CTAs/chips fully rounded · icon boxes 14dp · gutter 18dp · 4dp grid.
Touch targets ≥ 48dp, primary CTA 54dp.

## Motion
Spring press scale 0.96 · rings/bars fill on load · staggered list rise · carousel with morphing pill dots ·
slow gradient shine on primary buttons · confetti only on results. Reduce Motion / battery saver / E-ink → none.

## Navigation & screens (plain names)
Bottom bar, 4 tabs: **Home · Library · Words · Me** (active = gradient pill behind a filled icon).
Screens: Splash/Onboarding (first run) · Home dashboard (greeting + bell, gradient hero with Bolt, daily goal bar,
streak & words metrics, shelf) · Streak · Activity · Library (+ filter sheet) · Reader (top progress + fraction,
paper card, bottom toolbar: Contents · Translate · Dictionary · Listen · Appearance) · Contents as a chapter roadmap ·
Highlights · Highlight detail · Card studio · Words (Quiz A–D + flip review) · Quiz result celebration · Stats + badges ·
Me (profile hub) · Settings (Day/Night switch, theme packs) · Help (FAQ accordion) · Offline packs · Online books.

## Rules
No raw `Color(0x…)` / `.dp` / `.sp` outside `ui/theme/` (enforced by `ZeusTokenGuardTest`). Text ≥ 7:1 on canvas
and cards, accent text ≥ 4.5:1. Hindi/Gujarati never clip at font scale 2.0. Never decorate the reading page.
