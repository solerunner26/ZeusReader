package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Six curated Ink & Hue presets from DESIGN.md v2 §2.
 */
enum class HuePreset(val hue: Float, val label: String) {
    INDIGO_DUSK(250f, "Indigo Dusk"),
    SAFFRON(32f, "Saffron"),
    MOSS(140f, "Moss"),
    LAGOON(186f, "Lagoon"),
    ROSE(345f, "Rose"),
    PLUM(290f, "Plum");

    companion object {
        const val DEFAULT_HUE = 250f
    }
}

/**
 * Semantic two-tone palette derived from a single hue angle [h] in [0, 360).
 * No third colour is introduced anywhere in app chrome.
 */
@Immutable
data class ZeusHuePalette(
    val hueAngle: Float,
    val dark: Boolean,
    val surface: Color,
    val paper: Color,
    val ink: Color,
    val inkSoft: Color,
    val hue: Color,
    val onHue: Color,
    val hueContainer: Color,
    val onHueContainer: Color,
    val outline: Color,
    val highlight: Color
)

val LocalZeusPalette = staticCompositionLocalOf {
    buildZeusHuePalette(HuePreset.DEFAULT_HUE, dark = false)
}

/**
 * Pure function building the Material 3 [ColorScheme] from a single hue angle [hue] and [dark] flag,
 * following DESIGN.md v2 §2 fixed HSL lightness steps.
 */
@Suppress("FunctionName")
fun HueTheme(hue: Float, dark: Boolean): ColorScheme {
    val palette = buildZeusHuePalette(hue, dark)
    return if (dark) {
        darkColorScheme(
            primary = palette.hue,
            onPrimary = palette.onHue,
            primaryContainer = palette.hueContainer,
            onPrimaryContainer = palette.onHueContainer,
            secondary = palette.hue,
            onSecondary = palette.onHue,
            secondaryContainer = palette.hueContainer,
            onSecondaryContainer = palette.onHueContainer,
            tertiary = palette.ink,
            onTertiary = palette.surface,
            tertiaryContainer = palette.paper,
            onTertiaryContainer = palette.ink,
            background = palette.surface,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.paper,
            onSurfaceVariant = palette.inkSoft,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = palette.paper,
            surfaceContainer = palette.paper,
            surfaceContainerHigh = palette.hueContainer,
            surfaceContainerHighest = palette.hueContainer,
            outline = palette.outline,
            outlineVariant = palette.outline,
            error = palette.hue,
            onError = palette.onHue,
            errorContainer = palette.hueContainer,
            onErrorContainer = palette.onHueContainer
        )
    } else {
        lightColorScheme(
            primary = palette.hue,
            onPrimary = palette.onHue,
            primaryContainer = palette.hueContainer,
            onPrimaryContainer = palette.onHueContainer,
            secondary = palette.hue,
            onSecondary = palette.onHue,
            secondaryContainer = palette.hueContainer,
            onSecondaryContainer = palette.onHueContainer,
            tertiary = palette.ink,
            onTertiary = palette.surface,
            tertiaryContainer = palette.paper,
            onTertiaryContainer = palette.ink,
            background = palette.surface,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.paper,
            onSurfaceVariant = palette.inkSoft,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = palette.paper,
            surfaceContainer = palette.paper,
            surfaceContainerHigh = palette.hueContainer,
            surfaceContainerHighest = palette.hueContainer,
            outline = palette.outline,
            outlineVariant = palette.outline,
            error = palette.hue,
            onError = palette.onHue,
            errorContainer = palette.hueContainer,
            onErrorContainer = palette.onHueContainer
        )
    }
}

fun buildZeusHuePalette(hueAngle: Float, dark: Boolean): ZeusHuePalette {
    val h = normalizeHue(hueAngle)
    return if (dark) {
        val ink = hslColor(h, 0.18f, 0.90f)
        ZeusHuePalette(
            hueAngle = h,
            dark = true,
            surface = hslColor(h, 0.24f, 0.08f),
            paper = hslColor(h, 0.20f, 0.11f),
            ink = ink,
            inkSoft = hslColor(h, 0.12f, 0.68f),
            hue = hslColor(h, 0.62f, 0.72f),
            onHue = hslColor(h, 0.35f, 0.10f),
            hueContainer = hslColor(h, 0.30f, 0.20f),
            onHueContainer = ink,
            outline = hslColor(h, 0.14f, 0.20f),
            highlight = hslColor(h, 0.35f, 0.26f)
        )
    } else {
        val ink = hslColor(h, 0.32f, 0.13f)
        // Clamp light-mode hue lightness slightly only when needed so white text stays >= 4.5:1 across 0-345 deg
        val lightHueBase = hslColor(h, 0.52f, 0.38f)
        val lightHue = ensureMinContrastOnWhite(h, 0.52f, 0.38f, minContrast = 4.5)
        ZeusHuePalette(
            hueAngle = h,
            dark = false,
            surface = hslColor(h, 0.28f, 0.95f),
            paper = hslColor(h, 0.30f, 0.98f),
            ink = ink,
            inkSoft = hslColor(h, 0.14f, 0.34f),
            hue = lightHue.takeIf { contrastRatio(Color.White, lightHueBase) < 4.5 } ?: lightHueBase,
            onHue = Color.White,
            hueContainer = hslColor(h, 0.45f, 0.91f),
            onHueContainer = ink,
            outline = hslColor(h, 0.20f, 0.85f),
            highlight = hslColor(h, 0.60f, 0.88f)
        )
    }
}

private fun ensureMinContrastOnWhite(
    h: Float,
    saturation: Float,
    initialLightness: Float,
    minContrast: Double
): Color {
    var l = initialLightness
    var candidate = hslColor(h, saturation, l)
    while (contrastRatio(Color.White, candidate) < minContrast && l > 0.15f) {
        l -= 0.01f
        candidate = hslColor(h, saturation, l)
    }
    return candidate
}

fun normalizeHue(hue: Float): Float {
    val mod = hue % 360f
    return if (mod < 0f) mod + 360f else mod
}

fun hslColor(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color {
    val h = normalizeHue(hue)
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

/**
 * WCAG 2.1 relative luminance and contrast ratio utilities.
 */
fun Color.relativeLuminance(): Double {
    fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * linearize(red) + 0.7152 * linearize(green) + 0.0722 * linearize(blue)
}

fun contrastRatio(foreground: Color, background: Color): Double {
    val l1 = foreground.relativeLuminance()
    val l2 = background.relativeLuminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}
