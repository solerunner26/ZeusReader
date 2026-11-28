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
    ZeusSemanticColors(sky = ZeusSky.STORM, paper = ZeusPaper.CLOUD)
}
val LocalZeusReduceMotion = staticCompositionLocalOf { false }

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

    val motion: ZeusMotion
        get() = ZeusMotion

    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusReduceMotion.current
}

/**
 * Single Stormlight v4 theme wrapper (DESIGN.md §3–5).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZeusTheme(
    sky: ZeusSky = ZeusSky.STORM,
    paper: ZeusPaper = ZeusPaper.CLOUD,
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    hueOverride: Float? = null,
    content: @Composable () -> Unit
) {
    val effectiveReduceMotion = reduceMotion || paper.isEink
    val semanticColors = ZeusSemanticColors(sky = sky, paper = paper)
    val palette = buildZeusHuePalette(hueOverride ?: HuePreset.DEFAULT_HUE, dark = true)
    val colorScheme = stormlightColorScheme(sky = sky, paper = paper)

    CompositionLocalProvider(
        LocalZeusSky provides sky,
        LocalZeusPaper provides paper,
        LocalZeusColors provides semanticColors,
        LocalZeusReduceMotion provides effectiveReduceMotion,
        LocalZeusPalette provides palette,
        LocalContentColor provides semanticColors.textOnSky
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = ZeusType.materialTypography,
            content = content
        )
    }
}
