package com.solerunner26.zeusreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode

/**
 * Five reading surfaces from DESIGN.md v2 §3.
 * Replaces v1 Day / Sepia / Night / OLED / E-ink colour palettes.
 */
enum class ReaderSurface(val label: String) {
    PAPER("Paper"),
    DUSK("Dusk"),
    NIGHT("Night"),
    OLED("OLED"),
    EINK("E-ink");

    companion object {
        fun fromLegacyThemeMode(mode: ReaderThemeMode): ReaderSurface = when (mode) {
            ReaderThemeMode.DAY -> PAPER
            ReaderThemeMode.SEPIA -> DUSK
            ReaderThemeMode.NIGHT -> NIGHT
            ReaderThemeMode.PURE_BLACK_OLED -> OLED
            ReaderThemeMode.E_INK -> EINK
        }
    }
}

@Immutable
data class ReaderSurfacePalette(
    val surface: ReaderSurface,
    val background: Color,
    val text: Color,
    val highlight: Color,
    val chromeSurface: Color,
    val isDark: Boolean,
    val underlineOnlyHighlight: Boolean,
    val allowHue: Boolean,
    val allowBlur: Boolean,
    val allowMotion: Boolean,
    val allowVignette: Boolean
)

fun buildReaderSurfacePalette(surface: ReaderSurface, hue: Float): ReaderSurfacePalette {
    val h = normalizeHue(hue)
    return when (surface) {
        ReaderSurface.PAPER -> ReaderSurfacePalette(
            surface = surface,
            background = hslColor(h, 0.30f, 0.98f),
            text = hslColor(h, 0.32f, 0.13f),
            highlight = hslColor(h, 0.60f, 0.90f),
            chromeSurface = hslColor(h, 0.28f, 0.95f),
            isDark = false,
            underlineOnlyHighlight = false,
            allowHue = true,
            allowBlur = true,
            allowMotion = true,
            allowVignette = true
        )

        ReaderSurface.DUSK -> ReaderSurfacePalette(
            surface = surface,
            background = hslColor(h, 0.18f, 0.88f),
            text = hslColor(h, 0.30f, 0.14f),
            highlight = hslColor(h, 0.50f, 0.79f),
            chromeSurface = hslColor(h, 0.18f, 0.84f),
            isDark = false,
            underlineOnlyHighlight = false,
            allowHue = true,
            allowBlur = true,
            allowMotion = true,
            allowVignette = true
        )

        ReaderSurface.NIGHT -> ReaderSurfacePalette(
            surface = surface,
            background = hslColor(h, 0.18f, 0.12f),
            text = hslColor(h, 0.14f, 0.84f),
            highlight = hslColor(h, 0.35f, 0.26f),
            chromeSurface = hslColor(h, 0.20f, 0.15f),
            isDark = true,
            underlineOnlyHighlight = false,
            allowHue = true,
            allowBlur = true,
            allowMotion = true,
            allowVignette = true
        )

        ReaderSurface.OLED -> ReaderSurfacePalette(
            surface = surface,
            background = Color.Black,
            text = hslColor(h, 0.10f, 0.78f),
            highlight = hslColor(h, 0.35f, 0.20f),
            chromeSurface = Color(0xFF0A0A0A),
            isDark = true,
            underlineOnlyHighlight = false,
            allowHue = true,
            allowBlur = true,
            allowMotion = true,
            allowVignette = false
        )

        ReaderSurface.EINK -> ReaderSurfacePalette(
            surface = surface,
            background = Color.White,
            text = Color.Black,
            highlight = Color.Black,
            chromeSurface = Color.White,
            isDark = false,
            underlineOnlyHighlight = true,
            allowHue = false,
            allowBlur = false,
            allowMotion = false,
            allowVignette = false
        )
    }
}
