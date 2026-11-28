package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Sunset design system (design/sunset/DESIGN.md): five theme packs, each one accent gradient
 * pair ([g1] → [g2]) on shared neutrals. [hue] is the value stored in preferences for the pack.
 */
enum class ZeusPack(
    val label: String,
    val hue: Float,
    val g1: Color,
    val g2: Color,
    val tintDay: Color,
    val onAccent: Color,
    val accentInkDay: Color,
    val accentInkNight: Color,
    val highlightDay: Color
) {
    SUNSET("Sunset", 30f, Color(0xFFFF7A00), Color(0xFFFFB800), Color(0xFFFFF0E5), Color(0xFF1A1D24), Color(0xFFA84C00), Color(0xFFFF9A3D), Color(0xFFFFE2C2)),
    EMERALD("Emerald", 150f, Color(0xFF10B981), Color(0xFFA3E635), Color(0xFFE3F8EE), Color(0xFF0B2A1D), Color(0xFF047857), Color(0xFF34D399), Color(0xFFD6F5C4)),
    OCEAN("Ocean", 200f, Color(0xFF0EA5E9), Color(0xFF67E8F9), Color(0xFFE3F4FD), Color(0xFF062A3A), Color(0xFF0369A1), Color(0xFF38BDF8), Color(0xFFCFF1FB)),
    IRIS("Iris", 270f, Color(0xFF6D28D9), Color(0xFF9333EA), Color(0xFFF1EAFE), Color(0xFFFFFFFF), Color(0xFF6D28D9), Color(0xFFC4A1FF), Color(0xFFEADCFE)),
    ROSE("Rose", 350f, Color(0xFFF43F5E), Color(0xFFFDBA74), Color(0xFFFFE9EC), Color(0xFF2A0A12), Color(0xFFBE123C), Color(0xFFFB7185), Color(0xFFFFDDE3));

    companion object {
        val DEFAULT = SUNSET

        fun fromHue(hue: Float): ZeusPack {
            val h = normalizeHue(hue)
            return when {
                h < 15f || h >= 320f -> ROSE
                h < 90f -> SUNSET
                h < 175f -> EMERALD
                h < 235f -> OCEAN
                else -> IRIS
            }
        }
    }
}

/** Chrome mode: Day (alabaster), Night (obsidian), OLED (true black) or E-ink (monochrome). */
enum class ZeusMode(val label: String, val isDark: Boolean, val isEink: Boolean = false) {
    DAY("Day", false),
    NIGHT("Night", true),
    OLED("OLED", true),
    EINK("E-ink", false, isEink = true);

    companion object {
        fun fromReaderSurface(surface: ReaderSurface, darkTheme: Boolean): ZeusMode = when (surface) {
            ReaderSurface.OLED -> OLED
            ReaderSurface.EINK -> EINK
            else -> if (darkTheme) NIGHT else DAY
        }
    }
}

