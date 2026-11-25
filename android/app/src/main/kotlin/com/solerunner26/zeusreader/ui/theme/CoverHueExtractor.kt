package com.solerunner26.zeusreader.ui.theme

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

/**
 * Extracts only the dominant swatch hue angle (in degrees, [0, 360)) from a book cover bitmap.
 * Lightness and saturation are always controlled by [HueTheme].
 */
object CoverHueExtractor {
    fun extractHueOrNull(bitmap: Bitmap?): Float? {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return null
        }
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
        val swatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch ?: return null
        val hsl = swatch.hsl
        // Ignore near-achromatic covers (black/white/grey) so they don't snap arbitrarily to 0 deg
        if (hsl[1] < 0.08f) return null
        return normalizeHue(hsl[0])
    }
}
