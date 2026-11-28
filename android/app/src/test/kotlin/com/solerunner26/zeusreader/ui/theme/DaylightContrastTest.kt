package com.solerunner26.zeusreader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** Contrast for the five Theme Packs in both moods (Night + Daylight) and E-ink. */
class DaylightContrastTest {

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = fg.luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    @Test
    fun `every pack meets 7 to 1 body and 4_5 to 1 accent contrast in both moods`() {
        ZeusSky.entries.forEach { sky ->
            listOf(false, true).forEach { light ->
                val c = zeusSemanticColors(sky, if (light) ZeusPaper.CLOUD else ZeusPaper.NIGHT, light)
                listOf(c.skyTop, c.skyBottom).forEach { bg ->
                    val body = ratio(c.textOnSky, bg)
                    val secondary = ratio(c.textOnSkySecondary.compositeOver(bg), bg)
                    val accent = ratio(c.accentOnSky, bg)
                    assertTrue("${sky.packName} light=$light body $body", body >= 7.0)
                    assertTrue("${sky.packName} light=$light secondary $secondary", secondary >= 4.5)
                    assertTrue("${sky.packName} light=$light accent $accent", accent >= 4.5)
                }
                val onAccent = ratio(c.textOnAccent, sky.accent)
                assertTrue("${sky.packName} text on accent pill $onAccent", onAccent >= 7.0)
                val chip = ratio(c.chipSelectedFg, c.chipSelectedBg)
                assertTrue("${sky.packName} light=$light selected chip $chip", chip >= 7.0)
            }
        }
    }

    @Test
    fun `eink chrome is pure black on white`() {
        val c = zeusSemanticColors(ZeusSky.STORM, ZeusPaper.EINK, isLight = false)
        assertTrue(ratio(c.textOnSky, c.skyTop) >= 20.0)
        assertTrue(c.isLight)
    }

    @Test
    fun `theme pack hues round-trip to their own sky`() {
        ZeusSky.entries.forEach { sky -> assertTrue(sky.packName, ZeusSky.fromHue(sky.hue) == sky) }
    }
}
