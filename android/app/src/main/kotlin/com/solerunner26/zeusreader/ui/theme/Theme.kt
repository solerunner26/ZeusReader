package com.solerunner26.zeusreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Activity-level theme. The sky comes from [hue] (Theme Pack), [darkTheme] picks
 * Night (dark) or Daylight (light) mood, [pureBlackOled] uses the OLED paper.
 */
@Composable
fun ZeusReaderTheme(
    hue: Float = HuePreset.DEFAULT_HUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackOled: Boolean = false,
    animateHue: Boolean = true,
    content: @Composable () -> Unit
) {
    val sky = ZeusSky.fromHue(hue)
    val paper = when {
        pureBlackOled -> ZeusPaper.OLED
        darkTheme -> ZeusPaper.NIGHT
        else -> ZeusPaper.CLOUD
    }
    ZeusTheme(
        sky = sky,
        paper = paper,
        darkTheme = darkTheme,
        reduceMotion = !animateHue,
        hueOverride = hue,
        daylight = !darkTheme && !pureBlackOled,
        content = content
    )
}
