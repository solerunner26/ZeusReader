package com.solerunner26.zeusreader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class StormlightContrastTest {

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = fg.luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun allSkiesMeet7to1ContrastWithTextOnSkyAndAccentButtons() {
        val textOnSky = Color(0xFFF4F1FF)
        ZeusSky.entries.forEach { sky ->
            val onBaseA = contrastRatio(textOnSky, sky.baseA)
            val onBaseB = contrastRatio(textOnSky, sky.baseB)
            val accentBtn = contrastRatio(sky.baseA, sky.accent)
            assertTrue("${sky.label} baseA contrast $onBaseA < 7.0", onBaseA >= 7.0)
            assertTrue("${sky.label} baseB contrast $onBaseB < 4.5", onBaseB >= 4.5)
            assertTrue("${sky.label} accent button contrast $accentBtn < 7.0", accentBtn >= 7.0)
        }
    }

    @Test
    fun allPapersMeet7to1ContrastWithInk() {
        ZeusPaper.entries.forEach { paper ->
            val inkRatio = contrastRatio(paper.ink, paper.bg)
            val softRatio = contrastRatio(paper.soft, paper.bg)
            assertTrue("${paper.label} ink contrast $inkRatio < 7.0", inkRatio >= 7.0)
            assertTrue("${paper.label} soft contrast $softRatio < 4.5", softRatio >= 4.5)
        }
    }
}
