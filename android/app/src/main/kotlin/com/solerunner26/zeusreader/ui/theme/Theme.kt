package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZeusReaderTheme(
    hue: Float = HuePreset.DEFAULT_HUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackOled: Boolean = false,
    animateHue: Boolean = true,
    content: @Composable () -> Unit
) {
    val targetHue = normalizeHue(hue)
    val animatedHue by animateFloatAsState(
        targetValue = targetHue,
        animationSpec = tween(durationMillis = if (animateHue) 400 else 0),
        label = "zeus_theme_hue"
    )
    val effectiveHue = if (animateHue) animatedHue else targetHue
    val palette = buildZeusHuePalette(effectiveHue, darkTheme)
    val baseScheme = HueTheme(effectiveHue, darkTheme)
    val colorScheme = if (darkTheme && pureBlackOled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalZeusPalette provides palette) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = ZeusTypography,
            content = content
        )
    }
}
