package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Stormlight v4 Skies (DESIGN.md §3) — each sky is also one of the five Theme Packs.
 *
 * Every sky has two moods:
 *  - **Night** (default): deep two-tone gradient [baseA] → [baseB] with neon glows.
 *  - **Daylight**: pale two-tone gradient [lightA] → [lightB]; ink is the sky's own [baseA]
 *    and accents use [accentInk] so small text keeps ≥ 4.5:1 (body ≥ 7:1).
 */
enum class ZeusSky(
    val label: String,
    val packName: String,
    val tagline: String,
    val hue: Float,
    val baseA: Color,
    val baseB: Color,
    val glow1: Color,
    val glow2: Color,
    val accent: Color,
    val lightA: Color,
    val lightB: Color,
    val accentInk: Color
) {
    STORM(
        label = "Storm",
        packName = "Thunder Violet",
        tagline = "Violet storm, lightning gold",
        hue = 250f,
        baseA = Color(0xFF120B2E),
        baseB = Color(0xFF3A1C8C),
        glow1 = Color(0xFF7C5CFF),
        glow2 = Color(0xFFFF4FD8),
        accent = Color(0xFFFFE14D),
        lightA = Color(0xFFF5F2FF),
        lightB = Color(0xFFE2D9FF),
        accentInk = Color(0xFF4B26C9)
    ),
    AURORA(
        label = "Aurora",
        packName = "Northern Mint",
        tagline = "Polar teal, electric lime",
        hue = 150f,
        baseA = Color(0xFF04161A),
        baseB = Color(0xFF0C4A4A),
        glow1 = Color(0xFF3DF5D0),
        glow2 = Color(0xFFA6FF4D),
        accent = Color(0xFFA6FF4D),
        lightA = Color(0xFFEEFCF8),
        lightB = Color(0xFFC9F2E6),
        accentInk = Color(0xFF075C4B)
    ),
    SUNSET(
        label = "Sunset",
        packName = "Rose Dusk",
        tagline = "Plum dusk, neon tangerine",
        hue = 50f,
        baseA = Color(0xFF1E0A16),
        baseB = Color(0xFF5B1240),
        glow1 = Color(0xFFFF4FD8),
        glow2 = Color(0xFFFF9A3D),
        accent = Color(0xFFFFB23D),
        lightA = Color(0xFFFFF3F8),
        lightB = Color(0xFFFFD6E8),
        accentInk = Color(0xFF93134F)
    ),
    MONSOON(
        label = "Monsoon",
        packName = "Ocean Rain",
        tagline = "Midnight sea, cyan rain",
        hue = 200f,
        baseA = Color(0xFF06101F),
        baseB = Color(0xFF10306B),
        glow1 = Color(0xFF3D8BFF),
        glow2 = Color(0xFF3DF5F5),
        accent = Color(0xFF6FE7FF),
        lightA = Color(0xFFEFF5FF),
        lightB = Color(0xFFD2E3FF),
        accentInk = Color(0xFF16449E)
    ),
    EMBER(
        label = "Ember",
        packName = "Lava Gold",
        tagline = "Volcanic red, molten gold",
        hue = 10f,
        baseA = Color(0xFF180806),
        baseB = Color(0xFF5A1A0C),
        glow1 = Color(0xFFFF5A36),
        glow2 = Color(0xFFFFC24D),
        accent = Color(0xFFFFC24D),
        lightA = Color(0xFFFFF5EF),
        lightB = Color(0xFFFFDCC9),
        accentInk = Color(0xFF8E2F10)
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
        EINK -> ZeusPalette.einkHighlight
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

/** Raw constants that are not sky-specific. Screens never use these directly. */
object ZeusPalette {
    val starlight = Color(0xFFF4F1FF)
    val stickerCream = Color(0xFFFFF4D6)
    val stickerInk = Color(0xFF120B2E)
    val errorGlow = Color(0xFFFF6B8B)
    val errorInk = Color(0xFFB3123E)
    val successGlow = Color(0xFF4DFFB0)
    val successInk = Color(0xFF0B6B45)
    val einkHighlight = Color(0xFFE5E5E5)
    val oledCard = Color(0xFF0E0B1A)
    val warmLight = Color(0xFFFFA726)
    val searchHit = Color(0xFFFFD54F)
    val cardMidGrey = Color(0xFF5A5A5A)
    val shadow = Color(0xFF000000)
}

/**
 * Semantic colours every screen uses. Built by [zeusSemanticColors] for (sky, paper, daylight).
 */
@Immutable
data class ZeusSemanticColors(
    val sky: ZeusSky,
    val paper: ZeusPaper,
    val isLight: Boolean = false,
    val skyTop: Color = sky.baseA,
    val skyBottom: Color = sky.baseB,
    val textOnSky: Color = ZeusPalette.starlight,
    val textOnSkySecondary: Color = ZeusPalette.starlight.copy(alpha = 0.80f),
    val textOnSkyMuted: Color = ZeusPalette.starlight.copy(alpha = 0.66f),
    val accentOnSky: Color = sky.accent,
    val textOnAccent: Color = sky.baseA,
    val glassFill: Color = Color.White.copy(alpha = 0.10f),
    val glassFillSubtle: Color = Color.White.copy(alpha = 0.06f),
    val glassFillStrong: Color = Color.White.copy(alpha = 0.16f),
    val glassBorder: Color = Color.White.copy(alpha = 0.20f),
    val glassHighlight: Color = Color.White.copy(alpha = 0.28f),
    val glassFallback: Color = sky.baseA.copy(alpha = 0.85f),
    val neonA: Color = sky.glow1,
    val neonB: Color = sky.glow2,
    val dockFill: Color = sky.baseA.copy(alpha = 0.72f),
    val sheetBackground: Color = sky.baseA,
    val sheetScrim: Color = sky.baseA.copy(alpha = 0.92f),
    val scrim: Color = ZeusPalette.shadow.copy(alpha = 0.55f),
    val chipSelectedBg: Color = ZeusPalette.starlight,
    val chipSelectedFg: Color = sky.baseA,
    val trackInactive: Color = Color.White.copy(alpha = 0.16f),
    val divider: Color = Color.White.copy(alpha = 0.12f),
    val stickerLightBg: Color = ZeusPalette.starlight,
    val stickerCreamBg: Color = ZeusPalette.stickerCream,
    val stickerInk: Color = ZeusPalette.stickerInk,
    val errorGlow: Color = ZeusPalette.errorGlow,
    val success: Color = ZeusPalette.successGlow,
    val shadow: Color = ZeusPalette.shadow
)

fun zeusSemanticColors(sky: ZeusSky, paper: ZeusPaper, isLight: Boolean): ZeusSemanticColors {
    if (paper.isEink) {
        val ink = ZeusPaper.EINK.ink
        return ZeusSemanticColors(
            sky = sky,
            paper = paper,
            isLight = true,
            skyTop = ZeusPaper.EINK.bg,
            skyBottom = ZeusPaper.EINK.bg,
            textOnSky = ink,
            textOnSkySecondary = ZeusPaper.EINK.soft,
            textOnSkyMuted = ZeusPaper.EINK.soft,
            accentOnSky = ink,
            textOnAccent = ZeusPaper.EINK.bg,
            glassFill = ZeusPaper.EINK.bg,
            glassFillSubtle = ZeusPaper.EINK.bg,
            glassFillStrong = ZeusPalette.einkHighlight,
            glassBorder = ink,
            glassHighlight = ink,
            glassFallback = ZeusPaper.EINK.bg,
            neonA = ink,
            neonB = ink,
            dockFill = ZeusPaper.EINK.bg,
            sheetBackground = ZeusPaper.EINK.bg,
            sheetScrim = ZeusPaper.EINK.bg,
            scrim = ink.copy(alpha = 0.30f),
            chipSelectedBg = ink,
            chipSelectedFg = ZeusPaper.EINK.bg,
            trackInactive = ZeusPalette.einkHighlight,
            divider = ink.copy(alpha = 0.30f),
            stickerLightBg = ZeusPaper.EINK.bg,
            stickerCreamBg = ZeusPaper.EINK.bg,
            stickerInk = ink,
            errorGlow = ink,
            success = ink
        )
    }
    if (isLight) {
        val ink = sky.baseA
        return ZeusSemanticColors(
            sky = sky,
            paper = paper,
            isLight = true,
            skyTop = sky.lightA,
            skyBottom = sky.lightB,
            textOnSky = ink,
            textOnSkySecondary = ink.copy(alpha = 0.78f),
            textOnSkyMuted = ink.copy(alpha = 0.64f),
            accentOnSky = sky.accentInk,
            textOnAccent = sky.baseA,
            glassFill = Color.White.copy(alpha = 0.62f),
            glassFillSubtle = Color.White.copy(alpha = 0.42f),
            glassFillStrong = Color.White.copy(alpha = 0.82f),
            glassBorder = ink.copy(alpha = 0.10f),
            glassHighlight = Color.White.copy(alpha = 0.95f),
            glassFallback = sky.lightA.copy(alpha = 0.92f),
            neonA = sky.glow1,
            neonB = sky.glow2,
            dockFill = Color.White.copy(alpha = 0.86f),
            sheetBackground = sky.lightA,
            sheetScrim = sky.lightA.copy(alpha = 0.95f),
            scrim = ink.copy(alpha = 0.40f),
            chipSelectedBg = ink,
            chipSelectedFg = sky.lightA,
            trackInactive = ink.copy(alpha = 0.12f),
            divider = ink.copy(alpha = 0.10f),
            stickerLightBg = Color.White,
            stickerCreamBg = ZeusPalette.stickerCream,
            stickerInk = ZeusPalette.stickerInk,
            errorGlow = ZeusPalette.errorInk,
            success = ZeusPalette.successInk
        )
    }
    val oled = paper == ZeusPaper.OLED
    return ZeusSemanticColors(
        sky = sky,
        paper = paper,
        isLight = false,
        skyTop = if (oled) ZeusPalette.shadow else sky.baseA,
        skyBottom = if (oled) sky.baseA else sky.baseB,
        dockFill = if (oled) ZeusPalette.oledCard.copy(alpha = 0.88f) else sky.baseA.copy(alpha = 0.72f),
        sheetBackground = if (oled) ZeusPalette.oledCard else sky.baseA
    )
}

fun stormlightColorScheme(sky: ZeusSky, paper: ZeusPaper, isLight: Boolean = false): ColorScheme {
    val c = zeusSemanticColors(sky, paper, isLight)
    return if (c.isLight) {
        lightColorScheme(
            primary = c.accentOnSky,
            onPrimary = c.skyTop,
            primaryContainer = c.neonA.copy(alpha = 0.20f),
            onPrimaryContainer = c.textOnSky,
            secondary = c.accentOnSky,
            onSecondary = c.skyTop,
            secondaryContainer = c.neonB.copy(alpha = 0.20f),
            onSecondaryContainer = c.textOnSky,
            tertiary = c.accentOnSky,
            onTertiary = c.skyTop,
            background = c.skyTop,
            onBackground = c.textOnSky,
            surface = c.sheetBackground,
            onSurface = c.textOnSky,
            surfaceVariant = c.glassFillStrong,
            onSurfaceVariant = c.textOnSkySecondary,
            surfaceContainerLowest = c.skyTop,
            surfaceContainerLow = c.sheetBackground,
            surfaceContainer = c.sheetBackground,
            surfaceContainerHigh = c.skyBottom,
            surfaceContainerHighest = c.skyBottom,
            outline = c.textOnSky.copy(alpha = 0.30f),
            outlineVariant = c.divider,
            error = c.errorGlow,
            onError = c.skyTop
        )
    } else {
        darkColorScheme(
            primary = c.accentOnSky,
            onPrimary = sky.baseA,
            primaryContainer = sky.glow1.copy(alpha = 0.28f),
            onPrimaryContainer = c.textOnSky,
            secondary = sky.glow1,
            onSecondary = sky.baseA,
            secondaryContainer = sky.glow2.copy(alpha = 0.24f),
            onSecondaryContainer = c.textOnSky,
            tertiary = sky.glow2,
            onTertiary = sky.baseA,
            background = c.skyTop,
            onBackground = c.textOnSky,
            surface = c.sheetBackground,
            onSurface = c.textOnSky,
            surfaceVariant = if (paper == ZeusPaper.OLED) ZeusPalette.oledCard else sky.baseB.copy(alpha = 0.48f),
            onSurfaceVariant = c.textOnSkySecondary,
            surfaceContainerLowest = c.skyTop,
            surfaceContainerLow = c.sheetBackground,
            surfaceContainer = c.sheetBackground,
            surfaceContainerHigh = sky.baseB,
            surfaceContainerHighest = sky.baseB,
            outline = Color.White.copy(alpha = 0.24f),
            outlineVariant = Color.White.copy(alpha = 0.14f),
            error = c.errorGlow,
            onError = sky.baseA
        )
    }
}
