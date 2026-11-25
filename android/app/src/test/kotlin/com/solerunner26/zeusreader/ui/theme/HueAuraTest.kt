package com.solerunner26.zeusreader.ui.theme

import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HueAuraTest {

    @Test
    fun `aura and motion are disabled under Reduce Motion, Battery Saver, E-ink, or Master animations off`() {
        val base = AppUiConfig(
            auraIntensity = 0.4f,
            aestheticElements = true,
            masterAnimations = true,
            readerSurface = ReaderSurface.PAPER
        )
        assertTrue(shouldEnableHueAura(base, reduceMotion = false, batterySaver = false))
        assertFalse(shouldEnableHueAura(base, reduceMotion = true, batterySaver = false))
        assertFalse(shouldEnableHueAura(base, reduceMotion = false, batterySaver = true))
        assertFalse(shouldEnableHueAura(base.copy(readerSurface = ReaderSurface.EINK), reduceMotion = false, batterySaver = false))
        assertFalse(shouldEnableHueAura(base.copy(masterAnimations = false), reduceMotion = false, batterySaver = false))
        assertFalse(shouldEnableHueAura(base.copy(aestheticElements = false), reduceMotion = false, batterySaver = false))
        assertFalse(shouldEnableHueAura(base.copy(auraIntensity = 0f), reduceMotion = false, batterySaver = false))
    }
}
