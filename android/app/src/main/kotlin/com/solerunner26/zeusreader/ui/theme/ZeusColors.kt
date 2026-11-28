package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Stormlight v4 Skies (DESIGN.md §3).
 */
enum class ZeusSky(
    val label: String,
    val baseA: Color,
    val baseB: Color,
    val glow1: Color,
    val glow2: Color,
    val accent: Color
) {
    STORM(
        label = "Storm",
        baseA = Color(0xFF120B2E),
        baseB = Color(0xFF3A1C8C),
        glow1 = Color(0xFF7C5CFF),
        glow2 = Color(0xFFFF4FD8),
        accent = Color(0xFFFFE14D)
    ),
    AURORA(
        label = "Aurora",
        baseA = Color(0xFF04161A),
        baseB = Color(0xFF0C4A4A),
        glow1 = Color(0xFF3DF5D0),
        glow2 = Color(0xFFA6FF4D),
        accent = Color(0xFFA6FF4D)
    ),
    SUNSET(
        label = "Sunset",
        baseA = Color(0xFF1E0A16),
        baseB = Color(0xFF5B1240),
        glow1 = Color(0xFFFF4FD8),
        glow2 = Color(0xFFFF9A3D),
        accent = Color(0xFFFFB23D)
    ),
    MONSOON(
        label = "Monsoon",
        baseA = Color(0xFF06101F),
        baseB = Color(0xFF10306B),
        glow1 = Color(0xFF3D8BFF),
        glow2 = Color(0xFF3DF5F5),
        accent = Color(0xFF6FE7FF)
    ),
    EMBER(
        label = "Ember",
        baseA = Color(0xFF180806),
        baseB = Color(0xFF5A1A0C),
        glow1 = Color(0xFFFF5A36),
        glow2 = Color(0xFFFFC24D),
        accent = Color(0xFFFFC24D)
    );

    companion object {
        fun fromHue(hue: Float): ZeusSky {
            val h = normalizeHue(hue)
            return when {
                h < 30f || h >= 335f -> EMBER
                h < 80f -> SUNSET
                h < 175f -> AURORA
                h < 225f -> MONSOON
                else -> STORM
            }
        }
    }
}

/**
 * Stormlight v4 Papers (DESIGN.md §3).
 */
enum class ZeusPaper(
    val label: String,
    val bg: Color,
    val ink: Color,
    val soft: Color,
    val isDark: Boolean,
    val isEink: Boolean = false
) {
    CLOUD(
        label = "Cloud",
        bg = Color(0xFFF6F4FF),
        ink = Color(0xFF17142B),
        soft = Color(0xFF5B5675),
        isDark = false
    ),
    NIGHT(
        label = "Night",
        bg = Color(0xFF141125),
        ink = Color(0xFFECE8FA),
        soft = Color(0xFFA9A3C4),
        isDark = true
    ),
    OLED(
        label = "OLED",
        bg = Color(0xFF000000),
        ink = Color(0xFFD6D2E6),
        soft = Color(0xFF8E89A6),
        isDark = true
    ),
    EINK(
        label = "E-ink",
        bg = Color(0xFFFFFFFF),
        ink = Color(0xFF000000),
        soft = Color(0xFF333333),
        isDark = false,
        isEink = true
    );

    fun highlightFor(sky: ZeusSky): Color = when (this) {
        EINK -> Color(0xFFE5E5E5)
        CLOUD -> sky.glow1.copy(alpha = 0.18f)
        NIGHT, OLED -> sky.glow1.copy(alpha = 0.33f)
    }

    companion object {
        fun fromReaderSurface(surface: ReaderSurface): ZeusPaper = when (surface) {
            ReaderSurface.PAPER, ReaderSurface.DUSK -> CLOUD
            ReaderSurface.NIGHT -> NIGHT
            ReaderSurface.OLED -> OLED
            ReaderSurface.EINK -> EINK
        }
    }
}

@Immutable
data class ZeusSemanticColors(
    val sky: ZeusSky,
    val paper: ZeusPaper,
    val textOnSky: Color = Color(0xFFF4F1FF),
    val textOnSkySecondary: Color = Color(0xFFF4F1FF).copy(alpha = 0.80f),
    val textOnAccent: Color = sky.baseA,
    val glassFill: Color = Color.White.copy(alpha = 0.10f),
    val glassFillSubtle: Color = Color.White.copy(alpha = 0.08f),
    val glassBorder: Color = Color.White.copy(alpha = 0.20f),
    val glassFallback: Color = sky.baseA.copy(alpha = 0.85f),
    val dockFill: Color = sky.baseA.copy(alpha = 0.62f),
    val sheetScrim: Color = sky.baseA.copy(alpha = 0.92f),
    val stickerLightBg: Color = Color(0xFFF4F1FF),
    val stickerCreamBg: Color = Color(0xFFFFF4D6),
    val stickerInk: Color = Color(0xFF120B2E),
    val errorGlow: Color = Color(0xFFFF6B8B)
)

fun stormlightColorScheme(sky: ZeusSky, paper: ZeusPaper): ColorScheme {
    val textOnSky = Color(0xFFF4F1FF)
    val textOnSkySoft = Color(0xFFF4F1FF).copy(alpha = 0.80f)
    val bgBase = if (paper == ZeusPaper.OLED) Color.Black else sky.baseA
    val cardBase = if (paper == ZeusPaper.OLED) Color(0xFF0E0B1A) else sky.baseB.copy(alpha = 0.48f)
    return darkColorScheme(
        primary = sky.accent,
        onPrimary = sky.baseA,
        primaryContainer = sky.glow1.copy(alpha = 0.28f),
        onPrimaryContainer = textOnSky,
        secondary = sky.glow1,
        onSecondary = sky.baseA,
        secondaryContainer = sky.glow2.copy(alpha = 0.24f),
        onSecondaryContainer = textOnSky,
        tertiary = sky.glow2,
        onTertiary = sky.baseA,
        background = bgBase,
        onBackground = textOnSky,
        surface = bgBase,
        onSurface = textOnSky,
        surfaceVariant = cardBase,
        onSurfaceVariant = textOnSkySoft,
        surfaceContainerLowest = bgBase,
        surfaceContainerLow = sky.baseA.copy(alpha = 0.92f),
        surfaceContainer = sky.baseA,
        surfaceContainerHigh = sky.baseB.copy(alpha = 0.65f),
        surfaceContainerHighest = sky.baseB,
        outline = Color.White.copy(alpha = 0.22f),
        outlineVariant = Color.White.copy(alpha = 0.14f),
        error = Color(0xFFFF6B8B),
        onError = Color(0xFF120B2E)
    )
}
