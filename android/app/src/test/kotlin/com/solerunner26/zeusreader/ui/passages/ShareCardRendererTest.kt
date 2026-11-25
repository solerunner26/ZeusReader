package com.solerunner26.zeusreader.ui.passages

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.solerunner26.zeusreader.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareCardRendererTest {

    @Test
    fun `renders exact pixel dimensions across all styles sizes formats and scripts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cover = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color(0xFF2E7D32).toArgb())
        }

        val samples = mapOf(
            "en" to "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            "hi" to "ज्ञान ही शक्ति है और अभ्यास से ही कौशल निखरता है।",
            "gu" to "જ્ઞાન એ જ સાચી શક્તિ છે અને નિરંતર અભ્યાસથી સિદ્ધિ મળે છે."
        )

        for ((lang, text) in samples) {
            for (style in CardStyle.entries) {
                for (size in CardSize.entries) {
                    val result = renderShareCardBitmap(
                        context = context,
                        passageText = text,
                        bookTitle = "Nicomachean Ethics",
                        author = "Aristotle",
                        language = lang,
                        hueAngle = 250f,
                        style = style,
                        size = size,
                        coverBitmap = cover
                    )
                    assertEquals(size.widthPx, result.bitmap.width)
                    assertEquals(size.heightPx, result.bitmap.height)

                    for (format in CardFormat.entries) {
                        val bytes = encodeCardBytes(result.bitmap, format)
                        assertTrue("Empty bytes for $style $size $format", bytes.isNotEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun `Cover blur maintains at least 4_5 contrast on pure white and pure black covers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pureWhite = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        val pureBlack = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLACK)
        }

        for (cover in listOf(pureWhite, pureBlack)) {
            val res = renderShareCardBitmap(
                context = context,
                passageText = "Contrast check on extreme cover.",
                bookTitle = "Test Book",
                author = "Author",
                language = "en",
                hueAngle = 250f,
                style = CardStyle.COVER_BLUR,
                size = CardSize.SQUARE_1_1,
                coverBitmap = cover
            )
            val ratio = contrastRatio(res.effectiveTextColor, res.effectiveBackdropSample)
            assertTrue("Contrast $ratio < 4.5", ratio >= 4.5)
        }
    }
}
