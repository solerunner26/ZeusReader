package com.solerunner26.zeusreader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** Sunset design: contrast for all five theme packs in Day, Night, OLED and E-ink. */
class SunsetContrastTest {

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = fg.luminance().toDouble()
        val l2 = bg.luminance().toDouble()
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    @Test
    fun `ink and muted text meet WCAG on canvas and cards in every mode`() {
        ZeusPack.entries.forEach { pack ->
            ZeusMode.entries.forEach { mode ->
                val c = zeusColors(pack, mode)
                listOf(c.canvas, c.card).forEach { bg ->
                    assertTrue("${pack.label}/${mode.label} ink ${ratio(c.ink, bg)}", ratio(c.ink, bg) >= 7.0)
                    assertTrue("${pack.label}/${mode.label} muted ${ratio(c.muted, bg)}", ratio(c.muted, bg) >= 4.5)
                    assertTrue("${pack.label}/${mode.label} accent text ${ratio(c.accentInk, bg)}", ratio(c.accentInk, bg) >= 4.5)
                }
                val chipBg = c.tint.compositeOver(c.card)
                assertTrue("${pack.label}/${mode.label} accent on tint ${ratio(c.accentInk, chipBg)}", ratio(c.accentInk, chipBg) >= 4.5)
            }
        }
    }

    @Test
    fun `text on the accent gradient is readable at both ends`() {
        ZeusPack.entries.forEach { pack ->
            assertTrue("${pack.label} onAccent/g1 ${ratio(pack.onAccent, pack.g1)}", ratio(pack.onAccent, pack.g1) >= 4.5)
            assertTrue("${pack.label} onAccent/g2 ${ratio(pack.onAccent, pack.g2)}", ratio(pack.onAccent, pack.g2) >= 4.5)
        }
    }

    @Test
    fun `semantic colours stay readable`() {
        ZeusPack.entries.forEach { pack ->
            listOf(ZeusMode.DAY, ZeusMode.NIGHT).forEach { mode ->
                val c = zeusColors(pack, mode)
                listOf(c.ok, c.info, c.rose).forEach { assertTrue("${pack.label}/${mode.label} semantic", ratio(it, c.card) >= 4.5) }
            }
        }
    }

    @Test
    fun `papers keep reading contrast`() {
        ReaderSurface.entries.forEach { s ->
            val p = buildReaderSurfacePalette(s, 30f)
            assertTrue("${s.label} ${ratio(p.text, p.background)}", ratio(p.text, p.background) >= 7.0)
        }
    }

    @Test
    fun `pack hues round-trip and default is Sunset`() {
        ZeusPack.entries.forEach { assertEquals(it, ZeusPack.fromHue(it.hue)) }
        assertEquals(ZeusPack.SUNSET, ZeusPack.fromHue(HuePreset.DEFAULT_HUE))
    }
}