/** Raw constants shared by every pack. Screens use [ZeusColors] tokens instead. */
object ZeusPalette {
    val alabaster = Color(0xFFFAF9F6)
    val white = Color(0xFFFFFFFF)
    val obsidian = Color(0xFF0D0F12)
    val charcoal = Color(0xFF1A1D24)
    val black = Color(0xFF000000)
    val oledCard = Color(0xFF121418)
    val inkNight = Color(0xFFF3F4F6)
    val mutedDay = Color(0xFF5F636E)
    val mutedNight = Color(0xFFA3A7B1)
    val lineDay = Color(0xFFEEEBE5)
    val lineNight = Color(0xFF262A33)
    val okDay = Color(0xFF14833F)
    val okNight = Color(0xFF3DD68C)
    val infoDay = Color(0xFF2F5FCB)
    val infoNight = Color(0xFF7DA2FF)
    val roseDay = Color(0xFFC92F57)
    val roseNight = Color(0xFFFF7A99)
    val goldLight = Color(0xFFFFE58A)
    val gold = Color(0xFFFFB800)
    val goldDeep = Color(0xFFD97800)
    val goldEdge = Color(0xFFC26C00)
    val flameCore = Color(0xFFFFF3B0)
    val flameEdge = Color(0xFFFF5A1F)
    val ruby = Color(0xFFFF4D6D)
    val confettiBlue = Color(0xFF3B82F6)
    val confettiGreen = Color(0xFF22C55E)
    val mascotBelly = Color(0xFFFFE9D2)
    val mascotEye = Color(0xFFE9E4DC)
    val bookDark = Color(0xFF2F3542)
    val einkGrey = Color(0xFFE5E5E5)
    val einkHighlight = einkGrey
    val starlight = Color(0xFFFFFFFF)
    val einkSoft = Color(0xFF333333)
    val warmLight = Color(0xFFFFA726)
    val searchHit = Color(0xFFFFD54F)
    val cardMidGrey = Color(0xFF5A5A5A)
    val stickerCream = Color(0xFFFFF4D6)
    val stickerInk = Color(0xFF120B2E)
    val shadow = Color(0xFF000000)
    val coverMoss = Color(0xFF5E8C62)
    val coverMossDeep = Color(0xFF23402B)
    val coverBrick = Color(0xFFC2533F)
    val coverBrickDeep = Color(0xFF5B211A)
    val coverNavy = Color(0xFF3F6BA6)
    val coverNavyDeep = Color(0xFF1B2C48)
    val coverOchre = Color(0xFFC9952F)
    val coverOchreDeep = Color(0xFF5E3F10)
    val coverPlum = Color(0xFF7A5BA6)
    val coverPlumDeep = Color(0xFF2E2244)
    val coverTeal = Color(0xFF2F8F8A)
    val coverTealDeep = Color(0xFF123B39)
}

/** Semantic colour tokens used by every screen. Built by [zeusColors]. */
@Immutable
data class ZeusColors(
    val pack: ZeusPack,
    val mode: ZeusMode,
    val canvas: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accent2: Color,
    val accentInk: Color,
    val onAccent: Color,
    val tint: Color,
    val highlight: Color,
    val glow: Color,
    val shadow: Color,
    val scrim: Color,
    val ok: Color,
    val okTint: Color,
    val info: Color,
    val infoTint: Color,
    val rose: Color,
    val roseTint: Color,
    val gradientEnabled: Boolean
) {
    val isDark: Boolean get() = mode.isDark
    val isEink: Boolean get() = mode.isEink
}

