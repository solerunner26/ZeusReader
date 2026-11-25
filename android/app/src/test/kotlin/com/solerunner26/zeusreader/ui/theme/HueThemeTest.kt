package com.solerunner26.zeusreader.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class HueThemeTest {

    @Test
    fun `HueTheme meets WCAG AA text and icon contrast for hues 0 to 345 step 15 light and dark`() {
        for (deg in 0..345 step 15) {
            val h = deg.toFloat()
            for (dark in listOf(false, true)) {
                val scheme = HueTheme(h, dark)
                val palette = buildZeusHuePalette(h, dark)

                // Text contrast >= 4.5:1
                val inkOnSurface = contrastRatio(scheme.onSurface, scheme.surface)
                val inkOnPaper = contrastRatio(scheme.onSurface, palette.paper)
                val inkSoftOnSurface = contrastRatio(scheme.onSurfaceVariant, scheme.surface)
                val onHueOnHue = contrastRatio(scheme.onPrimary, scheme.primary)
                val onContainerOnContainer = contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)

                assertTrue("h=$deg dark=$dark inkOnSurface=$inkOnSurface", inkOnSurface >= 4.5)
                assertTrue("h=$deg dark=$dark inkOnPaper=$inkOnPaper", inkOnPaper >= 4.5)
                assertTrue("h=$deg dark=$dark inkSoftOnSurface=$inkSoftOnSurface", inkSoftOnSurface >= 4.5)
                assertTrue("h=$deg dark=$dark onHueOnHue=$onHueOnHue", onHueOnHue >= 4.5)
                assertTrue("h=$deg dark=$dark onContainerOnContainer=$onContainerOnContainer", onContainerOnContainer >= 4.5)

                // Icon / interactive accent contrast >= 3.0:1
                val hueIconContrast = contrastRatio(scheme.primary, scheme.surface)
                assertTrue("h=$deg dark=$dark hueIconContrast=$hueIconContrast", hueIconContrast >= 3.0)
            }
        }
    }
}
