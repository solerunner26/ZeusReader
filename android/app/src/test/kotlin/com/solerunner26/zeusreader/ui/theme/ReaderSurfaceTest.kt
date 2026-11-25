package com.solerunner26.zeusreader.ui.theme

import androidx.compose.ui.graphics.Color
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSurfaceTest {

    @Test
    fun `legacy theme modes map to v2 ReaderSurface correctly`() {
        assertEquals(ReaderSurface.PAPER, ReaderSurface.fromLegacyThemeMode(ReaderThemeMode.DAY))
        assertEquals(ReaderSurface.DUSK, ReaderSurface.fromLegacyThemeMode(ReaderThemeMode.SEPIA))
        assertEquals(ReaderSurface.NIGHT, ReaderSurface.fromLegacyThemeMode(ReaderThemeMode.NIGHT))
        assertEquals(ReaderSurface.OLED, ReaderSurface.fromLegacyThemeMode(ReaderThemeMode.PURE_BLACK_OLED))
        assertEquals(ReaderSurface.EINK, ReaderSurface.fromLegacyThemeMode(ReaderThemeMode.E_INK))
    }

    @Test
    fun `OLED and EINK enforce strict surface invariants`() {
        val oled = buildReaderSurfacePalette(ReaderSurface.OLED, 250f)
        assertEquals(Color.Black, oled.background)
        assertTrue(oled.isDark)
        assertFalse(oled.allowVignette)

        val eink = buildReaderSurfacePalette(ReaderSurface.EINK, 250f)
        assertEquals(Color.White, eink.background)
        assertEquals(Color.Black, eink.text)
        assertTrue(eink.underlineOnlyHighlight)
        assertFalse(eink.allowHue)
        assertFalse(eink.allowBlur)
        assertFalse(eink.allowMotion)
        assertFalse(eink.allowVignette)
    }

    @Test
    fun `all ReaderSurface palettes maintain text contrast at least 4_5 across hues`() {
        for (deg in 0..345 step 15) {
            for (surface in ReaderSurface.entries) {
                val p = buildReaderSurfacePalette(surface, deg.toFloat())
                val ratio = contrastRatio(p.text, p.background)
                assertTrue("surface=$surface h=$deg ratio=$ratio", ratio >= 4.5)
            }
        }
    }
}