fun zeusColors(pack: ZeusPack, mode: ZeusMode): ZeusColors = when (mode) {
    ZeusMode.EINK -> ZeusColors(
        pack = pack, mode = mode,
        canvas = ZeusPalette.white, card = ZeusPalette.white, ink = ZeusPalette.black, muted = ZeusPalette.einkSoft,
        line = ZeusPalette.black, accent = ZeusPalette.black, accent2 = ZeusPalette.black, accentInk = ZeusPalette.black,
        onAccent = ZeusPalette.white, tint = ZeusPalette.einkGrey, highlight = ZeusPalette.einkGrey,
        glow = Color.Transparent, shadow = Color.Transparent, scrim = ZeusPalette.black.copy(alpha = 0.3f),
        ok = ZeusPalette.black, okTint = ZeusPalette.einkGrey, info = ZeusPalette.black, infoTint = ZeusPalette.einkGrey,
        rose = ZeusPalette.black, roseTint = ZeusPalette.einkGrey, gradientEnabled = false
    )
    ZeusMode.DAY -> ZeusColors(
        pack = pack, mode = mode,
        canvas = ZeusPalette.alabaster, card = ZeusPalette.white, ink = ZeusPalette.charcoal, muted = ZeusPalette.mutedDay,
        line = ZeusPalette.lineDay, accent = pack.g1, accent2 = pack.g2, accentInk = pack.accentInkDay,
        onAccent = pack.onAccent, tint = pack.tintDay, highlight = pack.highlightDay,
        glow = pack.g1.copy(alpha = 0.35f), shadow = ZeusPalette.shadow.copy(alpha = 0.10f), scrim = ZeusPalette.charcoal.copy(alpha = 0.35f),
        ok = ZeusPalette.okDay, okTint = ZeusPalette.okDay.copy(alpha = 0.12f), info = ZeusPalette.infoDay,
        infoTint = ZeusPalette.infoDay.copy(alpha = 0.12f), rose = ZeusPalette.roseDay, roseTint = ZeusPalette.roseDay.copy(alpha = 0.12f),
        gradientEnabled = true
    )
    ZeusMode.NIGHT, ZeusMode.OLED -> {
        val oled = mode == ZeusMode.OLED
        val tintBase = if (pack == ZeusPack.IRIS) pack.g2 else pack.g1
        ZeusColors(
            pack = pack, mode = mode,
            canvas = if (oled) ZeusPalette.black else ZeusPalette.obsidian,
            card = if (oled) ZeusPalette.oledCard else ZeusPalette.charcoal,
            ink = ZeusPalette.inkNight, muted = ZeusPalette.mutedNight, line = ZeusPalette.lineNight,
            accent = pack.g1, accent2 = pack.g2, accentInk = pack.accentInkNight, onAccent = pack.onAccent,
            tint = tintBase.copy(alpha = 0.14f), highlight = pack.g2.copy(alpha = 0.24f),
            glow = pack.g1.copy(alpha = 0.40f), shadow = ZeusPalette.shadow.copy(alpha = 0.45f),
            scrim = ZeusPalette.black.copy(alpha = 0.55f),
            ok = ZeusPalette.okNight, okTint = ZeusPalette.okNight.copy(alpha = 0.14f), info = ZeusPalette.infoNight,
            infoTint = ZeusPalette.infoNight.copy(alpha = 0.14f), rose = ZeusPalette.roseNight,
            roseTint = ZeusPalette.roseNight.copy(alpha = 0.14f), gradientEnabled = true
        )
    }
}

fun zeusColorScheme(c: ZeusColors): ColorScheme = if (c.isDark) {
    darkColorScheme(
        primary = c.accentInk, onPrimary = c.canvas, primaryContainer = c.tint, onPrimaryContainer = c.ink,
        secondary = c.accentInk, onSecondary = c.canvas, secondaryContainer = c.tint, onSecondaryContainer = c.ink,
        tertiary = c.accent2, onTertiary = c.onAccent,
        background = c.canvas, onBackground = c.ink, surface = c.card, onSurface = c.ink,
        surfaceVariant = c.card, onSurfaceVariant = c.muted,
        surfaceContainerLowest = c.canvas, surfaceContainerLow = c.card, surfaceContainer = c.card,
        surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
        outline = c.line, outlineVariant = c.line, error = c.rose, onError = c.canvas
    )
} else {
    lightColorScheme(
        primary = c.accentInk, onPrimary = ZeusPalette.white, primaryContainer = c.tint, onPrimaryContainer = c.ink,
        secondary = c.accentInk, onSecondary = ZeusPalette.white, secondaryContainer = c.tint, onSecondaryContainer = c.ink,
        tertiary = c.accent2, onTertiary = c.onAccent,
        background = c.canvas, onBackground = c.ink, surface = c.card, onSurface = c.ink,
        surfaceVariant = c.card, onSurfaceVariant = c.muted,
        surfaceContainerLowest = c.canvas, surfaceContainerLow = c.card, surfaceContainer = c.card,
        surfaceContainerHigh = c.card, surfaceContainerHighest = c.card,
        outline = c.line, outlineVariant = c.line, error = c.rose, onError = ZeusPalette.white
    )
}
