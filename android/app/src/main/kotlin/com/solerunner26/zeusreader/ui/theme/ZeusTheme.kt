package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalZeusColors = staticCompositionLocalOf { zeusColors(ZeusPack.DEFAULT, ZeusMode.DAY) }
val LocalZeusReduceMotion = staticCompositionLocalOf { false }
val LocalZeusCelebrations = staticCompositionLocalOf { true }

object ZeusTheme {
    val colors: ZeusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusColors.current

    val type: ZeusType get() = ZeusType
    val shapes: ZeusShapes get() = ZeusShapes
    val spacing: ZeusSpacing get() = ZeusSpacing
    val sizes: ZeusSizes get() = ZeusSizes
    val motion: ZeusMotion get() = ZeusMotion

    /** True under system Reduce Motion, battery saver, E-ink or Settings → Motion off. */
    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusReduceMotion.current

    /** Confetti / mascot celebrations (Settings → Celebrations). */
    val celebrations: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalZeusCelebrations.current && !LocalZeusReduceMotion.current
}

/** The Sunset theme wrapper. */
@Composable
fun ZeusTheme(
    pack: ZeusPack = ZeusPack.DEFAULT,
    mode: ZeusMode = ZeusMode.DAY,
    reduceMotion: Boolean = false,
    celebrations: Boolean = true,
    hueOverride: Float? = null,
    content: @Composable () -> Unit
) {
    val colors = zeusColors(pack, mode)
    val legacyPalette = buildZeusHuePalette(hueOverride ?: pack.hue, dark = mode.isDark)
    CompositionLocalProvider(
        LocalZeusColors provides colors,
        LocalZeusReduceMotion provides (reduceMotion || mode.isEink),
        LocalZeusCelebrations provides celebrations,
        LocalZeusPalette provides legacyPalette,
        LocalContentColor provides colors.ink
    ) {
        MaterialTheme(
            colorScheme = zeusColorScheme(colors),
            typography = ZeusType.materialTypography,
            content = content
        )
    }
}
