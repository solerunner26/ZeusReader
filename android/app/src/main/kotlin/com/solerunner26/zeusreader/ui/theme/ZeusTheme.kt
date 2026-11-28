package com.solerunner26.zeusreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalZeusSky = staticCompositionLocalOf { ZeusSky.STORM }
val LocalZeusPaper = staticCompositionLocalOf { ZeusPaper.CLOUD }
val LocalZeusColors = staticCompositionLocalOf {
    zeusSemanticColors(sky = ZeusSky.STORM, paper = ZeusPaper.CLOUD, isLight = false)
}
val LocalZeusReduceMotion = staticCompositionLocalOf { false }
val LocalZeusGlass = staticCompositionLocalOf { true }
val LocalZeusNeon = staticCompositionLocalOf { true }
val LocalZeusNeonAnimated = staticCompositionLocalOf { true }
val LocalZeusShimmer = staticCompositionLocalOf { true }

object ZeusTheme {
    val sky: ZeusSky
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusSky.current

    val paper: ZeusPaper
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusPaper.current

    val colors: ZeusSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusColors.current

    val type: ZeusType
        get() = ZeusType

    val shapes: ZeusShapes
        get() = ZeusShapes

    val spacing: ZeusSpacing
        get() = ZeusSpacing

    val sizes: ZeusSizes
        get() = ZeusSizes

    val motion: ZeusMotion
        get() = ZeusMotion

    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusReduceMotion.current

    /** Glass UI toggle (Settings → Glass UI). */
    val glass: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusGlass.current

    /** Neon borders toggle (Settings → Neon borders). */
    val neon: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusNeon.current

    /** Neon borders sweep (Settings → Animated neon borders). */
    val neonAnimated: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusNeonAnimated.current && !LocalZeusReduceMotion.current

    /** Gradient glare on primary pills / headings (Settings → Animated glare). */
    val shimmer: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusShimmer.current && !LocalZeusReduceMotion.current

    val isLight: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusColors.current.isLight
}

/**
 * Single Stormlight v4 theme wrapper (DESIGN.md §3–5).
 * [daylight] switches the sky to its pale "Daylight" mood (app theme = Light).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZeusTheme(
    sky: ZeusSky = ZeusSky.STORM,
    paper: ZeusPaper = ZeusPaper.CLOUD,
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    hueOverride: Float? = null,
    daylight: Boolean = false,
    glass: Boolean = true,
    neon: Boolean = true,
    neonAnimated: Boolean = true,
    shimmer: Boolean = true,
    content: @Composable () -> Unit
) {
    val effectiveReduceMotion = reduceMotion || paper.isEink
    val semanticColors = zeusSemanticColors(sky = sky, paper = paper, isLight = daylight)
    val palette = buildZeusHuePalette(hueOverride ?: sky.hue, dark = !semanticColors.isLight)
    val colorScheme = stormlightColorScheme(sky = sky, paper = paper, isLight = semanticColors.isLight)

    CompositionLocalProvider(
        LocalZeusSky provides sky,
        LocalZeusPaper provides paper,
        LocalZeusColors provides semanticColors,
        LocalZeusReduceMotion provides effectiveReduceMotion,
        LocalZeusGlass provides glass,
        LocalZeusNeon provides (neon && !paper.isEink),
        LocalZeusNeonAnimated provides neonAnimated,
        LocalZeusShimmer provides shimmer,
        LocalZeusPalette provides palette,
        LocalContentColor provides semanticColors.textOnSky
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = if (effectiveReduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            typography = ZeusType.materialTypography,
            content = content
        )
    }
}
