package com.solerunner26.zeusreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Activity-level theme. [hue] selects the theme pack, [darkTheme] picks Night or Day,
 * [pureBlackOled] uses true black.
 */
@Composable
fun ZeusReaderTheme(
    hue: Float = HuePreset.DEFAULT_HUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackOled: Boolean = false,
    animateHue: Boolean = true,
    content: @Composable () -> Unit
) {
    val mode = when {
        pureBlackOled -> ZeusMode.OLED
        darkTheme -> ZeusMode.NIGHT
        else -> ZeusMode.DAY
    }
    ZeusTheme(pack = ZeusPack.fromHue(hue), mode = mode, reduceMotion = !animateHue, hueOverride = hue, content = content)
}
